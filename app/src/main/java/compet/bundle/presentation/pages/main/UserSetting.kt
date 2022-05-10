package compet.bundle.presentation.pages.main

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class UserSetting {
	companion object {
		const val prefKey = "user_setting"
	}

	@Expose
	@SerializedName("force_percent")
	var forcePercent = 0f

	@Expose
	@SerializedName("player_x")
	var playerX = 0f

	@Expose
	@SerializedName("player_y")
	var playerY = 0f

	@Expose
	@SerializedName("ruler_left")
	var rulerLeft = 0f

	@Expose
	@SerializedName("ruler_top")
	var rulerTop = 0f

	@Expose
	@SerializedName("ruler_right")
	var rulerRight = 0f

	@Expose
	@SerializedName("ruler_bottom")
	var rulerBottom = 0f
}
