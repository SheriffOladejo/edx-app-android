package com.grassroot.academy.view

import android.app.Activity
import android.content.Intent
import com.grassroot.academy.base.BaseFragment
import com.grassroot.academy.core.IEdxEnvironment
import com.grassroot.academy.event.NetworkConnectivityChangeEvent
import com.grassroot.academy.model.api.EnrolledCoursesResponse
import com.grassroot.academy.model.course.CourseComponent
import com.grassroot.academy.services.CourseManager
import javax.inject.Inject

/**
 * Provides support for offline mode handling within a Fragment.
 * <br></br>
 * Ensures that no two types of errors appear at the same time in a Fragment e.g. if
 * [FullScreenErrorNotification] is already appearing in an activity
 * [SnackbarErrorNotification] should never appear until and unless the
 * [FullScreenErrorNotification] is hidden.
 */
abstract class OfflineSupportBaseFragment : BaseFragment() {

    @Inject
    protected lateinit var courseManager: CourseManager

    @Inject
    protected lateinit var environment: IEdxEnvironment

    /**
     * Tells if the Fragment is currently showing a [FullScreenErrorNotification].
     *
     * @return `true` if [FullScreenErrorNotification] is visible,
     * `false` otherwise.
     */
    protected abstract fun isShowingFullScreenError(): Boolean

    override fun onResume() {
        super.onResume()
        OfflineSupportUtils.setUserVisibleHint(activity, true, isShowingFullScreenError())
    }

    override fun onPause() {
        super.onPause()
        OfflineSupportUtils.setUserVisibleHint(activity, false, isShowingFullScreenError())
    }

    @Suppress("UNUSED_PARAMETER")
    fun onNetworkConnectivityChangeEvent(event: NetworkConnectivityChangeEvent?) {
        OfflineSupportUtils.onNetworkConnectivityChangeEvent(
            activity,
            isResumed,
            isShowingFullScreenError()
        )
    }

    override fun onRevisit() {
        OfflineSupportUtils.onRevisit(activity)
    }

    /**
     * Method to redirect the user to course outline screen for maintaining the
     * back stack in case when user directly move to the course unit detail screen.
     */
    fun navigateToCourseUnit(data: Intent, courseData: EnrolledCoursesResponse, outlineComp: CourseComponent) {
        val leafCompId = data.getSerializableExtra(Router.EXTRA_COURSE_COMPONENT_ID) as String
        val leafComp = courseManager.getComponentByIdFromAppLevelCache(
                courseData.courseId, leafCompId)
        val outlinePath = outlineComp.path
        val leafPath = leafComp?.path
        val outlinePathSize = outlinePath.path.size
        if (outlineComp != leafPath?.get(outlinePathSize - 1)) {
            activity?.setResult(Activity.RESULT_OK, data)
            activity?.finish()
        } else {
            val leafPathSize = leafPath.path.size
            if (canUpdateRowSelection() && outlinePathSize == leafPathSize - 2) {
                updateRowSelection(leafCompId)
            } else {
                var i = outlinePathSize + 1
                while (i < leafPathSize - 1) {
                    val nextComp = leafPath[i]
                    environment.router?.showCourseContainerOutline(
                            this@OfflineSupportBaseFragment,
                            REQUEST_SHOW_COURSE_UNIT_DETAIL, courseData, null,
                            nextComp.id, leafCompId, false)
                    i += 2
                }
            }
        }
    }

    open fun updateRowSelection(leafCompId: String?) {}

    /**
     * Method to check if the course outline screen is visible so that selected Component needs be highlighted.
     */
    open fun canUpdateRowSelection(): Boolean {
        return false
    }

    companion object {
        const val REQUEST_SHOW_COURSE_UNIT_DETAIL = 101
    }
}
