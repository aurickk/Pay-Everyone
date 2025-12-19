package pay.everyone.mod;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import pay.everyone.mod.gui.PayEveryoneHud;

public class InputHandler {
    private static boolean initialized = false;
    private static long windowHandle = 0;
    
    private static GLFWMouseButtonCallback originalMouseButton;
    private static GLFWScrollCallback originalScroll;
    private static GLFWKeyCallback originalKey;
    private static GLFWCharCallback originalChar;
    private static GLFWCursorPosCallback originalCursorPos;
    
    private static GLFWMouseButtonCallback ourMouseButton;
    private static GLFWScrollCallback ourScroll;
    private static GLFWKeyCallback ourKey;
    private static GLFWCharCallback ourChar;
    private static GLFWCursorPosCallback ourCursorPos;
    
    private static double lastMouseX = 0;
    private static double lastMouseY = 0;
    private static int lastButton = -1;
    
    public static void init() {
        if (initialized) return;
        
        try {
            windowHandle = GLFW.glfwGetCurrentContext();
            if (windowHandle == 0) {
                PayEveryone.LOGGER.warn("[InputHandler] No GLFW context available yet");
                return;
            }
            
            originalMouseButton = GLFW.glfwSetMouseButtonCallback(windowHandle, null);
            originalScroll = GLFW.glfwSetScrollCallback(windowHandle, null);
            originalKey = GLFW.glfwSetKeyCallback(windowHandle, null);
            originalChar = GLFW.glfwSetCharCallback(windowHandle, null);
            originalCursorPos = GLFW.glfwSetCursorPosCallback(windowHandle, null);
            
            ourMouseButton = GLFWMouseButtonCallback.create((window, button, action, mods) -> {
                boolean handled = handleMouseButton(button, action, mods);
                if (!handled && originalMouseButton != null) {
                    originalMouseButton.invoke(window, button, action, mods);
                }
            });
            
            ourScroll = GLFWScrollCallback.create((window, xOffset, yOffset) -> {
                boolean handled = handleScroll(xOffset, yOffset);
                if (!handled && originalScroll != null) {
                    originalScroll.invoke(window, xOffset, yOffset);
                }
            });
            
            ourKey = GLFWKeyCallback.create((window, key, scancode, action, mods) -> {
                boolean handled = handleKey(key, scancode, action, mods);
                if (!handled && originalKey != null) {
                    originalKey.invoke(window, key, scancode, action, mods);
                }
            });
            
            ourChar = GLFWCharCallback.create((window, codepoint) -> {
                boolean handled = handleChar(codepoint);
                if (!handled && originalChar != null) {
                    originalChar.invoke(window, codepoint);
                }
            });
            
            ourCursorPos = GLFWCursorPosCallback.create((window, xpos, ypos) -> {
                boolean handled = handleCursorPos(xpos, ypos);
                if (!handled && originalCursorPos != null) {
                    originalCursorPos.invoke(window, xpos, ypos);
                }
            });
            
            GLFW.glfwSetMouseButtonCallback(windowHandle, ourMouseButton);
            GLFW.glfwSetScrollCallback(windowHandle, ourScroll);
            GLFW.glfwSetKeyCallback(windowHandle, ourKey);
            GLFW.glfwSetCharCallback(windowHandle, ourChar);
            GLFW.glfwSetCursorPosCallback(windowHandle, ourCursorPos);
            
            initialized = true;
            PayEveryone.LOGGER.info("[InputHandler] GLFW callbacks installed successfully");
            
        } catch (Throwable t) {
            PayEveryone.LOGGER.error("[InputHandler] Failed to install GLFW callbacks", t);
        }
    }
    
    private static double[] getScaledMousePos(double rawX, double rawY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return new double[]{rawX, rawY};
        
        double scaledX = rawX * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getWidth();
        double scaledY = rawY * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getHeight();
        return new double[]{scaledX, scaledY};
    }
    
    private static boolean handleMouseButton(int button, int action, int mods) {
        Minecraft mc = Minecraft.getInstance();
        PayEveryoneHud hud = PayEveryoneHud.getInstance();
        double[] scaled = getScaledMousePos(lastMouseX, lastMouseY);

        if (mc.screen != null && hud.isInventoryMode()) {
            if (action == GLFW.GLFW_PRESS) {
                lastButton = button;
                return hud.handleInventoryClick(scaled[0], scaled[1], button);
            } else if (action == GLFW.GLFW_RELEASE) {
                boolean handled = hud.handleInventoryRelease(scaled[0], scaled[1], button);
                lastButton = -1;
                return handled;
            }
            return false;
        }

        if (mc.screen != null) return false;
        if (!hud.shouldCaptureInput()) return false;
        
        if (action == GLFW.GLFW_PRESS) {
            lastButton = button;
            hud.handleMouseClicked(scaled[0], scaled[1], button);
        } else if (action == GLFW.GLFW_RELEASE) {
            hud.handleMouseReleased(scaled[0], scaled[1], button);
            lastButton = -1;
        }
        
        return true;
    }
    
    private static boolean handleScroll(double xOffset, double yOffset) {
        Minecraft mc = Minecraft.getInstance();
        PayEveryoneHud hud = PayEveryoneHud.getInstance();
        double[] scaled = getScaledMousePos(lastMouseX, lastMouseY);

        if (mc.screen != null && hud.isInventoryMode()) {
            return hud.handleInventoryScroll(scaled[0], scaled[1], yOffset);
        }

        if (mc.screen != null) return false;
        if (!hud.shouldCaptureInput()) return false;

        hud.handleMouseScrolled(scaled[0], scaled[1], yOffset);
        return true;
    }
    
