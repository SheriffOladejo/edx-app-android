package com.grassroot.academy.repository

import com.grassroot.academy.course.CourseAPI
import com.grassroot.academy.http.HttpStatusException
import com.grassroot.academy.http.model.NetworkResponseCallback
import com.grassroot.academy.http.model.Result
import com.grassroot.academy.model.api.EnrollmentResponse
import com.grassroot.academy.viewModel.CourseViewModel.CoursesRequestType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val courseAPI: CourseAPI
) {
    fun fetchEnrolledCourses(
        type: CoursesRequestType,
        callback: NetworkResponseCallback<EnrollmentResponse>
    ) {
        val call = when (type) {
            CoursesRequestType.STALE -> courseAPI.enrolledCourses
            CoursesRequestType.CACHE -> courseAPI.enrolledCoursesFromCache
            CoursesRequestType.LIVE -> courseAPI.enrolledCoursesWithoutStale
        }

        call.enqueue(object : Callback<EnrollmentResponse> {
            override fun onResponse(
                call: Call<EnrollmentResponse>,
                response: Response<EnrollmentResponse>
            ) {
                when (response.isSuccessful && response.body() != null) {
                    true -> callback.onSuccess(
                        Result.Success(
                            isSuccessful = response.isSuccessful,
                            data = response.body(),
                            code = response.code(),
                            message = response.message()
                        )
                    )
                    false -> callback.onError(
                        Result.Error(
                            HttpStatusException(response.code(), response.message())
                        )
                    )
                }
            }

            override fun onFailure(call: Call<EnrollmentResponse>, t: Throwable) {
                callback.onError(Result.Error(t))
            }
        })
    }
}
