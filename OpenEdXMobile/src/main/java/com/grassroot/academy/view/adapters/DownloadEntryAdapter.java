package com.grassroot.academy.view.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import com.grassroot.academy.R;
import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.util.MemoryUtil;

public abstract class DownloadEntryAdapter extends BaseListAdapter<DownloadEntryAdapter.Item> {

    public DownloadEntryAdapter(Context context, IEdxEnvironment environment) {
        super(context, R.layout.row_download_list, environment);
    }

    @Override
    public void render(BaseViewHolder tag, final Item item) {
        final ViewHolder holder = (ViewHolder) tag;
        holder.title.setText(item.getTitle());
        if (TextUtils.isEmpty(item.getDuration())) {
            holder.duration.setVisibility(View.GONE);
        } else {
            holder.duration.setVisibility(View.VISIBLE);
            holder.duration.setText(item.getDuration());
        }
        holder.progress.setProgress(item.getPercent());
        @ColorRes final int progressColor;
        final String progressText;
        final String errorText;
        switch (item.getStatus()) {
            case PENDING: {
                progressText = getContext().getString(R.string.download_pending);
                progressColor = R.color.successBase;
                errorText = null;
                break;
            }
            case DOWNLOADING: {
                progressText = getByteCountProgressText(item);
                progressColor = R.color.successBase;
                errorText = null;
                break;
            }
            case FAILED: {
                errorText = getContext().getString(R.string.error_download_failed);
                progressColor = R.color.errorLight;
                if (item.getDownloadedByteCount() > 0) {
                    progressText = getByteCountProgressText(item);
                } else {
                    progressText = null;
                }
                break;
            }
            default: {
                throw new IllegalArgumentException(item.getStatus().name());
            }
        }
        holder.progress.setIndicatorColor(getContext().getResources().getColor(progressColor));
        holder.progress.setTrackColor(getContext().getResources().getColor(R.color.neutralDark));
        if (null == progressText) {
            holder.percent.setVisibility(View.GONE);
        } else {
            holder.percent.setText(progressText);
            holder.percent.setVisibility(View.VISIBLE);
        }
        if (null == errorText) {
            holder.error.setVisibility(View.GONE);
        } else {
            holder.error.setText(errorText);
            holder.error.setVisibility(View.VISIBLE);
        }

        holder.cross_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onDeleteClicked(item);
            }
        });
    }

    @NonNull
    private String getByteCountProgressText(Item item) {
        final Long totalByteCount = item.getTotalByteCount();
        String downloadedText = MemoryUtil.format(getContext(), item.getDownloadedByteCount());
        if (null != totalByteCount) {
            downloadedText += " / " + MemoryUtil.format(getContext(), totalByteCount);
        }
        return downloadedText;
    }

    @Override
    public BaseViewHolder getTag(View convertView) {
        return new ViewHolder(convertView);
    }

    private static class ViewHolder extends BaseViewHolder {
        final TextView title;
        final TextView duration;
        final TextView percent;
        final AppCompatImageView cross_button;
        final TextView error;
        final LinearProgressIndicator progress;

        public ViewHolder(@NonNull View view) {
            title = (TextView) view.findViewById(R.id.downloads_name);
            duration = (TextView) view
                    .findViewById(R.id.download_time);
            percent = (TextView) view
                    .findViewById(R.id.download_percentage);
            error = (TextView) view
                    .findViewById(R.id.txtDownloadFailed);
            progress = (LinearProgressIndicator) view
                    .findViewById(R.id.progressBar);
            cross_button = (AppCompatImageView) view
                    .findViewById(R.id.close_btn);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        DownloadEntryAdapter.Item item = getItem(position);
        if (item != null) onItemClicked(item);
    }

    public abstract void onItemClicked(DownloadEntryAdapter.Item model);

    public abstract void onDeleteClicked(DownloadEntryAdapter.Item model);

    public interface Item {
        @NonNull
        String getTitle();

        @NonNull
        String getDuration();

        @NonNull
        Status getStatus();

        /**
         * @return Total download size in bytes, or null if size is not yet known
         */
        @Nullable
        Long getTotalByteCount();

        long getDownloadedByteCount();

        int getPercent();

        enum Status {
            PENDING,
            DOWNLOADING,
            FAILED
        }
    }
}
