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
        
        # New Features & Enhancements
        * Added a dedicated Settings Page for the plugin
        * Added a "Quick Edit" toggle in settings to instantly edit images without opening the attachment menu
        * Brush Layer Engine: Added a new "Brush Layer Mode" to settings to control whether strokes draw behind, in front of, or weave between stickers
        * Custom Filter Engine: Build your own effects using 6 simultaneous sliders (Brightness, Contrast, Saturation, Hue, Temperature, Tint)
        * Advanced Cropping: Added a lag-free real-time rotation slider (-180° to 180°), mirroring, and quick-snap rotation to the Crop Modal
        * Overlay Management: Long-press any sticker, emoji, or image overlay to open an Options Menu where you can individually crop and edit it without affecting the background
        * Color Picker Modal: A massive new picker with HSV sliders, live HEX input, and a drag-to-preview Eyedropper tool
        * Typography Engine: The Text Modal now features a live preview box, sizing sliders, and recursively parses local .ttf and .otf files for custom fonts
        * Global Actions: Undo, Redo, and Reset are now global actions with dynamic state tracking (buttons grey out when there's nothing to undo)

        # UI & UX Polish
        * Replaced the S/L brush size buttons with a dynamic slider, locking the sub-toolbar in place
        * Added Delete, Spoiler, and Save buttons directly to the top header for instant access without scrolling
        * Implemented a native Discord-style visual Spoiler overlay that hides automatically when you touch the screen
        * Replaced all awkward placeholder graphics with native Discord vector icons
        * The editor now instantly boots up with the Pen tool active and ready to draw
        * Smoother animations, ripples, and a fully theme-aware layout that elegantly floats above keyboards

        # Bug Fixes & Technical Improvements
        * Built a downscaled-preview rendering engine so the crop rotation slider is completely lag-free on all devices
        * Fixed OpenGL occlusion bugs so live previews work instantly with custom ColorMatrix filters
        * Fixed a critical bug where selecting a sticker in the editor sent it to chat (resolved conflict with the FakeStickers plugin)
        * Fixed "Could not open image editor" crashes caused by invalid window tokens by bulletproofing Activity fetching
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





