version = "1.0.2"
description = "Swipe between multiple images in Discord's media viewer"

android {
    namespace = "com.aliucord.plugins"
}

aliucord.changelog.set("""
    - Refactored video swipe previews to match native full-screen layout.
    - Fixed black preview screen bug by preserving pre-existing query parameters.
    - Implemented zero-dimension fallback to screen size.
""".trimIndent())
