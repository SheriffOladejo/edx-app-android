package com.grassroot.academy.profiles;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grassroot.academy.base.PresenterTest;
import com.grassroot.academy.model.profile.UserProfileViewModel;
import com.grassroot.academy.module.analytics.AnalyticsRegistry;
import com.grassroot.academy.module.prefs.UserPrefs;
import com.grassroot.academy.util.observer.CachingObservable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

public class UserProfilePresenterTest extends PresenterTest<UserProfilePresenter, UserProfilePresenter.ViewInterface> {

    static final String PROFILE_USERNAME = "john_doe";

    @Mock
    private UserProfileInteractor userProfileInteractor;
    @Mock
    private UserProfileTabsInteractor userProfileTabsInteractor;
    @Mock
    private UserPrefs userPrefs;
    @Mock
    private AnalyticsRegistry analyticsRegistry;

    private CachingObservable<UserProfileViewModel> accountObservable;

    private CachingObservable<UserProfileImageViewModel> photoObservable;

    private CachingObservable<List<UserProfileTab>> tabsObservable;

    @Before
    public void before() {
        accountObservable = new CachingObservable<>();
        photoObservable = new CachingObservable<>();
        tabsObservable = new CachingObservable<>();
        when(userProfileInteractor.getUsername()).thenReturn(PROFILE_USERNAME);
        when(userProfileInteractor.observeProfile()).thenReturn(accountObservable);
        when(userProfileInteractor.observeProfileImage()).thenReturn(photoObservable);
        when(userProfileTabsInteractor.observeTabs()).thenReturn(tabsObservable);
        startPresenter(new UserProfilePresenter(
                analyticsRegistry,
                userProfileInteractor,
                userProfileTabsInteractor
        ));
    }

    @Test
    public void whenPresenterIsCreated_tracksProfileView() {
        verify(analyticsRegistry).trackProfileViewed(PROFILE_USERNAME);
    }

    @Test
    public void whenInteractorEmitsProfileData_setsProfileOnView() {
        final UserProfileViewModel model = mock(UserProfileViewModel.class);
        accountObservable.onData(model);
        verify(view).showProfile(model);
    }

    @Test
    public void whenInteractorEmitsProfileError_showsErrorOnView() {
        final RuntimeException error = new RuntimeException();
        accountObservable.onError(error);
        verify(view).showError(error);
    }

    @Test
    public void whenInteractorEmitsProfileImage_setsProfileImageOnView() {
        final UserProfileImageViewModel model = mock(UserProfileImageViewModel.class);
        photoObservable.onData(model);
        verify(view).setPhotoImage(model);
    }

    @Test
    public void whenInteractorEmitsProfileImageError_noInteractionsWithView() {
        photoObservable.onError(new RuntimeException());
        verify(view, never()).showError(any(Throwable.class));
    }

    @Test
    public void whenInteractorEmitsTabs_showsTabsOnView() {
        final List<UserProfileTab> tabs = Collections.emptyList();
        tabsObservable.onData(tabs);
        verify(view).showTabs(tabs);
    }

    @Test
    public void onEditProfile_navigatesToProfileEditor() {
        presenter.onEditProfile();
        verify(view).navigateToProfileEditor(PROFILE_USERNAME);
    }
}
