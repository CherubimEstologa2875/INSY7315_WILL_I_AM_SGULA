package com.example.insy7315_wil_.data

import android.content.Context
import com.example.insy7315_wil_.data.`Data classes`.UserProfile
import com.google.firebase.auth.FirebaseAuth

// firebase remembers the signed in user, this only caches the profile and the guest flag
class SessionManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    val isLoggedIn get() = FirebaseAuth.getInstance().currentUser != null
    val isGuest get() = !isLoggedIn && preferences.getBoolean(KEY_GUEST, false)
    val displayName get() = preferences.getString(KEY_NAME, "").orEmpty().ifBlank { "Sgula member" }
    val email get() = preferences.getString(KEY_EMAIL, "").orEmpty()
    val isAdmin get() = isLoggedIn && preferences.getString(KEY_ROLE, "") == UserProfile.ROLE_ADMIN

    val cachedProfile
        get() = UserProfile(
            userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
            email = email,
            displayName = preferences.getString(KEY_NAME, "").orEmpty(),
            role = preferences.getString(KEY_ROLE, UserProfile.ROLE_MEMBER) ?: UserProfile.ROLE_MEMBER,
        )

    fun cache(profile: UserProfile) {
        preferences.edit()
            .putBoolean(KEY_GUEST, false)
            .putString(KEY_NAME, profile.displayName)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_ROLE, profile.role)
            .apply()
    }

    fun continueAsGuest() {
        preferences.edit().putBoolean(KEY_GUEST, true).apply()
    }

    // wipe the cache so nothing from the last account is left on a shared device
    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES = "sgula_session"
        const val KEY_GUEST = "guest"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_ROLE = "role"
    }
}
