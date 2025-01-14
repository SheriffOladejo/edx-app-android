package com.grassroot.academy.exception

import com.grassroot.academy.http.HttpStatusException
import com.grassroot.academy.util.InAppPurchasesException

/**
 * Error handling model class for ViewModel
 * handle exceptions/error based on requestType
 */
data class ErrorMessage(
    val requestType: Int = 0,
    val throwable: Throwable
) {
    companion object {
        // Custom Codes for request types
        const val BANNER_INFO_CODE = 101
        const val COURSE_RESET_DATES_CODE = 102
        const val COURSE_DATES_CODE = 103

        const val ADD_TO_BASKET_CODE = 0x201
        const val CHECKOUT_CODE = 0x202
        const val EXECUTE_ORDER_CODE = 0x203
        const val PAYMENT_SDK_CODE = 0x204
        const val COURSE_REFRESH_CODE = 0x205
        const val PRICE_CODE = 0x206
    }

    private fun isPreUpgradeErrorType(): Boolean = requestType == PRICE_CODE ||
            requestType == ADD_TO_BASKET_CODE || requestType == CHECKOUT_CODE ||
            requestType == PAYMENT_SDK_CODE

    fun isPostUpgradeErrorType(): Boolean = !isPreUpgradeErrorType()

    fun getHttpErrorCode(): Int {
        if (throwable is InAppPurchasesException) {
            return throwable.httpErrorCode
        } else if (throwable is HttpStatusException) {
            return throwable.statusCode
        }
        return -1
    }

    fun getErrorMessage(): String? {
        if (throwable is InAppPurchasesException) {
            return throwable.errorMessage
        }
        return throwable.message
    }

    fun canRetry(): Boolean {
        return requestType == PRICE_CODE ||
                requestType == EXECUTE_ORDER_CODE ||
                requestType == COURSE_REFRESH_CODE
    }
}
