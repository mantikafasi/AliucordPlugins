version = "1.0.2"
description = "Favorite images, videos, and audio files from chat"

android {
    namespace = "com.aliucord.plugins"
}

aliucord.changelog.set("""
    # 1.0.2
    * Refresh expired Discord CDN favorite URLs
    * Move audio favorite star away from download button

    # 1.0.1
    * Flatten Media expression tab styling
    * Add video thumbnails in Media Favorites

    # 1.0.0 - Initial release
    * Star button on image, video, and audio attachments
    * Separate favorites lists for each media type
    * Browse and manage favorites in settings
""".trimIndent())
