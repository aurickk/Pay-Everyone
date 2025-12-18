package pay.everyone.mod.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pay.everyone.mod.PayEveryone;
import pay.everyone.mod.compat.RenderHelper;

import java.lang.reflect.Method;

public class KeybindWidget extends Widget {
    // Cached reflection for cross-version compatibility
    private static Method getKeyMethod = null;
    private static Method getKeyFromTypeMethod = null;
    private static boolean reflectionInitialized = false;
    // Fixed label width so all keybind boxes align and have the same width
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
        
        // Draw label on the left
        RenderHelper.drawString(graphics, font, label, x, y + (height - 8) / 2, Theme.TEXT_PRIMARY, false);
        
        // Calculate button area (right side) using fixed label width
        int btnX = x + LABEL_WIDTH;
        int btnWidth = width - LABEL_WIDTH;
        
        // Draw button background
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
        RenderHelper.fill(graphics, btnX, y, btnX + btnWidth, y + 1, Theme.BORDER);
        RenderHelper.fill(graphics, btnX, y + height - 1, btnX + btnWidth, y + height, Theme.BORDER);
        RenderHelper.fill(graphics, btnX, y, btnX + 1, y + height, Theme.BORDER);
        RenderHelper.fill(graphics, btnX + btnWidth - 1, y, btnX + btnWidth, y + height, Theme.BORDER);
        
        // Draw key name or "Press a key..."
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
        // Use the standard translated key message, which always reflects the current binding
        try {
            return keyMapping.getTranslatedKeyMessage().getString();
        } catch (Exception ignored) {}
        
        // Fallbacks for edge cases / older mappings
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
        
        // Check if clicked on the button area
        if (mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= y && mouseY < y + height) {
            if (listening) {
                // Clicking again while listening cancels
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
            // Cancel without changing
            stopListening();
            return true;
        }
        
        // Set the new key using reflection for cross-version compatibility
        InputConstants.Key key = getKeyCompat(keyCode, scanCode);
        if (key != null) {
            keyMapping.setKey(key);
            try {
                KeyMapping.resetMapping();
            } catch (Throwable t) {
                // Some versions might not have this method
                PayEveryone.LOGGER.debug("KeyMapping.resetMapping() not available");
            }
        }
        
        stopListening();
        return true;
    }
    
    /**
     * Get InputConstants.Key using reflection to handle API differences between versions.
     * In legacy versions: InputConstants.getKey(int keyCode, int scanCode)
     * In modern versions: InputConstants.Type.KEYSYM.getOrCreate(int keyCode)
     */
    private static InputConstants.Key getKeyCompat(int keyCode, int scanCode) {
        if (!reflectionInitialized) {
            initReflection();
        }
        
        // Try legacy method first: InputConstants.getKey(int, int)
        if (getKeyMethod != null) {
            try {
                return (InputConstants.Key) getKeyMethod.invoke(null, keyCode, scanCode);
            } catch (Throwable t) {
                PayEveryone.LOGGER.debug("getKey(int, int) failed: {}", t.getMessage());
            }
        }
        
        // Try modern method: InputConstants.Type.KEYSYM.getOrCreate(int)
        if (getKeyFromTypeMethod != null) {
            try {
                Object keysymType = InputConstants.Type.KEYSYM;
                return (InputConstants.Key) getKeyFromTypeMethod.invoke(keysymType, keyCode);
            } catch (Throwable t) {
                PayEveryone.LOGGER.debug("KEYSYM.getOrCreate(int) failed: {}", t.getMessage());
            }
        }
        
        // Final fallback: try direct UNKNOWN key
        PayEveryone.LOGGER.warn("Could not get key for keyCode={}, scanCode={}", keyCode, scanCode);
        return InputConstants.UNKNOWN;
    }
    
    private static void initReflection() {
        reflectionInitialized = true;
        
        // Try to find InputConstants.getKey(int, int) - legacy
        try {
            getKeyMethod = InputConstants.class.getMethod("getKey", int.class, int.class);
            PayEveryone.LOGGER.debug("Found InputConstants.getKey(int, int)");
        } catch (NoSuchMethodException e) {
            // Try obfuscated name
            for (Method m : InputConstants.class.getMethods()) {
                if (m.getParameterCount() == 2 && 
                    m.getParameterTypes()[0] == int.class && 
                    m.getParameterTypes()[1] == int.class &&
                    m.getReturnType() == InputConstants.Key.class) {
                    getKeyMethod = m;
                    PayEveryone.LOGGER.debug("Found InputConstants key method: {}", m.getName());
                    break;
                }
            }
        }
        
        // Try to find InputConstants.Type.getOrCreate(int) - modern
        try {
            getKeyFromTypeMethod = InputConstants.Type.class.getMethod("getOrCreate", int.class);
            PayEveryone.LOGGER.debug("Found InputConstants.Type.getOrCreate(int)");
        } catch (NoSuchMethodException e) {
            // Try to find any method on Type that returns Key and takes int
            for (Method m : InputConstants.Type.class.getMethods()) {
                if (m.getParameterCount() == 1 && 
                    m.getParameterTypes()[0] == int.class &&
                    m.getReturnType() == InputConstants.Key.class) {
                    getKeyFromTypeMethod = m;
                    PayEveryone.LOGGER.debug("Found InputConstants.Type key method: {}", m.getName());
                    break;
                }
            }
        }
    }
    
    private void startListening() {
        // Stop any other widget that was listening
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

