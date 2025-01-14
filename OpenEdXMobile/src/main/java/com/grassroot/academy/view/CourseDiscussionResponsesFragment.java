package com.grassroot.academy.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseFragment;
import com.grassroot.academy.base.BaseFragmentActivity;
import com.grassroot.academy.core.EdxDefaultModule;
import com.grassroot.academy.databinding.FragmentDiscussionResponsesOrCommentsBinding;
import com.grassroot.academy.discussion.DiscussionCommentPostedEvent;
import com.grassroot.academy.discussion.DiscussionService;
import com.grassroot.academy.discussion.DiscussionThreadUpdatedEvent;
import com.grassroot.academy.discussion.DiscussionUtils;
import com.grassroot.academy.http.callback.CallTrigger;
import com.grassroot.academy.http.callback.ErrorHandlingCallback;
import com.grassroot.academy.http.notifications.FullScreenErrorNotification;
import com.grassroot.academy.model.Page;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.model.discussion.DiscussionComment;
import com.grassroot.academy.model.discussion.DiscussionRequestFields;
import com.grassroot.academy.model.discussion.DiscussionThread;
import com.grassroot.academy.module.analytics.Analytics;
import com.grassroot.academy.module.analytics.AnalyticsRegistry;
import com.grassroot.academy.view.adapters.CourseDiscussionResponsesAdapter;
import com.grassroot.academy.view.adapters.InfiniteScrollUtils;
import com.grassroot.academy.view.common.TaskMessageCallback;
import com.grassroot.academy.view.common.TaskProgressCallback;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.scopes.FragmentScoped;
import retrofit2.Call;

@AndroidEntryPoint
public class CourseDiscussionResponsesFragment extends BaseFragment implements CourseDiscussionResponsesAdapter.Listener {

    private DiscussionThread discussionThread;
    private String threadId;
    private EnrolledCoursesResponse courseData;
    private CourseDiscussionResponsesAdapter courseDiscussionResponsesAdapter;

    @Inject
    DiscussionService discussionService;

    @Inject
    Router router;

    @Inject
    AnalyticsRegistry analyticsRegistry;

    private FullScreenErrorNotification errorNotification;

    @Nullable
    private Call<DiscussionThread> getAndReadThreadCall;

    private ResponsesLoader responsesLoader;
    private Call<DiscussionThread> getThreadCall;
    private FragmentDiscussionResponsesOrCommentsBinding binding;

