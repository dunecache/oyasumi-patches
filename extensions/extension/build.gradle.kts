extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.oyasumi.extension"
}

dependencies {
    // AdvertisingIdClient is already in every APK this extension targets, so it is needed only
    // to compile against, never to bundle. Bundling it would put a second copy of the class
    // into the merged DEX.
    compileOnly("com.google.android.gms:play-services-ads-identifier:18.0.1")

    // Same arrangement for the Material 3 widgets (MaterialToolbar, MaterialDivider,
    // MaterialTextView, MaterialSwitch): Pinterest bundles the Material library, so these are
    // resolved from the target APK at runtime. Only widgets verified present in the reference
    // DEX may be used; anything R8 stripped from the app would compile here and crash there.
    // DynamicColors is absent from the APK, so there is no wallpaper-tinted dynamic color.
    compileOnly("com.google.android.material:material:1.12.0")
}
