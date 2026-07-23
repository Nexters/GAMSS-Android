plugins {
    alias(libs.plugins.gamss.jvm.library)
}

dependencies {
    implementation(projects.core.common)

    testImplementation(libs.junit)
}
