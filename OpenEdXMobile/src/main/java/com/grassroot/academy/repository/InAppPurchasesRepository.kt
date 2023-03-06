package com.grassroot.academy.repository

import com.grassroot.academy.extenstion.toInAppPurchasesResult
import com.grassroot.academy.http.model.NetworkResponseCallback
import com.grassroot.academy.http.model.Result
import com.grassroot.academy.inapppurchases.InAppPurchasesAPI
import com.grassroot.academy.model.iap.AddToBasketResponse
import com.grassroot.academy.model.iap.CheckoutResponse
import com.grassroot.academy.model.iap.ExecuteOrderResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InAppPurchasesRepository(private var iapAPI: InAppPurchasesAPI) {

    fun addToBasket(productId: String, callback: NetworkResponseCallback<AddToBasketResponse>) {
        iapAPI.addToBasket(productId).enqueue(object : Callback<AddToBasketResponse> {
            override fun onResponse(
                call: Call<AddToBasketResponse>,
                response: Response<AddToBasketResponse>
            ) {
                response.toInAppPurchasesResult(callback)
            }

            override fun onFailure(call: Call<AddToBasketResponse>, t: Throwable) {
                callback.onError(Result.Error(t))
            }
        })
    }

    fun proceedCheckout(basketId: Long, callback: NetworkResponseCallback<CheckoutResponse>) {
        iapAPI.proceedCheckout(basketId = basketId).enqueue(object : Callback<CheckoutResponse> {
            override fun onResponse(
                call: Call<CheckoutResponse>,
                response: Response<CheckoutResponse>
            ) {
                response.toInAppPurchasesResult(callback)
            }

            override fun onFailure(call: Call<CheckoutResponse>, t: Throwable) {
                callback.onError(Result.Error(t))
            }
        })
    }

    fun executeOrder(
        basketId: Long,
        productId: String,
        purchaseToken: String,
        callback: NetworkResponseCallback<ExecuteOrderResponse>
    ) {
        iapAPI.executeOrder(
            basketId = basketId,
            productId = productId,
            purchaseToken = purchaseToken
        ).enqueue(object : Callback<ExecuteOrderResponse> {
            override fun onResponse(
                call: Call<ExecuteOrderResponse>,
                response: Response<ExecuteOrderResponse>
            ) {
                response.toInAppPurchasesResult(callback)
            }

            override fun onFailure(call: Call<ExecuteOrderResponse>, t: Throwable) {
                callback.onError(Result.Error(t))
            }
        })
    }
}
