package com.nuvio.app.features.quickwatch

import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.library.LibraryItem
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class QuickWatchItem(
    val id: String,
    val youtubeVideoId: String,
    val youtubeUrl: String,
    val title: String,
    val videoType: String,
    val movieTitle: String,
    val movieOverview: String,
    val moviePoster: String? = null,
    val movieBackdrop: String? = null,
    val movieLogo: String? = null,
    val releaseDate: String? = null,
    val releaseYear: String? = null,
    val tmdbId: Int,
    val imdbId: String? = null,
    val mediaType: String = "movie",
    val genres: List<String> = emptyList(),
    val voteAverage: Double? = null,
    val isWatchable: Boolean = true,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
) {
    val metaId: String
        get() = "tmdb:$tmdbId"
}

fun QuickWatchItem.toMetaPreview(): MetaPreview = MetaPreview(
    id = metaId,
    type = mediaType,
    name = movieTitle,
    poster = moviePoster,
    banner = movieBackdrop,
    logo = movieLogo,
    description = movieOverview,
    releaseInfo = releaseYear ?: releaseDate,
    imdbRating = voteAverage?.let { (it * 10.0).roundToInt() / 10.0 }?.toString(),
    genres = genres,
)

fun QuickWatchItem.toLibraryItem(savedAtEpochMs: Long = 0L): LibraryItem = LibraryItem(
    id = metaId,
    type = mediaType,
    name = movieTitle,
    poster = moviePoster,
    banner = movieBackdrop,
    logo = movieLogo,
    description = movieOverview,
    releaseInfo = releaseYear ?: releaseDate,
    imdbRating = voteAverage?.let { (it * 10.0).roundToInt() / 10.0 }?.toString(),
    genres = genres,
    tmdbId = tmdbId,
    imdbId = imdbId,
    savedAtEpochMs = savedAtEpochMs,
)

@Serializable
data class QuickWatchComment(
    val id: String,
    val videoId: String,
    val userName: String,
    val userAvatar: String? = null,
    val text: String,
    val timeAgo: String = "Just now",
)

data class QuickWatchPlaybackState(
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
)
