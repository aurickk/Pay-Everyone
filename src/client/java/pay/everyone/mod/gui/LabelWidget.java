package pay.everyone.mod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pay.everyone.mod.compat.RenderHelper;

public class LabelWidget extends Widget {
    private String text;
    private int color = Theme.TEXT_PRIMARY;
    private boolean centered = false;
    
    public LabelWidget(int x, int y, int width, String text) {
        super(x, y, width, 10);
        this.text = text;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible || text == null || text.isEmpty()) return;
        
        var font = Minecraft.getInstance().font;
        int textX = centered ? x + (width - font.width(text)) / 2 : x;
        RenderHelper.drawString(graphics, font, text, textX, y, color, false);
    }
    
    public void setText(String text) { this.text = text; }
    public String getText() { return text; }
    public void setColor(int color) { this.color = color; }
    public void setCentered(boolean centered) { this.centered = centered; }
}
