package com.grassroot.academy.view

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.grassroot.academy.BuildConfig
import com.grassroot.academy.R
import com.grassroot.academy.base.BaseFragment
import com.grassroot.academy.core.IEdxEnvironment
import com.grassroot.academy.databinding.FragmentAccountBinding
import com.grassroot.academy.deeplink.Screen
import com.grassroot.academy.deeplink.ScreenDef
import com.grassroot.academy.event.*
import com.grassroot.academy.exception.ErrorMessage
import com.grassroot.academy.extenstion.isVisible
import com.grassroot.academy.extenstion.setVisibility
import com.grassroot.academy.http.HttpStatus
import com.grassroot.academy.model.iap.IAPFlowData
import com.grassroot.academy.model.user.Account
import com.grassroot.academy.model.video.VideoQuality
import com.grassroot.academy.module.analytics.Analytics
import com.grassroot.academy.module.analytics.InAppPurchasesAnalytics
import com.grassroot.academy.module.prefs.LoginPrefs
import com.grassroot.academy.module.prefs.PrefManager
import com.grassroot.academy.user.UserAPI.AccountDataUpdatedCallback
import com.grassroot.academy.user.UserService
import com.grassroot.academy.util.*
import com.grassroot.academy.util.observer.EventObserver
import com.grassroot.academy.view.dialog.*
import com.grassroot.academy.viewModel.CourseViewModel
import com.grassroot.academy.viewModel.InAppPurchasesViewModel
import com.grassroot.academy.wrapper.InAppPurchasesDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import retrofit2.Call
import javax.inject.Inject


@AndroidEntryPoint
class AccountFragment : BaseFragment() {

    private lateinit var binding: FragmentAccountBinding

    @Inject
    lateinit var config: Config

    @Inject
    lateinit var environment: IEdxEnvironment

    @Inject
    lateinit var loginPrefs: LoginPrefs

    @Inject
    lateinit var userService: UserService

    @Inject
    lateinit var iapDialog: InAppPurchasesDialog

    @Inject
    lateinit var iapAnalytics: InAppPurchasesAnalytics

    private val courseViewModel: CourseViewModel by viewModels()
    private val iapViewModel: InAppPurchasesViewModel by viewModels()

