package com.grassroot.academy.view.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import com.grassroot.academy.R
import com.grassroot.academy.core.IEdxEnvironment
import com.grassroot.academy.model.video.VideoQuality
import com.grassroot.academy.util.TextUtils

abstract class VideoQualityAdapter(
    context: Context?,
    environment: IEdxEnvironment?,
    private var selectedVideoQuality: VideoQuality
) :
    BaseListAdapter<VideoQuality>(
        context,
        R.layout.video_quality_row_item,
        environment
    ) {

    override fun render(tag: BaseViewHolder?, model: VideoQuality) {
        val holder = tag as ViewHolder
        holder.tvVideoQualityRowTitle.text = context.getString(model.titleResId)
        val typeface = holder.tvVideoQualityRowTitle.typeface
        if (selectedVideoQuality == model) {
            holder.tvVideoQualityRowTitle.setTypeface(typeface, Typeface.BOLD)
            holder.ivVideoQualityCheck.visibility = View.VISIBLE
        } else {
            TextUtils.setTextAppearance(
                context,
                holder.tvVideoQualityRowTitle,
                R.style.regular_edx_black_text
            )
            holder.ivVideoQualityCheck.visibility = View.GONE
        }
    }

    override fun getTag(convertView: View): BaseViewHolder {
        val holder = ViewHolder()
        holder.tvVideoQualityRowTitle = convertView.findViewById(R.id.tv_video_quality_row_title)
        holder.ivVideoQualityCheck = convertView.findViewById(R.id.iv_video_quality_check)
        return holder
    }

    private class ViewHolder : BaseViewHolder() {
        lateinit var tvVideoQualityRowTitle: TextView
        lateinit var ivVideoQualityCheck: ImageView
    }

    override fun onItemClick(
        adapter: AdapterView<*>?, view: View?, position: Int,
        id: Long
    ) {
        getItem(position)?.let { onItemClicked(it) }
    }

    abstract fun onItemClicked(videoQuality: VideoQuality)
}
