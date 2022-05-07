package compet.bundle.presentation.pages.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import compet.bundle.R

class ToolService : Service() {
	override fun onBind(intent: Intent): IBinder? {
		throw UnsupportedOperationException("Not yet implemented")
	}

	override fun onCreate() {
		super.onCreate()

//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//			startViaNotinication()
//		}
//		else {
//			startForeground(ID_FOREGROUND, Notification())
//		}

		// Create an instance of Window class
		// and display the content on screen
//		val window = GunnyToolPopupWindow(this, this)
//		window.open()
	}

//	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//		return super.onStartCommand(intent, flags, startId)
//	}

	private val ID_FOREGROUND = 1
	private val ID_NOTIFICATION = 2
	private val NOTIFICATION_CHANNEL_ID = "compet.bundle"

	// for android version >=O we need to create
	// custom notification stating
	// foreground service is running
	@RequiresApi(Build.VERSION_CODES.O)
	private fun startViaNotinication() {
		val channelName = "Background Service"
		val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_MIN)
		val notificationManager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)

		notificationManager.createNotificationChannel(channel)

		val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
		val notification = notificationBuilder.setOngoing(true)
			.setContentTitle("Dk~ Service is running")
			.setContentText("Displaying over other apps")
			// This is important, otherwise the notification will show the way
			// you want i.e. it will show some default notification
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setPriority(NotificationManager.IMPORTANCE_MIN)
			.setCategory(Notification.CATEGORY_SERVICE)
			.build()

		startForeground(ID_NOTIFICATION, notification)
	}
}
