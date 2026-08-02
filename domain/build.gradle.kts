plugins {
    alias(libs.plugins.gamss.jvm.library)
}

dependencies {
    api(projects.core.common)

    implementation(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}
