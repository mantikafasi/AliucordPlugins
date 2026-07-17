version = "1.0.5"
description = "Adds Klipy and Giphy results to Discord's GIF picker"

aliucord.changelog.set(
    """
        # 1.0.5
        - Restore Giphy search and trending results
		- Also fixes Tenor API by using official tenor with API key

        # 1.0.1
        - Bug fix
    """.trimIndent())

android {
    namespace = "com.aliucord.plugins"
}