    private var getAccountCall: Call<Account>? = null
    private var loaderDialog: AlertDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        environment.analyticsRegistry.trackScreenView(Analytics.Screens.PROFILE)
        EventBus.getDefault().register(this)
        sendGetUpdatedAccountCall()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun handleIntentBundle(bundle: Bundle?) {
        if (bundle != null) {
            @ScreenDef val screenName = bundle.getString(Router.EXTRA_SCREEN_NAME)
            if (loginPrefs.isUserLoggedIn && screenName == Screen.USER_PROFILE) {
                environment.router.showUserProfile(requireContext(), loginPrefs.username)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPersonalInfo()
        handleIntentBundle(arguments)
        initVideoQuality()
        updateWifiSwitch()
        updateSDCardSwitch()
        initHelpFields()
        initPrivacyFields()

        val iapEnabled =
            environment.appFeaturesPrefs.isIAPEnabled(loginPrefs.isOddUserId)
        if (iapEnabled) {
            initRestorePurchasesObservers()
            binding.containerPurchases.setVisibility(true)
            binding.btnRestorePurchases.setOnClickListener {
                iapAnalytics.reset()
                iapAnalytics.trackIAPEvent(Analytics.Events.IAP_RESTORE_PURCHASE_CLICKED)
                showLoader()
                lifecycleScope.launch {
                    courseViewModel.fetchEnrolledCourses(
                        type = CourseViewModel.CoursesRequestType.STALE,
                        showProgress = false
                    )
                }
            }
        } else {
            binding.containerPurchases.setVisibility(false)
        }
        if (loginPrefs.isUserLoggedIn) {
            binding.btnSignOut.visibility = View.VISIBLE
            binding.btnSignOut.setOnClickListener {
                environment.router.performManualLogout(
                    context,
                    environment.analyticsRegistry, environment.notificationDelegate
                )
            }

            config.deleteAccountUrl?.let { deleteAccountUrl ->
                binding.containerDeleteAccount.visibility = View.VISIBLE
                binding.btnDeleteAccount.setOnClickListener {
                    environment.router.showAuthenticatedWebViewActivity(
                        this.requireContext(),
                        deleteAccountUrl, getString(R.string.title_delete_my_account), false
                    )
                    trackEvent(
                        Analytics.Events.DELETE_ACCOUNT_CLICKED,
                        Analytics.Values.DELETE_ACCOUNT_CLICKED
                    )
                }
            }
        }

        binding.appVersion.text = String.format(
            "%s %s %s", getString(R.string.label_app_version),
            BuildConfig.VERSION_NAME, config.environmentDisplayName
        )

        environment.analyticsRegistry.trackScreenViewEvent(
            Analytics.Events.PROFILE_PAGE_VIEWED,
            Analytics.Screens.PROFILE
        )
    }

    private fun initRestorePurchasesObservers() {
        courseViewModel.enrolledCoursesResponse.observe(
            viewLifecycleOwner,
            EventObserver { enrolledCourses ->
                iapViewModel.detectUnfulfilledPurchase(
                    loginPrefs.userId,
                    enrolledCourses,
                    IAPFlowData.IAPFlowType.RESTORE,
                    Analytics.Screens.PROFILE
                )
            })

        courseViewModel.handleError.observe(viewLifecycleOwner, NonNullObserver {
            loaderDialog?.dismiss()
        })

        iapViewModel.refreshCourseData.observe(viewLifecycleOwner, EventObserver {
            iapDialog.showNewExperienceAlertDialog(this, { _, _ ->
                iapAnalytics.trackIAPEvent(
                    eventName = Analytics.Events.IAP_NEW_EXPERIENCE_ALERT_ACTION,
                    actionTaken = Analytics.Values.ACTION_REFRESH
                )
                loaderDialog?.dismiss()
                iapAnalytics.initUnlockContentTime()
                showFullScreenLoader()
            }, { _, _ ->
                iapAnalytics.trackIAPEvent(
                    eventName = Analytics.Events.IAP_NEW_EXPERIENCE_ALERT_ACTION,
                    actionTaken = Analytics.Values.ACTION_CONTINUE_WITHOUT_UPDATE
                )
                loaderDialog?.dismiss()
            })
        })

        iapViewModel.fakeUnfulfilledCompletion.observe(
            viewLifecycleOwner,
            EventObserver { isCompleted ->
                if (isCompleted) {
                    loaderDialog?.dismiss()
                    iapDialog.showNoUnFulfilledPurchasesDialog(this)
                }
            })

        iapViewModel.errorMessage.observe(viewLifecycleOwner, EventObserver { errorMessage ->
            loaderDialog?.dismiss()
            var retryListener: DialogInterface.OnClickListener? = null
            if (errorMessage.canRetry()) {
                retryListener = DialogInterface.OnClickListener { _, _ ->
                    if (errorMessage.requestType == ErrorMessage.EXECUTE_ORDER_CODE) {
                        iapViewModel.executeOrder()
                    } else if (HttpStatus.NOT_ACCEPTABLE == (errorMessage.throwable as InAppPurchasesException).httpErrorCode) {
                        showFullScreenLoader()
                    }
                }
            }

            var cancelListener: DialogInterface.OnClickListener? = null
            if (errorMessage.isPostUpgradeErrorType()) {
                cancelListener =
                    DialogInterface.OnClickListener { _, _ -> iapViewModel.iapFlowData.clear() }
            }

            iapDialog.handleIAPException(
                fragment = this@AccountFragment,
                errorMessage = errorMessage,
                retryListener = retryListener,
                cancelListener = cancelListener
            )
        })
    }

    private fun showFullScreenLoader() {
        // To proceed with the same instance of dialog fragment in case of orientation change
        var fullScreenLoader =
            FullscreenLoaderDialogFragment.getRetainedInstance(fragmentManager = childFragmentManager)
        if (fullScreenLoader == null) {
            fullScreenLoader =
                FullscreenLoaderDialogFragment.newInstance(iapData = iapViewModel.iapFlowData)
        }
        fullScreenLoader.show(childFragmentManager, FullscreenLoaderDialogFragment.TAG)
    }

    private fun showLoader() {
        loaderDialog = AlertDialogFragment.newInstance(
            R.string.title_checking_purchases,
            R.layout.alert_dialog_progress
        )
        loaderDialog?.isCancelable = false
        loaderDialog?.showNow(childFragmentManager, null)
    }

    private fun initVideoQuality() {
        binding.containerVideoQuality.setOnClickListener {
            val videoQualityDialog: VideoDownloadQualityDialogFragment =
                VideoDownloadQualityDialogFragment.getInstance(
                    environment,
                    callback = object : VideoDownloadQualityDialogFragment.IListDialogCallback {
                        override fun onItemClicked(videoQuality: VideoQuality) {
                            setVideoQualityDescription(videoQuality)
                        }
                    })
            videoQualityDialog.show(
                childFragmentManager,
                VideoDownloadQualityDialogFragment.TAG
            )

            trackEvent(
                Analytics.Events.PROFILE_VIDEO_DOWNLOAD_QUALITY_CLICKED,
                Analytics.Values.PROFILE_VIDEO_DOWNLOAD_QUALITY_CLICKED
            )
        }
    }

    private fun setVideoQualityDescription(videoQuality: VideoQuality) {
        binding.tvVideoDownloadQuality.setText(videoQuality.titleResId)
    }

    private fun initHelpFields() {
        if (!config.feedbackEmailAddress.isNullOrBlank() || !config.faqUrl.isNullOrBlank()) {
            binding.tvHelp.visibility = View.VISIBLE
            if (!config.feedbackEmailAddress.isNullOrBlank()) {
                binding.containerFeedback.visibility = View.VISIBLE
                binding.btnEmailSupport.setOnClickListener {
                    environment.router.showFeedbackScreen(
                        requireActivity(),
                        getString(R.string.email_subject)
                    )
                    trackEvent(
                        Analytics.Events.EMAIL_SUPPORT_CLICKED,
                        Analytics.Values.EMAIL_SUPPORT_CLICKED
                    )
                }
            }

            if (!config.faqUrl.isNullOrBlank()) {
                binding.containerFaq.visibility = View.VISIBLE
                binding.tvGetSupportDescription.text = ResourceUtil.getFormattedString(
                    resources, R.string.description_get_support,
                    AppConstants.PLATFORM_NAME, config.platformName
                ).toString()
                binding.btnFaq.setOnClickListener {
                    BrowserUtil.open(requireActivity(), environment.config.faqUrl, false)
                    trackEvent(Analytics.Events.FAQ_CLICKED, Analytics.Values.FAQ_CLICKED)
                }
            }
        }
    }

    private fun sendGetUpdatedAccountCall() {
        loginPrefs.username.let { username ->
            getAccountCall = userService.getAccount(username)
            getAccountCall?.enqueue(
                AccountDataUpdatedCallback(
                    requireContext(),
                    username,
                    null,  // Disable global loading indicator
                    null
                )
            ) // No place to show an error notification
        }
    }

    private fun initPersonalInfo() {
        if (!config.isUserProfilesEnabled || !loginPrefs.isUserLoggedIn) {
            binding.containerPersonalInfo.visibility = View.GONE
            return
        }

        binding.tvEmail.setVisibility(loginPrefs.userEmail.isNullOrEmpty().not())
        binding.tvEmail.text = ResourceUtil.getFormattedString(
            resources,
            R.string.profile_email_description,
            AppConstants.EMAIL,
            loginPrefs.userEmail
        )

        binding.tvUsername.setVisibility(loginPrefs.username.isNotEmpty())
        binding.tvUsername.text = ResourceUtil.getFormattedString(
            resources,
            R.string.profile_username_description,
            AppConstants.USERNAME,
            loginPrefs.username
        )

        binding.tvLimitedProfile.setVisibility(loginPrefs.currentUserProfile.hasLimitedProfile)

        loginPrefs.profileImage?.let { imageUrl ->
            Glide.with(requireContext())
                .load(imageUrl.imageUrlMedium)
                .into(binding.profileImage)
        } ?: run { binding.profileImage.setImageResource(R.drawable.profile_photo_placeholder) }

        binding.containerPersonalInfo.visibility = View.VISIBLE
        binding.containerPersonalInfo.setOnClickListener {
            trackEvent(
                Analytics.Events.PERSONAL_INFORMATION_CLICKED,
                Analytics.Values.PERSONAL_INFORMATION_CLICKED
            )
            environment.router.showUserProfile(requireActivity(), loginPrefs.username)
            setVideoQualityDescription(loginPrefs.videoQuality)
        }
    }

    private fun initPrivacyFields() {
        binding.tvPrivacyPolicy.setVisibility(true)
        binding.tvPrivacyPolicy.setOnClickListener {
            trackEvent(
                Analytics.Events.PRIVACY_POLICY_CLICKED,
                Analytics.Values.PRIVACY_POLICY_CLICKED
            )
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.edx.org/webview/edx-privacy-policy"))
            startActivity(browserIntent)
        }

        binding.tvCookiePolicy.setVisibility(true)
        binding.tvCookiePolicy.setOnClickListener {
            trackEvent(
                Analytics.Events.COOKIE_POLICY_CLICKED,
                Analytics.Values.COOKIE_POLICY_CLICKED
            )
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.edx.org/webview/edx-privacy-policy/cookies"))
            startActivity(browserIntent)
        }

        binding.tvDataConsentPolicy.setVisibility(true)
        binding.tvDataConsentPolicy.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.edx.org/webview/edx-privacy-policy/do-not-sell-my-personal-data"))
            startActivity(browserIntent)
            trackEvent(
                Analytics.Events.DO_NOT_SELL_DATA_CLICKED,
                Analytics.Values.DO_NOT_SELL_DATA_CLICKED
            )
        }

        val isContainerVisible = binding.tvPrivacyPolicy.isVisible()
                || binding.tvCookiePolicy.isVisible() || binding.tvDataConsentPolicy.isVisible()
        binding.containerPrivacy.setVisibility(isContainerVisible)
    }

    private fun updateWifiSwitch() {
        val wifiPrefManager = PrefManager(requireContext(), PrefManager.Pref.WIFI)
        binding.switchWifi.setOnCheckedChangeListener(null)
        binding.switchWifi.isChecked =
            wifiPrefManager.getBoolean(PrefManager.Key.DOWNLOAD_ONLY_ON_WIFI, true)
        binding.switchWifi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wifiPrefManager.put(PrefManager.Key.DOWNLOAD_ONLY_ON_WIFI, true)
                wifiPrefManager.put(PrefManager.Key.DOWNLOAD_OFF_WIFI_SHOW_DIALOG_FLAG, true)
                trackEvent(Analytics.Events.WIFI_ON, Analytics.Values.WIFI_ON)
            } else {
                showWifiDialog()
            }
        }
    }

    private fun showWifiDialog() {
        val dialogFragment =
            NetworkCheckDialogFragment.newInstance(getString(R.string.wifi_dialog_title_help),
                getString(R.string.wifi_dialog_message_help),
                object : IDialogCallback {
                    override fun onPositiveClicked() {
                        try {
                            val wifiPrefManager =
                                PrefManager(requireContext(), PrefManager.Pref.WIFI)
                            wifiPrefManager.put(PrefManager.Key.DOWNLOAD_ONLY_ON_WIFI, false)
                            trackEvent(Analytics.Events.WIFI_ALLOW, Analytics.Values.WIFI_ALLOW)
                            trackEvent(Analytics.Events.WIFI_OFF, Analytics.Values.WIFI_OFF)
                            updateWifiSwitch()
                        } catch (ex: Exception) {
                        }
                    }

                    override fun onNegativeClicked() {
                        try {
                            val wifiPrefManager =
                                PrefManager(requireContext(), PrefManager.Pref.WIFI)
                            wifiPrefManager.put(PrefManager.Key.DOWNLOAD_ONLY_ON_WIFI, true)
                            wifiPrefManager.put(
                                PrefManager.Key.DOWNLOAD_OFF_WIFI_SHOW_DIALOG_FLAG,
                                true
                            )
                            trackEvent(
                                Analytics.Events.WIFI_DONT_ALLOW,
                                Analytics.Values.WIFI_DONT_ALLOW
                            )
                            updateWifiSwitch()
                        } catch (ex: Exception) {
                        }
                    }
                })
        dialogFragment.isCancelable = false
        activity?.let { dialogFragment.show(it.supportFragmentManager, AppConstants.DIALOG) }
    }

    private fun updateSDCardSwitch() {
        val prefManager = PrefManager(requireContext(), PrefManager.Pref.USER_PREF)
        if (!environment.config.isDownloadToSDCardEnabled || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.containerSdCard.visibility = View.GONE
            binding.tvDescriptionSdCard.visibility = View.GONE
            prefManager.put(PrefManager.Key.DOWNLOAD_TO_SDCARD, false)
        } else {
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }
            binding.switchSdCard.setOnCheckedChangeListener(null)
            binding.switchSdCard.isChecked = environment.userPrefs.isDownloadToSDCardEnabled
            binding.switchSdCard.setOnCheckedChangeListener { _, isChecked ->
                prefManager.put(PrefManager.Key.DOWNLOAD_TO_SDCARD, isChecked)
                // Send analytics
                if (isChecked) trackEvent(
                    Analytics.Events.DOWNLOAD_TO_SD_CARD_ON,
                    Analytics.Values.DOWNLOAD_TO_SD_CARD_SWITCH_ON
                )
                else trackEvent(
                    Analytics.Events.DOWNLOAD_TO_SD_CARD_OFF,
                    Analytics.Values.DOWNLOAD_TO_SD_CARD_SWITCH_OFF
                )
            }
            binding.switchSdCard.isEnabled = FileUtil.isRemovableStorageAvailable(requireContext())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (null != getAccountCall) {
            getAccountCall?.cancel()
        }
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    fun onEventMainThread(event: IAPFlowEvent) {
        if (this.isResumed && event.flowAction == IAPFlowData.IAPAction.PURCHASE_FLOW_COMPLETE) {
            EventBus.getDefault().post(MainDashboardRefreshEvent())
            requireActivity().finish()
        }
    }

    @Subscribe(sticky = true)
    @SuppressWarnings("unused")
    fun onEventMainThread(event: MediaStatusChangeEvent) {
        binding.switchSdCard.isEnabled = event.isSdCardAvailable
    }

    @Subscribe(sticky = true)
    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(@NonNull event: AccountDataLoadedEvent) {
        if (!environment.config.isUserProfilesEnabled) {
            return
        }
        initPersonalInfo()
    }

    @Subscribe(sticky = true)
    @SuppressWarnings("unused")
    fun onEventMainThread(event: ProfilePhotoUpdatedEvent) {
        UserProfileUtils.loadProfileImage(requireContext(), event, binding.profileImage)
    }

    private fun trackEvent(eventName: String, biValue: String) {
        environment.analyticsRegistry.trackEvent(eventName, biValue)
    }

    companion object {
        @JvmStatic
        fun newInstance(@Nullable bundle: Bundle?): AccountFragment {
            val fragment = AccountFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
