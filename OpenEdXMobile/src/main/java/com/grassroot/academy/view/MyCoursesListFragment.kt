package com.grassroot.academy.view

import android.content.DialogInterface
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.grassroot.academy.R
import com.grassroot.academy.authentication.LoginAPI
import com.grassroot.academy.base.MainApplication
import com.grassroot.academy.databinding.FragmentMyCoursesListBinding
import com.grassroot.academy.databinding.PanelFindCourseBinding
import com.grassroot.academy.deeplink.DeepLink
import com.grassroot.academy.deeplink.DeepLinkManager
import com.grassroot.academy.deeplink.Screen
import com.grassroot.academy.event.EnrolledInCourseEvent
import com.grassroot.academy.event.IAPFlowEvent
import com.grassroot.academy.event.MainDashboardRefreshEvent
import com.grassroot.academy.event.MoveToDiscoveryTabEvent
import com.grassroot.academy.event.NetworkConnectivityChangeEvent
import com.grassroot.academy.exception.ErrorMessage
import com.grassroot.academy.extenstion.setVisibility
import com.grassroot.academy.http.HttpStatus
import com.grassroot.academy.http.HttpStatusException
import com.grassroot.academy.http.notifications.FullScreenErrorNotification
import com.grassroot.academy.http.notifications.SnackbarErrorNotification
import com.grassroot.academy.interfaces.RefreshListener
import com.grassroot.academy.model.api.EnrolledCoursesResponse
import com.grassroot.academy.model.iap.IAPFlowData
import com.grassroot.academy.module.analytics.Analytics
import com.grassroot.academy.module.analytics.InAppPurchasesAnalytics
import com.grassroot.academy.util.InAppPurchasesException
import com.grassroot.academy.util.NetworkUtil
import com.grassroot.academy.util.NonNullObserver
import com.grassroot.academy.util.UiUtils
import com.grassroot.academy.util.observer.EventObserver
import com.grassroot.academy.view.adapters.MyCoursesAdapter
import com.grassroot.academy.view.dialog.CourseModalDialogFragment
import com.grassroot.academy.view.dialog.FullscreenLoaderDialogFragment
import com.grassroot.academy.viewModel.CourseViewModel
import com.grassroot.academy.viewModel.CourseViewModel.CoursesRequestType
import com.grassroot.academy.viewModel.InAppPurchasesViewModel
import com.grassroot.academy.wrapper.InAppPurchasesDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import javax.inject.Inject

@AndroidEntryPoint
class MyCoursesListFragment : OfflineSupportBaseFragment(), RefreshListener {

    private lateinit var adapter: MyCoursesAdapter
    private lateinit var binding: FragmentMyCoursesListBinding

    private val courseViewModel: CourseViewModel by viewModels()
    private val iapViewModel: InAppPurchasesViewModel by viewModels()

    @Inject
    lateinit var loginAPI: LoginAPI

    @Inject
    lateinit var iapAnalytics: InAppPurchasesAnalytics

    @Inject
    lateinit var iapDialog: InAppPurchasesDialog

