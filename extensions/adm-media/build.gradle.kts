import com.android.build.api.dsl.ApplicationExtension

extension {
    name = "extensions/adm-media.mpe"
}

android {
    namespace = "app.adm.extension.media"
}

configure<ApplicationExtension> {
    defaultConfig {
        minSdk = 26
    }
}
