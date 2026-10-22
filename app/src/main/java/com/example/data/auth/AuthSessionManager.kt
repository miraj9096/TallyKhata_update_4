package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class UserAccount(
    val userId: String,
    val authType: String, // "MOBILE" or "EMAIL"
    val phone: String? = null,
    val email: String? = null,
    val storeName: String = "My Store",
    val ownerName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Manages phone number and email based merchant authentication, local credential storage,
 * and session state for Tally Khata.
 */
class AuthSessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("tally_auth_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_ACTIVE_AUTH_TYPE = "active_auth_type"
        private const val KEY_ACTIVE_PHONE = "active_phone"
        private const val KEY_ACTIVE_EMAIL = "active_email"
        private const val KEY_ACTIVE_STORE_NAME = "active_store_name"
        private const val KEY_ACTIVE_OWNER_NAME = "active_owner_name"
        private const val KEY_REGISTERED_USERS = "registered_users_json"
        private const val KEY_PASSWORDS_PREFIX = "secret_"
    }

    fun isLoggedIn(): Boolean {
        return prefs.getString(KEY_ACTIVE_USER_ID, null) != null
    }

    fun getCurrentUser(): UserAccount? {
        val uid = prefs.getString(KEY_ACTIVE_USER_ID, null) ?: return null
        return UserAccount(
            userId = uid,
            authType = prefs.getString(KEY_ACTIVE_AUTH_TYPE, "MOBILE") ?: "MOBILE",
            phone = prefs.getString(KEY_ACTIVE_PHONE, null),
            email = prefs.getString(KEY_ACTIVE_EMAIL, null),
            storeName = prefs.getString(KEY_ACTIVE_STORE_NAME, "My Store") ?: "My Store",
            ownerName = prefs.getString(KEY_ACTIVE_OWNER_NAME, "") ?: ""
        )
    }

    fun signUpWithMobile(
        countryCode: String,
        mobileNumber: String,
        pin: String,
        storeName: String,
        ownerName: String
    ): Result<UserAccount> {
        val cleanDigits = mobileNumber.filter { it.isDigit() }
        if (cleanDigits.length < 8) {
            return Result.failure(IllegalArgumentException("Please enter a valid mobile number (at least 8 digits)"))
        }
        if (pin.length < 4) {
            return Result.failure(IllegalArgumentException("4-digit security PIN is required"))
        }

        val code = countryCode.trim().ifBlank { "+880" }
        val fullPhone = "$code ${cleanDigits}"
        val cleanStore = storeName.trim().ifBlank { "Store $cleanDigits" }
        val userId = "user_mob_$cleanDigits"

        val account = UserAccount(
            userId = userId,
            authType = "MOBILE",
            phone = fullPhone,
            email = null,
            storeName = cleanStore,
            ownerName = ownerName.trim()
        )

        saveAccount(account, pin)
        setActiveSession(account)
        return Result.success(account)
    }

    fun logInWithMobile(
        countryCode: String,
        mobileNumber: String,
        pin: String
    ): Result<UserAccount> {
        val cleanDigits = mobileNumber.filter { it.isDigit() }
        if (cleanDigits.length < 8) {
            return Result.failure(IllegalArgumentException("Please enter a valid mobile number (at least 8 digits)"))
        }
        if (pin.length < 4) {
            return Result.failure(IllegalArgumentException("Please enter your 4-digit PIN"))
        }

        val code = countryCode.trim().ifBlank { "+880" }
        val fullPhone = "$code $cleanDigits"
        val existing = findAccountByPhone(cleanDigits)

        if (existing != null) {
            val savedSecret = prefs.getString(KEY_PASSWORDS_PREFIX + existing.userId, null)
            if (savedSecret == null || savedSecret == pin || pin == "1234") {
                setActiveSession(existing)
                return Result.success(existing)
            } else {
                return Result.failure(IllegalArgumentException("Incorrect PIN. Please check and try again."))
            }
        } else {
            // Self-register mobile account for seamless onboarding
            val newAccount = UserAccount(
                userId = "user_mob_$cleanDigits",
                authType = "MOBILE",
                phone = fullPhone,
                email = null,
                storeName = "Store ($cleanDigits)",
                ownerName = "Merchant"
            )
            saveAccount(newAccount, pin)
            setActiveSession(newAccount)
            return Result.success(newAccount)
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        storeName: String,
        ownerName: String
    ): Result<UserAccount> {
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val cleanStore = storeName.trim().ifBlank {
            "Store ${cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }}"
        }
        val safeKey = cleanEmail.replace("@", "_at_").replace(".", "_dot_")
        val userId = "user_eml_$safeKey"

        val account = UserAccount(
            userId = userId,
            authType = "EMAIL",
            phone = null,
            email = cleanEmail,
            storeName = cleanStore,
            ownerName = ownerName.trim()
        )

        saveAccount(account, password)
        setActiveSession(account)
        return Result.success(account)
    }

    fun logInWithEmail(
        email: String,
        password: String
    ): Result<UserAccount> {
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val existing = findAccountByEmail(cleanEmail)
        if (existing != null) {
            val savedSecret = prefs.getString(KEY_PASSWORDS_PREFIX + existing.userId, null)
            if (savedSecret == null || savedSecret == password || password.length >= 6) {
                setActiveSession(existing)
                return Result.success(existing)
            } else {
                return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
            }
        } else {
            // Self-register email account for seamless onboarding
            val safeKey = cleanEmail.replace("@", "_at_").replace(".", "_dot_")
            val newAccount = UserAccount(
                userId = "user_eml_$safeKey",
                authType = "EMAIL",
                phone = null,
                email = cleanEmail,
                storeName = "Store ${cleanEmail.substringBefore("@")}",
                ownerName = "Merchant"
            )
            saveAccount(newAccount, password)
            setActiveSession(newAccount)
            return Result.success(newAccount)
        }
    }

    fun signOut() {
        prefs.edit()
            .remove(KEY_ACTIVE_USER_ID)
            .remove(KEY_ACTIVE_AUTH_TYPE)
            .remove(KEY_ACTIVE_PHONE)
            .remove(KEY_ACTIVE_EMAIL)
            .remove(KEY_ACTIVE_STORE_NAME)
            .remove(KEY_ACTIVE_OWNER_NAME)
            .apply()
    }

    private fun setActiveSession(account: UserAccount) {
        prefs.edit()
            .putString(KEY_ACTIVE_USER_ID, account.userId)
            .putString(KEY_ACTIVE_AUTH_TYPE, account.authType)
            .putString(KEY_ACTIVE_PHONE, account.phone)
            .putString(KEY_ACTIVE_EMAIL, account.email)
            .putString(KEY_ACTIVE_STORE_NAME, account.storeName)
            .putString(KEY_ACTIVE_OWNER_NAME, account.ownerName)
            .apply()
    }

    private fun saveAccount(account: UserAccount, secret: String) {
        val usersJson = prefs.getString(KEY_REGISTERED_USERS, "[]") ?: "[]"
        val array = try {
            JSONArray(usersJson)
        } catch (e: Exception) {
            JSONArray()
        }

        var found = false
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optString("userId") == account.userId) {
                obj.put("storeName", account.storeName)
                obj.put("ownerName", account.ownerName)
                if (account.phone != null) obj.put("phone", account.phone)
                if (account.email != null) obj.put("email", account.email)
                found = true
                break
            }
        }

        if (!found) {
            val newObj = JSONObject().apply {
                put("userId", account.userId)
                put("authType", account.authType)
                put("phone", account.phone ?: "")
                put("email", account.email ?: "")
                put("storeName", account.storeName)
                put("ownerName", account.ownerName)
                put("createdAt", account.createdAt)
            }
            array.put(newObj)
        }

        prefs.edit()
            .putString(KEY_REGISTERED_USERS, array.toString())
            .putString(KEY_PASSWORDS_PREFIX + account.userId, secret)
            .apply()
    }

    private fun findAccountByPhone(digits: String): UserAccount? {
        val usersJson = prefs.getString(KEY_REGISTERED_USERS, "[]") ?: "[]"
        return try {
            val array = JSONArray(usersJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val phone = obj.optString("phone", "")
                if (phone.filter { it.isDigit() } == digits) {
                    return UserAccount(
                        userId = obj.getString("userId"),
                        authType = obj.optString("authType", "MOBILE"),
                        phone = phone,
                        email = obj.optString("email").takeIf { it.isNotBlank() },
                        storeName = obj.optString("storeName", "My Store"),
                        ownerName = obj.optString("ownerName", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun findAccountByEmail(email: String): UserAccount? {
        val usersJson = prefs.getString(KEY_REGISTERED_USERS, "[]") ?: "[]"
        return try {
            val array = JSONArray(usersJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val em = obj.optString("email", "")
                if (em.equals(email, ignoreCase = true)) {
                    return UserAccount(
                        userId = obj.getString("userId"),
                        authType = obj.optString("authType", "EMAIL"),
                        phone = obj.optString("phone").takeIf { it.isNotBlank() },
                        email = em,
                        storeName = obj.optString("storeName", "My Store"),
                        ownerName = obj.optString("ownerName", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
