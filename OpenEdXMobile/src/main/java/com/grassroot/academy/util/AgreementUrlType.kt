package com.grassroot.academy.util

import com.grassroot.academy.R

/**
 * This enum defines the URL type of Agreement
 */
enum class AgreementUrlType {
    EULA, TOS, PRIVACY_POLICY, COOKIE_POLICY, DATA_CONSENT;

    /**
     * @return The string resource's ID if it's a valid enum inside [AgreementUrlType].
     */
    fun getStringResId(): Int? {
        return when (this) {
            EULA -> R.string.eula_file_link
            TOS -> R.string.terms_file_link
            PRIVACY_POLICY -> R.string.privacy_file_link
            COOKIE_POLICY -> null
            DATA_CONSENT -> null
        }
    }
}
