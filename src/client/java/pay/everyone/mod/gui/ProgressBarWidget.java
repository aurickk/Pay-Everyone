package pay.everyone.mod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pay.everyone.mod.compat.RenderHelper;

public class ProgressBarWidget extends Widget {
    private String label;
    private float progress = 0;
    private String statusText = "";
    
    public ProgressBarWidget(int x, int y, int width, String label) {
        super(x, y, width, 20);
        this.label = label;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        
        var font = Minecraft.getInstance().font;
        
        String labelWithProgress = label + ": " + (int)(progress * 100) + "%";
        RenderHelper.drawString(graphics, font, labelWithProgress, x, y, Theme.TEXT_PRIMARY, false);
        
        int barY = y + 10;
        int barHeight = 6;
        
        RenderHelper.fill(graphics, x, barY, x + width, barY + barHeight, Theme.PROGRESS_BG);
        RenderHelper.fill(graphics, x, barY, x + width, barY + 1, Theme.BORDER);
        RenderHelper.fill(graphics, x, barY + barHeight - 1, x + width, barY + barHeight, Theme.BORDER);
        
        int fillWidth = (int)(width * progress);
        if (fillWidth > 0) {
            RenderHelper.fill(graphics, x, barY, x + fillWidth, barY + barHeight, Theme.PROGRESS_FILL);
        }
        
        if (!statusText.isEmpty()) {
            int textWidth = font.width(statusText);
            int textX = x + (width - textWidth) / 2;
            RenderHelper.drawString(graphics, font, statusText, textX, barY + barHeight + 2, Theme.TEXT_SECONDARY, false);
        }
    }
    
    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(1, progress));
    }
    
    public float getProgress() { return progress; }
    
    public void setStatusText(String text) {
        this.statusText = text != null ? text : "";
    }
    
    public void setLabel(String label) { this.label = label; }
}
