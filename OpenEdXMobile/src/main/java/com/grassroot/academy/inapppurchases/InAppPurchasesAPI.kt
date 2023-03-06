package com.grassroot.academy.inapppurchases

import com.grassroot.academy.http.constants.ApiConstants
import com.grassroot.academy.model.iap.AddToBasketResponse
import com.grassroot.academy.model.iap.CheckoutResponse
import com.grassroot.academy.model.iap.ExecuteOrderResponse
import retrofit2.Call
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppPurchasesAPI @Inject constructor(private val iapService: InAppPurchasesService) {

    fun addToBasket(productId: String): Call<AddToBasketResponse> {
        return iapService.addToBasket(productId)
    }

    fun proceedCheckout(basketId: Long): Call<CheckoutResponse> {
        return iapService.proceedCheckout(
            basketId = basketId,
            paymentProcessor = ApiConstants.PAYMENT_PROCESSOR
        )
    }

    fun executeOrder(
        basketId: Long,
        productId: String,
        purchaseToken: String
    ): Call<ExecuteOrderResponse> {
        return iapService.executeOrder(
            basketId = basketId,
            productId = productId,
            paymentProcessor = ApiConstants.PAYMENT_PROCESSOR,
            purchaseToken = purchaseToken
        )
    }
}
