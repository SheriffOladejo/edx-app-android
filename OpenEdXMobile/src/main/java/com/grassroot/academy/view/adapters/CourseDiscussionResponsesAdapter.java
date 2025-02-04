package com.grassroot.academy.view.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseFragment;
import com.grassroot.academy.core.EdxDefaultModule;
import com.grassroot.academy.discussion.DiscussionService;
import com.grassroot.academy.discussion.DiscussionService.FlagBody;
import com.grassroot.academy.discussion.DiscussionService.FollowBody;
import com.grassroot.academy.discussion.DiscussionService.VoteBody;
import com.grassroot.academy.discussion.DiscussionTextUtils;
import com.grassroot.academy.discussion.DiscussionThreadUpdatedEvent;
import com.grassroot.academy.http.callback.ErrorHandlingCallback;
import com.grassroot.academy.http.notifications.DialogErrorNotification;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.model.discussion.DiscussionComment;
import com.grassroot.academy.model.discussion.DiscussionThread;
import com.grassroot.academy.module.prefs.LoginPrefs;
import com.grassroot.academy.util.Config;
import com.grassroot.academy.util.ResourceUtil;
import com.grassroot.academy.util.UiUtils;
import com.grassroot.academy.view.custom.EdxDiscussionBody;
import com.grassroot.academy.view.view_holders.AuthorLayoutViewHolder;
import com.grassroot.academy.view.view_holders.DiscussionSocialLayoutViewHolder;
import com.grassroot.academy.view.view_holders.NumberResponsesViewHolder;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;

import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.scopes.FragmentScoped;

@FragmentScoped
public class CourseDiscussionResponsesAdapter extends RecyclerView.Adapter implements InfiniteScrollUtils.ListContentController<DiscussionComment> {

    public interface Listener {
        void onClickAuthor(@NonNull String username);

        void onClickAddComment(@NonNull DiscussionComment comment);

        void onClickViewComments(@NonNull DiscussionComment comment);
    }

    Config config;

    DiscussionService discussionService;

    LoginPrefs loginPrefs;

    @NonNull
    private final Context context;

    @NonNull
    private final BaseFragment baseFragment;

    @NonNull
    private final Listener listener;

    @NonNull
    private DiscussionThread discussionThread;

    @NonNull
    private EnrolledCoursesResponse courseData;

    private final List<DiscussionComment> discussionResponses = new ArrayList<>();

    private boolean progressVisible = false;
    // Record the current time at initialization to keep the display of the elapsed time durations stable.
    private long initialTimeStampMs = System.currentTimeMillis();

    static class RowType {
        static final int THREAD = 0;
        static final int RESPONSE = 1;
        static final int PROGRESS = 2;
    }

    @Inject
    public CourseDiscussionResponsesAdapter(@ActivityContext @NonNull Context context,
                                            @NonNull BaseFragment baseFragment,
                                            @NonNull Listener listener,
                                            @NonNull DiscussionThread discussionThread,
                                            @NonNull EnrolledCoursesResponse courseData) {
        this.context = context;
        this.baseFragment = baseFragment;
        this.discussionThread = discussionThread;
        this.listener = listener;
        this.courseData = courseData;
        EdxDefaultModule.ProviderEntryPoint provider = EntryPointAccessors.fromApplication(context, EdxDefaultModule.ProviderEntryPoint.class);
        this.config = provider.getEnvironment().getConfig();
        this.discussionService = provider.getDiscussionService();
        this.loginPrefs = provider.getLoginPrefs();
    }

