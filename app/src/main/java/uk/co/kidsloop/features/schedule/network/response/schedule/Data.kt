package uk.co.kidsloop.features.schedule.network.response.schedule

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Data(
    @Json(name = "assessment_status") val assessmentStatus: String,
    @Json(name = "class_id") val classId: String,
    @Json(name = "class_type") val classType: String,
    @Json(name = "content_end_at") val contentEndAt: Int,
    @Json(name = "content_start_at") val contentStartAt: Int,
    @Json(name = "created_at") val createdAt: Int,
    @Json(name = "due_at") val dueAt: Int,
    @Json(name = "end_at") val endAt: Int,
    @Json(name = "exist_feedback") val existFeedback: Boolean,
    @Json(name = "id") val id: String,
    @Json(name = "is_hidden") val isHidden: Boolean,
    @Json(name = "is_home_fun") val isHomeFun: Boolean,
    @Json(name = "is_locked_lesson_plan") val isLockedLessonPlan: Boolean,
    @Json(name = "is_repeat") val isRepeat: Boolean,
    @Json(name = "is_review") val isReview: Boolean,
    @Json(name = "lesson_plan_id") val lessonPlanId: String,
    @Json(name = "review_status") val reviewStatus: String,
    @Json(name = "role_type") val roleType: String,
    @Json(name = "start_at") val startAt: Int,
    @Json(name = "status") val status: String,
    @Json(name = "title") val title: String
)
