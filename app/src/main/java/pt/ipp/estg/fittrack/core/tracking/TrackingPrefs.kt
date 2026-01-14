package pt.ipp.estg.fittrack.core.tracking

import android.content.Context

object TrackingPrefs {
    private const val FILE = "tracking_prefs"
    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // user
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_PHONE = "user_phone"

    // active session
    private const val KEY_ACTIVE_SESSION = "active_session_id"
    private const val KEY_ACTIVE_START_TS = "active_start_ts"
    private const val KEY_ACTIVE_TYPE = "active_type"
    private const val KEY_ACTIVE_DISTANCE_M_BITS = "active_distance_m_bits"
    private const val KEY_ACTIVE_SPEED_MPS = "active_speed_mps"
    private const val KEY_ACTIVE_STEPS = "active_steps"

    // auto
    private const val KEY_AUTO_ENABLED = "auto_enabled"

    private const val KEY_ACTIVE_MODE = "active_mode"

    // auto (novo)
    private const val KEY_AUTO_LAST_START_TS = "auto_last_start_ts"
    private const val KEY_AUTO_LAST_MOVING_TS = "auto_last_moving_ts"

    // last detected
    private const val KEY_LAST_DET_TYPE = "last_det_type"
    private const val KEY_LAST_DET_CONF = "last_det_conf"
    private const val KEY_LAST_DET_TS = "last_det_ts"

    // photos
    private const val KEY_PHOTO_BEFORE = "photo_before"
    private const val KEY_PHOTO_AFTER = "photo_after"

    private const val KEY_LIGHT_LOW = "light_low"


    // leaderboard
    private const val KEY_LB_LAST_RANK = "lb_last_rank"
    private const val KEY_LB_LAST_MONTH = "lb_last_month"
    private const val KEY_LB_LAST_OVERTAKER_UID = "lb_last_overtaker_uid"

    private const val KEY_BATTERY_LOW = "battery_low"

