version = "1.1.0"
description = "View channel content"
aliucord.changelog.set("""
    * Added Media Grid Mode Toggle to view items in a 3-column square collage.
    * Added Guild-Wide search accessible from the Guild Context Menu.
    * Premium themed indeterminate circular loader at the center & bottom.
    * Crisp circular author avatars with native clipping.
    * Dynamically styled Server Content and Channel Content title headers.
    * Safe context menu dismissals and robust reflective Fresco scale bindings.
""".trimIndent())


android {
    namespace = "com.aliucord.plugins.channelmediagrid"
}
