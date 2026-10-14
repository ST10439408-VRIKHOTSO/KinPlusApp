package za.co.kinplus.app.ui.screens.auth

import android.content.Context
import za.co.kinplus.app.R

/** Maps auth and validation error codes from the ViewModels to localised, user friendly text. */
fun mapAuthMessage(context: Context, code: String): String = when (code) {
    "INVALID_EMAIL" -> context.getString(R.string.error_invalid_email)
    "WEAK_PASSWORD" -> context.getString(R.string.error_weak_password)
    "PASSWORD_MISMATCH" -> context.getString(R.string.error_password_mismatch)
    "NAME_REQUIRED" -> context.getString(R.string.error_name_required)
    "INVALID_PHONE" -> context.getString(R.string.error_invalid_phone)
    "CONSENT_REQUIRED" -> context.getString(R.string.error_consent_required)
    "AUTH_FAILED" -> context.getString(R.string.error_auth_failed)
    "GOOGLE_NOT_CONFIGURED" -> context.getString(R.string.error_google_not_configured)
    "GOOGLE_NO_ACCOUNT" -> context.getString(R.string.error_google_no_account)
    "GOOGLE_FAILED" -> context.getString(R.string.error_google_failed)
    else -> code // Firebase already returns a readable message for most failures.
}
