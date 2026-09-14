package com.nuvio.app.features.quickwatch

import co.touchlab.kermit.Logger
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.tmdb.TmdbHomeCatalogResolver
import com.nuvio.app.features.tmdb.TmdbMetadataService
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object QuickWatchFeedRepository {
    private val log = Logger.withTag("QuickWatchFeedRepo")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _feed = MutableStateFlow<List<QuickWatchItem>>(emptyList())
    val feed: StateFlow<List<QuickWatchItem>> = _feed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _likedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val likedItemIds: StateFlow<Set<String>> = _likedItemIds.asStateFlow()

    private var currentPage = 1
    private var isFetching = false
    private val seenVideoIds = mutableSetOf<String>()
    private val seenTmdbIds = mutableSetOf<Int>()

    fun ensureLoaded() {
        if (_feed.value.isEmpty() && !_isLoading.value) {
            loadFeed(page = 1, reset = true)
        }
    }

    fun refresh() {
        loadFeed(page = 1, reset = true)
    }

    fun loadMore() {
        if (!isFetching && _feed.value.isNotEmpty()) {
            loadFeed(page = currentPage + 1, reset = false)
        }
    }

    fun toggleLike(videoId: String) {
        val current = _likedItemIds.value.toMutableSet()
        val isNowLiked = if (current.contains(videoId)) {
            current.remove(videoId)
            false
        } else {
            current.add(videoId)
            true
        }
        _likedItemIds.value = current

        // Optimistically update feed like count and status
        _feed.value = _feed.value.map { item ->
            if (item.youtubeVideoId == videoId) {
                val newCount = if (isNowLiked) item.likeCount + 1 else (item.likeCount - 1).coerceAtLeast(0)
                item.copy(isLiked = isNowLiked, likeCount = newCount)
            } else {
                item
            }
        }
    }

    private fun loadFeed(page: Int, reset: Boolean) {
        if (isFetching) return
        isFetching = true
        _isLoading.value = true

        scope.launch {
            try {
                val newItems = fetchFeedItems(page)
                withContext(Dispatchers.Main) {
                    if (reset) {
                        seenVideoIds.clear()
                        seenTmdbIds.clear()
                        seenVideoIds.addAll(newItems.map { it.youtubeVideoId })
                        seenTmdbIds.addAll(newItems.map { it.tmdbId })
                        _feed.value = newItems
                        currentPage = 1
                    } else {
                        val filtered = newItems.filter { seenVideoIds.add(it.youtubeVideoId) && seenTmdbIds.add(it.tmdbId) }
                        _feed.value = _feed.value + filtered
                        currentPage = page
                    }
                }
            } catch (e: Throwable) {
                log.w(e) { "Failed to load Quick Watch feed for page $page" }
            } finally {
                isFetching = false
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchFeedItems(page: Int): List<QuickWatchItem> = withContext(Dispatchers.Default) {
        // Fetch catalogs of popular/trending movies and series
        val movieCatalog = TmdbHomeCatalogResolver.fetchCatalog(
            endpoint = "trending/movie/day",
            mediaType = "movie",
            page = page,
        )
        val seriesCatalog = TmdbHomeCatalogResolver.fetchCatalog(
            endpoint = "trending/tv/day",
            mediaType = "series",
            page = page,
        )
        val bollywoodCatalog = if (page == 1) {
            TmdbHomeCatalogResolver.fetchCatalog(
                endpoint = "discover/movie",
                queryParams = mapOf("with_original_language" to "hi", "sort_by" to "popularity.desc"),
                mediaType = "movie",
                page = 1,
            )
        } else null

        val allPreviews = mutableListOf<MetaPreview>()
        val maxLen = maxOf(movieCatalog.items.size, seriesCatalog.items.size)
        for (i in 0 until maxLen) {
            movieCatalog.items.getOrNull(i)?.let { allPreviews.add(it) }
            seriesCatalog.items.getOrNull(i)?.let { allPreviews.add(it) }
            bollywoodCatalog?.items?.getOrNull(i)?.let { allPreviews.add(it) }
        }

        val todayIso = CurrentDateProvider.todayIsoDate()
        val items = mutableListOf<QuickWatchItem>()
        val localSeenTmdb = mutableSetOf<Int>()

        for (preview in allPreviews.distinctBy { it.id }) {
            val tmdbId = preview.id.removePrefix("tmdb:").toIntOrNull() ?: continue
            if (seenTmdbIds.contains(tmdbId) || localSeenTmdb.contains(tmdbId)) continue

            val mediaType = if (preview.type == "series" || preview.type == "tv") "tv" else "movie"

            val trailers = runCatching {
                TmdbMetadataService.fetchTrailers(
                    tmdbId = tmdbId,
                    mediaType = mediaType,
                    language = "en",
                )
            }.getOrDefault(emptyList())

            val validVideos = trailers.filter { it.site.equals("YouTube", ignoreCase = true) && it.key.isNotBlank() }
            if (validVideos.isEmpty()) continue

            // Pick the single best official trailer or teaser for this movie (never duplicate the same movie)
            val bestVideo = validVideos.firstOrNull { it.type.equals("Trailer", ignoreCase = true) }
                ?: validVideos.firstOrNull { it.type.equals("Teaser", ignoreCase = true) }
                ?: validVideos.first()

            if (seenVideoIds.contains(bestVideo.key)) continue

            val releaseYear = preview.releaseInfo?.take(4)
            val isWatchable = preview.releaseInfo?.let { it <= todayIso } ?: true

            val item = QuickWatchItem(
                id = "${bestVideo.key}_$tmdbId",
                youtubeVideoId = bestVideo.key,
                youtubeUrl = "https://www.youtube.com/watch?v=${bestVideo.key}",
                title = bestVideo.name.ifBlank { preview.name },
                videoType = bestVideo.type.ifBlank { "Trailer" },
                movieTitle = preview.name,
                movieOverview = preview.description.orEmpty(),
                moviePoster = preview.poster,
                movieBackdrop = preview.banner ?: preview.poster,
                movieLogo = preview.logo,
                releaseDate = preview.releaseInfo,
                releaseYear = releaseYear,
                tmdbId = tmdbId,
                mediaType = preview.type,
                genres = preview.genres,
                voteAverage = preview.imdbRating?.toDoubleOrNull(),
                isWatchable = isWatchable,
                likeCount = (120..4500).random(), // Initial display counter
                commentCount = (8..340).random(),
                isLiked = _likedItemIds.value.contains(bestVideo.key),
            )
            items.add(item)
            localSeenTmdb.add(tmdbId)
        }

        items
    }
}
