package pt.ipp.estg.fittrack.core.tracking

import android.content.Context

object TrackingPrefs {
    private const val FILE = "tracking_prefs"

    private const val KEY_ACTIVE_SESSION = "active_session_id"
    private const val KEY_ACTIVE_START_TS = "active_start_ts"
    private const val KEY_ACTIVE_TYPE = "active_type"

    private const val KEY_ACTIVE_DISTANCE_M_BITS = "active_distance_m_bits" // Double as Long bits
    private const val KEY_ACTIVE_SPEED_MPS = "active_speed_mps"            // Float

    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getActiveSessionId(context: Context): String? =
        sp(context).getString(KEY_ACTIVE_SESSION, null)

    fun setActiveSessionId(context: Context, id: String?) {
        sp(context).edit().putString(KEY_ACTIVE_SESSION, id).apply()
    }

    fun getActiveStartTs(context: Context): Long =
        sp(context).getLong(KEY_ACTIVE_START_TS, 0L)

    fun setActiveStartTs(context: Context, ts: Long) {
        sp(context).edit().putLong(KEY_ACTIVE_START_TS, ts).apply()
    }

    fun getActiveType(context: Context): String =
        sp(context).getString(KEY_ACTIVE_TYPE, "Walking") ?: "Walking"

    fun setActiveType(context: Context, type: String) {
        sp(context).edit().putString(KEY_ACTIVE_TYPE, type).apply()
    }

    fun setActiveDistanceM(context: Context, distanceM: Double) {
        val bits = java.lang.Double.doubleToRawLongBits(distanceM)
        sp(context).edit().putLong(KEY_ACTIVE_DISTANCE_M_BITS, bits).apply()
    }

    fun getActiveDistanceM(context: Context): Double {
        val bits = sp(context).getLong(KEY_ACTIVE_DISTANCE_M_BITS, 0L)
        return java.lang.Double.longBitsToDouble(bits)
    }

    fun setActiveSpeedMps(context: Context, speedMps: Float) {
        sp(context).edit().putFloat(KEY_ACTIVE_SPEED_MPS, speedMps).apply()
    }

    fun getActiveSpeedMps(context: Context): Float =
        sp(context).getFloat(KEY_ACTIVE_SPEED_MPS, 0f)

    fun clear(context: Context) {
        sp(context).edit()
            .remove(KEY_ACTIVE_SESSION)
            .remove(KEY_ACTIVE_START_TS)
            .remove(KEY_ACTIVE_TYPE)
            .remove(KEY_ACTIVE_DISTANCE_M_BITS)
            .remove(KEY_ACTIVE_SPEED_MPS)
            .apply()
    }
}
