package com.grassroot.academy.test.util

import org.assertj.core.api.Assertions.assertThat
import com.grassroot.academy.module.analytics.Analytics
import com.grassroot.academy.util.AnalyticsUtils
import org.junit.Test

class AnalyticsUtilsTest {
    @Test
    fun removeUnSupportedCharactersTest() {
        assertThat(AnalyticsUtils.removeUnSupportedCharacters("A: B-C D__E")).isEqualTo("A_B_C_D_E")
        assertThat(AnalyticsUtils.removeUnSupportedCharacters(Analytics.Screens.APP_REVIEWS_VIEW_RATING)).isEqualTo("AppReviews_View_Rating")
        assertThat(AnalyticsUtils.removeUnSupportedCharacters(Analytics.Screens.PROFILE_CHOOSE_BIRTH_YEAR)).isEqualTo("Choose_Form_Value_Birth_year")
    }
}
