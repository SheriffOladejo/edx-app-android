package com.grassroot.academy.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Xml.Encoding;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseFragment;
import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.databinding.FragmentWebviewWithPaddingsBinding;
import com.grassroot.academy.event.NetworkConnectivityChangeEvent;
import com.grassroot.academy.http.callback.ErrorHandlingOkCallback;
import com.grassroot.academy.http.notifications.FullScreenErrorNotification;
import com.grassroot.academy.http.notifications.SnackbarErrorNotification;
import com.grassroot.academy.http.provider.OkHttpClientProvider;
import com.grassroot.academy.interfaces.RefreshListener;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.model.api.HandoutModel;
import com.grassroot.academy.module.analytics.Analytics;
import com.grassroot.academy.module.analytics.AnalyticsRegistry;
import com.grassroot.academy.util.NetworkUtil;
import com.grassroot.academy.util.WebViewUtil;
import com.grassroot.academy.view.custom.URLInterceptorWebViewClient;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.Request;

@AndroidEntryPoint
public class CourseHandoutFragment extends BaseFragment implements RefreshListener {

    protected final Logger logger = new Logger(getClass().getName());

    @Inject
    AnalyticsRegistry analyticsRegistry;

    @Inject
    IEdxEnvironment environment;

    @Inject
    OkHttpClientProvider okHttpClientProvider;

    private EnrolledCoursesResponse courseData;
    private FullScreenErrorNotification errorNotification;
    private SnackbarErrorNotification snackbarErrorNotification;
    private FragmentWebviewWithPaddingsBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseExtras();
        analyticsRegistry.trackScreenView(Analytics.Screens.COURSE_HANDOUTS, courseData.getCourse().getId(), null);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentWebviewWithPaddingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        errorNotification = new FullScreenErrorNotification(binding.webview);
        snackbarErrorNotification = new SnackbarErrorNotification(binding.webview);
        new URLInterceptorWebViewClient(requireActivity(), binding.webview, false, null)
                .setAllLinksAsExternal(true);
        loadData();
    }

    private void parseExtras() {
        courseData = (EnrolledCoursesResponse) getArguments().getSerializable(Router.EXTRA_COURSE_DATA);
    }

    private void loadData() {
        okHttpClientProvider.getWithOfflineCache().newCall(new Request.Builder()
                .url(courseData.getCourse().getCourse_handouts())
                .get()
                .build())
                .enqueue(new ErrorHandlingOkCallback<HandoutModel>(requireActivity(),
                        HandoutModel.class, errorNotification, snackbarErrorNotification, this) {
                    @Override
                    protected void onResponse(@NonNull final HandoutModel result) {
                        if (getActivity() == null) {
                            return;
                        }

                        if (!TextUtils.isEmpty(result.handouts_html)) {
                            populateHandouts(result);
                        } else {
                            errorNotification.showError(R.string.no_handouts_to_display,
                                    R.drawable.ic_error, 0, null);
                        }
                    }

                    @Override
                    protected void onFinish() {
                        if (!EventBus.getDefault().isRegistered(CourseHandoutFragment.this)) {
                            EventBus.getDefault().register(CourseHandoutFragment.this);
                        }
                    }
                });
    }

    private void populateHandouts(HandoutModel handout) {
        hideErrorMessage();

        StringBuilder buff = WebViewUtil.getIntialWebviewBuffer(requireActivity(), logger);

        buff.append("<body>");
        buff.append("<div class=\"header\">");
        buff.append(handout.handouts_html);
        buff.append("</div>");
        buff.append("</body>");

        binding.webview.loadDataWithBaseURL(environment.getConfig().getApiHostURL(), buff.toString(),
                "text/html", Encoding.UTF_8.toString(), null);

    }

    private void hideErrorMessage() {
        binding.webview.setVisibility(View.VISIBLE);
        errorNotification.hideError();
    }

    @Subscribe(sticky = true)
    @SuppressWarnings("unused")
    public void onEventMainThread(NetworkConnectivityChangeEvent event) {
        if (!NetworkUtil.isConnected(requireContext())) {
            if (!errorNotification.isShowing()) {
                snackbarErrorNotification.showOfflineError(this);
            }
        }
    }

    @Override
    public void onRefresh() {
        errorNotification.hideError();
        loadData();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onRevisit() {
        if (NetworkUtil.isConnected(requireActivity())) {
            snackbarErrorNotification.hideError();
        }
    }
}
