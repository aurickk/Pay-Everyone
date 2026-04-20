package pay.everyone.mod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;*/
//?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if >=1.21.6 {
import org.joml.Matrix3x2fStack;
//?}

public class RenderHelper {

    //? if >=26.1 {
    /*public static void drawString(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color, boolean shadow) {
        if (graphics == null || font == null || text == null) return;
        graphics.text(font, text, x, y, color, shadow);
    }
    *///?} else {
    public static void drawString(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
        if (graphics == null || font == null || text == null) return;
        graphics.drawString(font, text, x, y, color, shadow);
    }
    //?}

    //? if >=26.1 {
    /*public static void fill(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        if (graphics == null) return;
        graphics.fill(x1, y1, x2, y2, color);
    }
    *///?} else {
    public static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (graphics == null) return;
        graphics.fill(x1, y1, x2, y2, color);
    }
    //?}

    //? if >=26.1 {
    /*public static void drawBorder(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        fill(graphics, x1, y1, x2, y1 + 1, color);
        fill(graphics, x1, y2 - 1, x2, y2, color);
        fill(graphics, x1, y1, x1 + 1, y2, color);
        fill(graphics, x2 - 1, y1, x2, y2, color);
    }
    *///?} else {
    public static void drawBorder(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        fill(graphics, x1, y1, x2, y1 + 1, color);
        fill(graphics, x1, y2 - 1, x2, y2, color);
        fill(graphics, x1, y1, x1 + 1, y2, color);
        fill(graphics, x2 - 1, y1, x2, y2, color);
    }
    //?}

    //? if >=26.1 {
    /*public static void enableScissor(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2) {
        if (graphics == null) return;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        minX = Math.max(0, Math.min(screenWidth, minX));
        maxX = Math.max(0, Math.min(screenWidth, maxX));
        minY = Math.max(0, Math.min(screenHeight, minY));
        maxY = Math.max(0, Math.min(screenHeight, maxY));

        graphics.enableScissor(minX, minY, maxX, maxY);
    }
    *///?} else {
    public static void enableScissor(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        if (graphics == null) return;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        minX = Math.max(0, Math.min(screenWidth, minX));
        maxX = Math.max(0, Math.min(screenWidth, maxX));
        minY = Math.max(0, Math.min(screenHeight, minY));
        maxY = Math.max(0, Math.min(screenHeight, maxY));

        graphics.enableScissor(minX, minY, maxX, maxY);
    }
    //?}

    //? if >=26.1 {
    /*public static void disableScissor(GuiGraphicsExtractor graphics) {
        if (graphics == null) return;
        graphics.disableScissor();
    }
    *///?} else {
    public static void disableScissor(GuiGraphics graphics) {
        if (graphics == null) return;
        graphics.disableScissor();
    }
    //?}

    //? if >=26.1 {
    /*public static void pushPose(GuiGraphicsExtractor graphics) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.pushMatrix();
        }
    }
    *///?} elif >=1.21.6 {
    public static void pushPose(GuiGraphics graphics) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.pushMatrix();
        }
    }
    //?} else {
    /*public static void pushPose(GuiGraphics graphics) {
        if (graphics == null) return;
        graphics.pose().pushPose();
    }
    *///?}

    //? if >=26.1 {
    /*public static void popPose(GuiGraphicsExtractor graphics) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.popMatrix();
        }
    }
    *///?} elif >=1.21.6 {
    public static void popPose(GuiGraphics graphics) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.popMatrix();
        }
    }
    //?} else {
    /*public static void popPose(GuiGraphics graphics) {
        if (graphics == null) return;
        graphics.pose().popPose();
    }
    *///?}

    //? if >=26.1 {
    /*public static void translate(GuiGraphicsExtractor graphics, float x, float y, float z) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.translate(x, y);
        }
    }
    *///?} elif >=1.21.6 {
    public static void translate(GuiGraphics graphics, float x, float y, float z) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.translate(x, y);
        }
    }
    //?} else {
    /*public static void translate(GuiGraphics graphics, float x, float y, float z) {
        if (graphics == null) return;
        graphics.pose().translate(x, y, z);
    }
    *///?}

    //? if >=26.1 {
    /*public static void scale(GuiGraphicsExtractor graphics, float x, float y, float z) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.scale(x, y);
        }
    }
    *///?} elif >=1.21.6 {
    public static void scale(GuiGraphics graphics, float x, float y, float z) {
        if (graphics == null) return;
        Matrix3x2fStack pose = graphics.pose();
        if (pose != null) {
            pose.scale(x, y);
        }
    }
    //?} else {
    /*public static void scale(GuiGraphics graphics, float x, float y, float z) {
        if (graphics == null) return;
        graphics.pose().scale(x, y, z);
    }
    *///?}

    public static boolean supportsMatrixScaling() {
        return true;
    }
}
