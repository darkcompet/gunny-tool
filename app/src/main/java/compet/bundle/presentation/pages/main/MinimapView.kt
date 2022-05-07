package compet.bundle.presentation.pages.main

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import compet.bundle.R
import compet.bundle.common.AppPrefs
import tool.compet.core.DkConfig
import tool.compet.core.DkMaths
import kotlin.math.PI
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

	private var isLeftShootDirection = true

	// Assigned from outside
	lateinit var angleBar: SeekBar
	lateinit var forceBar: SeekBar
	lateinit var windBar: SeekBar
	lateinit var playerSetupPositionLayout: JoystickView
	lateinit var rulerSetupBoundsLayout: JoystickView

	// [-180, 180]
	var angle = 0f

	// [0, 100]
	var force = 0f

	// [-5.0, 5.0]
	var wind = 0f

	var issetPlayerPosition: Boolean = true
	var issetEnemyPosition: Boolean = false

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

	init {
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
			this.strokeWidth = 0.5f
		}

		AppPrefs.commonPref().getJsonObject(MapSetting.pref_key, MapSetting::class.java)?.also { mapSetting ->
			this.playerX = mapSetting.playerX
			this.playerY = mapSetting.playerY
			this.rulerBounds.set(mapSetting.rulerLeft, mapSetting.rulerTop, mapSetting.rulerRight, mapSetting.rulerBottom)
		}

		this.playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			this.color = Color.YELLOW
			this.style = Paint.Style.FILL_AND_STROKE
			this.strokeWidth = 1.5f
		}

		this.bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			this.color = Color.YELLOW
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
		for (i in -rightCount..leftCount) {
			if (i % 2 == 0) {
				canvas.drawLine(
					this.playerX - i * rulerUnitSize, 0f,
					this.playerX - i * rulerUnitSize, height,
					this.rulerPaint
				)
				canvas.drawText("$i", this.playerX - i * rulerUnitSize, height, this.textPaint)
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
		// ok tam cao: 0.91f
		// goc 50 -> luc x 0.99f
		val v = this.force * 0.91f
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

	fun onSaveSetup() {
		AppPrefs.commonPref().storeJsonObject(MapSetting.pref_key, MapSetting().also { mapSetting ->
			mapSetting.playerX = this.playerX
			mapSetting.playerY = this.playerY
			mapSetting.rulerLeft = this.rulerBounds.left
			mapSetting.rulerTop = this.rulerBounds.top
			mapSetting.rulerRight = this.rulerBounds.right
			mapSetting.rulerBottom = this.rulerBounds.bottom
		})
	}

	fun setupPlayerPositionLayout(view: JoystickView) {
		this.playerSetupPositionLayout = view

		view.findViewById<View>(R.id.vIconLeft).visibility = GONE
		view.findViewById<View>(R.id.vIconTop).visibility = GONE
		view.findViewById<View>(R.id.vIconRight).visibility = GONE
		view.findViewById<View>(R.id.vIconBottom).visibility = GONE

		view.callback = object : JoystickView.Callback {
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

	fun setupRulerBoundsLayout(joystickView: JoystickView) {
		this.rulerSetupBoundsLayout = joystickView

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

	fun changeShootDirection(leftShootDirection: Boolean) {
		this.isLeftShootDirection = leftShootDirection
		invalidate()
	}
}
