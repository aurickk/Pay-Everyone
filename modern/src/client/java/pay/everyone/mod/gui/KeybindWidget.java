package pay.everyone.mod.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pay.everyone.mod.compat.RenderHelper;

public class KeybindWidget extends Widget {
    private static final int LABEL_WIDTH = 80;

    private final String label;
    private final KeyMapping keyMapping;
    private boolean listening = false;
    private static KeybindWidget currentlyListening = null;
    
    public KeybindWidget(int x, int y, int width, int height, String label, KeyMapping keyMapping) {
        super(x, y, width, height);
        this.label = label;
        this.keyMapping = keyMapping;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        updateHovered(mouseX, mouseY);
        
        var font = Minecraft.getInstance().font;
        
        RenderHelper.drawString(graphics, font, label, x, y + (height - 8) / 2, Theme.TEXT_PRIMARY, false);
        
        int btnX = x + LABEL_WIDTH;
        int btnWidth = width - LABEL_WIDTH;
        
        boolean btnHovered = mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= y && mouseY < y + height;
        int bgColor;
        if (listening) {
            bgColor = Theme.ACCENT;
        } else if (btnHovered) {
            bgColor = Theme.BG_HOVER;
        } else {
            bgColor = Theme.BG_TERTIARY;
        }
        
        RenderHelper.fill(graphics, btnX, y, btnX + btnWidth, y + height, bgColor);
        RenderHelper.drawBorder(graphics, btnX, y, btnX + btnWidth, y + height, Theme.BORDER);
        
        String keyText;
        int textColor;
        if (listening) {
            keyText = "> Press key <";
            textColor = Theme.BG_PRIMARY;
        } else {
            keyText = getKeyName();
            textColor = Theme.TEXT_PRIMARY;
        }
        
        int textWidth = font.width(keyText);
        int textX = btnX + (btnWidth - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        RenderHelper.drawString(graphics, font, keyText, textX, textY, textColor, false);
    }
    
    private String getKeyName() {
        try {
            return keyMapping.getTranslatedKeyMessage().getString();
        } catch (Exception ignored) {}
        
        try {
            return keyMapping.getDefaultKey().getDisplayName().getString();
        } catch (Exception ignored) {}
        
        return "???";
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        
        int btnX = x + LABEL_WIDTH;
        int btnWidth = width - LABEL_WIDTH;
        
        if (mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= y && mouseY < y + height) {
            if (listening) {
                stopListening();
            } else {
                startListening();
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening) return false;
        
        if (keyCode == InputConstants.KEY_ESCAPE) {
            stopListening();
            return true;
        }
        
        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        keyMapping.setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        
        stopListening();
        return true;
    }
    
    private void startListening() {
        if (currentlyListening != null && currentlyListening != this) {
            currentlyListening.stopListening();
        }
        listening = true;
        currentlyListening = this;
    }
    
    private void stopListening() {
        listening = false;
        if (currentlyListening == this) {
            currentlyListening = null;
        }
    }
    
    public boolean isListening() {
        return listening;
    }
    
    public static boolean isAnyListening() {
        return currentlyListening != null;
    }
}

