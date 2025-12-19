package pay.everyone.mod.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import pay.everyone.mod.PayEveryone;

import java.lang.reflect.Method;

/** Cross-version rendering helper for 1.21.1–1.21.10 rendering APIs. */
public class RenderHelper {
    private static boolean initialized = false;
    private static boolean loggedOnce = false;
    
    private static boolean hasPoseStack = false;
    private static Method poseMethod = null;
    private static Method pushMethod = null;
    private static Method popMethod = null;
    private static Method translateMethod = null;
    private static Method scaleMethod = null;
    private static int translateParamCount = 0;
    private static int scaleParamCount = 0;
    
    public static void init(GuiGraphics graphics) {
        if (initialized) return;
        
        try {
            poseMethod = GuiGraphics.class.getMethod("pose");
            Object poseStack = poseMethod.invoke(graphics);
            
            if (poseStack != null) {
                Class<?> poseClass = poseStack.getClass();
                PayEveryone.LOGGER.info("RenderHelper: pose() returns " + poseClass.getName());
                
                for (Method m : poseClass.getMethods()) {
                    String name = m.getName();
                    int params = m.getParameterCount();
                    
                    if ((name.equals("pushPose") || name.equals("push")) && params == 0) {
                        pushMethod = m;
                    } else if ((name.equals("popPose") || name.equals("pop")) && params == 0) {
                        popMethod = m;
                    } else if (name.equals("translate")) {
                        Class<?>[] paramTypes = m.getParameterTypes();
                        if (params >= 2 && params <= 3) {
                            if (paramTypes[0] == float.class || paramTypes[0] == double.class) {
                                translateMethod = m;
                                translateParamCount = params;
                            }
                        }
                    } else if (name.equals("scale")) {
                        Class<?>[] paramTypes = m.getParameterTypes();
                        if (params >= 2 && params <= 3) {
                            if (paramTypes[0] == float.class || paramTypes[0] == double.class) {
                                scaleMethod = m;
                                scaleParamCount = params;
                            }
                        }
                    }
                }
                
                hasPoseStack = pushMethod != null && popMethod != null;
                
                PayEveryone.LOGGER.info("RenderHelper: push=" + (pushMethod != null ? pushMethod.getName() : "null") +
                    ", pop=" + (popMethod != null ? popMethod.getName() : "null") +
                    ", translate=" + (translateMethod != null ? translateMethod.getName() + "(" + translateParamCount + ")" : "null") +
                    ", scale=" + (scaleMethod != null ? scaleMethod.getName() + "(" + scaleParamCount + ")" : "null"));
            }
        } catch (Throwable t) {
            hasPoseStack = false;
            PayEveryone.LOGGER.info("RenderHelper: pose() not available: " + t.getMessage());
        }
        
        initialized = true;
        PayEveryone.LOGGER.info("RenderHelper: Initialized, poseStack=" + hasPoseStack);
    }
    
    public static void drawString(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
        if (graphics == null || font == null || text == null) return;
        if (!initialized) init(graphics);
        
        try {
            graphics.drawString(font, text, x, y, color, shadow);
            return;
        } catch (Throwable t1) {}
        
        try {
            graphics.drawString(font, text, x, y, color);
            return;
        } catch (Throwable t2) {}
        
        try {
            graphics.drawString(font, Component.literal(text), x, y, color, shadow);
            return;
        } catch (Throwable t3) {}
        
        try {
            graphics.drawString(font, Component.literal(text), x, y, color);
            return;
        } catch (Throwable t4) {}
        
        if (!loggedOnce) {
            PayEveryone.LOGGER.warn("RenderHelper: All drawString methods failed for: " + text);
            loggedOnce = true;
        }
    }
    
    public static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (graphics == null) return;
        if (!initialized) init(graphics);
        
        try {
            graphics.fill(x1, y1, x2, y2, color);
        } catch (Throwable t) {}
    }
    
    public static void enableScissor(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        if (graphics == null) return;
        if (!initialized) init(graphics);
        
        try {
            graphics.enableScissor(x1, y1, x2, y2);
        } catch (Throwable t) {}
    }
    
    public static void disableScissor(GuiGraphics graphics) {
        if (graphics == null) return;
        if (!initialized) init(graphics);
        
        try {
            graphics.disableScissor();
        } catch (Throwable t) {}
    }
    
    public static void pushPose(GuiGraphics graphics) {
        if (graphics == null) return;
        if (!initialized) init(graphics);
        if (!hasPoseStack || pushMethod == null) return;
        
        try {
            Object poseStack = poseMethod.invoke(graphics);
            if (poseStack != null) {
                pushMethod.invoke(poseStack);
            }
        } catch (Throwable t) {}
    }
    
    public static void popPose(GuiGraphics graphics) {
        if (graphics == null) return;
        if (!initialized) init(graphics);
        if (!hasPoseStack || popMethod == null) return;
        
        try {
            Object poseStack = poseMethod.invoke(graphics);
            if (poseStack != null) {
                popMethod.invoke(poseStack);
            }
        } catch (Throwable t) {}
    }
    
    public static void translate(GuiGraphics graphics, double x, double y, double z) {
        if (graphics == null) return;
        if (!initialized) init(graphics);
        if (!hasPoseStack || translateMethod == null) return;
        
        try {
            Object poseStack = poseMethod.invoke(graphics);
            if (poseStack != null) {
                Class<?>[] paramTypes = translateMethod.getParameterTypes();
                if (translateParamCount == 3) {
                    if (paramTypes[0] == double.class) {
                        translateMethod.invoke(poseStack, x, y, z);
                    } else {
                        translateMethod.invoke(poseStack, (float)x, (float)y, (float)z);
                    }
                } else if (translateParamCount == 2) {
                    if (paramTypes[0] == double.class) {
                        translateMethod.invoke(poseStack, x, y);
                    } else {
                        translateMethod.invoke(poseStack, (float)x, (float)y);
                    }
                }
            }
        } catch (Throwable t) {}
    }
    
    public static void scale(GuiGraphics graphics, float x, float y, float z) {
        if (graphics == null) return;
        if (!initialized) init(graphics);
        if (!hasPoseStack || scaleMethod == null) return;
        
        try {
            Object poseStack = poseMethod.invoke(graphics);
            if (poseStack != null) {
                if (scaleParamCount == 3) {
                    scaleMethod.invoke(poseStack, x, y, z);
                } else if (scaleParamCount == 2) {
                    scaleMethod.invoke(poseStack, x, y);
                }
            }
        } catch (Throwable t) {}
    }
    
    public static boolean supportsMatrixScaling() {
        return hasPoseStack && scaleMethod != null && translateMethod != null;
    }
    
    public static boolean isModernVersion() {
        return false; // We can't reliably detect at runtime
    }
    
    public static void forceReinit() {
        initialized = false;
        loggedOnce = false;
    }
}
