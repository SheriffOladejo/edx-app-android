package com.grassroot.academy.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseFragment;
import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.databinding.FragmentMainDiscoveryBinding;
import com.grassroot.academy.deeplink.Screen;
import com.grassroot.academy.deeplink.ScreenDef;
import com.grassroot.academy.event.ScreenArgumentsEvent;
import com.grassroot.academy.view.dialog.NativeFindCoursesFragment;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainDiscoveryFragment extends BaseFragment {
    @Inject
    protected IEdxEnvironment environment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return FragmentMainDiscoveryBinding.inflate(inflater, container, false)
                .getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initFragments();
        EventBus.getDefault().register(this);
        if (getArguments() != null) {
            handleDeepLink(getArguments());
        }
    }

    private void initFragments() {
        if (environment.getConfig().getDiscoveryConfig().isDiscoveryEnabled()) {
            Fragment discoveryFragment;
            if (environment.getConfig().getDiscoveryConfig().isWebViewDiscoveryEnabled()) {
                discoveryFragment = getChildFragmentManager().findFragmentByTag("fragment_discovery_webview");
                if (discoveryFragment == null) {
                    discoveryFragment = new WebViewDiscoverFragment();
                    commitFragmentTransaction(discoveryFragment, "fragment_discovery_webview");
                }
            } else {
                discoveryFragment = getChildFragmentManager().findFragmentByTag("fragment_discovery_native");
                if (discoveryFragment == null) {
                    discoveryFragment = new NativeFindCoursesFragment();
                    commitFragmentTransaction(discoveryFragment, "fragment_discovery_native");
                }
            }
            discoveryFragment.setArguments(getArguments());
        }
    }

    private void commitFragmentTransaction(@NonNull Fragment fragment, @Nullable String tag) {
        final FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fl_container, fragment, tag);
        fragmentTransaction.commit();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEventMainThread(@NonNull ScreenArgumentsEvent event) {
        handleDeepLink(event.getBundle());
    }

    private void handleDeepLink(@NonNull Bundle bundle) {
        @ScreenDef final String screenName = bundle.getString(Router.EXTRA_SCREEN_NAME);
        if (screenName == null) {
            return;
        }

        final String pathId = bundle.getString(Router.EXTRA_PATH_ID);
        if (!TextUtils.isEmpty(pathId)) {
            switch (screenName) {
                case Screen.PROGRAM:
                    environment.getRouter().showProgramWebViewActivity(getActivity(),
                            environment, pathId, getActivity().getString(R.string.label_my_programs));
                    break;
                case Screen.DISCOVERY_COURSE_DETAIL:
                    environment.getRouter().showCourseInfo(getActivity(), pathId);
                    break;
                case Screen.DISCOVERY_PROGRAM_DETAIL:
                    environment.getRouter().showProgramInfo(getActivity(), pathId);
                    break;
            }
        }
        // Setting this to null, so that upon recreation of the fragment, relevant activity
        // shouldn't be auto created again.
        bundle.putString(Router.EXTRA_SCREEN_NAME, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }
}