    private lateinit var errorNotification: FullScreenErrorNotification
    private var refreshOnResume = false
    private var isObserversInitialized = true
    private var lastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = object : MyCoursesAdapter(activity, environment) {
            override fun onItemClicked(model: EnrolledCoursesResponse) {
                activity?.let { activity ->
                    environment.router.showCourseDashboardTabs(activity, model, false)
                }
            }

            override fun onAnnouncementClicked(model: EnrolledCoursesResponse) {
                activity?.let { activity ->
                    environment.router.showCourseDashboardTabs(activity, model, true)
                }
            }

            override fun onValuePropClicked(model: EnrolledCoursesResponse) {
                //This time is checked to avoid taps in quick succession
                val currentTime = SystemClock.elapsedRealtime()
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime
                    CourseModalDialogFragment.newInstance(
                        Analytics.Screens.COURSE_ENROLLMENT,
                        model.courseId,
                        model.courseSku,
                        model.course.name,
                        model.course.isSelfPaced
                    ).show(childFragmentManager, CourseModalDialogFragment.TAG)
                }
            }
        }
        detectDeeplink()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyCoursesListBinding.inflate(inflater, container, false)
        errorNotification = FullScreenErrorNotification(binding.myCourseList)

        binding.swipeContainer.setOnRefreshListener {
            errorNotification.hideError()
            courseViewModel.fetchEnrolledCourses(
                type = CoursesRequestType.STALE,
                showProgress = false
            )
        }
        UiUtils.setSwipeRefreshLayoutColors(binding.swipeContainer)

        // Add empty view to cause divider to render at the top of the list.
        binding.myCourseList.addHeaderView(View(context), null, false)
        binding.myCourseList.adapter = adapter
        binding.myCourseList.onItemClickListener = adapter

        initCourseObservers()
        courseViewModel.fetchEnrolledCourses(type = CoursesRequestType.CACHE)

        return binding.root
    }

    private fun initCourseObservers() {
        courseViewModel.showProgress.observe(viewLifecycleOwner, NonNullObserver {
            binding.loadingIndicator.root.setVisibility(it)
            if (it) {
                errorNotification.hideError()
            }
        })

        courseViewModel.enrolledCoursesResponse.observe(
            viewLifecycleOwner,
            EventObserver { enrolledCourses ->
                populateCourseData(data = enrolledCourses)
                // Checking if the user is logged-in as we need userId to enable IAP
                if (environment.loginPrefs.isUserLoggedIn &&
                    environment.appFeaturesPrefs.isIAPEnabled(environment.loginPrefs.isOddUserId)
                ) {
                    initInAppPurchaseSetup()
                    detectUnfulfilledPurchase(enrolledCourses)
                }
            })

        courseViewModel.handleError.observe(viewLifecycleOwner, NonNullObserver {
            when {
                (it is HttpStatusException) -> {
                    when (it.statusCode) {
                        HttpStatus.UNAUTHORIZED -> {
                            context?.let { context ->
                                environment.router?.forceLogout(
                                    context,
                                    environment.analyticsRegistry,
                                    environment.notificationDelegate
                                )
                            }
                        }
                        HttpStatus.UPGRADE_REQUIRED -> {
                            context?.let { context ->
                                errorNotification.showError(
                                    context,
                                    it
                                )
                            }
                        }
                    }
                }
                (FullscreenLoaderDialogFragment
                    .getRetainedInstance(fragmentManager = childFragmentManager)?.isAdded == true) -> {
                    iapViewModel.dispatchError(
                        requestType = ErrorMessage.COURSE_REFRESH_CODE,
                        errorMessage = it.message
                    )
                }
                else -> {
                    showError(it)
                }
            }
            invalidateView()
        })
    }

    private fun initIAPObservers() {
        iapViewModel.refreshCourseData.observe(viewLifecycleOwner, EventObserver { iapFlowData ->
            iapDialog.showNewExperienceAlertDialog(this, { _, _ ->
                iapAnalytics.trackIAPEvent(
                    eventName = Analytics.Events.IAP_NEW_EXPERIENCE_ALERT_ACTION,
                    actionTaken = Analytics.Values.ACTION_REFRESH
                )
                iapAnalytics.initUnlockContentTime()
                showFullscreenLoader(iapFlowData)
            }, { _, _ ->
                iapAnalytics.trackIAPEvent(
                    eventName = Analytics.Events.IAP_NEW_EXPERIENCE_ALERT_ACTION,
                    actionTaken = Analytics.Values.ACTION_CONTINUE_WITHOUT_UPDATE
                )
            })
        })

        iapViewModel.errorMessage.observe(viewLifecycleOwner, EventObserver { errorMessage ->
            var retryListener: DialogInterface.OnClickListener? = null
            if (errorMessage.canRetry()) {
                retryListener = DialogInterface.OnClickListener { _, _ ->
                    if (errorMessage.requestType == ErrorMessage.EXECUTE_ORDER_CODE) {
                        iapViewModel.executeOrder()
                    } else if (errorMessage.requestType == ErrorMessage.COURSE_REFRESH_CODE) {
                        courseViewModel.fetchEnrolledCourses(
                            type = CoursesRequestType.LIVE,
                            showProgress = false
                        )
                    } else if (HttpStatus.NOT_ACCEPTABLE == (errorMessage.throwable as InAppPurchasesException).httpErrorCode) {
                        showFullscreenLoader(iapFlowData = iapViewModel.iapFlowData)
                    }
                }
            }
            iapDialog.handleIAPException(
                fragment = this@MyCoursesListFragment,
                errorMessage = errorMessage,
                retryListener = retryListener,
                cancelListener = { _, _ ->
                    iapViewModel.iapFlowData.clear()
                    if (errorMessage.isPostUpgradeErrorType()) {
                        FullscreenLoaderDialogFragment.getRetainedInstance(childFragmentManager)
                            ?.dismiss()
                    }
                }
            )
        })
    }

    /**
     * Method to detect Unfulfilled Purchases if full screen loader is not visible.
     *
     * @param enrolledCourses User enrolled courses.
     * */
    private fun detectUnfulfilledPurchase(enrolledCourses: List<EnrolledCoursesResponse>) {
        val fullscreenLoader = FullscreenLoaderDialogFragment.getRetainedInstance(
            fragmentManager = childFragmentManager
        )
        if (fullscreenLoader != null) {
            SnackbarErrorNotification(binding.root).showUpgradeSuccessSnackbar(R.string.purchase_success_message)
            fullscreenLoader.closeLoader()
        } else {
            iapAnalytics.reset()
            iapViewModel.detectUnfulfilledPurchase(
                environment.loginPrefs.userId,
                enrolledCourses,
                IAPFlowData.IAPFlowType.SILENT,
                Analytics.Screens.COURSE_ENROLLMENT
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!EventBus.getDefault().isRegistered(this@MyCoursesListFragment)) {
            EventBus.getDefault().register(this@MyCoursesListFragment)
        }
        if (refreshOnResume) {
            courseViewModel.fetchEnrolledCourses(
                type = CoursesRequestType.CACHE,
                showProgress = false
            )
            refreshOnResume = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(sticky = true)
    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: EnrolledInCourseEvent?) {
        refreshOnResume = true
    }

    @Subscribe
    fun onEventMainThread(event: IAPFlowEvent) {
        if (!this.isResumed) {
            return
        }
        when (event.flowAction) {
            IAPFlowData.IAPAction.SHOW_FULL_SCREEN_LOADER -> {
                event.iapFlowData?.let {
                    showFullscreenLoader(event.iapFlowData)
                }
            }
            IAPFlowData.IAPAction.PURCHASE_FLOW_COMPLETE -> {
                onRefresh()
            }
        }
    }

    private fun initInAppPurchaseSetup() {
        if (isAdded && environment.appFeaturesPrefs.isValuePropEnabled() &&
            isObserversInitialized
        ) {
            initIAPObservers()
            isObserversInitialized = false
        }
    }

    private fun populateCourseData(
        data: List<EnrolledCoursesResponse>
    ) {
        if (data.isNotEmpty()) {
            adapter.setItems(data)
        }

        addFindCoursesFooter()
        adapter.notifyDataSetChanged()

        if (adapter.isEmpty && !environment.config.discoveryConfig.isDiscoveryEnabled) {
            errorNotification.showError(
                R.string.no_courses_to_display,
                R.drawable.ic_error, 0, null
            )
            binding.myCourseList.visibility = View.GONE
        }
        invalidateView()
    }

    private fun showFullscreenLoader(iapFlowData: IAPFlowData) {
        // To proceed with the same instance of dialog fragment in case of orientation change
        val fullscreenLoader = try {
            FullscreenLoaderDialogFragment
                .getRetainedInstance(fragmentManager = childFragmentManager) as FullscreenLoaderDialogFragment
        } catch (e: Exception) {
            FullscreenLoaderDialogFragment.newInstance(iapFlowData)
        }
        fullscreenLoader.show(childFragmentManager, FullscreenLoaderDialogFragment.TAG)
    }

    private fun detectDeeplink() {
        if (arguments?.get(Router.EXTRA_DEEP_LINK) != null) {
            (arguments?.get(Router.EXTRA_DEEP_LINK) as DeepLink).let { deeplink ->
                DeepLinkManager.proceedDeeplink(requireActivity(), deeplink)
                MainApplication.instance().showBanner(loginAPI, true)
            }
        } else {
            MainApplication.instance().showBanner(loginAPI, false)
        }
    }

    private fun invalidateView() {
        binding.swipeContainer.isRefreshing = false
        binding.loadingIndicator.root.visibility = View.GONE
    }

    private fun showError(error: Throwable) {
        context?.let { context ->
            errorNotification.showError(context, error, R.string.lbl_reload) {
                if (NetworkUtil.isConnected(context)) {
                    onRefresh()
                }
            }
        }
    }

    private fun addFindCoursesFooter() {
        // Validate footer is not already added.
        if (binding.myCourseList.footerViewsCount > 0) {
            return
        }
        if (environment.config.discoveryConfig.isDiscoveryEnabled) {
            // Add 'Find a Course' list item as a footer.
            val footer: PanelFindCourseBinding = PanelFindCourseBinding.inflate(
                LayoutInflater.from(activity), binding.myCourseList, false
            )
            binding.myCourseList.addFooterView(footer.root, null, false)
            footer.courseBtn.setOnClickListener {
                environment.analyticsRegistry?.trackUserFindsCourses()
                EventBus.getDefault().post(MoveToDiscoveryTabEvent(Screen.DISCOVERY))
            }
        }
        // Add empty view to cause divider to render at the bottom of the list.
        binding.myCourseList.addFooterView(View(context), null, false)
    }

    override fun onRefresh() {
        EventBus.getDefault().post(MainDashboardRefreshEvent())
    }

    @Subscribe(sticky = true)
    @Suppress("UNUSED_PARAMETER")
    fun onEvent(event: MainDashboardRefreshEvent) {
        courseViewModel.fetchEnrolledCourses(type = CoursesRequestType.LIVE)
    }

    override fun onRevisit() {
        super.onRevisit()
        if (NetworkUtil.isConnected(activity)) {
            binding.swipeContainer.isEnabled = true
        }
    }

    @Subscribe(sticky = true)
    fun onEvent(event: NetworkConnectivityChangeEvent?) {
        if (activity != null) {
            if (NetworkUtil.isConnected(context)) {
                binding.swipeContainer.isEnabled = true
            } else {
                //Disable swipe functionality and hide the loading view
                binding.swipeContainer.isEnabled = false
                binding.swipeContainer.isRefreshing = false
            }
            onNetworkConnectivityChangeEvent(event)
        }
    }

    override fun isShowingFullScreenError(): Boolean {
        return errorNotification.isShowing
    }
}
