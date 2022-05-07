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
class ToolLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

	private lateinit var controlLayout: View
	private lateinit var firstSetupLayout: View
	private lateinit var minimapView: MinimapView

	private lateinit var tvAngleIndicator: TextView
	private lateinit var angleBar: SeekBar

	private lateinit var tvForceIndicator: TextView
	private lateinit var forceBar: SeekBar

	private lateinit var tvWindIndicator: TextView
	private lateinit var windBar: SeekBar

	override fun onFinishInflate() {
		super.onFinishInflate()

		this.controlLayout = findViewById(R.id.controlLayout)

		// [Switchers]
		val firstSetupSwitcher = findViewById<SwitchCompat>(R.id.firstSetupShower)
		firstSetupSwitcher.setOnCheckedChangeListener { buttonView, isChecked ->
			if (isChecked) {
				this.firstSetupLayout.visibility = VISIBLE
				this.controlLayout.visibility = GONE
			}
			else {
				this.firstSetupLayout.visibility = GONE
				this.controlLayout.visibility = VISIBLE
			}
		}
		val shootDirectionSwitcher = findViewById<SwitchCompat>(R.id.shootDirectionSwitcher)
		shootDirectionSwitcher.setOnCheckedChangeListener { buttonView, isChecked ->
			this.minimapView.changeShootDirection(isChecked)
		}

		// [First setup layout]
		this.firstSetupLayout = findViewById(R.id.firstSetupLayout)
		if (AppPrefs.commonPref().contains(MapSetting.pref_key)) {
			firstSetupSwitcher.performClick()
		}
		else {
			firstSetupSwitcher.performClick()
		}
		findViewById<View>(R.id.setupPlayerPositionLayout).findViewById<TextView>(R.id.vContent).text = "《 Player 》"
		findViewById<View>(R.id.setupRulerBoundsLayout).findViewById<TextView>(R.id.vContent).text = "《 Ruler 》"
		this.firstSetupLayout.findViewById<View>(R.id.btnCancel).setOnClickListener {
			firstSetupSwitcher.performClick()
		}
		this.firstSetupLayout.findViewById<View>(R.id.btnSave).setOnClickListener {
			this.minimapView.onSaveSetup()
			firstSetupSwitcher.performClick()
		}

		tvAngleIndicator = findViewById(R.id.tvAngleIndicator)
		angleBar = findViewById(R.id.angleBar)

		tvForceIndicator = findViewById(R.id.tvForceIndicator)
		forceBar = findViewById(R.id.forceBar)

		tvWindIndicator = findViewById(R.id.tvWindIndicator)
		windBar = findViewById(R.id.windBar)

		this.minimapView = findViewById<MinimapView>(R.id.minimapView).also { mapView ->
			mapView.angleBar = angleBar
			mapView.forceBar = forceBar
			mapView.windBar = windBar

			mapView.setupPlayerPositionLayout(firstSetupLayout.findViewById(R.id.setupPlayerPositionLayout))
			mapView.setupRulerBoundsLayout(firstSetupLayout.findViewById(R.id.setupRulerBoundsLayout))
		}

		// [Angle]
		val defaultAngle = 38
		angleBar.max = 360
		angleBar.progress = 180 + defaultAngle
		post {
			onAngleChanged(angleBar.progress - 180f)
		}
		angleBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					val angle = angleBar.progress - 180f
					onAngleChanged(angle)
				}
			}

			override fun onStartTrackingTouch(seekBar: SeekBar) {
			}

			override fun onStopTrackingTouch(seekBar: SeekBar) {
			}
		})

		// [Force]
		val defaultForce = 80
		forceBar.max = 100
		forceBar.progress = defaultForce
		post {
			onForceChanged(forceBar.progress.toFloat())
		}
		forceBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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

		// [Wind]
		// [0 -> 100] <=> [-5.0 -> 5.0]
		// => (p - 0) / 100 = (w + 5.0) / 10.0
		// => p = 0 + 100 * (w + 5.0) / 10.0
		val defaultWind = 0f
		windBar.max = 100
		windBar.progress = (0 + 100 * (defaultWind + 5.0) / 10.0).toInt()
		post {
			onWindChanged(10f * (windBar.progress - 50f) / 100f)
		}
		windBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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

	private fun onAngleChanged(angle: Float) {
		tvAngleIndicator.x = DkMaths.clamp(
			(angleBar.width * angleBar.progress / angleBar.max - tvAngleIndicator.width / 2).toFloat(),
			0f,
			angleBar.width.toFloat() - tvAngleIndicator.width
		)
		tvAngleIndicator.text = "Angle: ${angle.toInt()}"
		minimapView.angle = angle
		minimapView.invalidate()
	}

	private fun onForceChanged(force: Float) {
		tvForceIndicator.x = DkMaths.clamp(
			(forceBar.width * forceBar.progress / forceBar.max - tvForceIndicator.width / 2).toFloat(),
			0f,
			forceBar.width.toFloat() - tvForceIndicator.width
		)
		tvForceIndicator.text = "Force: ${force.toInt()}"
		minimapView.force = force
		minimapView.invalidate()
	}

	private fun onWindChanged(wind: Float) {
		tvWindIndicator.x = DkMaths.clamp(
			(windBar.width * windBar.progress / windBar.max - tvWindIndicator.width / 2).toFloat(),
			0f,
			windBar.width.toFloat() - tvWindIndicator.width
		)
		tvWindIndicator.text = "Wind: $wind"
		minimapView.wind = wind
		minimapView.invalidate()
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

	fun onSetPlayer() {
//		this.choosePlayer = true
//		this.chooseEnemy = false

		// Force call onDraw()
		this.invalidate()
	}

	fun onSetEnemy() {
//		this.choosePlayer = false
//		this.chooseEnemy = true

		this.translationX = -320f

		// Force call onDraw()
		this.invalidate()
	}
}
