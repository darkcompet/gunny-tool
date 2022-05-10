package compet.bundle.presentation.pages.main

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import compet.bundle.R
import compet.bundle.common.AppPrefs
import tool.compet.core.DkConfig
import tool.compet.core.DkMaths
import tool.compet.core.DkStrings
import tool.compet.core.parseFloatDk
import tool.compet.view.DkViews
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

@SuppressLint("SetTextI18n")
class MinimapView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	lateinit var mainLayout: MainWindowLayout
	private var isLeftShootDirection = true

	// [-180, 180]
	var angle = 0f

	// [0, 100]
	var force = 0f

	// [-5.0, 5.0]
	var wind = 0f

	private var forcePercent = 0f

	private val playerPaint: Paint
	private var playerX: Float = 0f
	private var playerY: Float = 0f

	private val enemyPaint: Paint
	private var enemyX: Float = 0f
	private var enemyY: Float = 0f

	private val paint: Paint
	private val textPaint: Paint

	private val bulletPaint: Paint

	private val rulerPaint: Paint
	private val rulerBounds = RectF()

	private var shouldShowRulerDetail = false

	init {
		// Read/Apply user setting
		if (AppPrefs.commonPref.contains(UserSetting.prefKey)) {
			AppPrefs.commonPref.getJsonObject(UserSetting.prefKey, UserSetting::class.java)!!.also { setting ->
				this.forcePercent = setting.forcePercent
				this.playerX = setting.playerX
				this.playerY = setting.playerY
				this.rulerBounds.set(setting.rulerLeft, setting.rulerTop, setting.rulerRight, setting.rulerBottom)
			}
		}

		this.paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			this.color = Color.GREEN
			this.style = Paint.Style.STROKE
			this.strokeWidth = 2f
		}

		this.textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			this.color = Color.GREEN
			this.textSize = 8 * DkConfig.scaledDensity()
		}

		this.rulerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			this.color = Color.YELLOW
			this.style = Paint.Style.STROKE
			this.strokeWidth = 1f
		}

		this.playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			this.color = Color.YELLOW
			this.style = Paint.Style.FILL_AND_STROKE
			this.strokeWidth = 1.5f
		}

		this.bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			this.color = Color.GREEN
			this.style = Paint.Style.FILL_AND_STROKE
			this.strokeWidth = 1f
		}

		this.enemyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			this.color = Color.RED
			this.style = Paint.Style.FILL_AND_STROKE
			this.strokeWidth = 1.5f
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)

		if (this.playerX == 0f && this.playerY == 0f) {
			this.playerX = w / 2f
			this.playerY = h / 2f

			this.rulerBounds.set(this.playerX - 50, this.playerY - 50, this.playerX + 50, this.playerY + 50)
		}
	}

	fun setup(mainLayout: MainWindowLayout) {
		this.mainLayout = mainLayout
		setupForcePercentModifier()
		setupPlayerPositionLayout()
		setupRulerBoundsLayout()
	}

	private fun setupForcePercentModifier() {
		val joystickView = this.mainLayout.findViewById<JoystickView>(R.id.vForcePercent)
		val vforcePercentContent = joystickView.findViewById<TextView>(R.id.vContent)
		joystickView.findViewById<View>(R.id.vIconLeft).visibility = GONE
		joystickView.findViewById<View>(R.id.vIconTop).visibility = GONE
		joystickView.findViewById<View>(R.id.vIconRight).visibility = GONE
		joystickView.findViewById<View>(R.id.vIconBottom).visibility = GONE

		joystickView.callback = object : JoystickView.Callback {
			override fun onFreeMove(direction: Int, tickCount: Int) {
				when (direction) {
					JoystickView.dir_left, JoystickView.dir_right -> {
						val tmp = DkMaths.clamp(forcePercent + calcTranslation(direction, tickCount) / 100f, -10f, 10f)
						val prefix = when {
							tmp < 0f -> "-"
							else -> " "
						}
						forcePercent = DkStrings.format("%1.2f", abs(tmp)).parseFloatDk()
						vforcePercentContent.text = "Force *= $prefix$forcePercent"
					}
				}
			}
		}
	}

	private fun setupPlayerPositionLayout() {
		val joystickView = this.mainLayout.findViewById<JoystickView>(R.id.setupPlayerPositionLayout)
		joystickView.findViewById<View>(R.id.vIconLeft).visibility = GONE
		joystickView.findViewById<View>(R.id.vIconTop).visibility = GONE
		joystickView.findViewById<View>(R.id.vIconRight).visibility = GONE
		joystickView.findViewById<View>(R.id.vIconBottom).visibility = GONE

		joystickView.callback = object : JoystickView.Callback {
			override fun onFreeMove(direction: Int, tickCount: Int) {
				when (direction) {
					JoystickView.dir_left, JoystickView.dir_right -> {
						playerX = DkMaths.clamp(playerX + calcTranslation(direction, tickCount), 0f, width.toFloat())
					}
					JoystickView.dir_top, JoystickView.dir_bottom -> {
						playerY = DkMaths.clamp(playerY + calcTranslation(direction, tickCount), 0f, height.toFloat())
					}
				}
				invalidate()
			}
		}
	}

	private fun setupRulerBoundsLayout() {
		val joystickView = this.mainLayout.findViewById<JoystickView>(R.id.setupRulerBoundsLayout)
		joystickView.callback = object : JoystickView.Callback {
			override fun onButtonClicked(index: Int, view: View) {
				if (index == 0 || index == 2) {
					joystickView.prioritizeHorizontal()
				}
				else if (index == 1 || index == 3) {
					joystickView.prioritizeVertical()
				}
			}

			override fun onMoveLeftEdge(direction: Int, tickCount: Int) {
				rulerBounds.left = DkMaths.clamp(rulerBounds.left + calcTranslation(direction, tickCount), 0f, width.toFloat())
				invalidate()
			}

			override fun onMoveTopEdge(direction: Int, tickCount: Int) {
				rulerBounds.top = DkMaths.clamp(rulerBounds.top + calcTranslation(direction, tickCount), 0f, height.toFloat())
				invalidate()
			}

			override fun onMoveRightEdge(direction: Int, tickCount: Int) {
				rulerBounds.right =
					DkMaths.clamp(rulerBounds.right + calcTranslation(direction, tickCount), 0f, width.toFloat())
				invalidate()
			}

			override fun onMoveBottomEdge(direction: Int, tickCount: Int) {
				rulerBounds.bottom =
					DkMaths.clamp(rulerBounds.bottom + calcTranslation(direction, tickCount), 0f, height.toFloat())
				invalidate()
			}
		}
	}

	private fun calcTranslation(direction: Int, tickCount: Int): Int {
		when (direction) {
			JoystickView.dir_left, JoystickView.dir_top -> {
				return when {
					tickCount == 5 || tickCount == 10 || tickCount == 15 || tickCount == 20 -> -1
					tickCount > 40 -> -4
					tickCount > 30 -> -3
					tickCount > 20 -> -2
					else -> 0
				}
			}
			// Right, bottom
			JoystickView.dir_right, JoystickView.dir_bottom -> {
				return when {
					tickCount == 5 || tickCount == 10 || tickCount == 15 || tickCount == 20 -> 1
					tickCount > 40 -> 4
					tickCount > 30 -> 3
					tickCount > 20 -> 2
					else -> 0
				}
			}
			else -> {
				throw Exception("Invalid direction: $direction")
			}
		}
	}

	fun onSaveSettings() {
		AppPrefs.commonPref.edit()
			.putJsonObject(UserSetting.prefKey, UserSetting().also { setting ->
				setting.forcePercent = this.forcePercent
				setting.playerX = this.playerX
				setting.playerY = this.playerY
				setting.rulerLeft = this.rulerBounds.left
				setting.rulerTop = this.rulerBounds.top
				setting.rulerRight = this.rulerBounds.right
				setting.rulerBottom = this.rulerBounds.bottom
			})
			.commitNow()
	}

	fun changeShootDirection(leftShootDirection: Boolean) {
		this.isLeftShootDirection = leftShootDirection
		invalidate()
	}

	fun onSetupLayoutVisibilityChanged(visible: Boolean) {
		this.shouldShowRulerDetail = !visible
		invalidate()
	}

	private val tmpTextBounds = Rect()
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		// Bounds
		val width = this.width.toFloat()
		val height = this.height.toFloat()
		canvas.drawRect(0f, 0f, width, height, this.paint)

		// [Ruler]
		canvas.drawRect(this.rulerBounds, this.rulerPaint)

		// [Player]
		canvas.drawCircle(this.playerX, this.playerY, 1.5f, this.playerPaint)
