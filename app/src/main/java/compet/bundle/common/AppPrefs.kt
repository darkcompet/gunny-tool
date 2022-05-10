package compet.bundle.common

import compet.bundle.App
import tool.compet.preference.DkSharedPreference

class AppPrefs {
	companion object {
		val commonPref = DkSharedPreference(App.context, "pref_common")
	}
}
