package com.grassroot.academy.base;

import static com.grassroot.academy.view.Router.EXTRA_PATH_ID;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.grassroot.academy.databinding.FragmentWebviewBinding;
import com.grassroot.academy.http.notifications.FullScreenErrorNotification;
import com.grassroot.academy.interfaces.WebViewStatusListener;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.util.links.DefaultActionListener;
import com.grassroot.academy.view.BaseWebViewFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WebViewProgramInfoFragment extends BaseWebViewFragment
        implements WebViewStatusListener {
    private FragmentWebviewBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWebviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setWebViewActionListener();
        loadUrl(getInitialUrl());
    }

    public void setWebViewActionListener() {
        client.setActionListener(new DefaultActionListener(getActivity(), progressWheel,
                new DefaultActionListener.EnrollCallback() {
                    @Override
                    public void onResponse(@NonNull EnrolledCoursesResponse course) {
                    }

                    @Override
                    public void onFailure(@NonNull Throwable error) {
                    }

                    @Override
                    public void onUserNotLoggedIn(@NonNull String courseId, boolean emailOptIn) {
                    }
                }));
    }

    @Override
    public FullScreenErrorNotification initFullScreenErrorNotification() {
        return new FullScreenErrorNotification(binding.webview);
    }

    /**
     * Loads the given URL into {@link #webView}.
     *
     * @param url The URL to load.
     */
    @Override
    protected void loadUrl(@NonNull String url) {
        if (client != null) {
            client.setLoadingInitialUrl(true);
        }
        super.loadUrl(url);
    }

    /**
     * By default, all links will not be treated as external.
     * Depends on host, as long as the links have same host, they are treated as non-external links.
     *
     * @return
     */
    protected boolean isAllLinksExternal() {
        return true;
    }

    @Override
    public void onRefresh() {
        loadUrl(getInitialUrl());
    }

    @NonNull
    protected String getInitialUrl() {
        if (URLUtil.isValidUrl(binding.webview.getUrl())) {
            return binding.webview.getUrl();
        } else if (getArguments() != null) {
            final String pathId = getArguments().getString(EXTRA_PATH_ID);
            return environment.getConfig().getDiscoveryConfig()
                    .getProgramUrlTemplate()
                    .replace("{" + EXTRA_PATH_ID + "}", pathId);
        }
        return environment.getConfig().getDiscoveryConfig().getBaseUrl();
    }

    @Override
    protected boolean isShowingFullScreenError() {
        return errorNotification != null && errorNotification.isShowing();
    }
}
