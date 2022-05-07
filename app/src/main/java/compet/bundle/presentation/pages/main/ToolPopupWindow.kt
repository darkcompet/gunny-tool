package compet.bundle.presentation.pages.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.Build
import android.os.Debug
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import compet.bundle.R

@SuppressLint("InflateParams")
class ToolPopupWindow(
	private val context: Context,
	private val callback: Callback
) {
	// Listener
	interface Callback {
		fun onClickTakeScreenshot()
		fun onClickClosePopup()
	}

	companion object {
		const val REQUEST_EXTERNAL_STORAGE = 1
	}

	private val layout: View
	private lateinit var layoutParams: WindowManager.LayoutParams
	private val windowManager: WindowManager

	init {
		val context = this.context
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val metrics: DisplayMetrics = Resources.getSystem().displayMetrics
			// Set the layout parameters of the window
			this.layoutParams = WindowManager.LayoutParams(
				// Size on the screen
				WindowManager.LayoutParams.MATCH_PARENT,
				metrics.heightPixels / 4,
				// Don't let it grab the input focus
				WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
				// Make the underlying application window visible
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				// Through any transparent parts
				PixelFormat.TRANSLUCENT
			)
		}

		// Inflating the view with the custom layout we created.
		// Set onClickListener on the remove button, which removes the view from the window.
//		this.layout = View.inflate(context, R.layout.tool_popup_window, null) as ToolPopupLayout
		val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		this.layout = layoutInflater.inflate(R.layout.tool_popup_window, null) as ToolLayout
		val layout = this.layout as ToolLayout
//		layout.findViewById<View>(R.id.btnTakeScreenshot).setOnClickListener { this.callback.onClickTakeScreenshot() }
//		layout.findViewById<View>(R.id.btnClosePopup).setOnClickListener { this.callback.onClickClosePopup() }

		// Define the position of the window within the screen
		this.layoutParams.gravity = Gravity.TOP or Gravity.END
		this.windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
	}

	fun open() {
		try {
			// Check if the view is already inflated or present in the window
			if (this.layout.windowToken == null && this.layout.parent == null) {
				this.windowManager.addView(this.layout, this.layoutParams)
			}
		}
		catch (e: Exception) {
			Log.d("dkerror", e.toString())
		}
	}

	fun close() {
		try {
			// Remove the view from the window
			(this.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(layout)
			// Invalidate the view
			this.layout.invalidate()
			// Remove all views
			(this.layout.parent as ViewGroup).removeAllViews()

			// The above steps are necessary when you are adding and removing
			// the view simultaneously, it might give some exceptions
		}
		catch (e: Exception) {
			Log.d("dkerror", e.toString())
		}
	}

	private val permissionstorage =
		arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

	// Verifying if storage permission is given or not
	fun verifystoragepermissions(activity: Activity?) {
		val permissions = ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)

		// If storage permission is not given then request for External Storage Permission
		if (permissions != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(activity, permissionstorage, REQUEST_EXTERNAL_STORAGE)
		}
	}
}
