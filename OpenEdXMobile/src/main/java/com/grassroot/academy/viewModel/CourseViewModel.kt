package com.grassroot.academy.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.grassroot.academy.core.IEdxEnvironment
import com.grassroot.academy.http.HttpStatus
import com.grassroot.academy.http.HttpStatusException
import com.grassroot.academy.http.model.NetworkResponseCallback
import com.grassroot.academy.http.model.Result
import com.grassroot.academy.logger.Logger
import com.grassroot.academy.model.api.EnrolledCoursesResponse
import com.grassroot.academy.model.api.EnrollmentResponse
import com.grassroot.academy.module.db.DataCallback
import com.grassroot.academy.repository.CourseRepository
import com.grassroot.academy.util.observer.Event
import com.grassroot.academy.util.observer.postEvent
import com.grassroot.academy.viewModel.CourseViewModel.CoursesRequestType.CACHE
import com.grassroot.academy.viewModel.CourseViewModel.CoursesRequestType.STALE
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val environment: IEdxEnvironment,
    private val courseRepository: CourseRepository,
) : ViewModel() {

    private val logger: Logger = Logger(CourseViewModel::class.java.simpleName)

    private val _enrolledCourses = MutableLiveData<Event<List<EnrolledCoursesResponse>>>()
    val enrolledCoursesResponse: LiveData<Event<List<EnrolledCoursesResponse>>> = _enrolledCourses

    private val _showProgress = MutableLiveData(true)
    val showProgress: LiveData<Boolean> = _showProgress

    private val _handleError = MutableLiveData<Throwable>()
    val handleError: LiveData<Throwable> = _handleError

    fun fetchEnrolledCourses(
        type: CoursesRequestType,
        showProgress: Boolean = true
    ) {
        if (environment.loginPrefs.isUserLoggedIn.not()) {
            _handleError.value = HttpStatusException(HttpStatus.UNAUTHORIZED, "")
            return
        }
        _showProgress.postValue(showProgress)
        courseRepository.fetchEnrolledCourses(
            type = type,
            callback = object : NetworkResponseCallback<EnrollmentResponse> {

                override fun onSuccess(result: Result.Success<EnrollmentResponse>) {
                    result.data?.let {
                        _enrolledCourses.postEvent(it.enrollments)
                        environment.appFeaturesPrefs.setAppConfig(it.appConfig)

                        if (type != CACHE) {
                            updateDatabaseAfterDownload(it.enrollments)
                        } else {
                            fetchEnrolledCourses(type = STALE, it.enrollments.isEmpty())
                        }
                    }
                }

                override fun onError(error: Result.Error) {
                    if (type == CACHE) {
                        fetchEnrolledCourses(type = STALE)
                    } else {
                        _handleError.value = error.throwable
                    }
                }
            })
    }

    private fun updateDatabaseAfterDownload(list: List<EnrolledCoursesResponse>?) {
        val dataCallback: DataCallback<Int> = object : DataCallback<Int>() {
            override fun onResult(result: Int) {}
            override fun onFail(ex: Exception) {
                logger.error(ex)
            }
        }

        if (list != null && list.isEmpty()) {
            //update all videos in the DB as Deactivated
            environment.database?.updateAllVideosAsDeactivated(dataCallback)

            // Update all videos for a course fetched in the API as Activated
            list.filter { it.isActive }.forEach {
                environment.database?.updateVideosActivatedForCourse(
                    it.courseId,
                    dataCallback
                )
            }

            //Delete all videos which are marked as Deactivated in the database
            environment.storage?.deleteAllUnenrolledVideos()
        }
    }

    sealed class CoursesRequestType {
        object LIVE : CoursesRequestType()
        object STALE : CoursesRequestType()
        object CACHE : CoursesRequestType()
    }
}
