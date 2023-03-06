package com.grassroot.academy.event

import com.grassroot.academy.model.iap.IAPFlowData

class IAPFlowEvent(
    val flowAction: IAPFlowData.IAPAction,
    val iapFlowData: IAPFlowData? = null
) : BaseEvent()
