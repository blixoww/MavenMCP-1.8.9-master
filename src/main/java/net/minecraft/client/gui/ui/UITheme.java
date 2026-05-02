package net.minecraft.client.gui.ui;

/**
 * Active menu color scheme derived from the currently applied widget theme.
 * Updated whenever a palette or custom theme is applied.
 *
 * primary   → panel borders + accent lines + first letter of panel titles
 * secondary → remaining panel title text
 */
public class UITheme {

    private static int primary   = 0xFFE02828;  // default red
    private static int secondary = 0xFFF2F2F2;  // default white

    public static int getPrimary()   { return primary; }
    public static int getSecondary() { return secondary; }

    public static void set(int primary, int secondary) {
        UITheme.primary   = primary;
        UITheme.secondary = secondary;
    }

    /** Primary color with a custom alpha (0-255). */
    public static int primary(int alpha) {
        return ((alpha & 0xFF) << 24) | (primary & 0x00FFFFFF);
    }

    /** Dimmed (~50 % brightness) version of the primary color, fully opaque. */
    public static int getPrimaryDim() {
        int r = ((primary >> 16) & 0xFF) >> 1;
        int g = ((primary >> 8)  & 0xFF) >> 1;
        int b = (primary         & 0xFF) >> 1;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
