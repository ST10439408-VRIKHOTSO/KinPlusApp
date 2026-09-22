package za.co.kinplus.app.data.remote

import za.co.kinplus.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit description of the Kin+ REST API (Part 1B, Table 5.3). Every call is
 * a suspend function so it runs off the main thread. The bearer token is added
 * automatically by [AuthInterceptor].
 */
interface KinPlusApi {

    // ----- Users -----
    @POST("api/v1/users/sync")
    suspend fun syncUser(@Body body: UserSyncRequest): UserDto

    @GET("api/v1/users/me")
    suspend fun getMe(): UserDto

    @PATCH("api/v1/users/me")
    suspend fun patchMe(@Body body: UserPatchRequest): UserDto

    @DELETE("api/v1/users/me")
    suspend fun deleteMe(): Response<Unit>

    // ----- Circles -----
    @POST("api/v1/circles")
    suspend fun createCircle(@Body body: CreateCircleRequest): CircleDto

    @GET("api/v1/circles")
    suspend fun getCircles(): List<CircleDto>

    @GET("api/v1/circles/{id}")
    suspend fun getCircle(@Path("id") id: String): CircleDetailDto

    @POST("api/v1/circles/join")
    suspend fun joinCircle(@Body body: JoinCircleRequest): CircleDto

    @PATCH("api/v1/circles/{id}/sharing")
    suspend fun setCircleSharing(@Path("id") id: String, @Body body: SharingRequest): Response<Unit>

    @DELETE("api/v1/circles/{id}/members/{memberUid}")
    suspend fun removeMember(@Path("id") id: String, @Path("memberUid") memberUid: String): Response<Unit>

    // ----- Locations -----
    @POST("api/v1/locations")
    suspend fun uploadLocation(@Body body: LocationUploadRequest): Response<Unit>

    @POST("api/v1/locations/batch")
    suspend fun uploadLocationBatch(@Body body: LocationBatchRequest): Response<Unit>

    @GET("api/v1/circles/{circleId}/locations")
    suspend fun getCircleLocations(@Path("circleId") circleId: String): List<MemberLocationDto>

    // ----- SOS -----
    @POST("api/v1/sos")
    suspend fun raiseSos(@Body body: SosRequest): SosDto

    @POST("api/v1/sos/{id}/cancel")
    suspend fun cancelSos(@Path("id") id: String): Response<Unit>

    @GET("api/v1/sos/active")
    suspend fun getActiveSos(): List<SosDto>

    // ----- Safe Zones -----
    @POST("api/v1/zones")
    suspend fun createZone(@Body body: ZoneRequest): ZoneDto

    @GET("api/v1/zones")
    suspend fun getZones(): List<ZoneDto>

    @PATCH("api/v1/zones/{id}")
    suspend fun patchZone(@Path("id") id: String, @Body body: ZonePatchRequest): Response<Unit>

    @DELETE("api/v1/zones/{id}")
    suspend fun deleteZone(@Path("id") id: String): Response<Unit>

    @POST("api/v1/zones/{id}/events")
    suspend fun reportZoneEvent(@Path("id") id: String, @Body body: ZoneEventRequest): Response<Unit>

    // ----- Journeys -----
    @POST("api/v1/journeys")
    suspend fun createJourney(@Body body: JourneyRequest): JourneyDto

    @POST("api/v1/journeys/{id}/arrive")
    suspend fun arriveJourney(@Path("id") id: String): Response<Unit>

    @GET("api/v1/journeys/watching")
    suspend fun getWatchingJourneys(): List<JourneyDto>

    // ----- Community Alerts -----
    @POST("api/v1/alerts/upload-url")
    suspend fun createUploadUrl(@Body body: UploadUrlRequest): UploadUrlDto

    @POST("api/v1/alerts")
    suspend fun createAlert(@Body body: AlertRequest): AlertDto

    @GET("api/v1/alerts")
    suspend fun getNearbyAlerts(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double? = null
    ): List<AlertDto>

    @POST("api/v1/alerts/{id}/confirm")
    suspend fun confirmAlert(@Path("id") id: String): Response<Unit>
}
