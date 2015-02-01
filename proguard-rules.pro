# No need to obfuscate - this app is open source and not sensitive to security concerns.
-dontobfuscate

# This optimization seems to cause dex errors.
-optimizations !code/allocation/variable

# Wire dependencies refer to classes in OkHttp which aren't present and aren't used.
-dontwarn okio.**

# Keep all proto builder classes for use with parsing libraries which rely on reflection.
-keep class ** extends com.squareup.wire.Message$Builder {
    *;
}

# Strip out verbose logging at compile time.
-assumenosideeffects class com.jeffpdavidson.fantasywear.log.FWLog {
    public static void v(...);
}