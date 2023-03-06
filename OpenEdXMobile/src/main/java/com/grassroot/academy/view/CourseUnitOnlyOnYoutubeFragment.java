package com.grassroot.academy.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.grassroot.academy.R;
import com.grassroot.academy.model.course.CourseComponent;
import com.grassroot.academy.model.course.VideoBlockModel;
import com.grassroot.academy.util.AppConstants;
import com.grassroot.academy.util.BrowserUtil;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CourseUnitOnlyOnYoutubeFragment extends CourseUnitFragment {

    public static CourseUnitOnlyOnYoutubeFragment newInstance(CourseComponent unit) {
        final CourseUnitOnlyOnYoutubeFragment fragment = new CourseUnitOnlyOnYoutubeFragment();
        final Bundle args = new Bundle();
        args.putSerializable(Router.EXTRA_COURSE_UNIT, unit);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_course_unit_only_on_youtube, container, false);
        TextView tvYouTubeMessage = view.findViewById(R.id.only_youtube_available_message);
        if (environment.getConfig().getYoutubePlayerConfig().isYoutubePlayerEnabled()) {
            tvYouTubeMessage.setText(R.string.assessment_needed_updating_youtube);
            view.findViewById(R.id.update_youtube_button).setVisibility(View.VISIBLE);
            view.findViewById(R.id.update_youtube_button).setOnClickListener(v -> {
                BrowserUtil.open(getActivity(), AppConstants.BROWSER_PLAYSTORE_YOUTUBE_URI, true);
            });
        } else {
            tvYouTubeMessage.setText(R.string.assessment_only_on_youtube);
        }

        view.findViewById(R.id.view_on_youtube_button).setOnClickListener(v -> {
            BrowserUtil.open(getActivity(), ((VideoBlockModel) unit).getData().encodedVideos.getYoutubeVideoInfo().url, true);
        });

        return view;
    }
}
