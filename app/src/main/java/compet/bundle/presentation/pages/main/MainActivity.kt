package compet.bundle.presentation.pages.main

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import compet.bundle.R

class MainActivity : AppCompatActivity(), ToolPopupWindow.Callback {
	companion object {
		private const val RC_TAKE_SCREENSHOT = 999
	}

	private lateinit var toolPopupWindow: ToolPopupWindow

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		if (hasOverlayPermission()) {
			this.toolPopupWindow = ToolPopupWindow(this, this)
			this.toolPopupWindow.open()
		}
		else {
			requestOverlayPermission()
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		when (requestCode) {
			RC_TAKE_SCREENSHOT -> {
				if (resultCode == RESULT_OK) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						startForegroundService(ScreenCaptureService.getStartIntent(this, resultCode, data))
					}
					else {
						startService(ScreenCaptureService.getStartIntent(this, resultCode, data))
					}

					// We close this activity since it appears at this time.
					finish()
				}
			}
		}
	}

	private fun stopMediaProjection() {
//		startService(com.mtsahakis.mediaprojectiondemo.ScreenCaptureService.getStopIntent(this))
	}

	private fun hasOverlayPermission(): Boolean {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return Settings.canDrawOverlays(this)
		}
		return true
	}

	/// Ask user to grant the Overlay permission
	private fun requestOverlayPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!Settings.canDrawOverlays(this)) {
				// Bring user to the device settings
				val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
				startActivity(myIntent)
			}
		}
	}

	private fun startGunnyToolOverlayService() {
		if (hasOverlayPermission()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				startForegroundService(Intent(this, ToolService::class.java))
			}
			else {
				startService(Intent(this, ToolService::class.java))
			}
		}
	}

	// Check for permission again when user grants it from
	// the device settings, and start the service.
	override fun onResume() {
		super.onResume()

//		startPopupOverlayService()
	}

	override fun onClickTakeScreenshot() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//			this.startForegroundService(ScreenCaptureService.getStartIntent(this))
			this.startMediaProjection()
		}
		else {
			this.startService(Intent(this, ScreenCaptureService::class.java))
		}
	}

	private fun startMediaProjection() {
		val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
		startActivityForResult(projectionManager.createScreenCaptureIntent(), RC_TAKE_SCREENSHOT)
	}

	override fun onClickClosePopup() {
		this.toolPopupWindow.close()
	}
}
