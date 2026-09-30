package com.example.insy7315_wil_.data.`Data classes`

data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String = ROLE_MEMBER,
    val remindersEnabled: Boolean = true,
    val sharesAnonymousInsights: Boolean = true,
    val activityProgressVisible: Boolean = true,
    val active: Boolean = true,
    val createdAtMillis: Long? = null,
) {
    val isAdmin get() = role == ROLE_ADMIN

    companion object {
        const val ROLE_MEMBER = "member"
        const val ROLE_ADMIN = "admin"
    }
}