    @Override
    public void setProgressVisible(boolean visible) {
        if (progressVisible != visible) {
            progressVisible = visible;
            int progressRowIndex = 1 + discussionResponses.size();
            if (visible) {
                notifyItemInserted(progressRowIndex);
            } else {
                notifyItemRemoved(progressRowIndex);
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == RowType.THREAD) {
            View discussionThreadRow = LayoutInflater.
                    from(parent.getContext()).
                    inflate(R.layout.row_discussion_responses_thread, parent, false);

            return new DiscussionThreadViewHolder(discussionThreadRow);
        }
        if (viewType == RowType.PROGRESS) {
            return new LoadingViewHolder(parent);
        }

        View discussionResponseRow = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.row_discussion_responses_response, parent, false);
        // CardView adds extra padding on pre-lollipop devices for shadows
        // Since, we've set cardUseCompatPadding to true in the layout file
        // so we need to deduct the extra padding from margins in any case to get the desired results
        UiUtils.INSTANCE.adjustCardViewMargins(discussionResponseRow);

        return new DiscussionResponseViewHolder(discussionResponseRow);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int rowType = getItemViewType(position);
        switch (rowType) {
            case RowType.THREAD:
                bindViewHolderToThreadRow((DiscussionThreadViewHolder) holder);
                break;
            case RowType.RESPONSE:
                bindViewHolderToResponseRow((DiscussionResponseViewHolder) holder, position);
                break;
            case RowType.PROGRESS:
                bindViewHolderToShowMoreRow((LoadingViewHolder) holder);
                break;
        }

    }

    private void bindViewHolderToThreadRow(final DiscussionThreadViewHolder holder) {
        holder.authorLayoutViewHolder.populateViewHolder(config, discussionThread,
                discussionThread, initialTimeStampMs,
                new Runnable() {
                    @Override
                    public void run() {
                        listener.onClickAuthor(discussionThread.getAuthor());
                    }
                });

        holder.threadTitleTextView.setText(discussionThread.getTitle());

        holder.discussionBody.setBody(discussionThread.getRenderedBody());

        String groupName = discussionThread.getGroupName();
        if (groupName == null) {
            holder.threadVisibilityTextView.setText(R.string.discussion_post_visibility_everyone);
        } else {
            holder.threadVisibilityTextView.setText(ResourceUtil.getFormattedString(
                    context.getResources(), R.string.discussion_post_visibility_cohort,
                    "cohort", groupName));
        }

        bindNumberResponsesView(holder.numberResponsesViewHolder);

        if (TextUtils.equals(loginPrefs.getUsername(), discussionThread.getAuthor())) {
            holder.actionsBar.setVisibility(View.GONE);
        } else {
            holder.actionsBar.setVisibility(View.VISIBLE);

            bindSocialView(holder.socialLayoutViewHolder, discussionThread);
            holder.discussionReportViewHolder.reportLayout.setOnClickListener(new View.OnClickListener() {
                public void onClick(final View v) {
                    boolean isReported = holder.discussionReportViewHolder.toggleReported();
                    discussionService.setThreadFlagged(discussionThread.getIdentifier(),
                            new FlagBody(isReported))
                            .enqueue(new ErrorHandlingCallback<DiscussionThread>(
                                    context, null, new DialogErrorNotification(baseFragment)) {
                                @Override
                                protected void onResponse(@NonNull final DiscussionThread topicThread) {
                                    discussionThread = discussionThread.patchObject(topicThread);
                                    EventBus.getDefault().post(new DiscussionThreadUpdatedEvent(discussionThread));
                                }

                                @Override
                                protected void onFailure(@NonNull final Throwable error) {
                                    notifyItemChanged(0);
                                }
                            });
                }
            });

            holder.discussionReportViewHolder.setReported(discussionThread.isAbuseFlagged());
        }
    }

