package com.grassroot.academy.whatsnew;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.grassroot.academy.base.BaseTest;
import com.grassroot.academy.model.whatsnew.WhatsNewItemModel;
import com.grassroot.academy.model.whatsnew.WhatsNewItemModel.Platform;
import org.junit.Test;

import java.util.Arrays;

public class WhatsNewItemModelTest extends BaseTest {
    @Test
    public void testIsSupportedPlatform() {
        // True cases
        assertThat(Platform.isSupportedPlatform("android")).isTrue();
        assertThat(Platform.isSupportedPlatform("ios")).isTrue();
        // False cases
        assertThat(Platform.isSupportedPlatform(null)).isFalse();
        assertThat(Platform.isSupportedPlatform("")).isFalse();
        assertThat(Platform.isSupportedPlatform("andr0id")).isFalse();
    }

    @Test
    public void testIsAndroidMessage() {
        WhatsNewItemModel item = new WhatsNewItemModel();
        // True cases
        item.setPlatforms(Arrays.asList("android"));
        assertThat(item.isAndroidMessage()).isTrue();
        item.setPlatforms(Arrays.asList("android", "ios"));
        assertThat(item.isAndroidMessage()).isTrue();
        // False cases
        item.setPlatforms(Arrays.asList("ios", "andro1d"));
        assertThat(item.isAndroidMessage()).isFalse();
        item.setPlatforms(Arrays.asList("ios"));
        assertThat(item.isAndroidMessage()).isFalse();
    }
}
