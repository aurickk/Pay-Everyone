package pay.everyone.mod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Legacy RenderHelper for Minecraft 1.21.1-1.21.5
 * Uses GuiGraphics.pose() directly for matrix operations.
 * No reflection needed - this version is compiled against compatible mappings.
 */
public class RenderHelper {
    
    /**
     * Draw a string with the given parameters.
     */
    public static void drawString(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
        if (graphics == null || font == null || text == null) return;
        graphics.drawString(font, text, x, y, color, shadow);
    }
    
    /**
     * Fill a rectangle with the given color.
     */
    public static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (graphics == null) return;
        graphics.fill(x1, y1, x2, y2, color);
    }
    
    /**
     * Enable scissor clipping for the given region.
     * Coordinates should be in GUI-scaled screen space.
     * 
     * Note: When inside a matrix transform, the scissor coordinates must
     * already be converted to screen space by the caller.
     */
    public static void enableScissor(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        if (graphics == null) return;
        // Ensure coordinates are valid (x1 < x2, y1 < y2)
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        
        // Clamp to screen bounds
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        minX = Math.max(0, Math.min(screenWidth, minX));
        maxX = Math.max(0, Math.min(screenWidth, maxX));
        minY = Math.max(0, Math.min(screenHeight, minY));
        maxY = Math.max(0, Math.min(screenHeight, maxY));
        
        graphics.enableScissor(minX, minY, maxX, maxY);
    }
    
    /**
     * Disable scissor clipping.
     */
    public static void disableScissor(GuiGraphics graphics) {
        if (graphics == null) return;
        graphics.disableScissor();
    }
    
    /**
     * Push the current matrix state.
     */
    public static void pushPose(GuiGraphics graphics) {
        if (graphics == null) return;
        graphics.pose().pushPose();
    }
    
    /**
     * Pop the matrix state.
     */
    public static void popPose(GuiGraphics graphics) {
        if (graphics == null) return;
        graphics.pose().popPose();
    }
    
    /**
     * Translate the current matrix.
     */
    public static void translate(GuiGraphics graphics, float x, float y, float z) {
        if (graphics == null) return;
        graphics.pose().translate(x, y, z);
    }
    
    /**
     * Scale the current matrix.
     */
    public static void scale(GuiGraphics graphics, float x, float y, float z) {
        if (graphics == null) return;
        graphics.pose().scale(x, y, z);
    }
    
    /**
     * Returns true because legacy versions support full matrix scaling.
     */
    public static boolean supportsMatrixScaling() {
        return true;
    }
}

