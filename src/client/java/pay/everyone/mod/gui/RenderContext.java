package pay.everyone.mod.gui;

/** Stores window position/scale so widgets can convert to screen space. */
public class RenderContext {
    private static int windowX = 0;
    private static int windowY = 0;
    private static float scale = 1.0f;
    
    public static void set(int x, int y, float s) {
        windowX = x;
        windowY = y;
        scale = s;
    }
    
    public static void clear() {
        windowX = 0;
        windowY = 0;
        scale = 1.0f;
    }
    
    public static int toScreenX(int localX) {
        return windowX + (int)(localX * scale);
    }
    
    public static int toScreenY(int localY) {
        return windowY + (int)(localY * scale);
    }
    
    public static int getWindowX() { return windowX; }
    public static int getWindowY() { return windowY; }
    public static float getScale() { return scale; }
}

