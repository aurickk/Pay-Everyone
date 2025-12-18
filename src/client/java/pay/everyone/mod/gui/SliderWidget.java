package pay.everyone.mod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pay.everyone.mod.compat.RenderHelper;

import java.util.function.Consumer;

public class SliderWidget extends Widget {
    private String label;
    private long value;
    private long minValue;
    private long maxValue;
    private long step;
    private Consumer<Long> onChange;
    private boolean dragging = false;
    private String suffix = "";
    
    public SliderWidget(int x, int y, int width, String label, long value, long min, long max, long step, Consumer<Long> onChange) {
        super(x, y, width, 24);
        this.label = label;
        this.value = value;
        this.minValue = min;
        this.maxValue = max;
        this.step = step;
        this.onChange = onChange;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        updateHovered(mouseX, mouseY);
        
        var font = Minecraft.getInstance().font;
        int textColor = enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_DISABLED;
        
        String displayValue = value + suffix;
        String labelText = label + ": " + displayValue;
        RenderHelper.drawString(graphics, font, labelText, x, y, textColor, false);
        
        int sliderY = y + 12;
        int sliderHeight = 8;
        int trackY = sliderY + (sliderHeight - 4) / 2;
        
        RenderHelper.fill(graphics, x, trackY, x + width, trackY + 4, Theme.BG_TERTIARY);
        RenderHelper.fill(graphics, x, trackY, x + width, trackY + 1, Theme.BORDER);
        RenderHelper.fill(graphics, x, trackY + 3, x + width, trackY + 4, Theme.BORDER);
        
        float percent = (float)(value - minValue) / (float)(maxValue - minValue);
        int fillWidth = (int)(width * percent);
        RenderHelper.fill(graphics, x, trackY, x + fillWidth, trackY + 4, Theme.ACCENT);
        
        // Only the handle shows hover effect
        int handleX = x + fillWidth - 3;
        boolean handleHovered = hovered && mouseX >= handleX && mouseX < handleX + 6 && mouseY >= sliderY && mouseY < sliderY + sliderHeight;
        int handleColor = (dragging || handleHovered) ? Theme.ACCENT_HOVER : Theme.ACCENT;
        RenderHelper.fill(graphics, handleX, sliderY, handleX + 6, sliderY + sliderHeight, handleColor);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled || button != 0) return false;
        int sliderY = y + 12;
        if (mouseX >= x && mouseX < x + width && mouseY >= sliderY && mouseY < sliderY + 8) {
            dragging = true;
            updateValue(mouseX);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            updateValue(mouseX);
            return true;
        }
        return false;
    }
    
    private void updateValue(double mouseX) {
        float percent = (float)(mouseX - x) / (float)width;
        percent = Math.max(0, Math.min(1, percent));
        long newValue = minValue + (long)((maxValue - minValue) * percent);
        newValue = (newValue / step) * step;
        if (newValue != value) {
            value = newValue;
            if (onChange != null) onChange.accept(value);
        }
    }
    
    public long getValue() { return value; }
    public void setValue(long value) { this.value = Math.max(minValue, Math.min(maxValue, value)); }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public void setLabel(String label) { this.label = label; }
}
