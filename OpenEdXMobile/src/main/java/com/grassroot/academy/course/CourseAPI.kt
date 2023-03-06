package com.grassroot.academy.course

import android.content.Context
import okhttp3.ResponseBody
import com.grassroot.academy.event.EnrolledInCourseEvent
import com.grassroot.academy.exception.CourseContentNotValidException
import com.grassroot.academy.http.callback.ErrorHandlingCallback
import com.grassroot.academy.http.constants.ApiConstants
import com.grassroot.academy.http.constants.TimeInterval
import com.grassroot.academy.http.notifications.ErrorNotification
import com.grassroot.academy.http.notifications.SnackbarErrorNotification
import com.grassroot.academy.http.util.CallUtil
import com.grassroot.academy.interfaces.RefreshListener
import com.grassroot.academy.model.Page
import com.grassroot.academy.model.api.CourseComponentStatusResponse
import com.grassroot.academy.model.api.CourseUpgradeResponse
import com.grassroot.academy.model.api.EnrolledCoursesResponse
import com.grassroot.academy.model.api.EnrollmentResponse
import com.grassroot.academy.model.course.BlockModel
import com.grassroot.academy.model.course.BlockType
import com.grassroot.academy.model.course.BlocksCompletionBody
import com.grassroot.academy.model.course.CourseBannerInfoModel
import com.grassroot.academy.model.course.CourseComponent
import com.grassroot.academy.model.course.CourseDates
import com.grassroot.academy.model.course.CourseDetail
import com.grassroot.academy.model.course.CourseStatus
import com.grassroot.academy.model.course.CourseStructureV1Model
import com.grassroot.academy.model.course.DiscussionBlockModel
import com.grassroot.academy.model.course.DiscussionData
import com.grassroot.academy.model.course.EnrollBody
import com.grassroot.academy.model.course.HtmlBlockModel
import com.grassroot.academy.model.course.IBlock
import com.grassroot.academy.model.course.ResetCourseDates
import com.grassroot.academy.model.course.VideoBlockModel
import com.grassroot.academy.model.course.VideoData
import com.grassroot.academy.module.prefs.LoginPrefs
import com.grassroot.academy.util.Config
import com.grassroot.academy.view.common.TaskProgressCallback
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import retrofit2.Call
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseAPI @Inject constructor(
    private val config: Config,
    private val courseService: CourseService,
    private val loginPrefs: LoginPrefs
) {

    /**
     * @return App Config & Enrolled courses of given user.
     */
    val enrolledCourses: Call<EnrollmentResponse>
        get() = courseService.getEnrolledCourses(
            null,
            loginPrefs.username,
            config.apiUrlVersionConfig.enrollmentsApiVersion,
            config.organizationCode
        )

    /**
     * @return App Config & Enrolled courses of given user without stale response.
     */
    val enrolledCoursesWithoutStale: Call<EnrollmentResponse>
        get() = courseService.getEnrolledCourses(
            "stale-if-error=0",
            loginPrefs.username,
            config.apiUrlVersionConfig.enrollmentsApiVersion,
            config.organizationCode
        )

    /**
     * @return App Config & Enrolled courses of given user, only from the cache.
     */
    val enrolledCoursesFromCache: Call<EnrollmentResponse>
        get() = courseService.getEnrolledCourses(
            "only-if-cached, max-stale",
            loginPrefs.username,
            config.apiUrlVersionConfig.enrollmentsApiVersion,
            config.organizationCode
        )

    fun enrollInACourse(courseId: String, emailOptIn: Boolean): Call<ResponseBody> {
        return courseService.enrollInACourse(EnrollBody(courseId, emailOptIn))
    }

    fun getCourseList(page: Int): Call<Page<CourseDetail>> {
        val username = if (loginPrefs.isUserLoggedIn)
            loginPrefs.username else null
        return courseService.getCourseList(username, true, config.organizationCode, page)
    }

    fun getCourseDetail(courseId: String): Call<CourseDetail> {
        // Empty courseId will return a 200 for a list of course details, instead of a single course
        require(courseId.isNotEmpty())
        return courseService.getCourseDetail(courseId, loginPrefs.username)
    }

    fun getCourseStructure(courseId: String): Call<CourseStructureV1Model> {
        return courseService.getCourseStructure(
            "max-stale=" + TimeInterval.HOUR,
            config.apiUrlVersionConfig.blocksApiVersion,
            loginPrefs.username,
            courseId
        )
    }

    fun getCourseStructureWithoutStale(courseId: String): Call<CourseStructureV1Model> {
        return courseService.getCourseStructure(
            "stale-if-error=0",
            config.apiUrlVersionConfig.blocksApiVersion,
            loginPrefs.username,
            courseId
        )
    }

    @Throws(Exception::class)
    fun getCourseStructureFromCache(courseId: String): CourseComponent {
        val model = CallUtil.executeStrict(
            courseService.getCourseStructure(
                "only-if-cached, max-stale",
                config.apiUrlVersionConfig.blocksApiVersion,
                loginPrefs.username,
                courseId
            )
        )
        return normalizeCourseStructure(model, courseId) as CourseComponent
    }

    /**
     * Checks if a course is enrolled by the user or not from a cached network response.
     *
     * @param courseId Id of the course.
     * @return true if Course is enrolled by the user, false other wise
     */
    fun isCourseEnrolled(courseId: String): Boolean {
        return try {
            getCourseById(courseId) != null
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    /**
     * @param courseId The course Id.
     * @return The course identified by the provided Id if available from the cache, null if no
     * course is found.
     */
    @Throws(Exception::class)
    fun getCourseById(courseId: String): EnrolledCoursesResponse? {
        return CallUtil.executeStrict(enrolledCoursesFromCache).enrollments.firstOrNull {
            courseId == it.courseId
        }
    }

    /**
     * @return Course dates against the given course Id.
     */
    fun getCourseDates(courseId: String): Call<CourseDates> {
        return courseService.getCourseDates(courseId)
    }

    /**
     * @return Reset course dates against the given course Id.
     */
    fun resetCourseDates(courseId: String): Call<ResetCourseDates> {
        return courseService.resetCourseDates(mapOf(ApiConstants.COURSE_KEY to courseId))
    }

    /**
     * @return Course dates banner info against the given course Id.
     */
    fun getCourseBannerInfo(courseId: String): Call<CourseBannerInfoModel> {
        return courseService.getCourseBannerInfo(courseId)
    }

    fun getCourseStatus(courseId: String): Call<CourseStatus> {
        return courseService.getCourseStatus(courseId)
    }

    fun getCourseStatusInfo(courseId: String): Call<CourseComponentStatusResponse> {
        return courseService.getCourseStatusInfo(loginPrefs.username, courseId)
    }

    /**
     * @return Course upgrade status for the given user.
     */
    fun getCourseUpgradeStatus(courseId: String): Call<CourseUpgradeResponse> {
        return courseService.getCourseUpgradeStatus(courseId)
    }

    /**
     * @return Server response on video completion.
     */
    fun markBlocksCompletion(courseId: String, blockIds: List<String>): Call<JSONObject> {
        return courseService.markBlocksCompletion(
            BlocksCompletionBody(
                loginPrefs.username,
                courseId,
                blockIds
            )
        )
    }

    fun updateCourseCelebration(courseId: String): Call<Void> {
        return courseService.updateCoursewareCelebration(
            courseId,
            mapOf(ApiConstants.FIRST_SECTION_KEY to false)
        )
    }

    open class EnrollCallback(
        context: Context,
        progressCallback: TaskProgressCallback?
    ) : ErrorHandlingCallback<ResponseBody>(context, progressCallback) {

        override fun onResponse(responseBody: ResponseBody) {
            EventBus.getDefault().post(EnrolledInCourseEvent())
        }
    }

    abstract class GetCourseByIdCallback(
        context: Context,
        private val courseId: String,
        progressCallback: TaskProgressCallback?
    ) : ErrorHandlingCallback<EnrollmentResponse>(context, progressCallback) {

        override fun onResponse(courseResponses: EnrollmentResponse) {
            courseResponses.enrollments.forEach { coursesResponse ->
                if (courseId == coursesResponse.courseId) {
                    onResponse(coursesResponse)
                    return
                }
            }
            onFailure(Exception("Course not found in user's enrolled courses."))
        }

        protected abstract fun onResponse(coursesResponse: EnrolledCoursesResponse)
    }

    abstract class GetCourseStructureCallback(
        context: Context,
        private val courseId: String,
        progressCallback: TaskProgressCallback?,
        errorNotification: ErrorNotification?,
        snackbarErrorNotification: SnackbarErrorNotification?,
        refreshListener: RefreshListener?
    ) : ErrorHandlingCallback<CourseStructureV1Model>(
        context, progressCallback, errorNotification, snackbarErrorNotification, refreshListener
    ) {

        override fun onResponse(model: CourseStructureV1Model) {
            try {
                onResponse(normalizeCourseStructure(model, courseId) as CourseComponent)
            } catch (e: CourseContentNotValidException) {
                onFailure(e)
            }
        }

        protected abstract fun onResponse(courseComponent: CourseComponent)
    }

    companion object {
        /**
         * Mapping from raw data structure from getCourseStructure() API
         *
         * @param courseStructureV1Model
         * @return
         */
        @Throws(CourseContentNotValidException::class)
        fun normalizeCourseStructure(
            courseStructureV1Model: CourseStructureV1Model,
            courseId: String
        ): IBlock {
            val topBlock = courseStructureV1Model.getBlockById(courseStructureV1Model.root)
                ?: throw CourseContentNotValidException(
                    "Server didn't send a proper response for this course: " +
                            courseStructureV1Model.root
                )

            val course = CourseComponent(topBlock, null)
            course.courseId = courseId

            courseStructureV1Model.getDescendants(topBlock).forEach { blockModel ->
                normalizeCourseStructure(courseStructureV1Model, blockModel, course)
            }
            return course
        }

        private fun normalizeCourseStructure(
            courseStructureV1Model: CourseStructureV1Model,
            block: BlockModel,
            parent: CourseComponent
        ) {

            // TODO this(block.specialExamInfo == null) needs to be fixed as this a quick fix
            //  for LEARNER-8570
            if (block.isContainer && block.specialExamInfo == null) {
                val child = CourseComponent(block, parent)
                courseStructureV1Model.getDescendants(block).forEach { blockModel ->
                    normalizeCourseStructure(courseStructureV1Model, blockModel, child)
                }
            } else {
                if (BlockType.VIDEO == block.type && block.data is VideoData) {
                    VideoBlockModel(block, parent)
                } else if (BlockType.DISCUSSION == block.type && block.data is DiscussionData) {
                    DiscussionBlockModel(block, parent)
                } else {
                    // Fallback to html component for everything else
                    HtmlBlockModel(block, parent)
                }
            }
        }
    }
}
