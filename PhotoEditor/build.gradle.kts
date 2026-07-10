version = "2.0.0-dev"
description = "Edit image URLs with PhotoEditor"


android {
    namespace = "com.aliucord.plugins.photoeditor"
}

aliucord {
    author("furqanhun", 0L, hyperlink = false)

    changelog.set(
        """
        # 2.0.0-dev
        * Added a dedicated Settings Page for the plugin!
        * Tapping an image to edit it instantly is now an optional toggle in settings (Classic Bottom Sheet is back to being the default)
        * Added Delete & Spoiler buttons right inside the editor
        * Moved the Save button to the top header for instant access without scrolling
        * Added a native Discord-style visual overlay for spoilers that hides when you touch the screen
        * Restructured the toolbar to make Undo, Redo, and Reset global actions instead of burying them
        * Replaced awkward placeholder icons with native Discord vectors
        * The editor now instantly boots up with the Pen tool active and ready to draw
        * Smoother animations and ripples
        * Fixed critical bug where selecting a sticker in the editor sent it to chat (caused by conflict with the FakeStickers plugin)
        * Fixed "Could not open image editor" crashes caused by invalid window tokens by completely bulletproofing Activity fetching
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





