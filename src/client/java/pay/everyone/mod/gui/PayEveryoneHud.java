package pay.everyone.mod.gui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import pay.everyone.mod.compat.RenderHelper;

public class PayEveryoneHud {
    private static PayEveryoneHud instance;
    private final PayEveryoneWindow window;
    private boolean inputCaptured = false;
    private float savedYRot = 0, savedXRot = 0;
    private double savedMouseX = 0, savedMouseY = 0;
    private boolean needsCameraRestore = false;
    
    private PayEveryoneHud() {
        window = new PayEveryoneWindow();
    }
    
    public static PayEveryoneHud getInstance() {
        if (instance == null) {
            instance = new PayEveryoneHud();
        }
        return instance;
    }
    
    public static void reset() {
        if (instance != null) {
            instance.window.setVisible(false);
            instance.window.setPinned(false);
        }
        instance = null;
    }
    
    public void render(GuiGraphics graphics, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Render when visible or pinned - push z position to render above other HUD elements
        if (window.isVisible() || window.isPinned()) {
            int scaledWidth = mc.getWindow().getGuiScaledWidth();
            int scaledHeight = mc.getWindow().getGuiScaledHeight();
            int windowWidth = mc.getWindow().getWidth();
            int windowHeight = mc.getWindow().getHeight();
            
            // Get mouse position directly from GLFW for accurate tracking
            // mc.mouseHandler can return stale values when cursor mode changes
            double[] xpos = new double[1];
            double[] ypos = new double[1];
            GLFW.glfwGetCursorPos(GLFW.glfwGetCurrentContext(), xpos, ypos);
            int mouseX = (int)(xpos[0] * scaledWidth / windowWidth);
            int mouseY = (int)(ypos[0] * scaledHeight / windowHeight);
            
            // Push to extremely high z-level to render above ALL HUD elements including chat, items, scoreboard
            RenderHelper.pushPose(graphics);
            RenderHelper.translate(graphics, 0, 0, 10000);
            window.render(graphics, mouseX, mouseY, tickDelta);
            RenderHelper.popPose(graphics);
        }
        
        // Handle cursor mode for input capture
        boolean shouldCapture = shouldCaptureInput();
        if (shouldCapture != inputCaptured) {
            long windowHandle = GLFW.glfwGetCurrentContext();
            
            if (shouldCapture && !inputCaptured) {
                // Entering GUI mode - save player camera angles, mouse position, and enable cursor
                if (mc.player != null) {
                    savedYRot = mc.player.getYRot();
                    savedXRot = mc.player.getXRot();
                }
                savedMouseX = mc.mouseHandler.xpos();
                savedMouseY = mc.mouseHandler.ypos();
                GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
                needsCameraRestore = true;
                
                // Release all held keys to stop movement when GUI opens
                releaseAllKeys(mc);
            } else if (!shouldCapture && inputCaptured) {
                // Exiting GUI mode - restore camera angles and mouse position to prevent camera snap
                if (needsCameraRestore && mc.screen == null && mc.player != null) {
                    mc.player.setYRot(savedYRot);
                    mc.player.setXRot(savedXRot);
                }
                // Restore mouse position to prevent camera snap
                GLFW.glfwSetCursorPos(windowHandle, savedMouseX, savedMouseY);
                GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                needsCameraRestore = false;
            }
            
            inputCaptured = shouldCapture;
        }
    }
    
    public void tick() {
        window.tick();
    }
    
    public void toggle() {
        if (window.isVisible()) {
            // Currently focused - close/unfocus it
            window.setVisible(false);
        } else {
            // Not visible (pinned or not) - open/refocus it, keep pin state
            window.setVisible(true);
        }
    }
    
    public boolean isVisible() {
        return window.isVisible() || window.isPinned();
    }
    
    public boolean shouldCaptureInput() {
        // Capture input when visible (focused), regardless of pin state
        return window.isVisible();
    }
    
    public boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        return window.mouseClicked(mouseX, mouseY, button);
    }
    
    public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
        return window.mouseReleased(mouseX, mouseY, button);
    }
    
    public boolean handleMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return window.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    public boolean handleMouseScrolled(double mouseX, double mouseY, double amount) {
        return window.mouseScrolled(mouseX, mouseY, amount);
    }
    
    public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC closes the window, unless a TabScan is running
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && window.isVisible()) {
            if (!pay.everyone.mod.PayManager.getInstance().isTabScanning()) {
                window.setVisible(false);
            }
            return true;
        }
        return window.keyPressed(keyCode, scanCode, modifiers);
    }
    
    public boolean handleCharTyped(char chr, int modifiers) {
        return window.charTyped(chr, modifiers);
    }
    
    public PayEveryoneWindow getWindow() {
        return window;
    }
    
    private void releaseAllKeys(Minecraft mc) {
        // Release all movement and action keys to prevent stuck movement when opening GUI
        try {
            KeyMapping.releaseAll();
        } catch (Exception ignored) {
            // Fallback: manually release common movement keys
            try {
                for (KeyMapping key : mc.options.keyMappings) {
                    key.setDown(false);
                }
            } catch (Exception e2) {
                // If that also fails, just ignore - the key release is a nice-to-have
            }
        }
    }
}

