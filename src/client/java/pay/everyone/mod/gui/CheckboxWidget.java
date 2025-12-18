package pay.everyone.mod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pay.everyone.mod.compat.RenderHelper;

import java.util.function.Consumer;

public class CheckboxWidget extends Widget {
    private String label;
    private boolean checked;
    private Consumer<Boolean> onChange;
    private static final int BOX_SIZE = 10;
    
    public CheckboxWidget(int x, int y, int width, String label, boolean checked, Consumer<Boolean> onChange) {
        super(x, y, width, 12);
        this.label = label;
        this.checked = checked;
        this.onChange = onChange;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        updateHovered(mouseX, mouseY);
        
        int boxY = y + (height - BOX_SIZE) / 2;
        int bgColor = hovered && enabled ? Theme.BG_HOVER : Theme.BG_TERTIARY;
        int borderColor = (hovered && enabled) ? Theme.ACCENT : Theme.BORDER;
        RenderHelper.fill(graphics, x, boxY, x + BOX_SIZE, boxY + BOX_SIZE, bgColor);
        RenderHelper.fill(graphics, x, boxY, x + BOX_SIZE, boxY + 1, borderColor);
        RenderHelper.fill(graphics, x, boxY + BOX_SIZE - 1, x + BOX_SIZE, boxY + BOX_SIZE, borderColor);
        RenderHelper.fill(graphics, x, boxY, x + 1, boxY + BOX_SIZE, borderColor);
        RenderHelper.fill(graphics, x + BOX_SIZE - 1, boxY, x + BOX_SIZE, boxY + BOX_SIZE, borderColor);
        
        if (checked) {
            int checkColor = enabled ? Theme.ACCENT : Theme.TEXT_DISABLED;
            RenderHelper.fill(graphics, x + 2, boxY + 2, x + BOX_SIZE - 2, boxY + BOX_SIZE - 2, checkColor);
        }
        
        var font = Minecraft.getInstance().font;
        int textColor = enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_DISABLED;
        RenderHelper.drawString(graphics, font, label, x + BOX_SIZE + 4, y + (height - 8) / 2, textColor, false);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled || button != 0) return false;
        if (isMouseOver(mouseX, mouseY)) {
            checked = !checked;
            if (onChange != null) onChange.accept(checked);
            return true;
        }
        return false;
    }
    
    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { 
        this.checked = checked; 
    }
    public void setLabel(String label) { this.label = label; }
}
