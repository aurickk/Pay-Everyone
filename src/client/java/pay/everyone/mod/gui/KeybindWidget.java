package pay.everyone.mod.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
<<<<<<< HEAD
import pay.everyone.mod.PayEveryone;
import pay.everyone.mod.compat.RenderHelper;

import java.lang.reflect.Method;

public class KeybindWidget extends Widget {
    private static Method getKeyMethod = null;
    private static Method getKeyFromTypeMethod = null;
    private static boolean reflectionInitialized = false;
=======
import pay.everyone.mod.compat.RenderHelper;

public class KeybindWidget extends Widget {
>>>>>>> 230b532 (feat: migrate to stonecutter)
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
        
<<<<<<< HEAD
        InputConstants.Key key = getKeyCompat(keyCode, scanCode);
        if (key != null && key != InputConstants.UNKNOWN) {
            keyMapping.setKey(key);
            try {
                KeyMapping.resetMapping();
            } catch (Throwable t) {
                PayEveryone.LOGGER.debug("KeyMapping.resetMapping() not available");
            }
            try {
                Minecraft.getInstance().options.save();
            } catch (Throwable t) {
                PayEveryone.LOGGER.debug("Failed to save options: {}", t.getMessage());
            }
        }
=======
        //? if >=1.21.6 {
        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        keyMapping.setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        //? } else {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (key != null && key != InputConstants.UNKNOWN) {
            keyMapping.setKey(key);
            try { KeyMapping.resetMapping(); } catch (Throwable t) {}
            try { Minecraft.getInstance().options.save(); } catch (Throwable t) {}
        }
        //?}
>>>>>>> 230b532 (feat: migrate to stonecutter)
        
        stopListening();
        return true;
    }
    
<<<<<<< HEAD
    private static InputConstants.Key getKeyCompat(int keyCode, int scanCode) {
        if (!reflectionInitialized) {
            initReflection();
        }
        
        if (getKeyMethod != null) {
            try {
                return (InputConstants.Key) getKeyMethod.invoke(null, keyCode, scanCode);
            } catch (Throwable t) {
                PayEveryone.LOGGER.debug("getKey(int, int) failed: {}", t.getMessage());
            }
        }
        
        if (getKeyFromTypeMethod != null) {
            try {
                Object keysymType = InputConstants.Type.KEYSYM;
                return (InputConstants.Key) getKeyFromTypeMethod.invoke(keysymType, keyCode);
            } catch (Throwable t) {
                PayEveryone.LOGGER.debug("KEYSYM.getOrCreate(int) failed: {}", t.getMessage());
            }
        }
        
        try {
            return InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        } catch (Throwable t) {
            PayEveryone.LOGGER.debug("Direct KEYSYM.getOrCreate failed: {}", t.getMessage());
        }
        
        PayEveryone.LOGGER.warn("Could not get key for keyCode={}, scanCode={}", keyCode, scanCode);
        return InputConstants.UNKNOWN;
    }
    
    private static void initReflection() {
        reflectionInitialized = true;
        
        try {
            getKeyMethod = InputConstants.class.getMethod("getKey", int.class, int.class);
            PayEveryone.LOGGER.debug("Found InputConstants.getKey(int, int)");
        } catch (NoSuchMethodException e) {
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
        
        try {
            getKeyFromTypeMethod = InputConstants.Type.class.getMethod("getOrCreate", int.class);
            PayEveryone.LOGGER.debug("Found InputConstants.Type.getOrCreate(int)");
        } catch (NoSuchMethodException e) {
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
    
=======
>>>>>>> 230b532 (feat: migrate to stonecutter)
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
<<<<<<< HEAD

=======
>>>>>>> 230b532 (feat: migrate to stonecutter)
