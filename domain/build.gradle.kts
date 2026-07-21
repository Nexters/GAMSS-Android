plugins {
    alias(libs.plugins.gamss.jvm.library)
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.javax.inject)

    testImplementation(libs.junit)
}
