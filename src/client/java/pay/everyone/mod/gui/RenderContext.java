package pay.everyone.mod.gui;

/**
 * Holds the current window's screen position and scale during rendering.
 * This is needed for widgets that use scissoring, since scissor coordinates
 * must be in screen space, not the transformed local coordinate space.
 */
public class RenderContext {
    private static int windowX = 0;
    private static int windowY = 0;
    private static float scale = 1.0f;
    
    /**
     * Sets the current render context. Called by PayEveryoneWindow before rendering widgets.
     */
    public static void set(int x, int y, float s) {
        windowX = x;
        windowY = y;
        scale = s;
    }
    
    /**
     * Clears the render context after rendering.
     */
    public static void clear() {
        windowX = 0;
        windowY = 0;
        scale = 1.0f;
    }
    
    /**
     * Converts a local X coordinate to screen X coordinate.
     */
    public static int toScreenX(int localX) {
        return windowX + (int)(localX * scale);
    }
    
    /**
     * Converts a local Y coordinate to screen Y coordinate.
     */
    public static int toScreenY(int localY) {
        return windowY + (int)(localY * scale);
    }
    
    public static int getWindowX() { return windowX; }
    public static int getWindowY() { return windowY; }
    public static float getScale() { return scale; }
}

