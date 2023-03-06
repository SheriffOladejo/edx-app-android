package com.grassroot.academy.model.video

import com.grassroot.academy.R

enum class VideoQuality(val titleResId: Int) {
    AUTO(R.string.auto_recommended_text),
    OPTION_360P(R.string.video_quality_p360),
    OPTION_540P(R.string.video_quality_p540),
    OPTION_720P(R.string.video_quality_p720);

    val value: String = this.name.replace("OPTION_", "").lowercase()
}
