package compet.bundle.presentation.pages.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.Display
import android.view.OrientationEventListener
import android.view.WindowManager
import androidx.core.graphics.get
import androidx.core.util.Pair
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ScreenCaptureService : Service() {
	private var mMediaProjection: MediaProjection? = null
	private var mStoreDirPath: String? = null
	private var mImageReader: ImageReader? = null
	private var mHandler: Handler? = null
	private var mDisplay: Display? = null
	private var mVirtualDisplay: VirtualDisplay? = null
	private var mDensity = 0
	private var mWidth = 0
	private var mHeight = 0
	private var mRotation = 0
	private var mOrientationChangeCallback: OrientationChangeCallback? = null

	companion object {
		private const val TAG = "ScreenCaptureService"
		private const val RESULT_CODE = "RESULT_CODE"
		private const val DATA = "DATA"
		private const val ACTION = "ACTION"
		private const val START = "START"
		private const val STOP = "STOP"
		private const val SCREENCAP_NAME = "screencap"
		private var capturedImageCount = 0

		fun getStartIntent(context: Context?, resultCode: Int, data: Intent?): Intent {
			return Intent(context, ScreenCaptureService::class.java).also {
				it.putExtra(ACTION, START)
				it.putExtra(RESULT_CODE, resultCode)
				it.putExtra(DATA, data)
			}
		}

		fun getStopIntent(context: Context?): Intent {
			return Intent(context, ScreenCaptureService::class.java).also {
				it.putExtra(ACTION, STOP)
			}
		}

		private fun isStartCommand(intent: Intent): Boolean {
			return intent.hasExtra(RESULT_CODE)
				&& intent.hasExtra(DATA)
				&& intent.hasExtra(ACTION)
				&& intent.getStringExtra(ACTION) == START
		}

		private fun isStopCommand(intent: Intent): Boolean {
			return intent.hasExtra(ACTION) && intent.getStringExtra(ACTION) == STOP
		}

		private val virtualDisplayFlags: Int
			get() = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
	}

	override fun onBind(intent: Intent): IBinder? {
		return null
	}

	override fun onCreate() {
		super.onCreate()

		// Create store dir
		val externalFilesDir = getExternalFilesDir(null)
		if (externalFilesDir != null) {
			this.mStoreDirPath = (externalFilesDir.absolutePath + "/screenshots/").also { storeDirPath ->
				val storeDir = File(storeDirPath)
				if (!storeDir.exists() && !storeDir.mkdirs()) {
					Log.e(TAG, "Failed to create file storage directory.")
					stopSelf()
				}
			}
		}
		else {
			Log.e(TAG, "Failed to create file storage directory, getExternalFilesDir is null.")
			stopSelf()
		}

		// Start capture handling thread
//		object : Thread() {
//			override fun run() {
//				Looper.prepare()
//				mHandler = Handler(Looper.myLooper()!!)
//				Looper.loop()
//			}
//		}.start()
	}

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		when {
			isStartCommand(intent) -> {
				// Create notification
				val notification: Pair<Int, Notification> = NotificationUtils.getNotification(this)
				startForeground(notification.first, notification.second)

				// Start projection
				val resultCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)
				val data = intent.getParcelableExtra<Intent>(DATA)

				startProjection(resultCode, data)
			}
			isStopCommand(intent) -> {
				stopProjection()
				stopSelf()
			}
			else -> {
				stopSelf()
			}
		}
		return START_NOT_STICKY
	}

	private fun startProjection(resultCode: Int, data: Intent?) {
		val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
		if (mMediaProjection == null) {
			mMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)

			if (mMediaProjection != null) {
				// Display metrics
				val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
				mDensity = Resources.getSystem().displayMetrics.densityDpi
				mDisplay = windowManager.defaultDisplay

				// Create virtual display depending on device width / height
				createVirtualDisplay()

				// Register orientation change callback
				mOrientationChangeCallback = OrientationChangeCallback(this).also { callback ->
					if (callback.canDetectOrientation()) {
						callback.enable()
					}
				}

				// Register media projection stop callback
				mMediaProjection!!.registerCallback(MediaProjectionStopCallback(), mHandler)
			}
		}
	}

	private fun stopProjection() {
		mHandler?.post {
			mMediaProjection?.stop()
		}
	}

	@SuppressLint("WrongConstant")
	private fun createVirtualDisplay() {
		// Get width and height
		Resources.getSystem().displayMetrics.also { metrics ->
			mWidth = metrics.widthPixels
			mHeight = metrics.heightPixels
		}

		// Start capture reader
		mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2)
		mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
			SCREENCAP_NAME, mWidth, mHeight,
			mDensity, virtualDisplayFlags, mImageReader!!.surface, null, mHandler
		)
		mImageReader!!.setOnImageAvailableListener(ImageAvailableListener(), mHandler)
	}

	private inner class ImageAvailableListener : OnImageAvailableListener {
		override fun onImageAvailable(reader: ImageReader) {
			var fos: FileOutputStream? = null
			var bitmap: Bitmap? = null
			try {
				mImageReader!!.acquireLatestImage().use { image ->
					if (image != null) {
						val planes = image.planes
						val buffer = planes[0].buffer
						val pixelStride = planes[0].pixelStride
						val rowStride = planes[0].rowStride
						val rowPadding = rowStride - pixelStride * mWidth

						// Create bitmap
						bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888)
						bitmap!!.copyPixelsFromBuffer(buffer)

						// Write bitmap to a file
						fos = FileOutputStream("$mStoreDirPath/myscreen_$capturedImageCount.png")
						bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, fos)
						capturedImageCount++
						Log.e(TAG, "Captured image: $capturedImageCount")

						// Detect gunny
						val bm = bitmap!!
						for (x in 0 until bm.width) {
							for (y in 0 until bm.height) {
								val color = bm[x, y]
//								println("bm[$x][$y] = $color")
							}
						}
					}
				}
			}
			catch (e: Exception) {
				e.printStackTrace()
			}
			finally {
				if (fos != null) {
					try {
						fos!!.close()
					}
					catch (ioe: IOException) {
						ioe.printStackTrace()
					}
				}

				bitmap?.recycle()
			}
		}
	}

	private inner class OrientationChangeCallback constructor(context: Context?) : OrientationEventListener(context) {
		override fun onOrientationChanged(orientation: Int) {
			val rotation = mDisplay!!.rotation
			if (rotation != mRotation) {
				mRotation = rotation
				try {
					// Clean up
					mVirtualDisplay?.release()
					mImageReader?.setOnImageAvailableListener(null, null)

					// Re-create virtual display depending on device width / height
					createVirtualDisplay()
				}
				catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
	}

	private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
		override fun onStop() {
			Log.e(TAG, "stopping projection.")
			mHandler!!.post {
				mVirtualDisplay?.release()
				mImageReader?.setOnImageAvailableListener(null, null)
				mOrientationChangeCallback?.disable()
				mMediaProjection!!.unregisterCallback(this@MediaProjectionStopCallback)
			}
		}
	}
}
