# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.duncanbottrill.ridenamer.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.duncanbottrill.ridenamer.**$$serializer { *; }
