package myedu.oshsu.kg

import com.google.gson.annotations.SerializedName

// --- Auth ---

data class MoocLoginRequest(val email: String, val password: String)

data class MoocLoginResponse(
    val success: Boolean?,
    val token: String?,
    // Support alternative response shapes from the MOOC API
    val authorisation: MoocAuthData?,
    val access_token: String?,
    val message: String?
) {
    /** Extract token from whichever field the API returns it in. */
    fun extractToken(): String? = token ?: authorisation?.token ?: access_token
}

data class MoocAuthData(val token: String?, val type: String?)

// --- Streams / Subject listing ---

data class MoocStreamSubject(
    val credit: Int?,
    val is_kpv: Boolean?,
    val id_subject: Int?,
    val subject: String?,
    val streams: List<MoocStream>?
)

data class MoocStream(
    val id: Int,
    val id_subject: Int?,
    val id_teacher: Int?,
    val id_curricula: Int?,
    val id_semester: Int?,
    val numeric: Int?,
    val subject_type_name: MoocSubjectTypeName?,
    val curricula: MoocCurricula?
)

data class MoocSubjectTypeName(
    val id: Int?,
    val name_kg: String?,
    val name_ru: String?,
    val name_en: String?,
    val short_name_kg: String?,
    val short_name_ru: String?,
    val short_name_en: String?
) {
    fun getName(lang: String): String = when (lang) {
        "ky" -> name_kg ?: name_ru ?: name_en ?: "-"
        "en" -> name_en ?: name_ru ?: name_kg ?: "-"
        else -> name_ru ?: name_kg ?: name_en ?: "-"
    }
    fun getShortName(lang: String): String = when (lang) {
        "ky" -> short_name_kg ?: short_name_ru ?: short_name_en ?: "-"
        "en" -> short_name_en ?: short_name_ru ?: short_name_kg ?: "-"
        else -> short_name_ru ?: short_name_kg ?: short_name_en ?: "-"
    }
}

data class MoocCurricula(
    val id: Int?,
    val credit: Int?,
    val workload: Int?,
    val id_subject: Int?,
    val id_semester: Int?,
    val name_subject: MoocNameSubject?
)

data class MoocNameSubject(
    val id: Int?,
    val name_kg: String?,
    val name_ru: String?,
    val name_en: String?,
    val short_name_kg: String?,
    val short_name_ru: String?,
    val short_name_en: String?,
    val code: String?
) {
    fun getName(lang: String): String = when (lang) {
        "ky" -> name_kg ?: name_ru ?: name_en ?: "-"
        "en" -> name_en ?: name_ru ?: name_kg ?: "-"
        else -> name_ru ?: name_kg ?: name_en ?: "-"
    }
}

// --- Course / Lessons ---

data class MoocCourse(
    val id: Int,
    val user_id: Int?,
    val title: String?,
    val description: String?,
    val image: String?,
    val image_url: String?,
    val status: Boolean?,
    val is_published: Boolean?,
    val user: MoocCourseTeacher?,
    val lessons: List<MoocLesson>?
)

data class MoocCourseTeacher(
    val id: Int?,
    val name: String?,
    val last_name: String?,
    val father_name: String?
) {
    fun getFullName(): String {
        val parts = listOfNotNull(last_name, name, father_name).filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) parts.joinToString(" ") else "-"
    }
}

data class MoocLesson(
    val id: Int,
    val user_id: Int?,
    val course_id: Int?,
    val title: String?,
    val is_deleted: Boolean?,
    val is_published: Boolean?,
    val sequence_number: Int?,
    val from: String?,
    val to: String?,
    val active: Boolean?
)

// --- Steps ---

data class MoocStep(
    val id: Int,
    val id_parent: Int?,
    val type_id: Int?,
    val user_id: Int?,
    val lesson_id: Int?,
    val step: Int?,
    val active: Boolean?,
    val score: Int?,
    val content: MoocStepContent?,
    val chills: Boolean?,
    val count_attempt: Int?,
    val my_score: Int?,
    val type: MoocStepType?
)

data class MoocStepContent(
    val id: Int?,
    val lesson_id: Int?,
    // Document fields
    val title: String?,
    val description: String?,
    val document: String?,
    val document_path: String?,
    // Test fields
    val content: String?,
    val score: Int?,
    val image: String?,
    val answers: List<MoocTestAnswer>?,
    // Video fields
    val video_url: String?
)

data class MoocTestAnswer(
    val id: Int,
    val text: String?
)

data class MoocStepType(
    val id: Int?,
    val title: String?,
    @SerializedName("modelName") val modelName: String?,
    val name: String?,
    val order: Int?
)

// --- Step Detail Response ---

data class MoocStepDetailResponse(
    val success: Boolean?,
    val step: MoocStepDetail?
)

data class MoocStepDetail(
    val id: Int,
    val id_parent: Int?,
    val type_id: Int?,
    val lesson_id: Int?,
    val step: Int?,
    val active: Boolean?,
    val score: Int?,
    val content: MoocStepContent?,
    val chills: Boolean?,
    val count_attempt: Int?,
    val my_score: Int?,
    val details: List<Any>?,
    val answer_id: Int?,
    val type: MoocStepType?
)

// --- Test Submit Response ---

data class MoocTestSubmitResponse(
    val success: Boolean?,
    val message: String?
)

// --- Activity ---

data class MoocActivityResponse(
    val success: Boolean?,
    val data: MoocActivity?
)

data class MoocActivity(
    val streak: Int?,
    val last_visit: String?,
    val total_active_days: Int?
)

// --- UI Helper: Flat course item for display ---

data class MoocCourseDisplayItem(
    val semesterKey: String,
    val subjectName: String,
    val credit: Int?,
    val streamIds: List<Int>,
    val curriculaId: Int,
    val courseIds: List<Int>,
    val subjectTypeName: String?
)
