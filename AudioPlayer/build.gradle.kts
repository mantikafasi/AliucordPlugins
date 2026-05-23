version = "1.0.5"
description = "Play audio files with a beautiful inline player UI."

android {
    namespace = "com.aliucord.plugins.audioplayer"
}

aliucord.changelog.set(""")
    - 1.0.5
        - add a hide button for the sticky player
        - add sticky top player when the active audio scrolls off-screen
        - hide the sticky player when playback stops
        - show the filename in the sticky player
        - improve OGG/Opus duration handling
        - improve Themer color compatibility for custom player UI
    - 1.0.3
        - download opus files to make seekbar work (thank you android)
""".trimIndent())
