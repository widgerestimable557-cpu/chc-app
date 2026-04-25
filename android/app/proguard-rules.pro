# Proguard rules for CHC App
-keep class com.christianhelpcenter.app.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
