# Proguard rules for WebRTC and serialization
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
