package com.grassroot.academy.view;


import static com.grassroot.academy.deeplink.Screen.COURSE_ANNOUNCEMENT;
import static com.grassroot.academy.deeplink.Screen.COURSE_HANDOUT;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.grassroot.academy.R;
import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.deeplink.ScreenDef;
import com.grassroot.academy.event.NetworkConnectivityChangeEvent;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.util.UiUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ResourcesFragment extends OfflineSupportBaseFragment {

    @Inject
    IEdxEnvironment environment;

    private EnrolledCoursesResponse courseData;

    public static Bundle makeArguments(@NonNull EnrolledCoursesResponse model, @Nullable @ScreenDef String screenName) {
        final Bundle arguments = new Bundle();
        arguments.putSerializable(Router.EXTRA_COURSE_DATA, model);
        arguments.putSerializable(Router.EXTRA_SCREEN_NAME, screenName);
        return arguments;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        EventBus.getDefault().register(ResourcesFragment.this);
        courseData = (EnrolledCoursesResponse) getArguments().getSerializable(Router.EXTRA_COURSE_DATA);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_resources, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final LinearLayout parent = (LinearLayout) view.findViewById(R.id.root);
        ViewHolder holder;

        holder = createViewHolder(inflater, parent);

        holder.typeView.setImageDrawable(UiUtils.INSTANCE.getDrawable(requireContext(), R.drawable.ic_description));
        holder.titleView.setText(R.string.handouts_title);
        holder.subtitleView.setText(R.string.handouts_subtitle);
        holder.rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (courseData != null) {
                    environment.getRouter().showHandouts(getActivity(), courseData);
                }
            }
        });

        holder = createViewHolder(inflater, parent);

        holder.typeView.setImageDrawable(UiUtils.INSTANCE.getDrawable(requireContext(), R.drawable.ic_campaign));
        holder.titleView.setText(R.string.announcement_title);
        holder.subtitleView.setText(R.string.announcement_subtitle);
        holder.rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (courseData != null) {
                    environment.getRouter().showCourseAnnouncement(getActivity(), courseData);
                }
            }
        });

        final Bundle arguments = getArguments();
        @ScreenDef String screenName;
        if (arguments != null) {
            screenName = arguments.getString(Router.EXTRA_SCREEN_NAME);
            if (!TextUtils.isEmpty(screenName)) {
                switch (screenName) {
                    case COURSE_HANDOUT:
                        environment.getRouter().showHandouts(getActivity(), courseData);
                        break;
                    case COURSE_ANNOUNCEMENT:
                        environment.getRouter().showCourseAnnouncement(getActivity(), courseData);
                        break;
                }
                // Setting this to null, so that upon recreation of the fragment, relevant activity
                // shouldn't be auto created again.
                arguments.putString(Router.EXTRA_SCREEN_NAME, null);
            }
        }
    }

    private ViewHolder createViewHolder(LayoutInflater inflater, LinearLayout parent) {
        ViewHolder holder = new ViewHolder();
        holder.rowView = inflater.inflate(R.layout.row_resource_list, parent, false);
        holder.typeView = (AppCompatImageView) holder.rowView.findViewById(R.id.row_type);
        holder.titleView = (TextView) holder.rowView.findViewById(R.id.row_title);
        holder.subtitleView = (TextView) holder.rowView.findViewById(R.id.row_subtitle);
        parent.addView(holder.rowView);
        return holder;
    }

    private class ViewHolder {
        View rowView;
        AppCompatImageView typeView;
        TextView titleView;
        TextView subtitleView;
    }

    @Subscribe(sticky = true)
    @SuppressWarnings("unused")
    public void onEvent(NetworkConnectivityChangeEvent event) {
        onNetworkConnectivityChangeEvent(event);
    }

    @Override
    protected boolean isShowingFullScreenError() {
        return false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
    }
}
