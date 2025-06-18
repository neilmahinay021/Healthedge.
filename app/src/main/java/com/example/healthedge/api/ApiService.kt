package com.example.healthedge.api

import com.example.healthedge.models.User
import com.example.healthedge.models.Diagnosis
import com.example.healthedge.models.Workout
import com.example.healthedge.models.Vitals
import com.example.healthedge.models.UserRegistration
import com.example.healthedge.models.LoginRequest
import com.example.healthedge.models.LoginResponse
import com.example.healthedge.models.DoctorRegistration
import com.example.healthedge.models.Doctor
import com.example.healthedge.models.DiagnosisList
import com.example.healthedge.models.MedicineWithDosage
import com.example.healthedge.models.DiseaseWorkout
import com.example.healthedge.models.UserWorkoutsResponse
import com.example.healthedge.models.WorkoutHistoryLog
import com.example.healthedge.models.Notification
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody

interface ApiService {
    @GET("user")
    suspend fun getUser(@Query("id") userId: Int): Response<User>

    @GET("diagnoses")
    suspend fun getUserDiagnoses(@Query("user_id") userId: Int): Response<List<Diagnosis>>

    @GET("workouts")
    suspend fun getWorkouts(@Query("diagnosis_id") diagnosisId: Int): Response<List<Workout>>

    @POST("add_diagnosis")
    suspend fun addDiagnosis(@Body diagnosis: Diagnosis): Response<Map<String, Any>>

    @GET("vitals")
    suspend fun getVitals(@Query("user_id") userId: Int): Response<List<Vitals>>

    @POST("add_vitals")
    suspend fun addVitals(@Body vitals: Vitals): Response<Map<String, Any>>

    @POST("register_user")
    suspend fun registerUser(@Body user: UserRegistration): Response<Map<String, Any>>

    @POST("register_doctor")
    suspend fun registerDoctor(@Body doctor: DoctorRegistration): Response<Map<String, Any>>

    @GET("users")
    suspend fun getUsers(): Response<List<User>>

    @GET("doctors")
    suspend fun getDoctors(): Response<List<Doctor>>

    @GET("diagnoses_list")
    suspend fun getDiagnosesList(): Response<List<DiagnosisList>>

    @GET("medicines_by_disease")
    suspend fun getMedicinesByDisease(@Query("disease_id") diseaseId: Int): Response<List<MedicineWithDosage>>

    @POST("add_feedback")
    suspend fun addFeedback(@Body feedback: Map<String, Any>): Response<Map<String, Any>>

    @GET("feedback")
    suspend fun getFeedback(@Query("user_id") userId: Int): Response<List<Map<String, Any>>>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("workouts_by_disease")
    suspend fun getWorkoutsByDisease(@Query("disease_id") diseaseId: Int): Response<List<DiseaseWorkout>>

    @GET("user_workouts")
    suspend fun getUserWorkouts(@Query("user_id") userId: Int): Response<UserWorkoutsResponse>

    @POST("user_workout_history")
    suspend fun saveWorkoutHistory(@Body log: WorkoutHistoryLog): Response<Map<String, Any>>

    @GET("user_workout_history")
    suspend fun getWorkoutHistory(
        @Query("user_id") userId: Int,
        @Query("date") date: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<List<WorkoutHistoryLog>>

    @POST("reset_user_workout_history")
    suspend fun resetWorkoutHistory(@Body userIdMap: Map<String, Int>): Response<Map<String, Any>>

    // Notification Endpoints
    @GET("notifications")
    suspend fun getNotifications(@Query("user_id") userId: Int): Response<List<Notification>>

    @POST("mark_notification_read")
    suspend fun markNotificationRead(@Body notificationIdMap: Map<String, Int>): Response<Map<String, Any>>

    @Multipart
    @POST("verify_doctor_id")
    suspend fun verifyDoctorId(@Part idImage: MultipartBody.Part): Response<Map<String, Any>>
} 