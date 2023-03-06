package com.grassroot.academy.event

import android.os.Bundle
import com.grassroot.academy.deeplink.ScreenDef
import com.grassroot.academy.view.Router

/**
 * Event fired to pass the arguments ([Bundle]) to a Fragment inside a ViewPager
 */
class ScreenArgumentsEvent(val bundle: Bundle) {
    companion object {
        fun getNewInstance(@ScreenDef screenName: String): ScreenArgumentsEvent {
            val bundle = Bundle()
            bundle.putString(Router.EXTRA_SCREEN_NAME, screenName)
            return ScreenArgumentsEvent(bundle)
        }
    }
}
