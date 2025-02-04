package com.grassroot.academy.view.view_holders;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.grassroot.academy.R;
import com.grassroot.academy.util.UiUtils;

public class NumberResponsesViewHolder extends RecyclerView.ViewHolder {
    public TextView numberResponsesOrCommentsLabel;

    public NumberResponsesViewHolder(View itemView) {
        super(itemView);
        numberResponsesOrCommentsLabel = (TextView) itemView.
                findViewById(R.id.number_responses_or_comments_label);
        Context context = numberResponsesOrCommentsLabel.getContext();
        UiUtils.INSTANCE.setTextViewDrawableStart(context, numberResponsesOrCommentsLabel,
                R.drawable.ic_comment, R.dimen.edx_small);
    }
}
