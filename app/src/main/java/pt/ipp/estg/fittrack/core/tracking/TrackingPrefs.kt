package pt.ipp.estg.fittrack.core.tracking

import android.content.Context

object TrackingPrefs {
    private const val FILE = "tracking_prefs"
    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private const val KEY_ACTIVE_SESSION = "active_session_id"
    private const val KEY_ACTIVE_START_TS = "active_start_ts"
    private const val KEY_ACTIVE_TYPE = "active_type"
    private const val KEY_ACTIVE_DISTANCE_M_BITS = "active_distance_m_bits"
    private const val KEY_ACTIVE_SPEED_MPS = "active_speed_mps"
    private const val KEY_ACTIVE_STEPS = "active_steps"

    private const val KEY_AUTO_ENABLED = "auto_enabled"

    private const val KEY_LAST_DET_TYPE = "last_det_type"
    private const val KEY_LAST_DET_CONF = "last_det_conf"
    private const val KEY_LAST_DET_TS = "last_det_ts"

    private const val KEY_PHOTO_BEFORE = "photo_before"
    private const val KEY_PHOTO_AFTER = "photo_after"
    // ---- active session ----
    fun getActiveSessionId(context: Context): String? =
        sp(context).getString(KEY_ACTIVE_SESSION, null)

    fun getActiveSpeedMps(context: Context): Float =
        sp(context).getFloat(KEY_ACTIVE_SPEED_MPS, 0f)

    fun setActiveSteps(context: Context, steps: Int) {
        sp(context).edit().putInt(KEY_ACTIVE_STEPS, steps).apply()
    }

    fun getActiveSteps(context: Context): Int =
        sp(context).getInt(KEY_ACTIVE_STEPS, 0)

    // ---- auto mode ----
    fun isAutoEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_AUTO_ENABLED, false)

    fun setAutoEnabled(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_AUTO_ENABLED, enabled).apply()
    }

    // ---- last detected ----
    fun setLastDetected(context: Context, type: String, confidence: Int, ts: Long) {
        sp(context).edit()
            .putString(KEY_LAST_DET_TYPE, type)
            .putInt(KEY_LAST_DET_CONF, confidence)
            .putLong(KEY_LAST_DET_TS, ts)
            .apply()
    }

    fun getLastDetectedType(context: Context): String =
        sp(context).getString(KEY_LAST_DET_TYPE, "—") ?: "—"

    fun getLastDetectedConfidence(context: Context): Int =
        sp(context).getInt(KEY_LAST_DET_CONF, 0)

    fun getLastDetectedTs(context: Context): Long =
        sp(context).getLong(KEY_LAST_DET_TS, 0L)

    // ---- photos ----
    fun setPhotoBefore(context: Context, uri: String?) {
        sp(context).edit().putString(KEY_PHOTO_BEFORE, uri).apply()
    }

    fun getPhotoBefore(context: Context): String? =
        sp(context).getString(KEY_PHOTO_BEFORE, null)

    fun setPhotoAfter(context: Context, uri: String?) {
        sp(context).edit().putString(KEY_PHOTO_AFTER, uri).apply()
    }

    fun getPhotoAfter(context: Context): String? =
        sp(context).getString(KEY_PHOTO_AFTER, null)

    fun clear(context: Context) {
        sp(context).edit()
            .remove(KEY_ACTIVE_SESSION)
            .remove(KEY_ACTIVE_START_TS)
            .remove(KEY_ACTIVE_TYPE)
            .remove(KEY_ACTIVE_DISTANCE_M_BITS)
            .remove(KEY_ACTIVE_SPEED_MPS)
            .remove(KEY_ACTIVE_STEPS)
            .remove(KEY_PHOTO_BEFORE)
            .remove(KEY_PHOTO_AFTER)
            .apply()
    }
}


