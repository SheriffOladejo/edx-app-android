package com.grassroot.academy.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import com.grassroot.academy.R
import com.grassroot.academy.databinding.FragmentLockedCourseUnitBinding
import com.grassroot.academy.model.api.CourseUpgradeResponse
import com.grassroot.academy.model.api.EnrolledCoursesResponse
import com.grassroot.academy.model.course.CourseComponent
import com.grassroot.academy.module.analytics.Analytics
import com.grassroot.academy.module.analytics.AnalyticsRegistry
import javax.inject.Inject

@AndroidEntryPoint
class LockedCourseUnitFragment : CourseUnitFragment() {

    @Inject
    lateinit var analyticsRegistry: AnalyticsRegistry

    companion object {
        @JvmStatic
        fun newInstance(
            unit: CourseComponent,
            courseData: EnrolledCoursesResponse,
            courseUpgradeData: CourseUpgradeResponse
        ): LockedCourseUnitFragment {
            val fragment = LockedCourseUnitFragment()
            val bundle = Bundle()
            bundle.putSerializable(Router.EXTRA_COURSE_UNIT, unit)
            bundle.putSerializable(Router.EXTRA_COURSE_DATA, courseData)
            bundle.putParcelable(Router.EXTRA_COURSE_UPGRADE_DATA, courseUpgradeData)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLockedCourseUnitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val courseUpgradeData =
            arguments?.getParcelable<CourseUpgradeResponse>(Router.EXTRA_COURSE_UPGRADE_DATA) as CourseUpgradeResponse
        val courseData =
            arguments?.getSerializable(Router.EXTRA_COURSE_DATA) as EnrolledCoursesResponse
        loadPaymentBannerFragment(courseData, courseUpgradeData)
        analyticsRegistry.trackScreenView(Analytics.Screens.COURSE_UNIT_LOCKED)
    }

    private fun loadPaymentBannerFragment(
        courseData: EnrolledCoursesResponse,
        courseUpgradeData: CourseUpgradeResponse
    ) {
        PaymentsBannerFragment.loadPaymentsBannerFragment(
            R.id.fragment_container, courseData, unit,
            courseUpgradeData, false, childFragmentManager, false
        )
    }
}
