package com.gamss.android.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureComposeDependencies() {
    dependencies {
        "implementation"(platform(libs.findLibrary("compose-bom").get()))
        "implementation"(libs.findLibrary("compose-ui").get())
        "implementation"(libs.findLibrary("compose-ui-graphics").get())
        "implementation"(libs.findLibrary("compose-ui-tooling-preview").get())
        "implementation"(libs.findLibrary("compose-material3").get())
        "debugImplementation"(libs.findLibrary("compose-ui-tooling").get())
    }
}
