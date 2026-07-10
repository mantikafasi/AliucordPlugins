version = "2.0.0-dev"
description = "Edit image URLs with PhotoEditor"


android {
    namespace = "com.aliucord.plugins.photoeditor"
}

aliucord {
    author("furqanhun", 0L, hyperlink = false)

    changelog.set(
        """
        # 1.1.0-dev
        * Revamped UI with a categorized sub-toolbar
        * Tap images to edit them instantly (no more bottom sheet!)
        * Added Delete & Spoiler buttons right inside the editor
        * Added a native Discord-style visual overlay for spoilers that hides when you touch the screen
        * Smoother animations and ripples
        * Fixed critical bug where selecting a sticker in the editor sent it to chat (caused by conflict with the FakeStickers plugin)
        * Fixed "Could not open image editor" crashes caused by invalid window tokens
        """.trimIndent(),
    )

    deploy.set(true)
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
