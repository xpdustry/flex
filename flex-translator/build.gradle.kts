dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.deepl) {
        exclude("org.jetbrains", "annotations")
    }
    implementation(libs.caffeine) {
        exclude("org.checkerframework", "checker-qual")
        exclude("com.google.errorprone", "error_prone_annotations")
    }
}
