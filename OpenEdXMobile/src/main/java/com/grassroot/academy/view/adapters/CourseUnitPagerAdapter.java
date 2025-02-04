package com.grassroot.academy.view.adapters;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.model.api.AuthorizationDenialReason;
import com.grassroot.academy.model.api.CourseUpgradeResponse;
import com.grassroot.academy.model.api.EnrolledCoursesResponse;
import com.grassroot.academy.model.course.BlockType;
import com.grassroot.academy.model.course.CourseComponent;
import com.grassroot.academy.model.course.DiscussionBlockModel;
import com.grassroot.academy.model.course.HtmlBlockModel;
import com.grassroot.academy.model.course.VideoBlockModel;
import com.grassroot.academy.util.Config;
import com.grassroot.academy.util.VideoUtil;
import com.grassroot.academy.view.CourseBaseActivity;
import com.grassroot.academy.view.CourseUnitDiscussionFragment;
import com.grassroot.academy.view.CourseUnitEmptyFragment;
import com.grassroot.academy.view.CourseUnitFragment;
import com.grassroot.academy.view.CourseUnitMobileNotSupportedFragment;
import com.grassroot.academy.view.CourseUnitOnlyOnYoutubeFragment;
import com.grassroot.academy.view.CourseUnitVideoPlayerFragment;
import com.grassroot.academy.view.CourseUnitWebViewFragment;
import com.grassroot.academy.view.CourseUnitYoutubePlayerFragment;
import com.grassroot.academy.view.LockedCourseUnitFragment;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CourseUnitPagerAdapter extends FragmentStateAdapter {
    private IEdxEnvironment environment;
    private Config config;
    private List<CourseComponent> unitList;
    private EnrolledCoursesResponse courseData;
    private CourseUpgradeResponse courseUpgradeData;
    private CourseUnitFragment.HasComponent callback;
    private List<Fragment> fragments = new ArrayList<>();

    public CourseUnitPagerAdapter(FragmentActivity fragmentActivity,
                                  IEdxEnvironment environment,
                                  List<CourseComponent> unitList,
                                  EnrolledCoursesResponse courseData,
                                  CourseUpgradeResponse courseUpgradeData,
                                  CourseUnitFragment.HasComponent callback) {
        super(fragmentActivity);
        this.environment = environment;
        this.config = environment.getConfig();
        this.unitList = unitList;
        this.courseData = courseData;
        this.courseUpgradeData = courseUpgradeData;
        this.callback = callback;
    }

    public CourseComponent getUnit(int pos) {
        if (pos >= unitList.size())
            pos = unitList.size() - 1;
        if (pos < 0)
            pos = 0;
        return unitList.get(pos);
    }

    @NotNull
    @Override
    public Fragment createFragment(int pos) {
        final CourseComponent unit = getUnit(pos);
        // FIXME: Remove this code once LEARNER-6713 is merged
        final CourseComponent minifiedUnit;
        {
            // Create a deep copy of original CourseComponent object with `root` and `parent` objects
            // removed to save memory.
            if (unit instanceof VideoBlockModel) {
                minifiedUnit = new VideoBlockModel((VideoBlockModel) unit);
            } else if (unit instanceof DiscussionBlockModel) {
                minifiedUnit = new DiscussionBlockModel((DiscussionBlockModel) unit);
            } else if (unit instanceof HtmlBlockModel) {
                minifiedUnit = new HtmlBlockModel((HtmlBlockModel) unit);
            } else minifiedUnit = new CourseComponent(unit);
        }
        minifiedUnit.setCourseSku(courseData.getCourseSku());
        CourseUnitFragment unitFragment;
        final boolean isYoutubeVideo = (minifiedUnit instanceof VideoBlockModel && ((VideoBlockModel) minifiedUnit).getData().encodedVideos.getYoutubeVideoInfo() != null);
        if (minifiedUnit.getAuthorizationDenialReason() == AuthorizationDenialReason.FEATURE_BASED_ENROLLMENTS) {
            if (courseUpgradeData == null) {
                unitFragment = CourseUnitMobileNotSupportedFragment.newInstance(minifiedUnit, courseData);
            } else {
                unitFragment = LockedCourseUnitFragment.newInstance(minifiedUnit, courseData, courseUpgradeData);
            }
        }
        //FIXME - for the video, let's ignore studentViewMultiDevice for now
        else if (VideoUtil.isCourseUnitVideo(environment, minifiedUnit)) {
            final VideoBlockModel videoBlockModel = (VideoBlockModel) minifiedUnit;
            videoBlockModel.setVideoThumbnail(courseData.getCourse().getCourse_image());
            unitFragment = CourseUnitVideoPlayerFragment.newInstance(videoBlockModel, (pos < unitList.size()), (pos > 0));
        } else if (isYoutubeVideo && config.getYoutubePlayerConfig().isYoutubePlayerEnabled() && VideoUtil.isYoutubeAPISupported(((CourseBaseActivity) callback).getApplicationContext())) {
            unitFragment = CourseUnitYoutubePlayerFragment.newInstance((VideoBlockModel) minifiedUnit);
        } else if (isYoutubeVideo) {
            unitFragment = CourseUnitOnlyOnYoutubeFragment.newInstance(minifiedUnit);
        } else if (config.isDiscussionsEnabled() && minifiedUnit instanceof DiscussionBlockModel) {
            unitFragment = CourseUnitDiscussionFragment.newInstance(minifiedUnit, courseData);
        } else if (!minifiedUnit.isMultiDevice()) {
            unitFragment = CourseUnitMobileNotSupportedFragment.newInstance(minifiedUnit, courseData);
        } else if (minifiedUnit.getType() != BlockType.VIDEO &&
                minifiedUnit.getType() != BlockType.HTML &&
                minifiedUnit.getType() != BlockType.OTHERS &&
                minifiedUnit.getType() != BlockType.DISCUSSION &&
                minifiedUnit.getType() != BlockType.PROBLEM &&
                minifiedUnit.getType() != BlockType.OPENASSESSMENT &&
                minifiedUnit.getType() != BlockType.DRAG_AND_DROP_V2 &&
                minifiedUnit.getType() != BlockType.WORD_CLOUD &&
                minifiedUnit.getType() != BlockType.LTI_CONSUMER) {
            unitFragment = CourseUnitEmptyFragment.newInstance(minifiedUnit);
        } else if (minifiedUnit instanceof HtmlBlockModel) {
            minifiedUnit.setCourseId(courseData.getCourse().getId());
            unitFragment = CourseUnitWebViewFragment.newInstance((HtmlBlockModel) minifiedUnit, courseData.getCourse().getName(), courseData.getMode(), courseData.getCourse().isSelfPaced());
        }
        //fallback
        else {
            unitFragment = CourseUnitMobileNotSupportedFragment.newInstance(minifiedUnit, courseData);
        }

        unitFragment.setHasComponentCallback(callback);
        fragments.add(unitFragment);
        return unitFragment;
    }

    @Override
    public int getItemCount() {
        return unitList.size();
    }

    public Fragment getItem(int position) {
        return fragments.get(position);
    }
}
