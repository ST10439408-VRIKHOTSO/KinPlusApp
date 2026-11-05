package za.co.kinplus.app.ui.screens.auth

import android.content.Context
import za.co.kinplus.app.R

/**
 * Maps auth and validation error codes from the ViewModels to localised, user
 * friendly text. Firebase's own error messages are English only, so they are
 * never shown directly: an unknown code falls back to a general message.
 */
fun mapAuthMessage(context: Context, code: String): String = when (code) {
    "INVALID_EMAIL" -> context.getString(R.string.error_invalid_email)
    "EMAIL_REQUIRED" -> context.getString(R.string.error_email_required)
    "PASSWORD_REQUIRED" -> context.getString(R.string.error_password_required)
    "WEAK_PASSWORD" -> context.getString(R.string.error_weak_password)
    "PASSWORD_MISMATCH" -> context.getString(R.string.error_password_mismatch)
    "NAME_REQUIRED" -> context.getString(R.string.error_name_required)
    "INVALID_PHONE" -> context.getString(R.string.error_invalid_phone)
    "CONSENT_REQUIRED" -> context.getString(R.string.error_consent_required)
    "WRONG_CREDENTIALS" -> context.getString(R.string.error_wrong_credentials)
    "EMAIL_IN_USE" -> context.getString(R.string.error_email_in_use)
    "NETWORK" -> context.getString(R.string.error_network)
    "TOO_MANY_ATTEMPTS" -> context.getString(R.string.error_too_many_attempts)
    "GOOGLE_NOT_CONFIGURED" -> context.getString(R.string.error_google_not_configured)
    "GOOGLE_NO_ACCOUNT" -> context.getString(R.string.error_google_no_account)
    "GOOGLE_FAILED" -> context.getString(R.string.error_google_failed)
    else -> context.getString(R.string.error_auth_failed)
}