//		canvas.drawLine(this.playerX, 0f, this.playerX, height, this.playerPaint)
//		canvas.drawLine(0f, this.playerY, width, this.playerY, this.playerPaint)

		// [Enemy]
//		canvas.drawCircle(this.enemyX, this.enemyY, 4f, this.enemyPaint)

		// [Matrix]
		// Draw lines at Ox axis
		val rulerUnitSize = this.rulerBounds.width() / 10
		val rightCount = ((width - this.playerX) / rulerUnitSize).toInt()
		val leftCount = (this.playerX / rulerUnitSize).toInt()

		for (pos in -rightCount..leftCount) {
			if (pos % 2 == 0) {
				if (this.shouldShowRulerDetail) {
					canvas.drawLine(
						this.playerX - pos * rulerUnitSize, 0f,
						this.playerX - pos * rulerUnitSize, height,
						this.rulerPaint
					)
				}

				if (pos % 4 == 0) {
					val text = "${abs(pos)}"

					this.textPaint.getTextBounds(text, 0, text.length, tmpTextBounds)
					val xy = DkViews.calcTextViewDrawPoint(
						tmpTextBounds,
						this.playerX - pos * rulerUnitSize,
						height - tmpTextBounds.height()
					)

					canvas.drawText(text, xy[0], xy[1], this.textPaint)
				}
			}
		}
