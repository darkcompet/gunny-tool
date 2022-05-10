package compet.bundle.common

import compet.bundle.App
import tool.compet.preference.DkPreference

class AppPrefs {
	companion object {
		val commonPref = DkPreference(App.context, "pref_common")
	}
}
