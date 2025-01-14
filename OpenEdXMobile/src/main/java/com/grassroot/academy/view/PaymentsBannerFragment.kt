package com.grassroot.academy.view

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import dagger.hilt.android.AndroidEntryPoint
import com.grassroot.academy.R
import com.grassroot.academy.base.BaseFragment
import com.grassroot.academy.core.IEdxEnvironment
import com.grassroot.academy.databinding.FragmentPaymentsBannerBinding
import com.grassroot.academy.model.api.CourseUpgradeResponse
import com.grassroot.academy.model.api.EnrolledCoursesResponse
import com.grassroot.academy.model.course.CourseComponent
import javax.inject.Inject

@AndroidEntryPoint
class PaymentsBannerFragment : BaseFragment() {

    private lateinit var binding: FragmentPaymentsBannerBinding

    @Inject
    lateinit var environment: IEdxEnvironment

    companion object {
        private const val EXTRA_SHOW_INFO_BUTTON = "show_info_button"
        private fun newInstance(
            courseData: EnrolledCoursesResponse,
            courseUnit: CourseComponent?,
            courseUpgradeData: CourseUpgradeResponse,
            showInfoButton: Boolean
        ): Fragment {
            val fragment = PaymentsBannerFragment()
            val bundle = Bundle()
            bundle.putSerializable(Router.EXTRA_COURSE_DATA, courseData)
            bundle.putSerializable(Router.EXTRA_COURSE_UNIT, courseUnit)
            bundle.putParcelable(Router.EXTRA_COURSE_UPGRADE_DATA, courseUpgradeData)
            bundle.putBoolean(EXTRA_SHOW_INFO_BUTTON, showInfoButton)
            fragment.arguments = bundle
            return fragment
        }

        fun loadPaymentsBannerFragment(
            containerId: Int,
            courseData: EnrolledCoursesResponse,
            courseUnit: CourseComponent?,
            courseUpgradeData: CourseUpgradeResponse, showInfoButton: Boolean,
            childFragmentManager: FragmentManager, animate: Boolean
        ) {
            val frag: Fragment? = childFragmentManager.findFragmentByTag("payment_banner_frag")
            if (frag != null) {
                // Payment banner already exists
                return
            }
            val fragment: Fragment =
                newInstance(courseData, courseUnit, courseUpgradeData, showInfoButton)
            // This activity will only ever hold this lone fragment, so we
            // can afford to retain the instance during activity recreation
            val fragmentTransaction: FragmentTransaction = childFragmentManager.beginTransaction()
            if (animate) {
                fragmentTransaction.setCustomAnimations(R.anim.slide_up, android.R.anim.fade_out)
            }
            fragmentTransaction.replace(containerId, fragment, "payment_banner_frag")
            fragmentTransaction.disallowAddToBackStack()
            fragmentTransaction.commitAllowingStateLoss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaymentsBannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateCourseUpgradeBanner(view.context)
    }

    private fun populateCourseUpgradeBanner(context: Context) {
        val courseUpgradeData: CourseUpgradeResponse =
            arguments?.getParcelable<CourseUpgradeResponse>(Router.EXTRA_COURSE_UPGRADE_DATA) as CourseUpgradeResponse
        val courseData: EnrolledCoursesResponse =
            arguments?.getSerializable(Router.EXTRA_COURSE_DATA) as EnrolledCoursesResponse
        val showInfoButton: Boolean = arguments?.getBoolean(EXTRA_SHOW_INFO_BUTTON) ?: false
        binding.upgradeToVerifiedFooter.visibility = View.VISIBLE
        if (showInfoButton) {
            binding.info.visibility = View.VISIBLE
            binding.info.setOnClickListener {
                environment.router.showPaymentsInfoActivity(
                    context,
                    courseData,
                    courseUpgradeData
                )
            }
        } else {
            binding.info.visibility = View.GONE
        }
        if (!TextUtils.isEmpty(courseUpgradeData.price)) {
            binding.tvUpgradePrice.text = courseUpgradeData.price
        } else {
            binding.tvUpgradePrice.visibility = View.GONE
        }

        val courseUnit: CourseComponent? =
            arguments?.getSerializable(Router.EXTRA_COURSE_UNIT) as CourseComponent?

        courseUpgradeData.basketUrl?.let { basketUrl ->
            binding.llUpgradeButton.setOnClickListener {
                environment.router?.showCourseUpgradeWebViewActivity(
                    context, basketUrl, courseData, courseUnit
                )
            }
        }
    }
}
