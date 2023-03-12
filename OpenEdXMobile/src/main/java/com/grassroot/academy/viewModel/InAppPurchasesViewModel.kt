package com.grassroot.academy.viewModel

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import com.grassroot.academy.exception.ErrorMessage
import com.grassroot.academy.extenstion.decodeToLong
import com.grassroot.academy.http.model.NetworkResponseCallback
import com.grassroot.academy.http.model.Result
import com.grassroot.academy.inapppurchases.BillingProcessor
import com.grassroot.academy.model.api.EnrolledCoursesResponse
import com.grassroot.academy.model.api.getAuditCourses
import com.grassroot.academy.model.iap.AddToBasketResponse
import com.grassroot.academy.model.iap.CheckoutResponse
import com.grassroot.academy.model.iap.ExecuteOrderResponse
import com.grassroot.academy.model.iap.IAPFlowData
import com.grassroot.academy.module.analytics.Analytics
import com.grassroot.academy.module.analytics.InAppPurchasesAnalytics
import com.grassroot.academy.repository.InAppPurchasesRepository
import com.grassroot.academy.util.InAppPurchasesException
import com.grassroot.academy.util.InAppPurchasesUtils
import com.grassroot.academy.util.observer.Event
import com.grassroot.academy.util.observer.postEvent
import javax.inject.Inject

