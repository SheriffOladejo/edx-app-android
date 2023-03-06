package com.grassroot.academy.view;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;

import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseFragment;
import com.grassroot.academy.databinding.FragmentAddPostBinding;
import com.grassroot.academy.discussion.DiscussionService;
import com.grassroot.academy.discussion.DiscussionThreadPostedEvent;
import com.grassroot.academy.http.callback.CallTrigger;
import com.grassroot.academy.http.callback.ErrorHandlingCallback;
import com.grassroot.academy.http.notifications.DialogErrorNotification;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.model.discussion.CourseTopics;
import com.grassroot.academy.model.discussion.DiscussionThread;
import com.grassroot.academy.model.discussion.DiscussionTopic;
import com.grassroot.academy.model.discussion.DiscussionTopicDepth;
import com.grassroot.academy.model.discussion.ThreadBody;
import com.grassroot.academy.module.analytics.Analytics;
import com.grassroot.academy.module.analytics.AnalyticsRegistry;
import com.grassroot.academy.util.SoftKeyboardUtil;
import com.grassroot.academy.view.adapters.TopicSpinnerAdapter;
import com.grassroot.academy.view.common.TaskProgressCallback.ProgressViewController;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;

@AndroidEntryPoint
public class DiscussionAddPostFragment extends BaseFragment {

    protected final Logger logger = new Logger(getClass().getName());

    @Inject
    DiscussionService discussionService;

    @Inject
    AnalyticsRegistry analyticsRegistry;

    private EnrolledCoursesResponse courseData;
    private DiscussionTopic discussionTopic;

    private ViewGroup container;

    private Call<CourseTopics> getTopicListCall;
    private Call<DiscussionThread> createThreadCall;

    private int selectedTopicIndex;
    private FragmentAddPostBinding binding;

    @Inject
    public DiscussionAddPostFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseExtras();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        binding = FragmentAddPostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.discussionQuestionSegmentedGroup.setOnCheckedChangeListener((group, checkedId) -> {
            @StringRes final int bodyHint;
            @StringRes final int submitLabel;
            @StringRes final int submitDescription;
            if (binding.discussionQuestionSegmentedGroup.getCheckedRadioButtonId() == R.id.discussion_radio_button) {
                bodyHint = R.string.discussion_body_hint_discussion;
                submitLabel = R.string.discussion_add_post_button_label;
                submitDescription = R.string.discussion_add_post_button_description;
            } else {
                bodyHint = R.string.discussion_body_hint_question;
                submitLabel = R.string.discussion_add_question_button_label;
                submitDescription = R.string.discussion_add_question_button_description;
            }
            binding.bodyEditText.setHint(bodyHint);
            binding.addPostButtonText.setText(submitLabel);
            binding.addPostButton.setContentDescription(getText(submitDescription));
        });
        binding.discussionQuestionSegmentedGroup.check(R.id.discussion_radio_button);

        getTopicList();

