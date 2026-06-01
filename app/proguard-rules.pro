# ============================================================================
# Speak2Easy R8 / ProGuard rules
#
# Goal: aggressive obfuscation in release builds while keeping the libraries
# that rely on reflection or runtime annotations working. Test every change
# here by running a *release* build (./gradlew :app:assembleRelease) and
# launching the resulting APK — debug builds skip these rules entirely.
# ============================================================================

# -- Crash-line readability ---------------------------------------------------
# Keep file/line metadata so Play Console crash reports can be deobfuscated
# with mapping.txt. Rename the source file so the original .kt name is hidden.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# -- Kotlin metadata ----------------------------------------------------------
# Compose, Coroutines, and reflection-using libraries need the @Metadata
# annotation preserved on classes they introspect.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.Unit

# -- kotlinx.serialization ----------------------------------------------------
# The compiler plugin generates `$$serializer` classes per @Serializable type.
# R8 must not rename or drop these, nor the static `serializer()` accessors on
# companion objects.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sonari.speak2easy.**$$serializer { *; }
-keepclassmembers class com.sonari.speak2easy.** {
    *** Companion;
}
-keepclasseswithmembers class com.sonari.speak2easy.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep every DTO and domain model class + fields verbatim. Cheap (these classes
# are small) and removes any ambiguity around serializer/field-name resolution.
-keep class com.sonari.speak2easy.data.remote.dto.** { *; }
-keep class com.sonari.speak2easy.domain.model.** { *; }

# -- Retrofit -----------------------------------------------------------------
# Retrofit uses reflection over the API interfaces at runtime. Don't strip the
# annotated methods; allow R8 to rename the interface itself.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking interface retrofit2.Response

-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**

# -- OkHttp / Okio ------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# -- Coroutines ---------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# -- Compose ------------------------------------------------------------------
# Most Compose rules are bundled in compose-runtime's consumer-proguard config,
# but the @Composable inference helpers need to stay accessible.
-keepclassmembers class androidx.compose.runtime.** {
    *;
}

# -- Google Identity / Credential Manager -------------------------------------
# Google ID Token handling reaches into the credential libs reflectively.
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# -- WebView JS bridge (not used, keep template stub) -------------------------
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
