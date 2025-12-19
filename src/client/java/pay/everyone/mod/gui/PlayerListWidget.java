package pay.everyone.mod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pay.everyone.mod.compat.RenderHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlayerListWidget extends Widget {
    private String title;
    private List<String> players = new ArrayList<>();
    private Supplier<List<String>> playerProvider;
    private Consumer<String> onRemove;
    private BiConsumer<String, ContextAction> onContextAction;
    private int scrollOffset = 0;
    private boolean autoScroll = false;
    private boolean showCount = true;
    private int lastPlayerCount = 0;
    private static final int ITEM_HEIGHT = 14;
    private static final int REMOVE_BTN_SIZE = 10;
    private static final int SCROLLBAR_WIDTH = 4;
    
    private boolean contextMenuOpen = false;
    private int contextMenuX, contextMenuY;
    private String contextMenuPlayer = null;
    
    private boolean draggingScrollbar = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    
    public enum ContextAction { EXCLUDE }
    
    public PlayerListWidget(int x, int y, int width, int height, String title) {
        super(x, y, width, height);
        this.title = title;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        
        if (playerProvider != null) {
            players = new ArrayList<>(playerProvider.get());
        }
        
        int listHeight = height - 12;
        int maxVisible = (listHeight - 2) / ITEM_HEIGHT;
        
        int maxScroll = Math.max(0, players.size() - maxVisible);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
        
        if (autoScroll && players.size() > lastPlayerCount) {
            scrollOffset = maxScroll;
        }
        lastPlayerCount = players.size();
        
        var font = Minecraft.getInstance().font;
        
        String displayTitle = showCount ? title + " (" + players.size() + ")" : title;
        if (!displayTitle.isEmpty()) {
            RenderHelper.drawString(graphics, font, displayTitle, x, y, Theme.TEXT_PRIMARY, false);
        }
        
        int listY = y + 12;
        
        RenderHelper.fill(graphics, x, listY, x + width, listY + listHeight, Theme.BG_TERTIARY);
        RenderHelper.fill(graphics, x, listY, x + width, listY + 1, Theme.BORDER);
        RenderHelper.fill(graphics, x, listY + listHeight - 1, x + width, listY + listHeight, Theme.BORDER);
        RenderHelper.fill(graphics, x, listY, x + 1, listY + listHeight, Theme.BORDER);
        RenderHelper.fill(graphics, x + width - 1, listY, x + width, listY + listHeight, Theme.BORDER);
        
        int scissorX1 = toScreenX(x + 1);
        int scissorY1 = toScreenY(listY + 1);
        int scissorX2 = toScreenX(x + width - 1);
        int scissorY2 = toScreenY(listY + listHeight - 1);
        
        if (useMatrixScaling) {
            RenderHelper.popPose(graphics);
        }
        RenderHelper.enableScissor(graphics, scissorX1, scissorY1, scissorX2, scissorY2);
        if (useMatrixScaling) {
            RenderHelper.pushPose(graphics);
            RenderHelper.translate(graphics, screenOffsetX, screenOffsetY, 0);
            RenderHelper.scale(graphics, screenScale, screenScale, 1.0f);
        }
        
        for (int i = 0; i < Math.min(players.size() - scrollOffset, maxVisible); i++) {
            int index = i + scrollOffset;
            int itemY = listY + 2 + i * ITEM_HEIGHT;
            
            boolean isHovered = mouseX >= x + 1 && mouseX < x + width - 1 && 
                               mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
            if (isHovered) {
                RenderHelper.fill(graphics, x + 1, itemY, x + width - 1, itemY + ITEM_HEIGHT, Theme.BG_HOVER);
            }
            
            String playerName = players.get(index);
            int maxNameWidth = onRemove != null ? width - REMOVE_BTN_SIZE - 12 : width - 8;
            while (font.width(playerName) > maxNameWidth && playerName.length() > 1) {
                playerName = playerName.substring(0, playerName.length() - 1);
            }
            RenderHelper.drawString(graphics, font, playerName, x + 4, itemY + 3, Theme.TEXT_PRIMARY, false);
            
            if (onRemove != null) {
                int btnX = x + width - REMOVE_BTN_SIZE - 6;
                int btnY = itemY + (ITEM_HEIGHT - REMOVE_BTN_SIZE) / 2;
                boolean btnHovered = isHovered && mouseX >= btnX && mouseX < btnX + REMOVE_BTN_SIZE && 
                                    mouseY >= btnY && mouseY < btnY + REMOVE_BTN_SIZE;
                
                int btnColor = btnHovered ? Theme.ERROR : Theme.TEXT_SECONDARY;
                RenderHelper.drawString(graphics, font, "x", btnX + 2, btnY, btnColor, false);
            }
        }
        
        if (useMatrixScaling) {
            RenderHelper.popPose(graphics);
        }
        RenderHelper.disableScissor(graphics);
        if (useMatrixScaling) {
            RenderHelper.pushPose(graphics);
            RenderHelper.translate(graphics, screenOffsetX, screenOffsetY, 0);
            RenderHelper.scale(graphics, screenScale, screenScale, 1.0f);
        }
        
        if (players.size() > maxVisible) {
            int scrollbarHeight = listHeight - 4;
            int thumbHeight = Math.max(20, scrollbarHeight * maxVisible / players.size());
            int thumbY = listY + 2 + (scrollbarHeight - thumbHeight) * scrollOffset / Math.max(1, players.size() - maxVisible);
            
            RenderHelper.fill(graphics, x + width - 4, listY + 2, x + width - 1, listY + listHeight - 2, Theme.SCROLLBAR_BG);
            RenderHelper.fill(graphics, x + width - 4, thumbY, x + width - 1, thumbY + thumbHeight, Theme.SCROLLBAR_THUMB);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        
        if (contextMenuOpen) {
            int menuWidth = 70;
            int menuHeight = 18;
            
            if (mouseX >= contextMenuX && mouseX < contextMenuX + menuWidth && 
                mouseY >= contextMenuY && mouseY < contextMenuY + menuHeight) {
                if (contextMenuPlayer != null && onContextAction != null) {
                    onContextAction.accept(contextMenuPlayer, ContextAction.EXCLUDE);
                }
                contextMenuOpen = false;
                contextMenuPlayer = null;
                return true;
            }
            
            contextMenuOpen = false;
            contextMenuPlayer = null;
            return true;
        }
        
        int listY = y + 12;
        int listHeight = height - 12;
        int maxVisible = (listHeight - 2) / ITEM_HEIGHT;
        int maxScroll = Math.max(0, players.size() - maxVisible);
        
        if (button == 0 && players.size() > maxVisible) {
            int scrollbarX = x + width - SCROLLBAR_WIDTH - 1;
            if (mouseX >= scrollbarX && mouseX < x + width - 1 && mouseY >= listY + 2 && mouseY < listY + listHeight - 2) {
                int scrollbarHeight = listHeight - 4;
                int thumbHeight = Math.max(20, scrollbarHeight * maxVisible / players.size());
                int thumbY = listY + 2 + (scrollbarHeight - thumbHeight) * scrollOffset / Math.max(1, maxScroll);
                
                if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
                    draggingScrollbar = true;
                    dragStartY = (int) mouseY;
                    dragStartScrollOffset = scrollOffset;
                    return true;
                } else {
                    float clickRatio = (float)(mouseY - listY - 2 - thumbHeight / 2) / (scrollbarHeight - thumbHeight);
                    scrollOffset = Math.max(0, Math.min(maxScroll, (int)(clickRatio * maxScroll)));
                    return true;
                }
            }
        }
        
        if (mouseX >= x && mouseX < x + width && mouseY >= listY && mouseY < listY + listHeight) {
            int clickedIndex = -1;
            for (int i = 0; i < Math.min(players.size() - scrollOffset, maxVisible); i++) {
                int itemY = listY + 2 + i * ITEM_HEIGHT;
                if (mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                    clickedIndex = i + scrollOffset;
                    break;
                }
            }
            
            if (button == 1 && clickedIndex >= 0 && clickedIndex < players.size() && onContextAction != null) {
                contextMenuOpen = true;
                contextMenuX = (int) mouseX;
                contextMenuY = (int) mouseY;
                contextMenuPlayer = players.get(clickedIndex);
                return true;
            }
            
            if (button == 0 && clickedIndex >= 0 && clickedIndex < players.size() && onRemove != null) {
                int itemY = listY + 2 + (clickedIndex - scrollOffset) * ITEM_HEIGHT;
                int btnX = x + width - REMOVE_BTN_SIZE - 4;
                int btnY = itemY + (ITEM_HEIGHT - REMOVE_BTN_SIZE) / 2;
                
                if (mouseX >= btnX && mouseX < btnX + REMOVE_BTN_SIZE && 
                    mouseY >= btnY && mouseY < btnY + REMOVE_BTN_SIZE) {
                    String player = players.get(clickedIndex);
                    onRemove.accept(player);
                    return true;
                }
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar && button == 0) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScrollbar && button == 0) {
            int listHeight = height - 12;
            int maxVisible = (listHeight - 2) / ITEM_HEIGHT;
            int maxScroll = Math.max(0, players.size() - maxVisible);
            
            if (maxScroll > 0) {
                int scrollbarHeight = listHeight - 4;
                int thumbHeight = Math.max(20, scrollbarHeight * maxVisible / players.size());
                int trackHeight = scrollbarHeight - thumbHeight;
                
                if (trackHeight > 0) {
                    int deltaScroll = (int)((mouseY - dragStartY) * maxScroll / trackHeight);
                    scrollOffset = Math.max(0, Math.min(maxScroll, dragStartScrollOffset + deltaScroll));
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible) return false;
        
        int listY = y + 12;
        int listHeight = height - 12;
        
        if (mouseX >= x && mouseX < x + width && mouseY >= listY && mouseY < listY + listHeight) {
            int maxVisible = (listHeight - 2) / ITEM_HEIGHT;
            int maxScroll = Math.max(0, players.size() - maxVisible);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)amount));
            return true;
        }
        return false;
    }
    
    public void setPlayerProvider(Supplier<List<String>> provider) {
        this.playerProvider = provider;
    }
    
    public void setOnRemove(Consumer<String> onRemove) {
        this.onRemove = onRemove;
    }
    
    public void setTitle(String title) { this.title = title; }
    public List<String> getPlayers() { return new ArrayList<>(players); }
    public void setAutoScroll(boolean autoScroll) { this.autoScroll = autoScroll; }
    public void setShowCount(boolean showCount) { this.showCount = showCount; }
    public void setOnContextAction(BiConsumer<String, ContextAction> onContextAction) { this.onContextAction = onContextAction; }
    
    public boolean hasContextMenu() { return contextMenuOpen; }
    
    public void renderContextMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!contextMenuOpen || contextMenuPlayer == null) return;
        
        var font = Minecraft.getInstance().font;
        int menuWidth = 70;
        int menuHeight = 18;
        
        // Draw menu background with border
        RenderHelper.fill(graphics, contextMenuX, contextMenuY, contextMenuX + menuWidth, contextMenuY + menuHeight, Theme.BG_PRIMARY);
        RenderHelper.fill(graphics, contextMenuX, contextMenuY, contextMenuX + menuWidth, contextMenuY + 1, Theme.ERROR);
        RenderHelper.fill(graphics, contextMenuX, contextMenuY + menuHeight - 1, contextMenuX + menuWidth, contextMenuY + menuHeight, Theme.BORDER);
        RenderHelper.fill(graphics, contextMenuX, contextMenuY, contextMenuX + 1, contextMenuY + menuHeight, Theme.BORDER);
        RenderHelper.fill(graphics, contextMenuX + menuWidth - 1, contextMenuY, contextMenuX + menuWidth, contextMenuY + menuHeight, Theme.BORDER);
        
        boolean isHovered = mouseX >= contextMenuX && mouseX < contextMenuX + menuWidth &&
            mouseY >= contextMenuY && mouseY < contextMenuY + menuHeight;
        
        // Hover highlight
        if (isHovered) {
            RenderHelper.fill(graphics, contextMenuX + 1, contextMenuY + 1, contextMenuX + menuWidth - 1, contextMenuY + menuHeight - 1, 0x40FF5555);
        }
        
        RenderHelper.drawString(graphics, font, "- Exclude", contextMenuX + 4, contextMenuY + 5, Theme.ERROR, false);
    }
}
