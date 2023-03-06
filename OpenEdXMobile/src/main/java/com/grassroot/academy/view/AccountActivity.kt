package com.grassroot.academy.view

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import com.grassroot.academy.base.BaseSingleFragmentActivity
import com.grassroot.academy.deeplink.ScreenDef

@AndroidEntryPoint
class AccountActivity : BaseSingleFragmentActivity() {

    companion object {
        @JvmStatic
        fun newIntent(activity: Context?, @ScreenDef screenName: String?): Intent {
            val intent = Intent(activity, AccountActivity::class.java)
            intent.putExtra(Router.EXTRA_SCREEN_NAME, screenName)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            return intent
        }
    }

    override fun getFirstFragment(): Fragment {
        return AccountFragment.newInstance(intent.extras)
    }
}
