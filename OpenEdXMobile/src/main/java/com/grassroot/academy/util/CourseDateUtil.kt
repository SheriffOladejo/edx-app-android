package com.grassroot.academy.util

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.grassroot.academy.R
import com.grassroot.academy.model.course.CourseBannerInfoModel
import com.grassroot.academy.model.course.CourseBannerType
import com.grassroot.academy.module.analytics.Analytics
import com.grassroot.academy.module.analytics.AnalyticsRegistry

/**
 * Utils class to populate the CourseDates banner view
 */
object CourseDateUtil {

    fun setupCourseDatesBanner(
        view: View, courseId: String, enrollmentMode: String, isSelfPaced: Boolean,
        screenName: String, analyticsRegistry: AnalyticsRegistry,
        courseBannerInfoModel: CourseBannerInfoModel, clickListener: View.OnClickListener
    ) {
        setupCourseDatesBanner(
            view = view,
            isCourseDatePage = false,
            courseId = courseId,
            enrollmentMode = enrollmentMode,
            isSelfPaced = isSelfPaced,
            screenName = screenName,
            analyticsRegistry = analyticsRegistry,
            courseBannerInfoModel = courseBannerInfoModel,
            clickListener = clickListener
        )
    }

    fun setupCourseDatesBanner(
        view: View,
        isCourseDatePage: Boolean,
        courseId: String,
        enrollmentMode: String,
        isSelfPaced: Boolean,
        screenName: String,
        analyticsRegistry: AnalyticsRegistry,
        courseBannerInfoModel: CourseBannerInfoModel,
        clickListener: View.OnClickListener
    ) {
        val context = view.context as Context
        val containerLayout = view as LinearLayout
        val title = view.findViewById(R.id.banner_title) as TextView
        val bannerMessage = view.findViewById(R.id.banner_info) as TextView
        val imgView = view.findViewById(R.id.iv_calender) as ImageView
        val button = view.findViewById(R.id.btn_shift_dates) as Button
        var description: CharSequence = ""
        var buttonText = ""
        var bannerTypeValue = ""
        var biValue = ""
        val bannerType = courseBannerInfoModel.datesBannerInfo.getCourseBannerType()

        if (isCourseDatePage) {
            title.visibility = View.VISIBLE
            containerLayout.setBackgroundColor(Color.TRANSPARENT)

            when (bannerType) {
                CourseBannerType.UPGRADE_TO_GRADED -> {
                    description = context.getText(R.string.course_dates_banner_upgrade_to_graded)
                    biValue = Analytics.Values.COURSE_DATES_BANNER_UPGRADE_TO_PARTICIPATE
                    bannerTypeValue = Analytics.Values.PLS_BANNER_TYPE_UPGRADE_TO_PARTICIPATE
                }
                CourseBannerType.UPGRADE_TO_RESET -> {
                    description = context.getText(R.string.course_dates_banner_upgrade_to_reset)
                    biValue = Analytics.Values.COURSE_DATES_BANNER_UPGRADE_TO_SHIFT
                    bannerTypeValue = Analytics.Values.PLS_BANNER_TYPE_UPGRADE_TO_SHIFT
                }
                CourseBannerType.RESET_DATES -> {
                    description = context.getText(R.string.course_dates_banner_reset_date)
                    buttonText = context.getString(R.string.course_dates_banner_reset_date_button)
                    biValue = Analytics.Values.COURSE_DATES_BANNER_SHIFT_DATES
                    bannerTypeValue = Analytics.Values.PLS_BANNER_TYPE_SHIFT_DATES
                }
                CourseBannerType.INFO_BANNER -> {
                    description = context.getText(R.string.course_dates_info_banner)
                    biValue = Analytics.Values.COURSE_DATES_BANNER_INFO
                    bannerTypeValue = Analytics.Values.PLS_BANNER_TYPE_INFO
                }
                CourseBannerType.BLANK -> description = ""
            }
        } else if (bannerType == CourseBannerType.RESET_DATES) {
            description = context.getText(R.string.course_dashboard_banner_reset_date)
            buttonText = context.getString(R.string.course_dates_banner_reset_date_button)
            biValue = Analytics.Values.COURSE_DATES_BANNER_SHIFT_DATES
            bannerTypeValue = Analytics.Values.PLS_BANNER_TYPE_SHIFT_DATES
        }

        if (description.isNotBlank() && (isSelfPaced || (isSelfPaced.not() && bannerType == CourseBannerType.UPGRADE_TO_GRADED))) {
            bannerMessage.text = description
            if (!TextUtils.isEmpty(buttonText)) {
                button.text = buttonText
                button.visibility = View.VISIBLE
                imgView.visibility = if (isCourseDatePage) View.GONE else View.VISIBLE
                button.setOnClickListener { v ->
                    clickListener.onClick(v)
                    analyticsRegistry.trackPLSShiftButtonTapped(
                        courseId,
                        enrollmentMode,
                        screenName
                    )
                }
            } else {
                button.visibility = View.GONE
                imgView.visibility = View.GONE
            }
            view.visibility = View.VISIBLE
            analyticsRegistry.trackPLSCourseDatesBanner(
                biValue,
                courseId,
                enrollmentMode,
                screenName,
                bannerTypeValue
            )
        } else {
            view.visibility = View.GONE
        }
    }
}
