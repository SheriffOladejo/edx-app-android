package com.grassroot.academy.model.course

import com.google.gson.annotations.SerializedName

data class CourseBannerInfoModel(
        @SerializedName("dates_banner_info") val datesBannerInfo: CourseDatesBannerInfo,
        @SerializedName("has_ended") val hasEnded: Boolean = false
)
