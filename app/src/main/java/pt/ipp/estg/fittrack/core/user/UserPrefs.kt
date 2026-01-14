package pt.ipp.estg.fittrack.core.user

import android.content.Context

object UserPrefs {
    private const val FILE = "user_prefs"
    private const val KEY_NAME = "user_name"
    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getUserName(context: Context): String =
        sp(context).getString(KEY_NAME, "")?.trim().orEmpty()

    fun setUserName(context: Context, name: String) {
        sp(context).edit().putString(KEY_NAME, name.trim()).apply()
    }
}
