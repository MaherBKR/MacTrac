-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.macrotracker.**$$serializer { *; }
-keepclassmembers class com.macrotracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.macrotracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}