    private void bindSocialView(final DiscussionSocialLayoutViewHolder holder, DiscussionThread thread) {
        holder.setDiscussionThread(thread);

        holder.voteViewContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isVoted = holder.toggleVote(discussionThread.isVoted() ? discussionThread.getVoteCount() - 1 : discussionThread.getVoteCount());
                discussionService.setThreadVoted(discussionThread.getIdentifier(),
                        new VoteBody(isVoted))
                        .enqueue(new ErrorHandlingCallback<DiscussionThread>(
                                context, null, new DialogErrorNotification(baseFragment)) {
                            @Override
                            protected void onResponse(@NonNull final DiscussionThread updatedDiscussionThread) {
                                discussionThread = discussionThread.patchObject(updatedDiscussionThread);
                                EventBus.getDefault().post(new DiscussionThreadUpdatedEvent(discussionThread));
                            }

                            @Override
                            protected void onFailure(@NonNull final Throwable error) {
                                notifyItemChanged(0);
                            }
                        });
            }
        });

        holder.threadFollowContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isFollowing = holder.toggleFollow();
                discussionService.setThreadFollowed(discussionThread.getIdentifier(),
                        new FollowBody(isFollowing))
                        .enqueue(new ErrorHandlingCallback<DiscussionThread>(
                                context, null, new DialogErrorNotification(baseFragment)) {
                            @Override
                            protected void onResponse(@NonNull final DiscussionThread updatedDiscussionThread) {
                                discussionThread = discussionThread.patchObject(updatedDiscussionThread);
                                EventBus.getDefault().post(new DiscussionThreadUpdatedEvent(discussionThread));
                            }

                            @Override
                            protected void onFailure(@NonNull final Throwable error) {
                                notifyItemChanged(0);
                            }
                        });
            }
        });
    }

    private void bindNumberResponsesView(NumberResponsesViewHolder holder) {
        int responsesCount = discussionThread.getResponseCount();
        if (responsesCount < 0) {
            // The responses count is not available yet, so hide the view.
            holder.numberResponsesOrCommentsLabel.setVisibility(View.GONE);
        } else {
            holder.numberResponsesOrCommentsLabel.setVisibility(View.VISIBLE);
            holder.numberResponsesOrCommentsLabel.setText(holder.numberResponsesOrCommentsLabel.getResources().getQuantityString(
                    R.plurals.number_responses_or_comments_responses_label, responsesCount, responsesCount));
        }
    }

    private void bindViewHolderToShowMoreRow(LoadingViewHolder holder) {
    }


    private void bindViewHolderToResponseRow(final DiscussionResponseViewHolder holder, final int position) {
        final DiscussionComment comment = discussionResponses.get(position - 1); // Subtract 1 for the discussion thread row at position 0

        holder.authorLayoutViewHolder.populateViewHolder(config, comment,
                comment, initialTimeStampMs,
                new Runnable() {
                    @Override
                    public void run() {
                        listener.onClickAuthor(comment.getAuthor());
                    }
                });

        if (comment.isEndorsed()) {
            holder.authorLayoutViewHolder.answerTextView.setVisibility(View.VISIBLE);
            holder.responseAnswerAuthorTextView.setVisibility(View.VISIBLE);
            DiscussionThread.ThreadType threadType = discussionThread.getType();
            DiscussionTextUtils.AuthorAttributionLabel authorAttributionLabel;
            @StringRes int endorsementTypeStringRes;
            switch (threadType) {
                case QUESTION:
                    authorAttributionLabel = DiscussionTextUtils.AuthorAttributionLabel.ANSWER;
                    endorsementTypeStringRes = R.string.discussion_responses_answer;
                    break;
                case DISCUSSION:
                default:
                    authorAttributionLabel = DiscussionTextUtils.AuthorAttributionLabel.ENDORSEMENT;
                    endorsementTypeStringRes = R.string.discussion_responses_endorsed;
                    break;
            }
            holder.authorLayoutViewHolder.answerTextView.setText(endorsementTypeStringRes);
            DiscussionTextUtils.setAuthorAttributionText(holder.responseAnswerAuthorTextView,
                    authorAttributionLabel, comment.getEndorserData(), initialTimeStampMs,
                    new Runnable() {
                        @Override
                        public void run() {
                            listener.onClickAuthor(comment.getEndorsedBy());
                        }
                    });
        } else {
            holder.authorLayoutViewHolder.answerTextView.setVisibility(View.GONE);
            holder.responseAnswerAuthorTextView.setVisibility(View.GONE);
        }

        holder.discussionBody.setBody(comment.getRenderedBody());

        if (discussionThread.isClosed() && comment.getChildCount() == 0) {
            holder.addCommentLayout.setEnabled(false);
        } else if (courseData.isDiscussionBlackedOut() && comment.getChildCount() == 0) {
            holder.addCommentLayout.setEnabled(false);
        } else {
            holder.addCommentLayout.setEnabled(true);
            holder.addCommentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (comment.getChildCount() > 0) {
                        listener.onClickViewComments(comment);
                    } else {
                        listener.onClickAddComment(comment);
                    }
                }
            });
        }

        bindNumberCommentsView(holder.numberResponsesViewHolder, comment);

        if (TextUtils.equals(loginPrefs.getUsername(), comment.getAuthor())) {
            holder.actionsBar.setVisibility(View.GONE);
        } else {
            holder.actionsBar.setVisibility(View.VISIBLE);

            bindSocialView(holder.socialLayoutViewHolder, position, comment);
            holder.discussionReportViewHolder.reportLayout.setOnClickListener(new View.OnClickListener() {
                public void onClick(final View v) {
                    boolean isReported = holder.discussionReportViewHolder.toggleReported();
                    discussionService.setCommentFlagged(comment.getIdentifier(),
                            new FlagBody(isReported))
                            .enqueue(new ErrorHandlingCallback<DiscussionComment>(
                                    context, null, new DialogErrorNotification(baseFragment)) {
                                @Override
                                protected void onResponse(@NonNull final DiscussionComment comment) {
                                    discussionResponses.get(position - 1).patchObject(comment);
                                    discussionResponses.set(position - 1, comment);
                                }

                                @Override
                                protected void onFailure(@NonNull final Throwable error) {
                                    notifyItemChanged(position);
                                }
                            });
                }
            });

            holder.discussionReportViewHolder.setReported(comment.isAbuseFlagged());
            holder.socialLayoutViewHolder.threadFollowContainer.setVisibility(View.INVISIBLE);
        }
    }

    private void bindSocialView(final DiscussionSocialLayoutViewHolder holder, final int position, final DiscussionComment response) {
        holder.setDiscussionResponse(response);

        holder.voteViewContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isVoted = holder.toggleVote(response.isVoted() ? response.getVoteCount() - 1 : response.getVoteCount());
                discussionService.setCommentVoted(response.getIdentifier(),
                        new VoteBody(isVoted))
                        .enqueue(new ErrorHandlingCallback<DiscussionComment>(
                                context, null, new DialogErrorNotification(baseFragment)) {
                            @Override
                            protected void onResponse(@NonNull final DiscussionComment comment) {
                                discussionResponses.get(position - 1).patchObject(comment);
                                discussionResponses.set(position - 1, comment);
                            }

                            @Override
                            protected void onFailure(@NonNull final Throwable error) {
                                notifyItemChanged(position);
                            }
                        });
            }
        });
    }

    private void bindNumberCommentsView(NumberResponsesViewHolder holder, DiscussionComment response) {
        String text;
        int iconResId;

        int numChildren = response == null ? 0 : response.getChildCount();

        if (response.getChildCount() == 0) {
            if (discussionThread.isClosed() || courseData.isDiscussionBlackedOut()) {
                text = context.getString(R.string.discussion_add_comment_disabled_title);
                iconResId = R.drawable.ic_lock;
            } else {
                text = context.getString(R.string.number_responses_or_comments_add_comment_label);
                iconResId = R.drawable.ic_comment;
            }
        } else {
            text = context.getResources().getQuantityString(
                    R.plurals.number_responses_or_comments_comments_label, numChildren, numChildren);
            iconResId = R.drawable.ic_comment;
        }

        holder.numberResponsesOrCommentsLabel.setText(text);
        UiUtils.INSTANCE.setTextViewDrawableStart(context, holder.numberResponsesOrCommentsLabel,
                iconResId, R.dimen.edx_small, R.color.primaryBaseColor);
    }

    @Override
    public int getItemCount() {
        int total = 1 + discussionResponses.size();
        if (progressVisible)
            total++;
        return total;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return RowType.THREAD;
        }

        if (progressVisible && position == getItemCount() - 1) {
            return RowType.PROGRESS;
        }

        return RowType.RESPONSE;
    }

    public void updateDiscussionThread(@NonNull DiscussionThread discussionThread) {
        this.discussionThread = discussionThread;
        notifyDataSetChanged();
    }

    @Override
    public void clear() {
        int responsesCount = discussionResponses.size();
        discussionResponses.clear();
        notifyItemRangeRemoved(1, responsesCount);
    }

    @Override
    public void addAll(List<DiscussionComment> items) {
        int offset = 1 + discussionResponses.size();
        discussionResponses.addAll(items);
        notifyItemRangeInserted(offset, items.size());
    }

    public void addNewResponse(@NonNull DiscussionComment response) {
        // Since, we have a added a new response we need to update timestamps of all responses
        initialTimeStampMs = System.currentTimeMillis();
        int offset = 1 + discussionResponses.size();
        discussionResponses.add(response);
        incrementResponseCount();
        notifyItemInserted(offset);
    }

    public void incrementResponseCount() {
        discussionThread.incrementResponseCount();
        notifyItemChanged(0); // Response count is shown in the thread details header, so it also needs to be refreshed.
    }

    public void addNewComment(@NonNull DiscussionComment parent) {
        // Since, we have a added a new comment we need to update timestamps of all responses as well
        initialTimeStampMs = System.currentTimeMillis();
        discussionThread.incrementCommentCount();
        String parentId = parent.getIdentifier();
        for (ListIterator<DiscussionComment> responseIterator = discussionResponses.listIterator();
             responseIterator.hasNext(); ) {
            DiscussionComment response = responseIterator.next();
            if (parentId.equals(response.getIdentifier())) {
                response.incrementChildCount();
                notifyItemChanged(1 + responseIterator.previousIndex());
                break;
            }
        }
    }

    public static class DiscussionThreadViewHolder extends RecyclerView.ViewHolder {
        View actionsBar;
        TextView threadTitleTextView;
        EdxDiscussionBody discussionBody;
        TextView threadVisibilityTextView;

        AuthorLayoutViewHolder authorLayoutViewHolder;
        NumberResponsesViewHolder numberResponsesViewHolder;
        DiscussionSocialLayoutViewHolder socialLayoutViewHolder;
        DiscussionReportViewHolder discussionReportViewHolder;

        public DiscussionThreadViewHolder(View itemView) {
            super(itemView);

            actionsBar = itemView.findViewById(R.id.discussion_actions_bar);
            threadTitleTextView = (TextView) itemView.
                    findViewById(R.id.discussion_responses_thread_row_title_text_view);
            discussionBody = (EdxDiscussionBody) itemView.findViewById(R.id.discussion_render_body);
            threadVisibilityTextView = (TextView) itemView.
                    findViewById(R.id.discussion_responses_thread_row_visibility_text_view);

            authorLayoutViewHolder = new AuthorLayoutViewHolder(itemView.findViewById(R.id.discussion_user_profile_row));
            numberResponsesViewHolder = new NumberResponsesViewHolder(itemView);
            socialLayoutViewHolder = new DiscussionSocialLayoutViewHolder(itemView);
            discussionReportViewHolder = new DiscussionReportViewHolder(itemView);
        }
    }

    public static class DiscussionResponseViewHolder extends RecyclerView.ViewHolder {
        View actionsBar;
        RelativeLayout addCommentLayout;
        EdxDiscussionBody discussionBody;
        TextView responseAnswerAuthorTextView;

        AuthorLayoutViewHolder authorLayoutViewHolder;
        NumberResponsesViewHolder numberResponsesViewHolder;
        DiscussionSocialLayoutViewHolder socialLayoutViewHolder;
        DiscussionReportViewHolder discussionReportViewHolder;

        public DiscussionResponseViewHolder(View itemView) {
            super(itemView);

            actionsBar = itemView.findViewById(R.id.discussion_actions_bar);
            addCommentLayout = (RelativeLayout) itemView.findViewById(R.id.discussion_responses_comment_relative_layout);
            discussionBody = (EdxDiscussionBody) itemView.findViewById(R.id.discussion_render_body);
            responseAnswerAuthorTextView = (TextView) itemView.findViewById(R.id.discussion_responses_answer_author_text_view);

            authorLayoutViewHolder = new AuthorLayoutViewHolder(itemView.findViewById(R.id.discussion_user_profile_row));
            numberResponsesViewHolder = new NumberResponsesViewHolder(itemView);
            socialLayoutViewHolder = new DiscussionSocialLayoutViewHolder(itemView);
            discussionReportViewHolder = new DiscussionReportViewHolder(itemView);
        }
    }
}