    @Inject
    public CourseDiscussionResponsesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        parseExtras();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDiscussionResponsesOrCommentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        errorNotification = new FullScreenErrorNotification(view.findViewById(R.id.ll_content));
        if (discussionThread == null) {
            if (getThreadCall != null) {
                getThreadCall.cancel();
            }
            getThreadCall = discussionService.getThread(threadId);
            getThreadCall.enqueue(new ErrorHandlingCallback<DiscussionThread>(requireActivity(),
                    new TaskProgressCallback.ProgressViewController(binding.loadingIndicator.loadingIndicator), errorNotification) {
                @Override
                protected void onResponse(@NonNull DiscussionThread responseBody) {
                    discussionThread = responseBody;
                    setScreenTitle();
                    loadThreadResponses();
                }
            });
        } else {
            setScreenTitle();
            loadThreadResponses();
        }
    }

    private void parseExtras() {
        discussionThread = (DiscussionThread) getArguments().getSerializable(Router.EXTRA_DISCUSSION_THREAD);
        threadId = getArguments().getString(Router.EXTRA_DISCUSSION_THREAD_ID);
        courseData = (EnrolledCoursesResponse) getArguments().getSerializable(Router.EXTRA_COURSE_DATA);
    }

    private void loadThreadResponses() {
        final Activity activity = getActivity();
        responsesLoader = new ResponsesLoader(activity,
                discussionThread.getIdentifier(),
                discussionThread.getType() == DiscussionThread.ThreadType.QUESTION);

        courseDiscussionResponsesAdapter = new CourseDiscussionResponsesAdapter(
                activity, this, this, discussionThread, courseData);
        InfiniteScrollUtils.configureRecyclerViewWithInfiniteList(
                binding.discussionRecyclerView, courseDiscussionResponsesAdapter, responsesLoader);
        binding.discussionRecyclerView.setAdapter(courseDiscussionResponsesAdapter);

        responsesLoader.freeze();
        if (getAndReadThreadCall != null) {
            getAndReadThreadCall.cancel();
        }
        final TaskMessageCallback mCallback = activity instanceof TaskMessageCallback ? (TaskMessageCallback) activity : null;
        getAndReadThreadCall = discussionService.setThreadRead(
                discussionThread.getIdentifier(), new DiscussionService.ReadBody(true));
        // Setting a thread's "read" state gives us back the updated Thread object.
        getAndReadThreadCall.enqueue(new ErrorHandlingCallback<DiscussionThread>(
                activity, null, mCallback, CallTrigger.LOADING_UNCACHED) {
            @Override
            protected void onResponse(@NonNull final DiscussionThread discussionThread) {
                courseDiscussionResponsesAdapter.updateDiscussionThread(discussionThread);
                responsesLoader.unfreeze();
                EventBus.getDefault().post(new DiscussionThreadUpdatedEvent(discussionThread));
            }
        });

        DiscussionUtils.setStateOnTopicClosed(discussionThread.isClosed(),
                binding.createNewItem.createNewItemTextView, R.string.discussion_responses_add_response_button,
                R.string.discussion_add_response_disabled_title, binding.createNewItem.createNewItemLayout,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        router.showCourseDiscussionAddResponse(activity, discussionThread);
                    }
                });

        binding.createNewItem.createNewItemLayout.setEnabled(!courseData.isDiscussionBlackedOut());
        final Map<String, String> values = new HashMap<>();
        values.put(Analytics.Keys.TOPIC_ID, discussionThread.getTopicId());
        values.put(Analytics.Keys.THREAD_ID, discussionThread.getIdentifier());
        if (!discussionThread.isAuthorAnonymous()) {
            values.put(Analytics.Keys.AUTHOR, discussionThread.getAuthor());
        }
        analyticsRegistry.trackScreenView(Analytics.Screens.FORUM_VIEW_THREAD,
                courseData.getCourse().getId(), discussionThread.getTitle(), values);
    }

    private void setScreenTitle() {
        switch (discussionThread.getType()) {
            case DISCUSSION:
                getActivity().setTitle(R.string.discussion_title);
                break;
            case QUESTION:
                getActivity().setTitle(discussionThread.isHasEndorsed() ?
                        R.string.course_discussion_answered_title :
                        R.string.course_discussion_unanswered_title);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getThreadCall != null) {
            getThreadCall.cancel();
        }
        if (responsesLoader != null) {
            responsesLoader.reset();
        }
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEventMainThread(DiscussionCommentPostedEvent event) {
        if (discussionThread.containsComment(event.getComment())) {
            if (event.getParent() == null) {
                // We got a response
                ((BaseFragmentActivity) getActivity()).showInfoMessage(getString(R.string.discussion_response_posted));
                if (!responsesLoader.hasMorePages()) {
                    courseDiscussionResponsesAdapter.addNewResponse(event.getComment());
                    binding.discussionRecyclerView.smoothScrollToPosition(
                            courseDiscussionResponsesAdapter.getItemCount() - 1);
                } else {
                    // We still need to update the response count locally
                    courseDiscussionResponsesAdapter.incrementResponseCount();
                }
            } else {
                // We got a comment to a response
                if (event.getParent().getChildCount() == 0) {
                    // We only need to show this message when the first comment is added
                    ((BaseFragmentActivity) getActivity()).showInfoMessage(getString(R.string.discussion_comment_posted));
                }
                courseDiscussionResponsesAdapter.addNewComment(event.getParent());
            }
        }
    }

    @Override
    public void onClickAuthor(@NonNull String username) {
        router.showUserProfile(getActivity(), username);
    }

    @Override
    public void onClickAddComment(@NonNull DiscussionComment response) {
        router.showCourseDiscussionAddComment(getActivity(), response, discussionThread);
    }

    @Override
    public void onClickViewComments(@NonNull DiscussionComment response) {
        router.showCourseDiscussionComments(getActivity(), response, discussionThread, courseData);
    }

    @FragmentScoped
    static class ResponsesLoader implements
            InfiniteScrollUtils.PageLoader<DiscussionComment> {
        @NonNull
        private final Context context;
        @NonNull
        private final String threadId;
        private final boolean isQuestionTypeThread;
        private boolean hasMorePages = true;

        DiscussionService discussionService;

        @Nullable
        private Call<Page<DiscussionComment>> getResponsesListCall;
        private int nextPage = 1;
        private boolean isFetchingEndorsed;
        private boolean isFrozen;
        private Runnable deferredDeliveryRunnable;

        @Inject
        public ResponsesLoader(@ActivityContext @NonNull Context context, @NonNull String threadId,
                               boolean isQuestionTypeThread) {
            this.context = context;
            this.threadId = threadId;
            this.isQuestionTypeThread = isQuestionTypeThread;
            this.isFetchingEndorsed = isQuestionTypeThread;
            discussionService = EntryPointAccessors
                    .fromApplication(context, EdxDefaultModule.ProviderEntryPoint.class)
                    .getDiscussionService();
        }

        @Override
        public void loadNextPage(@NonNull final InfiniteScrollUtils.PageLoadCallback<DiscussionComment> callback) {
            if (getResponsesListCall != null) {
                getResponsesListCall.cancel();
            }
            final List<String> requestedFields = Collections.singletonList(
                    DiscussionRequestFields.PROFILE_IMAGE.getQueryParamValue());
            if (isQuestionTypeThread) {
                getResponsesListCall = discussionService.getResponsesListForQuestion(
                        threadId, nextPage, isFetchingEndorsed, requestedFields);
            } else {
                getResponsesListCall = discussionService.getResponsesList(
                        threadId, nextPage, requestedFields);
            }

            final TaskMessageCallback mCallback = context instanceof TaskMessageCallback ? (TaskMessageCallback) context : null;
            getResponsesListCall.enqueue(new ErrorHandlingCallback<Page<DiscussionComment>>(
                    context, null, mCallback, CallTrigger.LOADING_UNCACHED) {
                @Override
                protected void onResponse(
                        @NonNull final Page<DiscussionComment> threadResponsesPage) {
                    final Runnable deliverResultRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isFetchingEndorsed) {
                                boolean hasMoreEndorsed = threadResponsesPage.hasNext();
                                if (hasMoreEndorsed) {
                                    ++nextPage;
                                } else {
                                    isFetchingEndorsed = false;
                                    nextPage = 1;
                                }
                                final List<DiscussionComment> endorsedResponses =
                                        threadResponsesPage.getResults();
                                if (hasMoreEndorsed || !endorsedResponses.isEmpty()) {
                                    callback.onPageLoaded(endorsedResponses);
                                } else {
                                    // If there are no endorsed responses, then just start
                                    // loading the unendorsed ones without triggering the
                                    // callback, since that would just cause the controller
                                    // to wait for the scroll listener to be invoked, which
                                    // would not happen automatically without any changes
                                    // in the adapter dataset.
                                    loadNextPage(callback);
                                }
                            } else {
                                ++nextPage;
                                callback.onPageLoaded(threadResponsesPage);
                                hasMorePages = threadResponsesPage.hasNext();
                            }
                        }
                    };
                    if (isFrozen) {
                        deferredDeliveryRunnable = deliverResultRunnable;
                    } else {
                        deliverResultRunnable.run();
                    }
                }

                @Override
                protected void onFailure(@NonNull final Throwable error) {
                    callback.onError();
                    nextPage = 1;
                    hasMorePages = false;
                }
            });
        }

        public void freeze() {
            isFrozen = true;
        }

        public void unfreeze() {
            if (isFrozen) {
                isFrozen = false;
                if (deferredDeliveryRunnable != null) {
                    deferredDeliveryRunnable.run();
                    deferredDeliveryRunnable = null;
                }
            }
        }

        public void reset() {
            if (getResponsesListCall != null) {
                getResponsesListCall.cancel();
                getResponsesListCall = null;
            }
            isFetchingEndorsed = isQuestionTypeThread;
            nextPage = 1;
        }

        public boolean hasMorePages() {
            return hasMorePages;
        }
    }

}
