package com.grassroot.academy.view;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.gson.reflect.TypeToken;

import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseFragment;
import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.event.NetworkConnectivityChangeEvent;
import com.grassroot.academy.http.callback.ErrorHandlingOkCallback;
import com.grassroot.academy.http.notifications.FullScreenErrorNotification;
import com.grassroot.academy.http.notifications.SnackbarErrorNotification;
import com.grassroot.academy.http.provider.OkHttpClientProvider;
import com.grassroot.academy.interfaces.RefreshListener;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.model.api.AnnouncementsModel;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.social.facebook.FacebookProvider;
import com.grassroot.academy.util.NetworkUtil;
import com.grassroot.academy.util.WebViewUtil;
import com.grassroot.academy.view.custom.EdxWebView;
import com.grassroot.academy.view.custom.URLInterceptorWebViewClient;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.Request;

@AndroidEntryPoint
public class CourseAnnouncementsFragment extends BaseFragment implements RefreshListener {
    private final Logger logger = new Logger(getClass().getName());

    private EdxWebView webView;

    private EnrolledCoursesResponse courseData;
    private List<AnnouncementsModel> savedAnnouncements;

    @Inject
    protected IEdxEnvironment environment;

    @Inject
    OkHttpClientProvider okHttpClientProvider;

    private FullScreenErrorNotification errorNotification;

    private SnackbarErrorNotification snackbarErrorNotification;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_webview_with_paddings, container, false);

        webView = view.findViewById(R.id.webview);
        URLInterceptorWebViewClient client = new URLInterceptorWebViewClient(getActivity(), webView,
                false, null);
        // treat every link as external link in this view, so that all links will open in external browser
        client.setAllLinksAsExternal(true);


        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        errorNotification = new FullScreenErrorNotification(webView);
        snackbarErrorNotification = new SnackbarErrorNotification(webView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {

            try {
                savedAnnouncements = savedInstanceState.getParcelableArrayList(Router.EXTRA_ANNOUNCEMENTS);
            } catch (Exception ex) {
                logger.error(ex);
            }

        }

        try {
            final Bundle bundle = getArguments();
            courseData = (EnrolledCoursesResponse) bundle.getSerializable(Router.EXTRA_COURSE_DATA);
            FacebookProvider fbProvider = new FacebookProvider();

            if (courseData != null) {
                //Create the inflater used to create the announcement list
                if (savedAnnouncements == null) {
                    loadAnnouncementData(courseData);
                } else {
                    populateAnnouncements(savedAnnouncements);
                }
            }
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (savedAnnouncements != null) {
            outState.putParcelableArrayList(Router.EXTRA_ANNOUNCEMENTS, new ArrayList<Parcelable>(savedAnnouncements));
        }
    }

    private void loadAnnouncementData(EnrolledCoursesResponse enrollment) {
        okHttpClientProvider.getWithOfflineCache().newCall(new Request.Builder()
                .url(enrollment.getCourse().getCourse_updates())
                .get()
                .build())
                .enqueue(new ErrorHandlingOkCallback<List<AnnouncementsModel>>(getActivity(),
                        new TypeToken<List<AnnouncementsModel>>() {
                        }, errorNotification, snackbarErrorNotification,
                        this) {
                    @Override
                    protected void onResponse(final List<AnnouncementsModel> announcementsList) {
                        if (getActivity() == null) {
                            return;
                        }
                        savedAnnouncements = announcementsList;
                        if (announcementsList != null && announcementsList.size() > 0) {
                            populateAnnouncements(announcementsList);
                        } else {
                            errorNotification.showError(R.string.no_announcements_to_display,
                                    R.drawable.ic_error, 0, null);
                        }
                    }

                    @Override
                    protected void onFinish() {
                        if (getActivity() == null) {
                            return;
                        }
                        if (!EventBus.getDefault().isRegistered(CourseAnnouncementsFragment.this)) {
                            EventBus.getDefault().register(CourseAnnouncementsFragment.this);
                        }
                    }
                });

    }

    private void populateAnnouncements(@NonNull List<AnnouncementsModel> announcementsList) {
        errorNotification.hideError();

        StringBuilder buff = WebViewUtil.getIntialWebviewBuffer(getActivity(), logger);

        buff.append("<body>");
        for (AnnouncementsModel model : announcementsList) {
            buff.append("<div class=\"header\">");
            buff.append(model.getDate());
            buff.append("</div>");
            buff.append("<div class=\"separator\"></div>");
            buff.append("<div>");
            buff.append(model.getContent());
            buff.append("</div>");
        }
        buff.append("</body>");

        webView.loadDataWithBaseURL(environment.getConfig().getApiHostURL(), buff.toString(), "text/html", StandardCharsets.UTF_8.name(), null);
    }

    @Subscribe(sticky = true)
    @SuppressWarnings("unused")
    public void onEventMainThread(NetworkConnectivityChangeEvent event) {
        if (!NetworkUtil.isConnected(getContext())) {
            if (!errorNotification.isShowing()) {
                snackbarErrorNotification.showOfflineError(this);
            }
        }
    }

    @Override
    public void onRefresh() {
        errorNotification.hideError();
        loadAnnouncementData(courseData);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onRevisit() {
        if (NetworkUtil.isConnected(getActivity())) {
            snackbarErrorNotification.hideError();
        }
    }
}
