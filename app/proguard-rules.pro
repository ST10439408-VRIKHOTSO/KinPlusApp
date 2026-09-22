# Kin+ ProGuard / R8 rules for release builds.

# Keep Retrofit/Gson DTO models (serialized by reflection).
-keep class za.co.kinplus.app.data.remote.dto.** { *; }

# Retrofit + OkHttp.
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes *Annotation*

# Gson.
-keep class com.google.gson.** { *; }

# Room entities.
-keep class za.co.kinplus.app.data.local.** { *; }

# Firebase.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
