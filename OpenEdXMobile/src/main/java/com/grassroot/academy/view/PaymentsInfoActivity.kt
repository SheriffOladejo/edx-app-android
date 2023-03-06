package com.grassroot.academy.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import com.grassroot.academy.base.BaseSingleFragmentActivity
import com.grassroot.academy.event.CourseUpgradedEvent
import com.grassroot.academy.model.api.CourseUpgradeResponse
import com.grassroot.academy.model.api.EnrolledCoursesResponse
import com.grassroot.academy.module.analytics.Analytics
import com.grassroot.academy.view.Router.EXTRA_COURSE_DATA
import com.grassroot.academy.view.Router.EXTRA_COURSE_UPGRADE_DATA
import org.greenrobot.eventbus.Subscribe

@AndroidEntryPoint
class PaymentsInfoActivity : BaseSingleFragmentActivity() {
    companion object {
        fun newIntent(
            context: Context,
            courseData: EnrolledCoursesResponse,
            courseUpgrade: CourseUpgradeResponse
        ): Intent {
            val intent = Intent(context, PaymentsInfoActivity::class.java)
            intent.putExtra(EXTRA_COURSE_DATA, courseData)
            intent.putExtra(EXTRA_COURSE_UPGRADE_DATA, courseUpgrade)
            return intent
        }
    }

    override fun getToolbarLayoutId(): Int {
        return -1
    }

    override fun getFirstFragment(): Fragment {
        val extras = intent.extras
        if (extras == null) {
            throw IllegalArgumentException()
        } else {
            return PaymentsInfoFragment.newInstance(extras)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        environment.analyticsRegistry.trackScreenView(Analytics.Screens.PAYMENTS_INFO_SCREEN)
    }

    @Subscribe
    @Suppress("UNUSED_PARAMETER")
    fun onEvent(event: CourseUpgradedEvent) {
        finish()
    }
}
