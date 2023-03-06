package com.grassroot.academy.view.dialog

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.grassroot.academy.R
import com.grassroot.academy.core.IEdxEnvironment
import com.grassroot.academy.databinding.DialogFullscreenLoaderBinding
import com.grassroot.academy.event.IAPFlowEvent
import com.grassroot.academy.exception.ErrorMessage
import com.grassroot.academy.model.iap.IAPFlowData
import com.grassroot.academy.module.analytics.Analytics
import com.grassroot.academy.module.analytics.InAppPurchasesAnalytics
import com.grassroot.academy.util.InAppPurchasesException
import com.grassroot.academy.util.observer.EventObserver
import com.grassroot.academy.viewModel.InAppPurchasesViewModel
import com.grassroot.academy.wrapper.InAppPurchasesDialog
import org.greenrobot.eventbus.EventBus
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlin.concurrent.schedule

@AndroidEntryPoint
class FullscreenLoaderDialogFragment : DialogFragment() {

    private var dismissTimer: TimerTask? = null

    @Inject
    lateinit var environment: IEdxEnvironment

    @Inject
    lateinit var iapAnalytics: InAppPurchasesAnalytics

    @Inject
    lateinit var iapDialog: InAppPurchasesDialog

    private lateinit var binding: DialogFullscreenLoaderBinding

    private val iapViewModel: InAppPurchasesViewModel by viewModels()

    private var iapFlowData: IAPFlowData? = null
    private var loaderStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            R.style.AppTheme_NoActionBar
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogFullscreenLoaderBinding.inflate(inflater)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, args: Bundle?) {
        super.onViewCreated(view, args)
        iapFlowData = arguments?.getSerializable(KEY_IAP_DATA) as IAPFlowData?
        loaderStartTime = arguments?.getLong(LOADER_START_TIME, Calendar.getInstance().timeInMillis)
            ?: Calendar.getInstance().timeInMillis
        intiViews()
        initObservers()
        if (iapFlowData?.isVerificationPending == true) {
            iapViewModel.executeOrder(iapFlowData)
        } else if (iapFlowData?.flowType?.isSilentMode() == true) {
            purchaseFlowComplete()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(LOADER_START_TIME, loaderStartTime)
        outState.putSerializable(KEY_IAP_DATA, iapFlowData)
    }

    private fun intiViews() {
        binding.materialTextView.setText(getTitle(), TextView.BufferType.SPANNABLE)
    }

    private fun initObservers() {
        iapViewModel.refreshCourseData.observe(viewLifecycleOwner, EventObserver {
            purchaseFlowComplete()
        })

        iapViewModel.errorMessage.observe(viewLifecycleOwner, EventObserver { errorMessage ->
            if (errorMessage.isPostUpgradeErrorType()) {
                errorMessage.throwable as InAppPurchasesException
                iapDialog.handleIAPException(
                    fragment = this@FullscreenLoaderDialogFragment,
                    errorMessage = errorMessage,
                    retryListener = { _, _ ->
                        if (errorMessage.requestType == ErrorMessage.EXECUTE_ORDER_CODE) {
                            iapViewModel.executeOrder(iapFlowData)
                        } else {
                            purchaseFlowComplete()
                        }
                    },
                    cancelListener = { _, _ ->
                        dismiss()
                    })
            }
        })
    }

    private fun purchaseFlowComplete() {
        EventBus.getDefault().post(IAPFlowEvent(IAPFlowData.IAPAction.PURCHASE_FLOW_COMPLETE))
    }

    fun closeLoader() {
        if (dismissTimer == null) {
            dismissTimer = Timer("", false).schedule(getRemainingVisibleTime()) {
                iapAnalytics.trackIAPEvent(Analytics.Events.IAP_COURSE_UPGRADE_SUCCESS)
                iapAnalytics.trackIAPEvent(Analytics.Events.IAP_UNLOCK_UPGRADED_CONTENT_TIME)
                iapAnalytics.trackIAPEvent(Analytics.Events.IAP_UNLOCK_UPGRADED_CONTENT_REFRESH_TIME)
                iapFlowData?.clear()
                dismiss()
            }
        }
    }

    private fun getTitle(): SpannableStringBuilder {
        val unlocking = getString(R.string.fullscreen_loader_unlocking)
        val fullAccess = getString(R.string.fullscreen_loader_full_access)
        val toYourCourse = getString(R.string.fullscreen_loader_to_your_course)

        val spannable =
            SpannableStringBuilder(String.format("%s\n%s\n%s", unlocking, fullAccess, toYourCourse))

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.accentAColor)),
            unlocking.length,
            unlocking.length + fullAccess.length + 1,
            Spannable.SPAN_EXCLUSIVE_INCLUSIVE
        )
        return spannable
    }

    /**
     * Method to get the remaining visible time for the loader
     * As per requirements the loader needs to be visible for at least 3 seconds
     */
    private fun getRemainingVisibleTime(): Long {
        val totalVisibleTime = Calendar.getInstance().timeInMillis - loaderStartTime
        return if (totalVisibleTime < MINIMUM_DISPLAY_DELAY)
            MINIMUM_DISPLAY_DELAY - totalVisibleTime
        else
            0
    }

    companion object {
        const val TAG = "FULLSCREEN_LOADER"
        private const val LOADER_START_TIME = "LOADER_START_TIME"
        private const val KEY_IAP_DATA = "iap_flow_data"
        private const val MINIMUM_DISPLAY_DELAY = 3_000L

        @JvmStatic
        fun newInstance(iapData: IAPFlowData): FullscreenLoaderDialogFragment =
            FullscreenLoaderDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_IAP_DATA, iapData)
                }
            }

        @JvmStatic
        fun getRetainedInstance(fragmentManager: FragmentManager?): FullscreenLoaderDialogFragment? =
            fragmentManager?.findFragmentByTag(TAG) as FullscreenLoaderDialogFragment?
    }
}
