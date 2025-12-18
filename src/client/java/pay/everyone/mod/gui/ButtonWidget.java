package pay.everyone.mod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pay.everyone.mod.compat.RenderHelper;

public class ButtonWidget extends Widget {
    private String text;
    private Runnable onClick;
    private boolean pressed = false;
    
    public ButtonWidget(int x, int y, int width, int height, String text, Runnable onClick) {
        super(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        updateHovered(mouseX, mouseY);
        
        int bgColor;
        if (!enabled) {
            bgColor = Theme.BG_SECONDARY;
        } else if (pressed && hovered) {
            bgColor = Theme.BG_PRESSED;
        } else if (hovered) {
            bgColor = Theme.BG_HOVER;
        } else {
            bgColor = Theme.BG_TERTIARY;
        }
        
        RenderHelper.fill(graphics, x, y, x + width, y + height, bgColor);
        int borderColor = (hovered && enabled) ? Theme.ACCENT : Theme.BORDER;
        RenderHelper.fill(graphics, x, y, x + width, y + 1, borderColor);
        RenderHelper.fill(graphics, x, y + height - 1, x + width, y + height, borderColor);
        RenderHelper.fill(graphics, x, y, x + 1, y + height, borderColor);
        RenderHelper.fill(graphics, x + width - 1, y, x + width, y + height, borderColor);
        
        int textColor = enabled ? (hovered ? Theme.ACCENT : Theme.TEXT_PRIMARY) : Theme.TEXT_DISABLED;
        var font = Minecraft.getInstance().font;
        int textWidth = font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        RenderHelper.drawString(graphics, font, text, textX, textY, textColor, false);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled || button != 0) return false;
        if (isMouseOver(mouseX, mouseY)) {
            pressed = true;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (pressed && button == 0) {
            pressed = false;
            if (isMouseOver(mouseX, mouseY) && onClick != null) {
                onClick.run();
                return true;
            }
        }
        return false;
    }
    
    public void setText(String text) { this.text = text; }
    public String getText() { return text; }
    public void setOnClick(Runnable onClick) { this.onClick = onClick; }
}
