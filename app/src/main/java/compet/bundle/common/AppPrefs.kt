package compet.bundle.common

import compet.bundle.App
import tool.compet.storage.DkSharedPreferences

class AppPrefs {
	companion object {
		fun commonPref() : DkSharedPreferences {
			return DkSharedPreferences(App.context, "pref_common")
		}
	}
}
