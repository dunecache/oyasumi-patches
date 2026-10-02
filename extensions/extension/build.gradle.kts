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
}
