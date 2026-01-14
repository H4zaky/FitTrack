package pt.ipp.estg.fittrack.core.auth

data class UserProfile(
    val uid: String,
    val email: String,
    val name: String,
    val phone: String,
    val createdAt: Long
)