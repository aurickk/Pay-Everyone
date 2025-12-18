package pay.everyone.mod.gui;

import net.minecraft.client.gui.GuiGraphics;

public abstract class Widget {
    protected int x, y, width, height;
    protected boolean visible = true;
    protected boolean enabled = true;
    protected boolean focused = false;
    protected boolean hovered = false;
    
    // Screen-space transform for scissor calculations
    protected int screenOffsetX = 0;
    protected int screenOffsetY = 0;
    protected float screenScale = 1.0f;
    protected boolean useMatrixScaling = false;
    
    public Widget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float delta);
    
    public void setScreenTransform(int offsetX, int offsetY, float scale, boolean useMatrix) {
        this.screenOffsetX = offsetX;
        this.screenOffsetY = offsetY;
        this.screenScale = scale;
        this.useMatrixScaling = useMatrix;
    }
    
    public void setScreenTransform(int offsetX, int offsetY, float scale) {
        setScreenTransform(offsetX, offsetY, scale, true);
    }
    
    protected int toScreenX(int localX) {
        if (useMatrixScaling) {
            return screenOffsetX + (int)(localX * screenScale);
        } else {
            return localX;
        }
    }
    
    protected int toScreenY(int localY) {
        if (useMatrixScaling) {
            return screenOffsetY + (int)(localY * screenScale);
        } else {
            return localY;
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }
    
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }
    
    public boolean charTyped(char chr, int modifiers) {
        return false;
    }
    
    public void tick() {}
    
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }
    public boolean isHovered() { return hovered; }
    
    protected void updateHovered(int mouseX, int mouseY) {
        this.hovered = isMouseOver(mouseX, mouseY);
    }
}
