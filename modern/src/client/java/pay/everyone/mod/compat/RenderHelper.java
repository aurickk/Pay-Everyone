package pay.everyone.mod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

/**
 * Modern RenderHelper for Minecraft 1.21.6-1.21.10
 * 
 * In 1.21.6+, GuiGraphics.pose() returns a Matrix3x2fStack (2D matrix stack)
 * instead of a PoseStack. The Matrix3x2fStack has different method names:
 * - pushMatrix() instead of pushPose()
 * - popMatrix() instead of popPose()
 * - translate(float, float) for 2D translation
 * - scale(float, float) for 2D scaling
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
     * In 1.21.6+, Matrix3x2fStack uses pushMatrix() instead of pushPose().
     */
    public static void pushPose(GuiGraphics graphics) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.pushMatrix();
        }
    }
    
    /**
     * Pop the matrix state.
     * In 1.21.6+, Matrix3x2fStack uses popMatrix() instead of popPose().
     */
    public static void popPose(GuiGraphics graphics) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.popMatrix();
        }
    }
    
    /**
     * Translate the current matrix.
     * In 1.21.6+, Matrix3x2fStack only supports 2D translation.
     * The z parameter is ignored for compatibility.
     */
    public static void translate(GuiGraphics graphics, float x, float y, float z) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.translate(x, y);
        }
    }
    
    /**
     * Scale the current matrix.
     * In 1.21.6+, Matrix3x2fStack only supports 2D scaling.
     * The z parameter is ignored for compatibility.
     */
    public static void scale(GuiGraphics graphics, float x, float y, float z) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.scale(x, y);
        }
    }
    
    /**
     * Returns true because modern versions support matrix scaling via Matrix3x2fStack.
     */
    public static boolean supportsMatrixScaling() {
        return true;
    }
}
