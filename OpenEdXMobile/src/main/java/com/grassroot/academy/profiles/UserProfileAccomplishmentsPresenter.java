package com.grassroot.academy.profiles;

import androidx.annotation.NonNull;

import com.grassroot.academy.http.callback.Callback;
import com.grassroot.academy.model.Page;
import com.grassroot.academy.model.api.ProfileModel;
import com.grassroot.academy.model.profile.BadgeAssertion;
import com.grassroot.academy.module.prefs.UserPrefs;
import com.grassroot.academy.user.UserService;
import com.grassroot.academy.view.ViewHoldingPresenter;
import com.grassroot.academy.view.adapters.InfiniteScrollUtils;

import java.util.ArrayList;
import java.util.List;

public class UserProfileAccomplishmentsPresenter extends ViewHoldingPresenter<UserProfileAccomplishmentsPresenter.ViewInterface> {

    @NonNull
    private final UserService userService;

    @NonNull
    private final String username;

    private final boolean viewingOwnProfile;

    private InfiniteScrollUtils.PageLoadController pageLoadController;

    private int page = 1;

    @NonNull
    private List<BadgeAssertion> badges = new ArrayList<>();

    private boolean pageLoading = false;

    public UserProfileAccomplishmentsPresenter(@NonNull UserService userService, @NonNull UserPrefs userPrefs, @NonNull String username) {
        this.userService = userService;
        this.username = username;
        final ProfileModel model = userPrefs.getProfile();
        viewingOwnProfile = model.username.equalsIgnoreCase(username);
    }

    @Override
    public void attachView(@NonNull final ViewInterface view) {
        super.attachView(view);
        pageLoadController = new InfiniteScrollUtils.PageLoadController<>(new InfiniteScrollUtils.ListContentController<BadgeAssertion>() {
            @Override
            public void clear() {
                badges.clear();
                setViewModel();
            }

            @Override
            public void addAll(List<BadgeAssertion> items) {
                badges.addAll(items);
                setViewModel();
            }

            @Override
            public void setProgressVisible(boolean visible) {
                pageLoading = visible;
                setViewModel();
            }
        }, new InfiniteScrollUtils.PageLoader<BadgeAssertion>() {
            @Override
            public void loadNextPage(@NonNull final InfiniteScrollUtils.PageLoadCallback<BadgeAssertion> callback) {
                userService.getBadges(username, page).enqueue(new Callback<Page<BadgeAssertion>>() {
                    @Override
                    protected void onResponse(@NonNull final Page<BadgeAssertion> badges) {
                        ++page;
                        callback.onPageLoaded(badges);
                    }

                    @Override
                    protected void onFailure(@NonNull Throwable error) {
                        // do nothing. Better to just deal show what we can
                    }
                });
            }
        });
        pageLoadController.loadMore();
    }

    private void setViewModel() {
        assert getView() != null;
        getView().setModel(new ViewModel(badges, pageLoading, viewingOwnProfile));
    }

    public void onScrolledToEnd() {
        if (null == pageLoadController) {
            return;
        }
        pageLoadController.loadMore();
    }

    public void onClickShare(@NonNull BadgeAssertion badgeAssertion) {
        assert getView() != null;
        getView().startBadgeShareIntent(badgeAssertion.getAssertionUrl());
    }

    public interface ViewInterface {
        void setModel(@NonNull ViewModel model);

        void startBadgeShareIntent(@NonNull String sharedContent);
    }

    public static class ViewModel {
        @NonNull
        public final List<BadgeAssertion> badges;
        public final boolean pageLoading;
        public final boolean enableSharing;

        public ViewModel(@NonNull List<BadgeAssertion> badges, boolean pageLoading, boolean enableSharing) {
            this.badges = badges;
            this.pageLoading = pageLoading;
            this.enableSharing = enableSharing;
        }
    }
}
