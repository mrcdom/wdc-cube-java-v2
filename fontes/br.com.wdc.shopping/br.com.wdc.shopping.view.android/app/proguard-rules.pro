# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class br.com.wdc.shopping.view.android.**$$serializer { *; }
-keepclassmembers class br.com.wdc.shopping.view.android.** {
    *** Companion;
}
-keepclasseswithmembers class br.com.wdc.shopping.view.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
