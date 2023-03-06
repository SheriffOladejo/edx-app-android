package com.grassroot.academy.deeplink

import android.app.Activity
import com.grassroot.academy.logger.Logger
import org.json.JSONObject

object BranchLinkManager {
    val logger: Logger = Logger(this.javaClass)
    const val KEY_CLICKED_BRANCH_LINK = "+clicked_branch_link"

    fun checkAndReactIfReceivedLink(activity: Activity, paramsJson: JSONObject) {
        logger.debug("DeepLink received. JSON Details:\n$paramsJson")
        val screenName = paramsJson.optString(DeepLink.Keys.SCREEN_NAME)
        if (screenName.isNotEmpty()) {
            DeepLinkManager.onDeepLinkReceived(activity, DeepLink(screenName, paramsJson))
        }
    }
}
