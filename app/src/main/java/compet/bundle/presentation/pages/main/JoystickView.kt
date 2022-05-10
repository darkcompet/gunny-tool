package compet.bundle.presentation.pages.main

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import compet.bundle.R
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class JoystickView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

	interface Callback {
		fun onMoveLeftEdge(direction: Int, tickCount: Int) = Unit

		fun onMoveTopEdge(direction: Int, tickCount: Int) = Unit

		fun onMoveRightEdge(direction: Int, tickCount: Int) = Unit

		fun onMoveBottomEdge(direction: Int, tickCount: Int) = Unit

		fun onFreeMove(direction: Int, tickCount: Int) = Unit

		fun onButtonClicked(index: Int, view: View) = Unit
	}

	companion object {
		const val dir_left = 1
		const val dir_top = 2
		const val dir_right = 3
		const val dir_bottom = 4

		const val edge_left_index = 0
		const val edge_top_index = 1
		const val edge_right_index = 2
		const val edge_bottom_index = 3
	}

	private var horizontalPriority = false
	private var verticalPriority = false

	var callback: Callback? = null
	private val myHandler = Handler(Looper.getMainLooper())

	private var downX = 0f
	private var downY = 0f
	private var lastMoveX = 0f
	private var lastMoveY = 0f
	private var selectedEdgeIndex = -1
	private var leftTickCount = 0
	private var topTickCount = 0
	private var rightTickCount = 0
	private var bottomTickCount = 0
	private var commonTickCount = 0
	override fun onFinishInflate() {
		val icons = arrayOf<View>(
			findViewById(R.id.vIconLeft),
			findViewById(R.id.vIconTop),
			findViewById(R.id.vIconRight),
			findViewById(R.id.vIconBottom)
		)
		val edges = arrayOf(0, 1, 2, 3)

		for (index in icons.indices) {
			icons[index].setOnClickListener {
				// Unselect all icons
				for (tmp in icons) {
					tmp.setBackgroundColor(Color.BLACK)
				}

				// Unselect this icon
				if (this.selectedEdgeIndex == edges[index]) {
					this.selectedEdgeIndex = -1
				}
				// Select only this icon
				else {
					this.selectedEdgeIndex = edges[index]
					icons[index].setBackgroundColor(Color.RED)
				}

				// Callback
				this.callback?.onButtonClicked(index, icons[index])
			}
		}

		// Detect touch on content
		findViewById<View>(R.id.vContent).setOnTouchListener { v, event ->
			when (event.actionMasked) {
				MotionEvent.ACTION_DOWN -> {
					this.downX = event.x
					this.downY = event.y
					this.lastMoveX = event.x
					this.lastMoveY = event.y
				}
				MotionEvent.ACTION_MOVE -> {
					val curX = event.x
					val curY = event.y

					var autodetectedHorizontalPriority = false
					var autodetectedVerticalPriority = false
					if (!this.horizontalPriority && !this.verticalPriority) {
						when {
							abs(curX - this.lastMoveX) > abs(curY - this.lastMoveY) -> {
								autodetectedHorizontalPriority = true
								autodetectedVerticalPriority = false
							}
							abs(curY - this.lastMoveY) > abs(curX - this.lastMoveX) -> {
								autodetectedVerticalPriority = true
								autodetectedHorizontalPriority = false
							}
						}
					}

					when {
						autodetectedHorizontalPriority || this.horizontalPriority -> {
							when {
								curX < lastMoveX -> {
									onMove(dir_left)
								}
								curX > lastMoveX -> {
									onMove(dir_right)
								}
							}
						}
						autodetectedVerticalPriority || this.verticalPriority -> {
							when {
								curY < this.lastMoveY -> {
									onMove(dir_top)
								}
								curY > this.lastMoveY -> {
									onMove(dir_bottom)
								}
							}
						}
					}

					this.lastMoveX = curX
					this.lastMoveY = curY
				}
				MotionEvent.ACTION_CANCEL,
				MotionEvent.ACTION_OUTSIDE,
				MotionEvent.ACTION_UP -> {
					this.leftTickCount = 0
					this.topTickCount = 0
					this.rightTickCount = 0
					this.bottomTickCount = 0
					this.commonTickCount = 0
					this.myHandler.removeCallbacksAndMessages(null)
				}
			}
			true
		}

		super.onFinishInflate()
	}

	private fun onMove(direction: Int) {
		when (this.selectedEdgeIndex) {
			edge_left_index -> {
				this.callback?.onMoveLeftEdge(direction, ++this.leftTickCount)
			}
			edge_top_index -> {
				this.callback?.onMoveTopEdge(direction, ++this.topTickCount)
			}
			edge_right_index -> {
				this.callback?.onMoveRightEdge(direction, ++this.rightTickCount)
			}
			edge_bottom_index -> {
				this.callback?.onMoveBottomEdge(direction, ++this.bottomTickCount)
			}
			else -> {
				this.callback?.onFreeMove(direction, ++this.commonTickCount)
			}
		}
	}

	fun prioritizeHorizontal() {
		this.horizontalPriority = true
		this.verticalPriority = false
	}

	fun prioritizeVertical() {
		this.horizontalPriority = false
		this.verticalPriority = true
	}
}
