package org.edx.mobile.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.launch
import org.edx.mobile.R
import org.edx.mobile.databinding.FragmentCourseUnitGradeBinding
import org.edx.mobile.extenstion.isNotVisible
import org.edx.mobile.extenstion.setImageDrawable
import org.edx.mobile.extenstion.setVisibility
import org.edx.mobile.inapppurchases.BillingProcessor
import org.edx.mobile.inapppurchases.BillingProcessor.BillingFlowListeners
import org.edx.mobile.model.api.AuthorizationDenialReason
import org.edx.mobile.model.course.CourseComponent
import org.edx.mobile.util.BrowserUtil
import org.edx.mobile.view.dialog.AlertDialogFragment

class CourseUnitMobileNotSupportedFragment : CourseUnitFragment() {
    private lateinit var binding: FragmentCourseUnitGradeBinding
    private var billingProcessor: BillingProcessor? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCourseUnitGradeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (AuthorizationDenialReason.FEATURE_BASED_ENROLLMENTS == unit?.authorizationDenialReason) {
            if (environment.remoteFeaturePrefs.isValuePropEnabled()) {
                showGradedContent()
            } else {
                showNotAvailableOnMobile(isLockedContent = true)
            }
        } else {
            showNotAvailableOnMobile(isLockedContent = false)
        }
    }

    private fun showGradedContent() {
        val isSelfPaced = getBooleanArgument(Router.EXTRA_IS_SELF_PACED, false)
        val price = getStringArgument(Router.EXTRA_PRICE)
        binding.containerLayoutNotAvailable.setVisibility(false)
        binding.llGradedContentLayout.setVisibility(true)
        setUpUpgradeButton(isSelfPaced, price)

        binding.toggleShow.setOnClickListener {
            val showMore = binding.layoutUpgradeFeature.containerLayout.isNotVisible()
            binding.layoutUpgradeFeature.containerLayout.setVisibility(showMore)
            binding.toggleShow.text = getText(
                if (showMore) R.string.course_modal_graded_assignment_show_less
                else R.string.course_modal_graded_assignment_show_more
            )
            unit?.let {
                environment.analyticsRegistry.trackValuePropShowMoreLessClicked(
                    it.courseId, it.id, price, isSelfPaced, showMore
                )
            }
        }
    }

    private fun showNotAvailableOnMobile(isLockedContent: Boolean) {
        binding.containerLayoutNotAvailable.setVisibility(true)
        binding.llGradedContentLayout.setVisibility(false)
        binding.notAvailableMessage2.setVisibility(!isLockedContent)

        if (isLockedContent) {
            binding.contentErrorIcon.setImageDrawable(R.drawable.ic_lock)
            binding.notAvailableMessage.setText(R.string.not_available_on_mobile)
        } else {
            binding.contentErrorIcon.setImageDrawable(R.drawable.ic_laptop)
            binding.notAvailableMessage.setText(
                if (unit?.isVideoBlock == true) R.string.video_only_on_web_short
                else R.string.assessment_not_available
            )
        }

        binding.viewOnWebButton.setOnClickListener {
            unit?.let {
                BrowserUtil.open(activity, it.webUrl, true)
                environment.analyticsRegistry.trackOpenInBrowser(
                    it.id, it.courseId, it.isMultiDevice, it.blockId
                )
            }
        }
    }

    private fun setUpUpgradeButton(isSelfPaced: Boolean, price: String) {
        if (environment.config.isIAPEnabled) {
            binding.layoutUpgradeBtn.root.setVisibility(true)
            binding.layoutUpgradeBtn.btnUpgrade.setOnClickListener {
                enableUpgradeButton(false)
                purchaseProduct("org.edx.mobile.test_product")
                unit?.let {
                    environment.analyticsRegistry.trackUpgradeNowClicked(
                        it.courseId, price, it.id, isSelfPaced
                    )
                }
            }

            billingProcessor = BillingProcessor(requireContext(), object : BillingFlowListeners {
                override fun onPurchaseCancel() {
                    enableUpgradeButton(true)
                }

                override fun onPurchaseComplete(purchase: Purchase) {
                    onProductPurchased()
                }
            })
        } else {
            binding.layoutUpgradeBtn.root.setVisibility(false)
        }
    }

    private fun enableUpgradeButton(enable: Boolean) {
        binding.layoutUpgradeBtn.btnUpgrade.setVisibility(enable)
        binding.layoutUpgradeBtn.loadingIndicator.setVisibility(!enable)
    }

    private fun purchaseProduct(productId: String) {
        activity?.let { billingProcessor?.purchaseItem(it, productId) }
    }

    private fun onProductPurchased() {
        lifecycleScope.launch {
            enableUpgradeButton(true)
        }
        AlertDialogFragment.newInstance(
            getString(R.string.title_upgrade_complete),
            getString(R.string.upgrade_success_message),
            getString(R.string.label_continue),
            null,
            null,
            null
        ).show(childFragmentManager, null)
    }

    override fun onResume() {
        super.onResume()
        unit?.let {
            if (it.authorizationDenialReason == AuthorizationDenialReason.FEATURE_BASED_ENROLLMENTS
                && environment.remoteFeaturePrefs.isValuePropEnabled()
            ) {
                environment.analyticsRegistry.trackLockedContentTapped(it.courseId, it.blockId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        billingProcessor?.disconnect()
    }

    companion object {
        @JvmStatic
        fun newInstance(
            unit: CourseComponent,
            isSelfPaced: Boolean,
            price: String
        ): CourseUnitMobileNotSupportedFragment {
            val fragment = CourseUnitMobileNotSupportedFragment()
            val args = Bundle()
            args.putSerializable(Router.EXTRA_COURSE_UNIT, unit)
            args.putBoolean(Router.EXTRA_IS_SELF_PACED, isSelfPaced)
            args.putString(Router.EXTRA_PRICE, price)
            fragment.arguments = args
            return fragment
        }
    }
}