package myedu.oshsu.kg

import okhttp3.ResponseBody
import retrofit2.http.*

interface MoocApi {
    @GET("api/v1/student/streams")
    suspend fun getStreams(): ResponseBody

    @GET("api/v1/student/course/lessons")
    suspend fun getCourseLessons(
        @Query("id_curricula") idCurricula: Int,
        @Query("streams[]") streamIds: List<Int>,
        @Query("course_ids[]") courseIds: List<Int>? = null
    ): List<MoocCourse>

    @GET("api/v1/student/course/lesson/show")
    suspend fun getLessonSteps(
        @Query("lesson_id") lessonId: Int,
        @Query("stream_id") streamId: Int
    ): List<MoocStep>

    @GET("api/v1/student/course/lesson/step")
    suspend fun getStepDetail(
        @Query("step_id") stepId: Int,
        @Query("stream_id") streamId: Int
    ): MoocStepDetailResponse

    @FormUrlEncoded
    @POST("api/v1/student/course/lesson/step/test")
    suspend fun submitTestAnswer(
        @Field("step_id") stepId: Int,
        @Field("stream_id") streamId: Int,
        @Field("answers_id") answerId: Int
    ): MoocTestSubmitResponse

    @FormUrlEncoded
    @POST("api/v1/student/course/lesson/step/update/chills")
    suspend fun markStepCompleted(
        @Field("step_id") stepId: Int,
        @Field("stream_id") streamId: Int,
        @Field("chills") chills: Int = 1
    ): MoocTestSubmitResponse

    @GET("api/v1/student/students-activity")
    suspend fun getActivity(): MoocActivityResponse
}
