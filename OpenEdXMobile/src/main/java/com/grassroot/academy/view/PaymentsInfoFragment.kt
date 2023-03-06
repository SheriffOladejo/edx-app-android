package com.grassroot.academy.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.grassroot.academy.R
import com.grassroot.academy.base.BaseFragment
import com.grassroot.academy.databinding.FragmentPaymentsInfoBinding
import com.grassroot.academy.model.api.CourseUpgradeResponse
import com.grassroot.academy.model.api.EnrolledCoursesResponse
import com.grassroot.academy.util.DateUtil
import com.grassroot.academy.util.ResourceUtil
import java.text.SimpleDateFormat
import java.util.*

class PaymentsInfoFragment : BaseFragment() {

    private lateinit var binding: FragmentPaymentsInfoBinding

    companion object {
        fun newInstance(extras: Bundle): PaymentsInfoFragment {
            val fragment = PaymentsInfoFragment()
            fragment.arguments = extras
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaymentsInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = view.context
        binding.btnClose.setOnClickListener { activity?.finish() }
        val courseData =
            arguments?.getSerializable(Router.EXTRA_COURSE_DATA) as EnrolledCoursesResponse

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
        val stringBuilder: StringBuilder = java.lang.StringBuilder()

        // Populate access expires content
        if (!android.text.TextUtils.isEmpty(courseData.auditAccessExpires)) {
            val expiryDate = DateUtil.convertToDate(courseData.auditAccessExpires)
            binding.tvAuditAccessExpiresOn.text = ResourceUtil.getFormattedString(
                context.resources, R.string.audit_access_expires_on, "date",
                dateFormat.format(expiryDate)
            )
            stringBuilder.append(
                ResourceUtil.getFormattedString(
                    context.resources,
                    R.string.audit_access_expires_details,
                    "date",
                    dateFormat.format(expiryDate)
                ).toString()
            )
        }
        // Populate upgrade deadline content
        if (!android.text.TextUtils.isEmpty(courseData.course.dynamicUpgradeDeadline)) {
            val upgradeBy = DateUtil.convertToDate(courseData.course.dynamicUpgradeDeadline)
            stringBuilder.append("\n\n").append(
                ResourceUtil.getFormattedString(
                    context.resources,
                    R.string.upgrade_deadline_details,
                    "date",
                    dateFormat.format(upgradeBy)
                ).toString()
            )
        }

        binding.tvAuditAccessExpiresDetails.text = stringBuilder.toString()

        val courseUpgradeData =
            arguments?.getParcelable<CourseUpgradeResponse>(Router.EXTRA_COURSE_UPGRADE_DATA) as CourseUpgradeResponse
        PaymentsBannerFragment.loadPaymentsBannerFragment(
            R.id.fragment_container, courseData, null,
            courseUpgradeData, false, childFragmentManager, false
        )
    }
}