    // --- user ---
    fun setUser(context: Context, uid: String?, name: String?, phone: String?) {
        sp(context).edit()
            .putString(KEY_USER_ID, uid)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_PHONE, phone)
            .apply()
    }
    fun getUserId(context: Context): String? = sp(context).getString(KEY_USER_ID, null)
    fun getUserName(context: Context): String? = sp(context).getString(KEY_USER_NAME, null)
    fun getUserPhone(context: Context): String? = sp(context).getString(KEY_USER_PHONE, null)

    // --- active session ---
    fun getActiveSessionId(context: Context): String? = sp(context).getString(KEY_ACTIVE_SESSION, null)
    fun setActiveSessionId(context: Context, id: String?) { sp(context).edit().putString(KEY_ACTIVE_SESSION, id).apply() }

    fun getActiveStartTs(context: Context): Long = sp(context).getLong(KEY_ACTIVE_START_TS, 0L)
    fun setActiveStartTs(context: Context, ts: Long) { sp(context).edit().putLong(KEY_ACTIVE_START_TS, ts).apply() }

    fun getActiveType(context: Context): String = sp(context).getString(KEY_ACTIVE_TYPE, "Walking") ?: "Walking"
    fun setActiveType(context: Context, type: String) { sp(context).edit().putString(KEY_ACTIVE_TYPE, type).apply() }

    fun setActiveDistanceM(context: Context, distanceM: Double) {
        sp(context).edit().putLong(KEY_ACTIVE_DISTANCE_M_BITS, java.lang.Double.doubleToRawLongBits(distanceM)).apply()
    }
    fun getActiveDistanceM(context: Context): Double =
        java.lang.Double.longBitsToDouble(sp(context).getLong(KEY_ACTIVE_DISTANCE_M_BITS, 0L))

    fun setActiveSpeedMps(context: Context, speedMps: Float) { sp(context).edit().putFloat(KEY_ACTIVE_SPEED_MPS, speedMps).apply() }
    fun getActiveSpeedMps(context: Context): Float = sp(context).getFloat(KEY_ACTIVE_SPEED_MPS, 0f)

    fun setActiveSteps(context: Context, steps: Int) { sp(context).edit().putInt(KEY_ACTIVE_STEPS, steps).apply() }
    fun getActiveSteps(context: Context): Int = sp(context).getInt(KEY_ACTIVE_STEPS, 0)

    // --- auto ---
    fun isAutoEnabled(context: Context): Boolean = sp(context).getBoolean(KEY_AUTO_ENABLED, false)
    fun setAutoEnabled(context: Context, enabled: Boolean) { sp(context).edit().putBoolean(KEY_AUTO_ENABLED, enabled).apply() }

    // --- last detected ---
    fun setLastDetected(context: Context, type: String, confidence: Int, ts: Long) {
        sp(context).edit()
            .putString(KEY_LAST_DET_TYPE, type)
            .putInt(KEY_LAST_DET_CONF, confidence)
            .putLong(KEY_LAST_DET_TS, ts)
            .apply()
    }
    fun getLastDetectedType(context: Context): String = sp(context).getString(KEY_LAST_DET_TYPE, "—") ?: "—"
    fun getLastDetectedConfidence(context: Context): Int = sp(context).getInt(KEY_LAST_DET_CONF, 0)
    fun getLastDetectedTs(context: Context): Long = sp(context).getLong(KEY_LAST_DET_TS, 0L)

    // --- photos ---
    fun setPhotoBefore(context: Context, uri: String?) { sp(context).edit().putString(KEY_PHOTO_BEFORE, uri).apply() }
    fun getPhotoBefore(context: Context): String? = sp(context).getString(KEY_PHOTO_BEFORE, null)

    fun setPhotoAfter(context: Context, uri: String?) { sp(context).edit().putString(KEY_PHOTO_AFTER, uri).apply() }
    fun getPhotoAfter(context: Context): String? = sp(context).getString(KEY_PHOTO_AFTER, null)

    fun clearActiveSession(context: Context) {
        sp(context).edit()
            .remove(KEY_ACTIVE_SESSION)
            .remove(KEY_ACTIVE_START_TS)
            .remove(KEY_ACTIVE_TYPE)
            .remove(KEY_ACTIVE_DISTANCE_M_BITS)
            .remove(KEY_ACTIVE_SPEED_MPS)
            .remove(KEY_ACTIVE_STEPS)
            .remove(KEY_PHOTO_BEFORE)
            .remove(KEY_PHOTO_AFTER)
            .remove(KEY_ACTIVE_MODE)
            .apply()
    }

    fun setLastRank(context: Context, month: String, rank: Int, overtakerUid: String?) {
        sp(context).edit()
            .putString(KEY_LB_LAST_MONTH, month)
            .putInt(KEY_LB_LAST_RANK, rank)
            .putString(KEY_LB_LAST_OVERTAKER_UID, overtakerUid)
            .apply()
    }

    fun getLastRankMonth(context: Context): String = sp(context).getString(KEY_LB_LAST_MONTH, "") ?: ""
    fun getLastRank(context: Context): Int = sp(context).getInt(KEY_LB_LAST_RANK, -1)
    fun ensuringOvertakerUid(context: Context): String? = sp(context).getString(KEY_LB_LAST_OVERTAKER_UID, null)

    fun isBatteryLow(context: Context): Boolean = sp(context).getBoolean(KEY_BATTERY_LOW, false)
    fun setBatteryLow(context: Context, low: Boolean) { sp(context).edit().putBoolean(KEY_BATTERY_LOW, low).apply() }
    fun clearUser(context: Context) {
        sp(context).edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PHONE)
            .apply()
    }
    fun isLightLow(context: Context): Boolean =
        sp(context).getBoolean(KEY_LIGHT_LOW, false)

    fun setLightLow(context: Context, low: Boolean) {
        sp(context).edit().putBoolean(KEY_LIGHT_LOW, low).apply()
    }


    fun setActiveMode(context: Context, mode: String) {
        sp(context).edit().putString(KEY_ACTIVE_MODE, mode).apply()
    }
    fun getActiveMode(context: Context): String =
        sp(context).getString(KEY_ACTIVE_MODE, "MANUAL") ?: "MANUAL"

    fun setAutoLastStartTs(context: Context, ts: Long) {
        sp(context).edit().putLong(KEY_AUTO_LAST_START_TS, ts).apply()
    }
    fun getAutoLastStartTs(context: Context): Long =
        sp(context).getLong(KEY_AUTO_LAST_START_TS, 0L)

    fun setAutoLastMovingTs(context: Context, ts: Long) {
        sp(context).edit().putLong(KEY_AUTO_LAST_MOVING_TS, ts).apply()
    }
    fun getAutoLastMovingTs(context: Context): Long =
        sp(context).getLong(KEY_AUTO_LAST_MOVING_TS, 0L)
}
