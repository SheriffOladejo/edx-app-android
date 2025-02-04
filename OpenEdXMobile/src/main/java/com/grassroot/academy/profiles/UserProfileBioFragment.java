package com.grassroot.academy.profiles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.grassroot.academy.R;
import com.grassroot.academy.databinding.FragmentUserProfileBioBinding;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.model.profile.UserProfileBioModel;
import com.grassroot.academy.util.ResourceUtil;
import com.grassroot.academy.view.PresenterFragment;
import com.grassroot.academy.view.Router;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class UserProfileBioFragment extends PresenterFragment<UserProfileBioPresenter, UserProfileBioPresenter.ViewInterface> implements ScrollingPreferenceChild {

    private final Logger logger = new Logger(getClass().getName());

    @Inject
    Router router;

    private boolean prefersScrollingHeader = false;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return FragmentUserProfileBioBinding.inflate(inflater, container, false).getRoot();
    }

    public static UserProfileBioFragment newInstance() {
        UserProfileBioFragment fragment = new UserProfileBioFragment();
        return fragment;
    }

    @NonNull
    @Override
    protected UserProfileBioPresenter createPresenter() {
        Fragment parent = getParentFragment();
        UserProfileBioTabParent owner = (UserProfileBioTabParent) parent;

        return new UserProfileBioPresenter(owner.getBioInteractor());
    }

    @NonNull
    @Override
    protected UserProfileBioPresenter.ViewInterface createView() {
        final FragmentUserProfileBioBinding viewHolder = DataBindingUtil.getBinding(getView());

        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onEditProfile();
            }
        };
        viewHolder.incompleteEditProfileButton.setOnClickListener(listener);

        return new UserProfileBioPresenter.ViewInterface() {

            @Override
            public void showBio(UserProfileBioModel bio) {
                viewHolder.profileBioContent.setVisibility(View.VISIBLE);
                viewHolder.incompleteContainer.setVisibility(bio.contentType == UserProfileBioModel.ContentType.INCOMPLETE ? View.VISIBLE : View.GONE);
                viewHolder.noAboutMe.setVisibility(bio.contentType == UserProfileBioModel.ContentType.NO_ABOUT_ME ? View.VISIBLE : View.GONE);
                viewHolder.bioText.setVisibility(bio.contentType == UserProfileBioModel.ContentType.ABOUT_ME ? View.VISIBLE : View.GONE);
                viewHolder.bioText.setText(bio.bioText);
                viewHolder.bioText.setContentDescription(ResourceUtil.getFormattedString(getResources(), R.string.profile_about_me_description, "about_me", bio.bioText));
                prefersScrollingHeader = bio.contentType == UserProfileBioModel.ContentType.ABOUT_ME;
                ((ScrollingPreferenceParent) getParentFragment()).onChildScrollingPreferenceChanged();
            }

            @Override
            public void navigateToProfileEditor(String username) {
                router.showUserProfileEditor(getActivity(), username);
            }
        };
    }

    @Override
    public boolean prefersScrollingHeader() {
        return prefersScrollingHeader;
    }
}
