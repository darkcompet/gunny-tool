package compet.bundle.presentation.pages.main

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import compet.bundle.R
import compet.bundle.common.AppPrefs
import tool.compet.core.DkLogs
import tool.compet.core.DkMaths

@SuppressLint("SetTextI18n")
class MainWindowLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

	private lateinit var mainWindow: MainWindow
	private lateinit var controlLayout: View
	private lateinit var setupLayout: View
	private lateinit var minimapView: MinimapView

	private lateinit var tvAngleIndicator: TextView
	private lateinit var angleBar: SeekBar

	private lateinit var tvForceIndicator: TextView
	private lateinit var forceBar: SeekBar

	private lateinit var tvWindIndicator: TextView
	private lateinit var windBar: SeekBar

	init {
//		// Get/Init data
//		if (!AppPrefs.commonPref.contains(UserSetting.prefKey)) {
//			AppPrefs.commonPref.edit()
//				.putJsonObject(UserSetting.prefKey, UserSetting().also {
//					// Default force percent
//					it.forcePercent = 0.99f
//				})
//				.apply()
//		}
	}

	override fun onFinishInflate() {
		super.onFinishInflate()

		// [Init]
		this.setupLayout = findViewById(R.id.setupLayout)
		this.controlLayout = findViewById(R.id.controlLayout)

		this.tvAngleIndicator = findViewById(R.id.tvAngleIndicator)
		this.angleBar = findViewById(R.id.angleBar)

		this.tvForceIndicator = findViewById(R.id.tvForceIndicator)
		this.forceBar = findViewById(R.id.forceBar)

		this.tvWindIndicator = findViewById(R.id.tvWindIndicator)
		this.windBar = findViewById(R.id.windBar)

		findViewById<View>(R.id.vClose).setOnClickListener {
			this.mainWindow.close()
		}

		this.minimapView = findViewById<MinimapView>(R.id.minimapView).also { mapView ->
			mapView.setup(this@MainWindowLayout)
		}

		// [First setup-toggle]
		val setupToggle = findViewById<SwitchCompat>(R.id.setupToggle)
		setupToggle.setOnCheckedChangeListener { buttonView, isChecked ->
			if (isChecked) {
				this.setupLayout.visibility = VISIBLE
				this.controlLayout.visibility = GONE

				this.minimapView.onSetupLayoutVisibilityChanged(true)
			}
			else {
				this.setupLayout.visibility = GONE
				this.controlLayout.visibility = VISIBLE

				this.minimapView.onSetupLayoutVisibilityChanged(false)
			}
		}

		// [Direction switcher]
		val shootDirectionSwitcher = findViewById<SwitchCompat>(R.id.shootDirectionSwitcher)
		shootDirectionSwitcher.setOnCheckedChangeListener { buttonView, isChecked ->
			this.minimapView.changeShootDirection(isChecked)
		}

		// [Setup layout]
		if (AppPrefs.commonPref.contains(UserSetting.prefKey)) {
			val setting = AppPrefs.commonPref.getJsonObject(UserSetting.prefKey, UserSetting::class.java)!!
			findViewById<JoystickView>(R.id.vForcePercent).findViewById<TextView>(R.id.vContent).text = "Force *= ${setting.forcePercent}"

			if (setupToggle.isChecked) {
				setupToggle.performClick()
			}
		}
		else {
			if (!setupToggle.isChecked) {
				setupToggle.performClick()
			}
		}

		findViewById<View>(R.id.setupPlayerPositionLayout).findViewById<TextView>(R.id.vContent).text = "《 Player 》"
		findViewById<View>(R.id.setupRulerBoundsLayout).findViewById<TextView>(R.id.vContent).text = "《 Ruler 》"
		this.setupLayout.findViewById<View>(R.id.btnCancel).setOnClickListener {
			setupToggle.performClick()
		}
		this.setupLayout.findViewById<View>(R.id.btnSave).setOnClickListener {
			this.minimapView.onSaveSettings()
			setupToggle.performClick()
		}

		setupAngleBar()
		setupForceBar()
		setupWindBar()
	}

	private fun setupWindBar() {
		// [0 -> 100] <=> [-5.0 -> 5.0]
		// => (p - 0) / 100 = (w + 5.0) / 10.0
		// => p = 0 + 100 * (w + 5.0) / 10.0
		val defaultWind = 0f
		this.windBar.max = 100
		this.windBar.progress = (0 + 100 * (defaultWind + 5.0) / 10.0).toInt()
		post {
			onWindChanged(10f * (this.windBar.progress - 50f) / 100f)
		}
		this.windBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					val wind = 10f * (progress - 50f) / 100f
					onWindChanged(wind)
				}
			}

			override fun onStartTrackingTouch(seekBar: SeekBar) {
			}

			override fun onStopTrackingTouch(seekBar: SeekBar) {
			}
		})
	}

	private fun setupForceBar() {
		val defaultForce = 80
		this.forceBar.max = 100
		this.forceBar.progress = defaultForce
		post {
			onForceChanged(this.forceBar.progress.toFloat())
		}
		this.forceBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					val force = progress.toFloat()
					onForceChanged(force)
				}
			}

			override fun onStartTrackingTouch(seekBar: SeekBar) {
			}

			override fun onStopTrackingTouch(seekBar: SeekBar) {
			}
		})
	}

	private fun setupAngleBar() {
		val defaultAngle = 38
		this.angleBar.max = 360
		this.angleBar.progress = 180 + defaultAngle
		post {
			onAngleChanged(this.angleBar.progress - 180f)
		}
		this.angleBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					val angle = this@MainWindowLayout.angleBar.progress - 180f
					onAngleChanged(angle)
				}
			}

			override fun onStartTrackingTouch(seekBar: SeekBar) {
			}

			override fun onStopTrackingTouch(seekBar: SeekBar) {
			}
		})
	}

	private fun onAngleChanged(angle: Float) {
		this.tvAngleIndicator.x = DkMaths.clamp(
			(this.angleBar.width * this.angleBar.progress / this.angleBar.max - this.tvAngleIndicator.width / 2).toFloat(),
			0f,
			this.angleBar.width.toFloat() - this.tvAngleIndicator.width
		)
		this.tvAngleIndicator.text = "Angle: ${angle.toInt()}"
		this.minimapView.angle = angle
		this.minimapView.invalidate()
	}

	private fun onForceChanged(force: Float) {
		this.tvForceIndicator.x = DkMaths.clamp(
			(this.forceBar.width * this.forceBar.progress / this.forceBar.max - this.tvForceIndicator.width / 2).toFloat(),
			0f,
			this.forceBar.width.toFloat() - this.tvForceIndicator.width
		)
		this.tvForceIndicator.text = "Force: ${force.toInt()}"
		this.minimapView.force = force
		this.minimapView.invalidate()
	}

	private fun onWindChanged(wind: Float) {
		this.tvWindIndicator.x = DkMaths.clamp(
			(this.windBar.width * this.windBar.progress / this.windBar.max - this.tvWindIndicator.width / 2).toFloat(),
			0f,
			this.windBar.width.toFloat() - this.tvWindIndicator.width
		)
		this.tvWindIndicator.text = "Wind: $wind"
		this.minimapView.wind = wind
		this.minimapView.invalidate()
	}

//	var minimapLeftPercent = 0.800f
//	var minimapTopPercent = 0.160f
//	var minimapWidthPercent = 0.170f
//	var minimapHeightPercent = 0.800f

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)

		DkLogs.debug(this, "onSizeChanged: $oldw, $oldh -> $w, $h")

//		// Setup size for minimap
//		val lp = this.minimapView.layoutParams as LayoutParams
//		lp.width = (minimapWidthPercent * w).toInt()
//		lp.height = (minimapHeightPercent * h).toInt()
//		lp.leftMargin = (minimapLeftPercent * w).toInt()
//		lp.topMargin = (minimapTopPercent * h).toInt()
//		// Must call `post()` to make `minimapView.onSizeChanged()` be called.
//		post {
//			this.minimapView.requestLayout()
//		}
	}

	fun setup(mainWindow: MainWindow) {
		this.mainWindow = mainWindow
	}
}
