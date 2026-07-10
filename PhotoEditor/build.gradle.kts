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
        * Added dynamic state tracking so the Undo, Redo, and Reset buttons automatically grey out when there's nothing to undo
        * Replaced the S/L brush size buttons with a dynamic slider, and locked the sub-toolbar in place to prevent accidental sliding
        * Added a Custom Color Picker modal featuring HSV sliders, live HEX input, and an Eyedropper tool with drag-to-preview
        * Completely overhauled the Text Insertion Modal to feature a live preview input box and typography controls
        * Built a searchable font engine that recursively parses .ttf/.otf files from your system and local storage
        * Added a live font size slider and toggleable Bold, Italic, and Underline buttons
        * The font dropdown is now fully theme-aware and flawlessly floats over the keyboard
        * Created a powerful new Custom Filter engine that lets you build your own effects
        * Added Brightness, Contrast, Saturation, Hue, Temperature, and Tint sliders that can all be layered simultaneously
        * Fixed OpenGL occlusion bugs so live previews work instantly with custom ColorMatrix filters
        * Upgraded the Crop Modal to feature full real-time Free Rotation and Mirroring
        * Added a smooth -180° to 180° rotation slider, Flip H/V toggles, and a Rotate 90° snap button directly to the crop overlay
        * Built a downscaled-preview rendering engine so the rotation slider is completely lag-free on all devices
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





