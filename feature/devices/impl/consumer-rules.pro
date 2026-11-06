# Shipped with this module, so they apply to any app that includes it.

# play-review-ktx references a Play services annotation that is not on the classpath; it is compile-time only.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# ML Kit (Camera & QR demo) finds its component registrars through manifest meta-data and creates them with
# their no-arg constructor by reflection. R8 kept the classes but dropped those constructors, so nothing was
# registered and BarcodeScanning.getClient() crashed the release build on a null component.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
