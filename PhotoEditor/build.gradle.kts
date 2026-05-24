version = "1.0.0"
description = "Edit image URLs with PhotoEditor"


android {
    namespace = "com.aliucord.plugins.photoeditor"
}

dependencies {

    implementation(files("libs/photoeditor-3.1.0-classes.jar"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    }
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
}