        binding.topicsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Even though we disabled topics that aren't supposed to be selected, Android still allows you to select them using keyboard or finger-dragging
                // So, we have to revert the user's selection if they select a topic that cannot be posted to
                final DiscussionTopicDepth item = (DiscussionTopicDepth) parent.getItemAtPosition(position);
                if (null == item || item.isPostable()) {
                    selectedTopicIndex = position;
                } else {
                    // Revert selection
                    parent.setSelection(selectedTopicIndex);
                }
                setPostButtonEnabledState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setPostButtonEnabledState();
            }
        });

        ViewCompat.setBackgroundTintList(binding.topicsSpinner, getResources().getColorStateList(R.color.primaryBaseColor));

        binding.addPostButton.setOnClickListener(v -> {
            Activity activity = requireActivity();
            SoftKeyboardUtil.hide(activity);

            final String title = binding.titleEditText.getText().toString();
            final String body = binding.bodyEditText.getText().toString();

            final DiscussionThread.ThreadType discussionQuestion;
            if (binding.discussionQuestionSegmentedGroup.getCheckedRadioButtonId() == R.id.discussion_radio_button) {
                discussionQuestion = DiscussionThread.ThreadType.DISCUSSION;
            } else {
                discussionQuestion = DiscussionThread.ThreadType.QUESTION;
            }

            ThreadBody threadBody = new ThreadBody();
            threadBody.setCourseId(courseData.getCourse().getId());
            threadBody.setTitle(title);
            threadBody.setRawBody(body);
            threadBody.setTopicId(((DiscussionTopicDepth) binding.topicsSpinner.getSelectedItem()).getDiscussionTopic().getIdentifier());
            threadBody.setType(discussionQuestion);

            binding.addPostButton.setEnabled(false);
            createThread(threadBody);
        });
        binding.addPostButton.setEnabled(false);
        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setPostButtonEnabledState();
            }
        };
        binding.titleEditText.addTextChangedListener(textWatcher);
        binding.bodyEditText.addTextChangedListener(textWatcher);
    }

    private void parseExtras() {
        courseData = (EnrolledCoursesResponse) getArguments().getSerializable(Router.EXTRA_COURSE_DATA);
        discussionTopic = (DiscussionTopic) getArguments().getSerializable(Router.EXTRA_DISCUSSION_TOPIC);
    }

    private void setPostButtonEnabledState() {
        final String title = binding.titleEditText.getText().toString();
        final String body = binding.bodyEditText.getText().toString();
        final boolean topicSelected = null != binding.topicsSpinner.getSelectedItem();
        binding.addPostButton.setEnabled(topicSelected && title.trim().length() > 0 && body.trim().length() > 0);
    }

    protected void createThread(ThreadBody threadBody) {
        if (createThreadCall != null) {
            createThreadCall.cancel();
        }
        createThreadCall = discussionService.createThread(threadBody);
        createThreadCall.enqueue(new ErrorHandlingCallback<DiscussionThread>(
                requireActivity(),
                new ProgressViewController(binding.buttonProgressIndicator.progressIndicator),
                new DialogErrorNotification(this)) {
            @Override
            protected void onResponse(@NonNull final DiscussionThread courseTopics) {
                EventBus.getDefault().post(new DiscussionThreadPostedEvent(courseTopics));
                requireActivity().finish();
            }

            @Override
            protected void onFailure(@NonNull final Throwable error) {
                binding.addPostButton.setEnabled(true);
            }
        });
    }

    protected void getTopicList() {
        if (getTopicListCall != null) {
            getTopicListCall.cancel();
        }
        getTopicListCall = discussionService.getCourseTopics(courseData.getCourse().getId());
        getTopicListCall.enqueue(new ErrorHandlingCallback<CourseTopics>(
                requireActivity(), null, null, CallTrigger.LOADING_CACHED) {
            @Override
            protected void onResponse(@NonNull final CourseTopics courseTopics) {
                final ArrayList<DiscussionTopic> allTopics = new ArrayList<>();
                allTopics.addAll(courseTopics.getNonCoursewareTopics());
                allTopics.addAll(courseTopics.getCoursewareTopics());

                final TopicSpinnerAdapter adapter = new TopicSpinnerAdapter(container.getContext(), DiscussionTopicDepth.createFromDiscussionTopics(allTopics));
                binding.topicsSpinner.setAdapter(adapter);

                {
                    // Attempt to select the topic that we navigated from
                    // Otherwise, leave the default option, which is the first non-courseware topic
                    if (!discussionTopic.isAllType() && !discussionTopic.isFollowingType()) {
                        int selectedTopicIndex = -1;
                        if (discussionTopic.getIdentifier() == null) {
                            // In case of a parent topic, we need to select the first child topic
                            if (!discussionTopic.getChildren().isEmpty()) {
                                selectedTopicIndex = adapter.getPosition(discussionTopic.getChildren().get(0));
                            }
                        } else {
                            selectedTopicIndex = adapter.getPosition(discussionTopic);
                        }
                        if (selectedTopicIndex >= 0) {
                            binding.topicsSpinner.setSelection(selectedTopicIndex);
                        }
                    }
                }

                DiscussionTopic selectedTopic = ((DiscussionTopicDepth) binding.topicsSpinner.getSelectedItem()).getDiscussionTopic();
                Map<String, String> values = new HashMap<>();
                values.put(Analytics.Keys.TOPIC_ID, selectedTopic.getIdentifier());
                analyticsRegistry.trackScreenView(Analytics.Screens.FORUM_CREATE_TOPIC_THREAD,
                        courseData.getCourse().getId(), selectedTopic.getName(), values);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            SoftKeyboardUtil.clearViewFocus(binding.titleEditText);
        }
    }
}
