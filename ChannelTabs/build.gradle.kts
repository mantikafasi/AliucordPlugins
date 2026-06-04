version = "1.1.3"
description = "View channel content"
aliucord.changelog.set("""
    # 1.1.3
    * Reused Discord's native guild profile tab button for the server Media action.

    # 1.1.2
    * Fixed media grid/list switching reusing wrong-sized item views.
    * Forced media grid profile avatars to render circular.
    * Added a 3-column media grid view with sender avatars on image tiles.
    * Added the Media button to DM sidebars.
    * Aligned the Media sidebar button with Discord's native action button style.
    # 1.1.0
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