    private static boolean handleKey(int key, int scancode, int action, int mods) {
        Minecraft mc = Minecraft.getInstance();
        
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            try {
                if (PayEveryoneClient.getCancelPaymentKey() != null) {
                    boolean matched = matchesKey(PayEveryoneClient.getCancelPaymentKey(), key, scancode);
                    if (!matched && scancode != 0) matched = matchesKey(PayEveryoneClient.getCancelPaymentKey(), key, 0);
                    if (matched) {
                        PayManager pm = PayManager.getInstance();
                        if (pm.isPaying() || pm.isTabScanning()) {
                            pm.stopPaying();
                            pm.stopTabScan();
                            pm.clearTabScanList();
                            if (mc.player != null) {
                                mc.player.displayClientMessage(
                                    net.minecraft.network.chat.Component.literal("§e[Pay Everyone] Payment/Scan cancelled via keybind"), false);
                            }
                            return true;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        PayEveryoneHud hud = PayEveryoneHud.getInstance();

        if (mc.screen != null && hud.isInventoryMode()) {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                return hud.handleInventoryKey(key, scancode, mods);
            }
            return false;
        }

        if (mc.screen != null) return false;
        if (!hud.shouldCaptureInput()) return false;
        
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            hud.handleKeyPressed(key, scancode, mods);
        }
        
        return true;
    }
    
    private static boolean matchesKey(net.minecraft.client.KeyMapping keyMapping, int key, int scancode) {
        try {
            java.lang.reflect.Method matchesMethod = keyMapping.getClass().getMethod("matches", int.class, int.class);
            return (Boolean) matchesMethod.invoke(keyMapping, key, scancode);
        } catch (Throwable t1) {
            try {
                java.lang.reflect.Method getKeyMethod = keyMapping.getClass().getMethod("getKey");
                Object boundKey = getKeyMethod.invoke(keyMapping);
                if (boundKey != null) {
                    java.lang.reflect.Method getValueMethod = boundKey.getClass().getMethod("getValue");
                    int keyValue = (Integer) getValueMethod.invoke(boundKey);
                    return keyValue == key;
                }
            } catch (Throwable t2) {
                try {
                    java.lang.reflect.Field keyField = keyMapping.getClass().getDeclaredField("key");
                    keyField.setAccessible(true);
                    Object boundKey = keyField.get(keyMapping);
                    if (boundKey != null) {
                        java.lang.reflect.Method getValueMethod = boundKey.getClass().getMethod("getValue");
                        int keyValue = (Integer) getValueMethod.invoke(boundKey);
                        return keyValue == key;
                    }
                } catch (Throwable t3) {}
            }
            return false;
        }
    }
    
    private static boolean handleChar(int codepoint) {
        Minecraft mc = Minecraft.getInstance();
        PayEveryoneHud hud = PayEveryoneHud.getInstance();

        if (mc.screen != null && hud.isInventoryMode()) {
            return hud.handleInventoryChar((char) codepoint, 0);
        }

        if (mc.screen != null) return false;
        if (!hud.shouldCaptureInput()) return false;

        hud.handleCharTyped((char) codepoint, 0);
        return true;
    }
    
    private static boolean handleCursorPos(double xpos, double ypos) {
        double prevX = lastMouseX;
        double prevY = lastMouseY;
        lastMouseX = xpos;
        lastMouseY = ypos;
        
        Minecraft mc = Minecraft.getInstance();
        PayEveryoneHud hud = PayEveryoneHud.getInstance();

        if (mc.screen != null && hud.isInventoryMode()) {
            if (lastButton >= 0) {
                double[] scaled = getScaledMousePos(xpos, ypos);
                double[] prevScaled = getScaledMousePos(prevX, prevY);
                return hud.handleInventoryDrag(
                    scaled[0], scaled[1], lastButton,
                    scaled[0] - prevScaled[0], scaled[1] - prevScaled[1]
                );
            }
            return false;
        }

        if (mc.screen != null) return false;
        if (!hud.shouldCaptureInput()) return false;

        if (lastButton >= 0) {
            double[] scaled = getScaledMousePos(xpos, ypos);
            double[] prevScaled = getScaledMousePos(prevX, prevY);
            hud.handleMouseDragged(scaled[0], scaled[1], lastButton, 
                scaled[0] - prevScaled[0], scaled[1] - prevScaled[1]);
        }
        
        return true;
    }
    
    public static void cleanup() {
        if (!initialized || windowHandle == 0) return;
        
        try {
            if (originalMouseButton != null) {
                GLFW.glfwSetMouseButtonCallback(windowHandle, originalMouseButton);
            }
            if (originalScroll != null) {
                GLFW.glfwSetScrollCallback(windowHandle, originalScroll);
            }
            if (originalKey != null) {
                GLFW.glfwSetKeyCallback(windowHandle, originalKey);
            }
            if (originalChar != null) {
                GLFW.glfwSetCharCallback(windowHandle, originalChar);
            }
            if (originalCursorPos != null) {
                GLFW.glfwSetCursorPosCallback(windowHandle, originalCursorPos);
            }
            
            if (ourMouseButton != null) ourMouseButton.free();
            if (ourScroll != null) ourScroll.free();
            if (ourKey != null) ourKey.free();
            if (ourChar != null) ourChar.free();
            if (ourCursorPos != null) ourCursorPos.free();
            
            initialized = false;
            PayEveryone.LOGGER.info("[InputHandler] GLFW callbacks cleaned up");
            
        } catch (Throwable t) {
            PayEveryone.LOGGER.error("[InputHandler] Failed to cleanup GLFW callbacks", t);
        }
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
