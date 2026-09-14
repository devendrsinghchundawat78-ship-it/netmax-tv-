package com.nuvio.app.features.downloads

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

internal const val DOWNLOAD_SERVICE_TAG = "DownloadForeground"
internal const val DOWNLOAD_SERVICE_NOTIFICATION_ID = 0x444E
private const val ACTION_START_DOWNLOAD_SERVICE = "com.nuvio.app.downloads.START_SERVICE"
private const val ACTION_STOP_DOWNLOAD_SERVICE = "com.nuvio.app.downloads.STOP_SERVICE"

class DownloadForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_DOWNLOAD_SERVICE) {
            stopForegroundService()
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (action != ACTION_START_DOWNLOAD_SERVICE) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val notification = DownloadServiceState.notification
        val notifId = DownloadServiceState.notificationId
        if (notification == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val started = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    notifId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(notifId, notification)
            }
        }.onFailure { error ->
            Log.w(DOWNLOAD_SERVICE_TAG, "Failed to promote download foreground service", error)
        }.isSuccess

        if (!started) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        acquireWakeLock()
        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        stopForegroundService()
        DownloadServiceState.isRunning.set(false)
        super.onDestroy()
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        runCatching {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "NetMax:DownloadWakeLock",
            )?.apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L) // 4 hours safety timeout
            }
        }.onFailure { error ->
            Log.w(DOWNLOAD_SERVICE_TAG, "Failed to acquire download wake lock", error)
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }.onFailure { error ->
            Log.w(DOWNLOAD_SERVICE_TAG, "Failed to release download wake lock", error)
        }
        wakeLock = null
    }

    companion object {
        fun start(context: Context, notification: Notification, notificationId: Int = DOWNLOAD_SERVICE_NOTIFICATION_ID) {
            val appContext = context.applicationContext
            DownloadServiceState.notification = notification
            DownloadServiceState.notificationId = notificationId

            if (!DownloadServiceState.isRunning.getAndSet(true)) {
                val intent = Intent(appContext, DownloadForegroundService::class.java).apply {
                    action = ACTION_START_DOWNLOAD_SERVICE
                }
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appContext.startForegroundService(intent)
                    } else {
                        appContext.startService(intent)
                    }
                }.onFailure { error ->
                    Log.w(DOWNLOAD_SERVICE_TAG, "Unable to start DownloadForegroundService", error)
                    DownloadServiceState.isRunning.set(false)
                }
            } else {
                // Already running, update notification
                runCatching {
                    val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    manager?.notify(notificationId, notification)
                }
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            if (DownloadServiceState.isRunning.getAndSet(false)) {
                DownloadServiceState.notification = null
                val intent = Intent(appContext, DownloadForegroundService::class.java).apply {
                    action = ACTION_STOP_DOWNLOAD_SERVICE
                }
                runCatching {
                    appContext.startService(intent)
                }.onFailure {
                    appContext.stopService(Intent(appContext, DownloadForegroundService::class.java))
                }
            }
        }
    }
}

private object DownloadServiceState {
    val isRunning = AtomicBoolean(false)
    @Volatile
    var notification: Notification? = null
    @Volatile
    var notificationId: Int = DOWNLOAD_SERVICE_NOTIFICATION_ID
}