@HiltViewModel
class InAppPurchasesViewModel @Inject constructor(
    private val billingProcessor: BillingProcessor,
    private val repository: InAppPurchasesRepository,
    private val iapAnalytics: InAppPurchasesAnalytics
) : ViewModel() {

    private val _productPrice = MutableLiveData<Event<SkuDetails>>()
    val productPrice: LiveData<Event<SkuDetails>> = _productPrice

    private val _launchPurchaseFlow = MutableLiveData<Event<Boolean>>()
    val launchPurchaseFlow: LiveData<Event<Boolean>> = _launchPurchaseFlow

    private val _productPurchased = MutableLiveData<Event<IAPFlowData>>()
    val productPurchased: LiveData<Event<IAPFlowData>> = _productPurchased

    private val _refreshCourseData = MutableLiveData<Event<IAPFlowData>>()
    val refreshCourseData: LiveData<Event<IAPFlowData>> = _refreshCourseData

    private val _fakeUnfulfilledCompletion = MutableLiveData<Event<Boolean>>()
    val fakeUnfulfilledCompletion: LiveData<Event<Boolean>> = _fakeUnfulfilledCompletion

    private val _showLoader = MutableLiveData<Event<Boolean>>()
    val showLoader: LiveData<Event<Boolean>> = _showLoader

    private val _errorMessage = MutableLiveData<Event<ErrorMessage>>()
    val errorMessage: LiveData<Event<ErrorMessage>> = _errorMessage

    var iapFlowData = IAPFlowData()
    private var incompletePurchases: MutableList<IAPFlowData> = arrayListOf()

    private val listener = object : BillingProcessor.BillingFlowListeners {
        override fun onPurchaseCancel(responseCode: Int, message: String) {
            super.onPurchaseCancel(responseCode, message)
            dispatchError(
                requestType = ErrorMessage.PAYMENT_SDK_CODE,
                throwable = InAppPurchasesException(responseCode, message)
            )
            endLoading()
        }

        override fun onPurchaseComplete(purchase: Purchase) {
            super.onPurchaseComplete(purchase)
            iapFlowData.purchaseToken = purchase.purchaseToken
            _productPurchased.postEvent(iapFlowData)
            iapAnalytics.trackIAPEvent(eventName = Analytics.Events.IAP_PAYMENT_TIME)
            iapAnalytics.initUnlockContentTime()
        }
    }

    init {
        billingProcessor.setUpBillingFlowListeners(listener)
        billingProcessor.startConnection()
    }

    fun initializeProductPrice(courseSku: String?) {
        iapAnalytics.initPriceTime()
        courseSku?.let {
            billingProcessor.querySyncDetails(
                productId = courseSku
            ) { billingResult, skuDetails ->
                val skuDetail = skuDetails?.get(0)
                skuDetail?.let {
                    if (it.sku == courseSku) {
                        _productPrice.postEvent(it)
                        iapAnalytics.setPrice(skuDetail.price)
                        iapAnalytics.trackIAPEvent(Analytics.Events.IAP_LOAD_PRICE_TIME)
                    }
                } ?: dispatchError(
                    requestType = ErrorMessage.PRICE_CODE,
                    throwable = InAppPurchasesException(
                        httpErrorCode = billingResult.responseCode,
                        errorMessage = billingResult.debugMessage,
                    )
                )
            }
        } ?: dispatchError(requestType = ErrorMessage.PRICE_CODE)
    }

    fun startPurchaseFlow(productId: String) {
        iapFlowData.productId = productId
        iapFlowData.flowType = IAPFlowData.IAPFlowType.USER_INITIATED
        iapFlowData.isVerificationPending = true
        addProductToBasket()
    }

    private fun addProductToBasket() {
        startLoading()
        repository.addToBasket(
            productId = iapFlowData.productId,
            callback = object : NetworkResponseCallback<AddToBasketResponse> {
                override fun onSuccess(result: Result.Success<AddToBasketResponse>) {
                    result.data?.let {
                        iapFlowData.basketId = it.basketId
                        proceedCheckout()
                    } ?: endLoading()
                }

                override fun onError(error: Result.Error) {
                    dispatchError(
                        requestType = ErrorMessage.ADD_TO_BASKET_CODE,
                        throwable = error.throwable
                    )
                    endLoading()
                }
            })
    }

    fun proceedCheckout() {
        repository.proceedCheckout(
            basketId = iapFlowData.basketId,
            callback = object : NetworkResponseCallback<CheckoutResponse> {
                override fun onSuccess(result: Result.Success<CheckoutResponse>) {
                    result.data?.let {
                        if (iapFlowData.flowType.isSilentMode()) {
                            executeOrder(iapFlowData)
                        } else {
                            iapAnalytics.initPaymentTime()
                            _launchPurchaseFlow.postEvent(true)
                        }
                    } ?: endLoading()
                }

                override fun onError(error: Result.Error) {
                    dispatchError(
                        requestType = ErrorMessage.CHECKOUT_CODE,
                        throwable = error.throwable
                    )
                    endLoading()
                }
            })
    }

    fun purchaseItem(activity: Activity, userId: Long, courseSku: String?) {
        courseSku?.let {
            billingProcessor.purchaseItem(activity, courseSku, userId)
        }
    }

    fun executeOrder(iapFlowData: IAPFlowData? = this.iapFlowData) {
        iapFlowData?.let { iapData ->
            iapData.isVerificationPending = false
            this.iapFlowData = iapData
            repository.executeOrder(
                basketId = iapData.basketId,
                productId = iapData.productId,
                purchaseToken = iapData.purchaseToken,
                callback = object : NetworkResponseCallback<ExecuteOrderResponse> {
                    override fun onSuccess(result: Result.Success<ExecuteOrderResponse>) {
                        result.data?.let {
                            iapData.isVerificationPending = false
                            if (iapFlowData.flowType.isSilentMode()) {
                                markPurchaseComplete(iapData)
                            } else {
                                _refreshCourseData.postEvent(iapData)
                            }
                        }
                        endLoading()
                    }

                    override fun onError(error: Result.Error) {
                        dispatchError(
                            requestType = ErrorMessage.EXECUTE_ORDER_CODE,
                            throwable = error.throwable
                        )
                        endLoading()
                    }
                })
        }
    }

    /**
     * To detect and handle courses which are purchased but still not Verified
     *
     * @param userId          id of the user to detect un full filled purchases and process.
     * @param enrolledCourses user enrolled courses in the platform.
     * @param flowType        [IAPFlowData.IAPFlowType] of payment
     * @param screenName      name of the screen from where the flow is initiated
     */
    fun detectUnfulfilledPurchase(
        userId: Long,
        enrolledCourses: List<EnrolledCoursesResponse>,
        flowType: IAPFlowData.IAPFlowType,
        screenName: String
    ) {
        val auditCourses = enrolledCourses.getAuditCourses()
        if (auditCourses.isEmpty()) {
            _fakeUnfulfilledCompletion.postEvent(true)
            return
        }
        billingProcessor.queryPurchase { _, purchases ->
            if (purchases.isEmpty()) {
                _fakeUnfulfilledCompletion.postEvent(true)
                return@queryPurchase
            }
            val userPurchases = purchases.filter {
                it.accountIdentifiers?.obfuscatedAccountId?.decodeToLong() == userId
            }
            if (userPurchases.isNotEmpty()) {
                incompletePurchases =
                    InAppPurchasesUtils.getInCompletePurchases(
                        auditCourses,
                        userPurchases,
                        flowType,
                        screenName
                    )
                if (incompletePurchases.isEmpty()) {
                    _fakeUnfulfilledCompletion.postEvent(true)
                } else {
                    startUnfulfilledVerification()
                }
            } else {
                _fakeUnfulfilledCompletion.postEvent(true)
            }
        }
    }

    /**
     * Method to start the process to verify the un full filled courses skus
     */
    private fun startUnfulfilledVerification() {
        iapFlowData.clear()
        iapFlowData = incompletePurchases[0]
        // will perform verification as part of unfulfilled flow
        iapFlowData.isVerificationPending = false
        iapAnalytics.initCourseValues(
            courseId = iapFlowData.courseId,
            isSelfPaced = iapFlowData.isCourseSelfPaced,
            flowType = iapFlowData.flowType.value(),
            screenName = iapFlowData.screenName
        )
        iapAnalytics.trackIAPEvent(Analytics.Events.IAP_UNFULFILLED_PURCHASE_INITIATED)
        addProductToBasket()
    }

    private fun markPurchaseComplete(iapFlowData: IAPFlowData) {
        incompletePurchases = incompletePurchases.drop(1).toMutableList()
        if (incompletePurchases.isEmpty()) {
            _refreshCourseData.postEvent(iapFlowData)
        } else {
            startUnfulfilledVerification()
        }
    }

    fun dispatchError(
        requestType: Int = 0,
        errorMessage: String? = null,
        throwable: Throwable? = null
    ) {
        var actualErrorMessage = errorMessage
        throwable?.let {
            if (errorMessage.isNullOrBlank()) {
                actualErrorMessage = it.message
            }
        }
        if (throwable != null && throwable is InAppPurchasesException) {
            _errorMessage.postEvent(ErrorMessage(requestType = requestType, throwable = throwable))
        } else {
            _errorMessage.postEvent(
                ErrorMessage(
                    requestType = requestType,
                    throwable = InAppPurchasesException(errorMessage = actualErrorMessage)
                )
            )
        }
    }

    private fun startLoading() {
        _showLoader.postEvent(true)
    }

    fun endLoading() {
        _showLoader.postEvent(false)
    }
}