//		// Draw lines at Oy axis
//		val bottomCount = ((height - this.playerY) / rulerUnitSize).toInt()
//		val topCount = (this.playerY / rulerUnitSize).toInt()
//		for (i in -bottomCount..topCount) {
//			canvas.drawLine(
//				0f, this.playerY - i * rulerUnitSize,
//				width.toFloat(), this.playerY - i * rulerUnitSize,
//				this.rulerPaint
//			)
//		}

		// [Bullet]
//		drawProjectile1(canvas, width, height)
		drawProjectile2(canvas, width, height)
	}

	private var measuredRulerWidth = 0f

	private fun drawProjectile1(canvas: Canvas, width: Float, height: Float) {
		// From now, we make the player as O point of new coordinate system which is
		// different with Android Coordinate System (at left-top).
		// Ref: Projectile motion with air resistance: https://en.wikipedia.org/wiki/Projectile_motion
		// Consider initial velocity of bullet as some percent of the force which made by user.
		val v = this.force * 0.94f
		val v0x = (v * cos(this.angle * PI / 180.0)).toFloat()
		val v0y = (v * sin(this.angle * PI / 180.0)).toFloat()
		// When wind == 0f, then ax must be 0f
		val ax = -this.wind * 0.15f
		var t = 0f
		val x0 = 0f
		val y0 = 0f
		var curX = 0f
		var curY = 0f
		var nextX = 0f
		var nextY = 0f
		// Delta time (time step)
		val dt = 0.1f
		// Default air resistance value
		val miu = 0.042f
		val g = 9.81f
		val e = 2.71828f
		var loopCount = max(width / 0.1f, height / 0.1f).toInt()
		while (loopCount-- > 0 && (nextX in -(width - this.playerX)..this.playerX || nextY in -(height - this.playerY)..(this.playerY))) {
			t += dt

			val t2 = t * t
//			nextX = x0 + v0x * t + 0.5f * ax * t2
//			nextY = y0 + v0y * t + 0.5f * (-g) * t2

			nextX = x0 + v0x * (1 - e.pow(-miu * t)) / miu + 0.5f * ax * t2
			nextY = y0 + -g * t / miu + (v0y + g / miu) * (1 - e.pow(-miu * t)) / miu

			if (this.measuredRulerWidth == 0f && nextY < 0f) {
				this.measuredRulerWidth = curX
			}

			// Ti draw with canvas, we must convert position in Player Coordinate System (PCS, O at player position)
			// to Android View Coordinate System (ACS, O at left-top of the view).
			val directedCurX = if (this.isLeftShootDirection) curX else -curX
			val directedCurY = curY
			val directedNextX = if (this.isLeftShootDirection) nextX else -nextX
			val directedNextY = nextY
			canvas.drawLine(
				this.playerX - directedCurX, this.playerY - directedCurY,
				this.playerX - directedNextX, this.playerY - directedNextY, this.bulletPaint
			)

			curX = nextX
			curY = nextY
		}
	}

	// Ref formulas: https://web.physics.wustl.edu/~wimd/topic01.pdf
	private fun drawProjectile2(canvas: Canvas, width: Float, height: Float) {
		// From now, we make the player as O point of new coordinate system which is
		// different with Android Coordinate System (at left-top).
		// Ref: Projectile motion with air resistance: https://en.wikipedia.org/wiki/Projectile_motion
		// Consider initial velocity of bullet as some percent of the force which made by user.
		// forcePercent's tam cao: x0.91f
		// forcePercent's goc 50: x0.99f
		val v = this.force * this.forcePercent
		var vx = (v * cos(this.angle * PI / 180.0)).toFloat()
		var vy = (v * sin(this.angle * PI / 180.0)).toFloat()
		var t = 0f
		val x0 = 0f
		val y0 = 0f
		// In player-coordinate-system
		var curX = x0
		var curY = y0
		var nextX = 0f
		var nextY = 0f
		// Delta time (time step)
		val dt = 0.1f
		// Default air resistance value
		val g = 9.81f
		var ax: Float
		var ay: Float
		var loopCount = max(width / 0.1f, height / 0.1f).toInt()
		while (loopCount-- > 0 && (nextX in -(width - this.playerX)..this.playerX || nextY in -(height - this.playerY)..(this.playerY))) {
			t += dt

			val dt2 = dt * dt
			val dm = 0.001f
			ax = -dm * v * vx - this.wind * 0.39f
			ay = -g - dm * v * vy
			vx += ax * dt
			vy += ay * dt
			nextX = curX + vx * dt + 0.5f * ax * dt2
			nextY = curY + vy * dt + 0.5f * ay * dt2

			if (this.measuredRulerWidth == 0f && nextY < 0f) {
				this.measuredRulerWidth = curX
			}

			// Ti draw with canvas, we must convert position in Player Coordinate System (PCS, O at player position)
			// to Android View Coordinate System (ACS, O at left-top of the view).
			val directedCurX = if (this.isLeftShootDirection) curX else -curX
			val directedCurY = curY
			val directedNextX = if (this.isLeftShootDirection) nextX else -nextX
			val directedNextY = nextY
			canvas.drawLine(
				this.playerX - directedCurX, this.playerY - directedCurY,
				this.playerX - directedNextX, this.playerY - directedNextY, this.bulletPaint
			)

			curX = nextX
			curY = nextY
		}
	}
}
