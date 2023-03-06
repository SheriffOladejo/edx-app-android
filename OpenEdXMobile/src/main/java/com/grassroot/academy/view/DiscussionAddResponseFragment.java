package com.grassroot.academy.view;

import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.grassroot.academy.base.BaseFragment;
import com.grassroot.academy.databinding.FragmentAddResponseOrCommentBinding;
import com.grassroot.academy.discussion.DiscussionCommentPostedEvent;
import com.grassroot.academy.discussion.DiscussionService;
import com.grassroot.academy.http.callback.ErrorHandlingCallback;
import com.grassroot.academy.http.notifications.DialogErrorNotification;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.model.discussion.CommentBody;
import com.grassroot.academy.model.discussion.DiscussionComment;
import com.grassroot.academy.model.discussion.DiscussionThread;
import com.grassroot.academy.module.analytics.Analytics;
import com.grassroot.academy.module.analytics.AnalyticsRegistry;
import com.grassroot.academy.util.Config;
import com.grassroot.academy.util.SoftKeyboardUtil;
import com.grassroot.academy.view.common.TaskProgressCallback.ProgressViewController;
import com.grassroot.academy.view.view_holders.AuthorLayoutViewHolder;
import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;

@AndroidEntryPoint
public class DiscussionAddResponseFragment extends BaseFragment {

    static public String TAG = DiscussionAddResponseFragment.class.getCanonicalName();

    private DiscussionThread discussionThread;

    protected final Logger logger = new Logger(getClass().getName());

    private Call<DiscussionComment> createCommentCall;

    @Inject
    DiscussionService discussionService;

    @Inject
    Router router;

    @Inject
    AnalyticsRegistry analyticsRegistry;

    @Inject
    Config config;

    private FragmentAddResponseOrCommentBinding binding;

    @Inject
    public DiscussionAddResponseFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseExtras();

        final Map<String, String> values = new HashMap<>();
        values.put(Analytics.Keys.TOPIC_ID, discussionThread.getTopicId());
        values.put(Analytics.Keys.THREAD_ID, discussionThread.getIdentifier());
        if (!discussionThread.isAuthorAnonymous()) {
            values.put(Analytics.Keys.AUTHOR, discussionThread.getAuthor());
        }
        analyticsRegistry.trackScreenView(Analytics.Screens.FORUM_ADD_RESPONSE,
                discussionThread.getCourseId(), discussionThread.getTitle(), values);
    }

    private void parseExtras() {
        discussionThread = (DiscussionThread) getArguments().getSerializable(Router.EXTRA_DISCUSSION_THREAD);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddResponseOrCommentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.tvTitle.setVisibility(View.VISIBLE);
        binding.tvTitle.setText(discussionThread.getTitle());

        binding.discussionRenderBody.setBody(discussionThread.getRenderedBody());
        AuthorLayoutViewHolder authorLayoutViewHolder =
                new AuthorLayoutViewHolder(binding.rowDiscussionUserProfile.discussionUserProfileRow);
        authorLayoutViewHolder.populateViewHolder(config, discussionThread, discussionThread,
                System.currentTimeMillis(),
                () -> router.showUserProfile(requireActivity(), discussionThread.getAuthor()));

        binding.btnAddComment.setOnClickListener(v -> createComment());
        binding.btnAddComment.setEnabled(false);
        binding.etNewComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.btnAddComment.setEnabled(s.toString().trim().length() > 0);
            }
        });
    }

    private void createComment() {
        binding.btnAddComment.setEnabled(false);

        if (createCommentCall != null) {
            createCommentCall.cancel();
        }

        createCommentCall = discussionService.createComment(new CommentBody(
                discussionThread.getIdentifier(), binding.etNewComment.getText().toString(), null));
        createCommentCall.enqueue(new ErrorHandlingCallback<DiscussionComment>(
                requireContext(),
                new ProgressViewController(binding.buttonProgressIndicator.progressIndicator),
                new DialogErrorNotification(this)) {
            @Override
            protected void onResponse(@NonNull final DiscussionComment thread) {
                logger.debug(thread.toString());
                EventBus.getDefault().post(new DiscussionCommentPostedEvent(thread, null));
                requireActivity().finish();
            }

            @Override
            protected void onFailure(@NonNull final Throwable error) {
                binding.btnAddComment.setEnabled(true);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            SoftKeyboardUtil.clearViewFocus(binding.etNewComment);
        }
    }
}
