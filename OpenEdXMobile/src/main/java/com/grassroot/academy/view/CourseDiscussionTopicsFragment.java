package com.grassroot.academy.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.grassroot.academy.R;
import com.grassroot.academy.databinding.FragmentDiscussionTopicsBinding;
import com.grassroot.academy.discussion.DiscussionService;
import com.grassroot.academy.event.CourseDashboardRefreshEvent;
import com.grassroot.academy.event.NetworkConnectivityChangeEvent;
import com.grassroot.academy.http.callback.ErrorHandlingCallback;
import com.grassroot.academy.http.notifications.FullScreenErrorNotification;
import com.grassroot.academy.interfaces.RefreshListener;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.model.discussion.CourseTopics;
import com.grassroot.academy.model.discussion.DiscussionTopic;
import com.grassroot.academy.model.discussion.DiscussionTopicDepth;
import com.grassroot.academy.util.SoftKeyboardUtil;
import com.grassroot.academy.util.UiUtils;
import com.grassroot.academy.view.adapters.DiscussionTopicsAdapter;
import com.grassroot.academy.view.common.TaskProgressCallback;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;

@AndroidEntryPoint
public class CourseDiscussionTopicsFragment extends OfflineSupportBaseFragment
        implements RefreshListener {
    private static final Logger logger = new Logger(CourseDiscussionTopicsFragment.class.getName());

    private EnrolledCoursesResponse courseData;

    @Inject
    DiscussionService discussionService;

    @Inject
    DiscussionTopicsAdapter discussionTopicsAdapter;

    @Inject
    Router router;

    private Call<CourseTopics> getTopicListCall;

    private FullScreenErrorNotification errorNotification;
    private FragmentDiscussionTopicsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        courseData = (EnrolledCoursesResponse) getArguments().getSerializable(Router.EXTRA_COURSE_DATA);
        binding = FragmentDiscussionTopicsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        errorNotification = new FullScreenErrorNotification((View) binding.discussionTopicsListview.getParent());

        final LayoutInflater inflater = LayoutInflater.from(requireActivity());

        // Add "All posts" item
        {
            final TextView header = (TextView) inflater.inflate(R.layout.row_discussion_topic, binding.discussionTopicsListview, false);
            header.setText(R.string.discussion_posts_filter_all_posts);

            final DiscussionTopic discussionTopic = new DiscussionTopic();
            discussionTopic.setIdentifier(DiscussionTopic.ALL_TOPICS_ID);
            discussionTopic.setName(getString(R.string.discussion_posts_filter_all_posts));
            binding.discussionTopicsListview.addHeaderView(header, new DiscussionTopicDepth(discussionTopic, 0, true), true);
        }

        // Add "Posts I'm following" item
        {
            final TextView header = (TextView) inflater.inflate(R.layout.row_discussion_topic,
                    binding.discussionTopicsListview, false);
            header.setText(R.string.forum_post_i_am_following);
            UiUtils.INSTANCE.setTextViewDrawableStart(requireContext(), header, R.drawable.ic_star_rate,
                    R.dimen.edx_base, R.color.primaryBaseColor);
            final DiscussionTopic discussionTopic = new DiscussionTopic();
            discussionTopic.setIdentifier(DiscussionTopic.FOLLOWING_TOPICS_ID);
            discussionTopic.setName(getString(R.string.forum_post_i_am_following));
            binding.discussionTopicsListview.addHeaderView(header, new DiscussionTopicDepth(discussionTopic, 0, true), true);
        }

        binding.discussionTopicsListview.setAdapter(discussionTopicsAdapter);

        binding.discussionTopicsSearchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query == null || query.trim().isEmpty())
                    return false;
                router.showCourseDiscussionPostsForSearchQuery(requireActivity(), query, courseData);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        binding.discussionTopicsListview.setOnItemClickListener(
                (parent, view1, position, id) -> router.showCourseDiscussionPostsForDiscussionTopic(
                        requireActivity(),
                        ((DiscussionTopicDepth) parent.getItemAtPosition(position)).getDiscussionTopic(),
                        courseData));

        getTopicList();
        showCourseDiscussionTopic();
    }

    private void getTopicList() {
        if (getTopicListCall != null) {
            getTopicListCall.cancel();
        }
        final TaskProgressCallback.ProgressViewController progressViewController =
                new TaskProgressCallback.ProgressViewController(binding.loadingIndicator.loadingIndicator);
        getTopicListCall = discussionService.getCourseTopics(courseData.getCourse().getId());
        getTopicListCall.enqueue(new ErrorHandlingCallback<CourseTopics>(
                requireActivity(), progressViewController, errorNotification, null, this) {
            @Override
            protected void onResponse(@NonNull final CourseTopics courseTopics) {
                logger.debug("GetTopicListTask success=" + courseTopics);
                ArrayList<DiscussionTopic> allTopics = new ArrayList<>();
                allTopics.addAll(courseTopics.getNonCoursewareTopics());
                allTopics.addAll(courseTopics.getCoursewareTopics());

                List<DiscussionTopicDepth> allTopicsWithDepth = DiscussionTopicDepth.createFromDiscussionTopics(allTopics);
                discussionTopicsAdapter.setItems(allTopicsWithDepth);
                discussionTopicsAdapter.notifyDataSetChanged();
            }

            @Override
            protected void onFinish() {
                if (!EventBus.getDefault().isRegistered(CourseDiscussionTopicsFragment.this)) {
                    EventBus.getDefault().register(CourseDiscussionTopicsFragment.this);
                }
            }
        });
    }

    private void showCourseDiscussionTopic() {
        final String topicId = getArguments().getString(Router.EXTRA_DISCUSSION_TOPIC_ID);
        if (!TextUtils.isEmpty(topicId)) {
            router.showCourseDiscussionPostsForDiscussionTopic(
                    requireActivity(),
                    getArguments().getString(Router.EXTRA_DISCUSSION_TOPIC_ID),
                    getArguments().getString(Router.EXTRA_DISCUSSION_THREAD_ID),
                    courseData);

            // Setting this to null, so that upon recreation of the fragment, relevant activity
            // shouldn't be auto-created again (e.g. due to a deep link).
            getArguments().putString(Router.EXTRA_DISCUSSION_TOPIC_ID, null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SoftKeyboardUtil.clearViewFocus(binding.discussionTopicsSearchview);
    }

    @Subscribe(sticky = true)
    @SuppressWarnings("unused")
    public void onEvent(CourseDashboardRefreshEvent event) {
        errorNotification.hideError();
        getTopicList();
    }

    @Override
    public void onRefresh() {
        EventBus.getDefault().post(new CourseDashboardRefreshEvent());
    }

    @Override
    protected boolean isShowingFullScreenError() {
        return errorNotification != null && errorNotification.isShowing();
    }

    @Subscribe(sticky = true)
    @SuppressWarnings("unused")
    public void onEvent(NetworkConnectivityChangeEvent event) {
        onNetworkConnectivityChangeEvent(event);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
    }
}
