package pay.everyone.mod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import pay.everyone.mod.compat.RenderHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextFieldWidget extends Widget {
    private String text = "";
    private String hint = "";
    private int cursorPos = 0;
    private int scrollOffset = 0;
    private int cursorBlinkTimer = 0;
    private Consumer<String> onChange;
    private Supplier<List<String>> autocompleteProvider;
    private List<String> suggestions = new ArrayList<>();
    private int selectedSuggestion = -1;
    private boolean showSuggestions = false;
    private int maxLength = 256;
    
    public TextFieldWidget(int x, int y, int width, int height, String hint) {
        super(x, y, width, height);
        this.hint = hint;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        updateHovered(mouseX, mouseY);
        
        int bgColor = enabled ? (hovered ? Theme.BG_HOVER : Theme.BG_TERTIARY) : Theme.BG_SECONDARY;
        int borderColor = focused ? Theme.BORDER_FOCUS : (hovered ? Theme.ACCENT : Theme.BORDER);
        
        RenderHelper.fill(graphics, x, y, x + width, y + height, bgColor);
        RenderHelper.fill(graphics, x, y, x + width, y + 1, borderColor);
        RenderHelper.fill(graphics, x, y + height - 1, x + width, y + height, borderColor);
        RenderHelper.fill(graphics, x, y, x + 1, y + height, borderColor);
        RenderHelper.fill(graphics, x + width - 1, y, x + width, y + height, borderColor);
        
        var font = Minecraft.getInstance().font;
        int textY = y + (height - 8) / 2;
        int textX = x + 4;
        
        String displayText = text.isEmpty() ? hint : text;
        int textColor = text.isEmpty() ? Theme.TEXT_HINT : (enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_DISABLED);
        
        String visibleText = getVisibleText(font, displayText);
        RenderHelper.drawString(graphics, font, visibleText, textX, textY, textColor, false);
        
        if (focused && enabled && (cursorBlinkTimer / 10) % 2 == 0) {
            String beforeCursor = text.substring(Math.min(scrollOffset, text.length()), Math.min(cursorPos, text.length()));
            int cursorX = textX + font.width(beforeCursor);
            RenderHelper.fill(graphics, cursorX, textY - 1, cursorX + 1, textY + 9, Theme.TEXT_PRIMARY);
        }
    }
    
    private String getVisibleText(net.minecraft.client.gui.Font font, String fullText) {
        int maxWidth = width - 8;
        if (scrollOffset >= fullText.length()) return "";
        String sub = fullText.substring(scrollOffset);
        while (font.width(sub) > maxWidth && sub.length() > 0) {
            sub = sub.substring(0, sub.length() - 1);
        }
        return sub;
    }
    
    private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int sugY = y + height;
        int maxVisible = Math.min(suggestions.size(), 5);
        int sugHeight = maxVisible * 12 + 4;
        int maxTextWidth = width - 8;
        
        RenderHelper.fill(graphics, x, sugY, x + width, sugY + sugHeight, Theme.BG_PRIMARY);
        RenderHelper.fill(graphics, x, sugY, x + width, sugY + 1, Theme.BORDER);
        RenderHelper.fill(graphics, x, sugY + sugHeight - 1, x + width, sugY + sugHeight, Theme.BORDER);
        RenderHelper.fill(graphics, x, sugY, x + 1, sugY + sugHeight, Theme.BORDER);
        RenderHelper.fill(graphics, x + width - 1, sugY, x + width, sugY + sugHeight, Theme.BORDER);
        
        for (int i = 0; i < maxVisible; i++) {
            int itemY = sugY + 2 + i * 12;
            boolean isHovered = mouseX >= x && mouseX < x + width && mouseY >= itemY && mouseY < itemY + 12;
            boolean isSelected = i == selectedSuggestion;
            
            if (isSelected || isHovered) {
                RenderHelper.fill(graphics, x + 1, itemY, x + width - 1, itemY + 12, Theme.BG_HOVER);
            }
            
            String displayName = truncateToFit(font, suggestions.get(i), maxTextWidth);
            RenderHelper.drawString(graphics, font, displayName, x + 4, itemY + 2, Theme.TEXT_PRIMARY, false);
        }
    }
    
    private String truncateToFit(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        String truncated = text;
        while (truncated.length() > 1 && font.width(truncated) + ellipsisWidth > maxWidth) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated + ellipsis;
    }
    
    public boolean hasSuggestions() {
        return showSuggestions && !suggestions.isEmpty();
    }
    
    public void renderSuggestionsOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        if (showSuggestions && !suggestions.isEmpty()) {
            renderSuggestions(graphics, mouseX, mouseY);
        }
    }
    
    @Override
    public void tick() {
        cursorBlinkTimer++;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        
        if (showSuggestions && !suggestions.isEmpty()) {
            int sugY = y + height;
            int maxVisible = Math.min(suggestions.size(), 5);
            for (int i = 0; i < maxVisible; i++) {
                int itemY = sugY + 2 + i * 12;
                if (mouseX >= x && mouseX < x + width && mouseY >= itemY && mouseY < itemY + 12) {
                    text = suggestions.get(i);
                    cursorPos = text.length();
                    showSuggestions = false;
                    if (onChange != null) onChange.accept(text);
                    return true;
                }
            }
        }
        
        if (isMouseOver(mouseX, mouseY)) {
            setFocused(true);
            cursorBlinkTimer = 0;
            return true;
        } else {
            setFocused(false);
            showSuggestions = false;
        }
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused || !enabled) return false;
        
        if (showSuggestions && !suggestions.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                selectedSuggestion = Math.min(selectedSuggestion + 1, Math.min(suggestions.size() - 1, 4));
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_UP) {
                selectedSuggestion = Math.max(selectedSuggestion - 1, 0);
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_ENTER) {
                if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
                    text = suggestions.get(selectedSuggestion);
                    cursorPos = text.length();
                    showSuggestions = false;
                    if (onChange != null) onChange.accept(text);
                    return true;
                }
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                showSuggestions = false;
                return true;
            }
        }
        
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && cursorPos > 0) {
            text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
            cursorPos--;
            onTextChanged();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DELETE && cursorPos < text.length()) {
            text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
            onTextChanged();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT && cursorPos > 0) {
            cursorPos--;
            cursorBlinkTimer = 0;
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT && cursorPos < text.length()) {
            cursorPos++;
            cursorBlinkTimer = 0;
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorPos = 0;
            cursorBlinkTimer = 0;
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_END) {
            cursorPos = text.length();
            cursorBlinkTimer = 0;
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            cursorPos = text.length();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused || !enabled) return false;
        if (Character.isISOControl(chr)) return false;
        if (text.length() >= maxLength) return false;
        
        text = text.substring(0, cursorPos) + chr + text.substring(cursorPos);
        cursorPos++;
        onTextChanged();
        return true;
    }
    
    private void onTextChanged() {
        cursorBlinkTimer = 0;
        if (onChange != null) onChange.accept(text);
        updateSuggestions();
    }
    
    private void updateSuggestions() {
        if (autocompleteProvider != null && !text.isEmpty()) {
            List<String> all = autocompleteProvider.get();
            String lower = text.toLowerCase();
            suggestions.clear();
            
            for (String s : all) {
                if (suggestions.size() >= 5) break;
                String sLower = s.toLowerCase();
                if (sLower.startsWith(lower) && !sLower.equals(lower)) {
                    suggestions.add(s);
                }
            }
            
            if (suggestions.size() < 5) {
                for (String s : all) {
                    if (suggestions.size() >= 5) break;
                    String sLower = s.toLowerCase();
                    if (!sLower.startsWith(lower) && sLower.contains(lower) && !suggestions.contains(s)) {
                        suggestions.add(s);
                    }
                }
            }
            
            showSuggestions = !suggestions.isEmpty();
            selectedSuggestion = showSuggestions ? 0 : -1;
        } else {
            suggestions.clear();
            showSuggestions = false;
        }
    }
    
    public String getText() { return text; }
    public void setText(String text) { 
        this.text = text != null ? text : "";
        this.cursorPos = this.text.length();
    }
    public void setOnChange(Consumer<String> onChange) { this.onChange = onChange; }
    public void setAutocompleteProvider(Supplier<List<String>> provider) { this.autocompleteProvider = provider; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
    public void setHint(String hint) { this.hint = hint; }
    public int getSuggestionsHeight() {
        return showSuggestions ? Math.min(suggestions.size(), 5) * 12 + 4 : 0;
    }
}
