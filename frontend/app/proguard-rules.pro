# RULES PROGUARD — dự án chưa bật minify ở release.
# Bổ sung rule khi bật R8/minify cho release build.

# Retrofit / OkHttp / Gson
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson: giữ tên field của DTO
-keepclassmembers class com.eventapp.app.data.remote.dto.** { <fields>; }
-keep class com.eventapp.app.data.remote.dto.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }

# Fragment (Navigation Component tạo instance bằng reflection)
-keep public class * extends androidx.fragment.app.Fragment

# FCM
-keep class com.eventapp.app.fcm.** { *; }
