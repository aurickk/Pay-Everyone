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
    private boolean inventoryMode = false;
    private boolean manuallyHidden = false;
    
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
        
        if (inventoryMode) return;
        
        if (window.isPinned()) {
            int scaledWidth = mc.getWindow().getGuiScaledWidth();
            int scaledHeight = mc.getWindow().getGuiScaledHeight();
            int windowWidth = mc.getWindow().getWidth();
            int windowHeight = mc.getWindow().getHeight();
            long handle = GLFW.glfwGetCurrentContext();
            
            // If Minecraft has the cursor grabbed (camera look), GLFW may report a stale/fake cursor position.
            // In that case, render with an "offscreen" mouse position so widgets don't think they're hovered.
            int cursorMode = GLFW.glfwGetInputMode(handle, GLFW.GLFW_CURSOR);
            final int mouseX;
            final int mouseY;
            if (cursorMode != GLFW.GLFW_CURSOR_NORMAL) {
                mouseX = Integer.MIN_VALUE / 2;
                mouseY = Integer.MIN_VALUE / 2;
            } else {
                double[] xpos = new double[1];
                double[] ypos = new double[1];
                GLFW.glfwGetCursorPos(handle, xpos, ypos);
                mouseX = (int)(xpos[0] * scaledWidth / windowWidth);
                mouseY = (int)(ypos[0] * scaledHeight / windowHeight);
            }
            
            RenderHelper.pushPose(graphics);
            RenderHelper.translate(graphics, 0, 0, 10000);
            window.render(graphics, mouseX, mouseY, tickDelta);
            RenderHelper.popPose(graphics);
        }
    }
    
    private boolean hasBeenPositioned = false;
    
    public void setInventoryMode(boolean active) {
        this.inventoryMode = active;
        if (active) {
            if (!manuallyHidden) {
                window.setVisible(true);
            }
        } else {
            if (!window.isPinned()) {
                window.setVisible(false);
            }
        }
    }
    
    public void setManuallyHidden(boolean hidden) {
        this.manuallyHidden = hidden;
        if (hidden) {
            window.setVisible(false);
        } else if (inventoryMode) {
            window.setVisible(true);
        }
    }
    
    public boolean isInventoryMode() {
        return inventoryMode;
    }
    
    public void positionForInventory(int screenWidth, int screenHeight) {
        if (hasBeenPositioned) return;
        int invWidth = 176;
        int invLeft = (screenWidth - invWidth) / 2;
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        int newX = Math.max(4, invLeft - windowWidth - 8);
        int newY = (screenHeight - windowHeight) / 2;
        window.setPosition(newX, newY);
        hasBeenPositioned = true;
    }
    
    public void renderInScreen(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (manuallyHidden) return;
        if (!inventoryMode && !window.isPinned()) return;
        RenderHelper.pushPose(graphics);
        RenderHelper.translate(graphics, 0, 0, 500);
        window.render(graphics, mouseX, mouseY, delta);
        RenderHelper.popPose(graphics);
    }
    
    public boolean handleInventoryClick(double mouseX, double mouseY, int button) {
        if (manuallyHidden) return false;
        if (!inventoryMode && !window.isPinned()) return false;
        if (window.isMouseOver(mouseX, mouseY)) {
            return window.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }
    
    public boolean handleInventoryRelease(double mouseX, double mouseY, int button) {
        if (manuallyHidden) return false;
        if (!inventoryMode && !window.isPinned()) return false;
        return window.mouseReleased(mouseX, mouseY, button);
    }
    
    public boolean handleInventoryDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (manuallyHidden) return false;
        if (!inventoryMode && !window.isPinned()) return false;
        return window.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    public boolean handleInventoryScroll(double mouseX, double mouseY, double amount) {
        if (manuallyHidden) return false;
        if (!inventoryMode && !window.isPinned()) return false;
        if (window.isMouseOver(mouseX, mouseY)) {
            return window.mouseScrolled(mouseX, mouseY, amount);
        }
        return false;
    }
    
    public boolean handleInventoryChar(char chr, int modifiers) {
        if (manuallyHidden) return false;
        if (!inventoryMode && !window.isPinned()) return false;
        if (window.hasFocusedTextField()) {
            return window.charTyped(chr, modifiers);
        }
        return false;
    }
    
    public boolean handleInventoryKey(int keyCode, int scanCode, int modifiers) {
        if (manuallyHidden) return false;
        if (!inventoryMode && !window.isPinned()) return false;
        if (window.hasFocusedTextField()) {
            return window.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
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
        return window.isVisible() && inventoryMode;
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
        try {
            KeyMapping.releaseAll();
        } catch (Exception ignored) {
            try {
                for (KeyMapping key : mc.options.keyMappings) {
                    key.setDown(false);
                }
            } catch (Exception e2) {}
        }
    }
}

