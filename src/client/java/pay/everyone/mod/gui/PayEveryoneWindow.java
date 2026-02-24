package pay.everyone.mod.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import pay.everyone.mod.ModConfig;
import pay.everyone.mod.PayEveryoneClient;
import pay.everyone.mod.PayManager;
import pay.everyone.mod.compat.RenderHelper;

import java.util.ArrayList;
import java.util.List;

public class PayEveryoneWindow {
    private int x, y;
    private int width = 260;
    private int height = 340;
    
    private static final int BASE_WIDTH = 260;
    private static final int BASE_HEIGHT = 340;
    private static final float BASE_ASPECT_RATIO = (float) BASE_WIDTH / BASE_HEIGHT;
    
    private static final int MIN_WIDTH = 100;
    private static final int MIN_HEIGHT = (int)(MIN_WIDTH / BASE_ASPECT_RATIO);
    private static final int RESIZE_HANDLE_SIZE = 12;
    private static final int TITLE_BAR_HEIGHT = 16;
    private static final int TAB_HEIGHT = 18;
    
    private boolean visible = false;
    private boolean pinned = false;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;
    private boolean resizing = false;
    private int resizeStartMouseX, resizeStartMouseY;
    private int resizeStartWidth, resizeStartHeight;
    
    private int activeTab = 0;
    private final String[] tabNames = {"Main", "Settings", "Players", "Scan"};
    
    private final List<WidgetData> mainTabWidgets = new ArrayList<>();
    private final List<WidgetData> settingsTabWidgets = new ArrayList<>();
    private final List<WidgetData> playersTabWidgets = new ArrayList<>();
    private final List<WidgetData> scanTabWidgets = new ArrayList<>();
    
    private final PayManager payManager = PayManager.getInstance();
    
    private TextFieldWidget amountField;
    private SliderWidget delaySlider;
    private CheckboxWidget autoModeCheckbox;
    private CheckboxWidget tabScanEnabledCheckbox;
    private ButtonWidget startButton;
    private ButtonWidget pauseButton;
    private ButtonWidget stopButton;
    private ProgressBarWidget progressBar;
    private LabelWidget statusLabel;
    private LabelWidget infoLabel;
    private PlayerListWidget paymentLogList;
    private long errorDisplayUntil = 0;
    
    private TextFieldWidget commandField;
    private CheckboxWidget reverseSyntaxCheckbox;
    private CheckboxWidget doubleSendCheckbox;
    private SliderWidget doubleSendDelaySlider;
    private CheckboxWidget autoConfirmCheckbox;
    private TextFieldWidget confirmSlotField;
    private SliderWidget confirmDelaySlider;
    private CheckboxWidget dynamicSubdivisionCheckbox;
    
    private TextFieldWidget addPlayerField;
    private TextFieldWidget excludePlayerField;
    private PlayerListWidget manualList;
    private PlayerListWidget excludeList;
    private LabelWidget onlineLabel;
    private TextFieldWidget playerSearchField;
    private PlayerListWidget tablistView;
    private String playerSearchFilter = "";
    
    private SliderWidget scanIntervalSlider;
    private ButtonWidget startScanButton;
    private ButtonWidget clearScanListButton;
    private ProgressBarWidget scanProgressBar;
    private PlayerListWidget scanLogList;
    
    private static class WidgetData {
        Widget widget;
        int relX, relY;
        
        WidgetData(Widget w, int rx, int ry) {
            this.widget = w;
            this.relX = rx;
            this.relY = ry;
        }
    }
    
    public PayEveryoneWindow() {
        resetPosition();
        initWidgets();
    }
    
    public void resetPosition() {
        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int invWidth = 176;
        int invLeft = (screenW - invWidth) / 2;
        this.x = Math.max(4, invLeft - width - 8);
        this.y = (screenH - height) / 2;
    }
    
    public boolean hasFocusedTextField() {
        for (WidgetData wd : getActiveWidgets()) {
            if (wd.widget instanceof TextFieldWidget && wd.widget.isFocused()) {
                return true;
            }
        }
        return false;
    }

    public boolean isCapturingKeybind() {
        return KeybindWidget.isAnyListening();
    }
    
    private void addWidget(List<WidgetData> list, Widget w, int relX, int relY) {
        w.setPosition(relX, relY);
        list.add(new WidgetData(w, relX, relY));
    }
    
    private void initWidgets() {
        initMainTab();
        initSettingsTab();
        initPlayersTab();
        initScanTab();
    }
    
    private void initMainTab() {
        int cy = TITLE_BAR_HEIGHT + TAB_HEIGHT + 8;
        int cw = width - 16;
        
        LabelWidget amountLabel = new LabelWidget(0, 0, cw, "Amount:");
        addWidget(mainTabWidgets, amountLabel, 8, cy);
        
        amountField = new TextFieldWidget(0, 0, cw - 50, 18, "e.g. 1000, 5k, 1m, 100-500");
        amountField.setMaxLength(32);
        addWidget(mainTabWidgets, amountField, 8, cy + 12);
        
        autoModeCheckbox = new CheckboxWidget(0, 0, 45, "Auto", false, null);
        addWidget(mainTabWidgets, autoModeCheckbox, cw - 37, cy + 14);
        
        delaySlider = new SliderWidget(0, 0, cw, "Delay", payManager.getPaymentDelay(), 0, 10000, 100, payManager::setPaymentDelay);
        delaySlider.setSuffix("ms");
        addWidget(mainTabWidgets, delaySlider, 8, cy + 38);
        
        tabScanEnabledCheckbox = new CheckboxWidget(0, 0, cw, "Enable TabScan", 
            payManager.isTabScanEnabled(), payManager::setTabScanEnabled);
        addWidget(mainTabWidgets, tabScanEnabledCheckbox, 8, cy + 68);
        
        statusLabel = new LabelWidget(0, 0, cw, "Ready");
        statusLabel.setColor(Theme.TEXT_SECONDARY);
        addWidget(mainTabWidgets, statusLabel, 8, cy + 88);
        
        progressBar = new ProgressBarWidget(0, 0, cw, "Progress");
        addWidget(mainTabWidgets, progressBar, 8, cy + 104);
        
        int btnW = (cw - 8) / 3;
        startButton = new ButtonWidget(0, 0, btnW, 24, "Start", this::onStartClicked);
        addWidget(mainTabWidgets, startButton, 8, cy + 130);
        
        pauseButton = new ButtonWidget(0, 0, btnW, 24, "Pause", this::onPauseClicked);
        addWidget(mainTabWidgets, pauseButton, 8 + btnW + 4, cy + 130);
        
        stopButton = new ButtonWidget(0, 0, btnW, 24, "Cancel", this::onCancelClicked);
        addWidget(mainTabWidgets, stopButton, 8 + (btnW + 4) * 2, cy + 130);
        
        infoLabel = new LabelWidget(0, 0, cw, "Players: 0");
        infoLabel.setColor(Theme.TEXT_SECONDARY);
        addWidget(mainTabWidgets, infoLabel, 8, cy + 162);
        
        LabelWidget logLabel = new LabelWidget(0, 0, cw, "Payment Log:");
        logLabel.setColor(Theme.TEXT_SECONDARY);
        addWidget(mainTabWidgets, logLabel, 8, cy + 176);
        
        paymentLogList = new PlayerListWidget(0, 0, cw, 80, "");
        paymentLogList.setPlayerProvider(payManager::getPaymentLogs);
        paymentLogList.setAutoScroll(true);
        paymentLogList.setShowCount(false);
        addWidget(mainTabWidgets, paymentLogList, 8, cy + 186);
    }
    
    private void initSettingsTab() {
        int cy = TITLE_BAR_HEIGHT + TAB_HEIGHT + 8;
        int cw = width - 16;
        
        LabelWidget cmdLabel = new LabelWidget(0, 0, cw, "Pay command:");
        addWidget(settingsTabWidgets, cmdLabel, 8, cy);
        
        commandField = new TextFieldWidget(0, 0, cw, 18, "pay");
        commandField.setText(payManager.getPayCommand());
        commandField.setMaxLength(32);
        commandField.setOnChange(cmd -> {
            if (cmd != null && !cmd.isEmpty()) payManager.setPayCommand(cmd);
        });
        addWidget(settingsTabWidgets, commandField, 8, cy + 12);
        
        reverseSyntaxCheckbox = new CheckboxWidget(0, 0, cw, "Reverse syntax", 
            payManager.isReverseSyntaxEnabled(), payManager::setReverseSyntax);
        addWidget(settingsTabWidgets, reverseSyntaxCheckbox, 8, cy + 38);
        
        LabelWidget sep1 = new LabelWidget(0, 0, cw, "--- Auto Confirm ---");
        sep1.setColor(Theme.TEXT_SECONDARY);
        sep1.setCentered(true);
        addWidget(settingsTabWidgets, sep1, 8, cy + 58);
        
        autoConfirmCheckbox = new CheckboxWidget(0, 0, cw, "Enable auto confirm click", 
            payManager.getConfirmClickSlot() >= 0, 
            enabled -> payManager.setConfirmClickSlot(enabled ? 0 : -1));
        addWidget(settingsTabWidgets, autoConfirmCheckbox, 8, cy + 72);
        
        LabelWidget slotLabel = new LabelWidget(0, 0, 50, "Slot ID:");
        addWidget(settingsTabWidgets, slotLabel, 8, cy + 95);
        
        confirmSlotField = new TextFieldWidget(0, 0, 60, 18, "0");
        confirmSlotField.setText(String.valueOf(Math.max(0, payManager.getConfirmClickSlot())));
        confirmSlotField.setMaxLength(3);
        confirmSlotField.setOnChange(val -> {
            try {
                int slot = Integer.parseInt(val.trim());
                if (slot >= 0 && slot <= PayManager.MAX_CONTAINER_SLOT && payManager.getConfirmClickSlot() >= 0) {
                    payManager.setConfirmClickSlot(slot);
                }
            } catch (NumberFormatException ignored) {}
        });
        addWidget(settingsTabWidgets, confirmSlotField, 60, cy + 90);
        
        confirmDelaySlider = new SliderWidget(0, 0, cw, "Click delay", 
            payManager.getConfirmClickDelay(), 50, 2000, 50, payManager::setConfirmClickDelay);
        confirmDelaySlider.setSuffix("ms");
        addWidget(settingsTabWidgets, confirmDelaySlider, 8, cy + 116);
        
        LabelWidget sep2 = new LabelWidget(0, 0, cw, "--- Double Send ---");
        sep2.setColor(Theme.TEXT_SECONDARY);
        sep2.setCentered(true);
        addWidget(settingsTabWidgets, sep2, 8, cy + 148);
        
        doubleSendCheckbox = new CheckboxWidget(0, 0, cw, "Enable double send", 
            payManager.isDoubleSendEnabled(), payManager::setDoubleSend);
        addWidget(settingsTabWidgets, doubleSendCheckbox, 8, cy + 162);
        
        doubleSendDelaySlider = new SliderWidget(0, 0, cw, "Double send delay", 
            payManager.getDoubleSendDelay(), 0, 5000, 100, payManager::setDoubleSendDelay);
        doubleSendDelaySlider.setSuffix("ms");
        addWidget(settingsTabWidgets, doubleSendDelaySlider, 8, cy + 180);

        LabelWidget sep3 = new LabelWidget(0, 0, cw, "--- Keybinds ---");
        sep3.setColor(Theme.TEXT_SECONDARY);
        sep3.setCentered(true);
        addWidget(settingsTabWidgets, sep3, 8, cy + 204);

        KeybindWidget cancelKeybind = new KeybindWidget(0, 0, cw, 18, "Force Stop:",
            PayEveryoneClient.getCancelPaymentKey());
        addWidget(settingsTabWidgets, cancelKeybind, 8, cy + 218);

        ButtonWidget resetBtn = new ButtonWidget(0, 0, cw, 20, "Reset Settings", () -> {
            payManager.resetSettingsOnly();
            payManager.clearScanLogs();
            payManager.clearPaymentLogs();
            payManager.clearManualPlayers();
            payManager.clearExclusions();
            refreshSettings();
        });
        addWidget(settingsTabWidgets, resetBtn, 8, cy + 240);
    }
    
    private void initPlayersTab() {
        int cy = TITLE_BAR_HEIGHT + TAB_HEIGHT + 8;
        int cw = width - 16;
        int halfWidth = (cw - 8) / 2;
        
        LabelWidget addLabel = new LabelWidget(0, 0, halfWidth, "Add Player:");
        addLabel.setColor(Theme.TEXT_PRIMARY);
        addWidget(playersTabWidgets, addLabel, 8, cy);
        
        addPlayerField = new TextFieldWidget(0, 0, halfWidth - 36, 18, "Username");
        addPlayerField.setMaxLength(64);
        addWidget(playersTabWidgets, addPlayerField, 8, cy + 12);
        
        ButtonWidget addBtn = new ButtonWidget(0, 0, 32, 18, "+", () -> {
            String name = addPlayerField.getText().trim();
            if (!name.isEmpty()) {
                payManager.addManualPlayers(name);
                addPlayerField.setText("");
            }
        });
        addWidget(playersTabWidgets, addBtn, 8 + halfWidth - 32, cy + 12);
        
        LabelWidget excludeLabel = new LabelWidget(0, 0, halfWidth, "Exclude Player:");
        excludeLabel.setColor(Theme.TEXT_PRIMARY);
        addWidget(playersTabWidgets, excludeLabel, 8 + halfWidth + 8, cy);
        
        excludePlayerField = new TextFieldWidget(0, 0, halfWidth - 36, 18, "Username");
        excludePlayerField.setMaxLength(64);
        excludePlayerField.setAutocompleteProvider(this::getTablistPlayerNames);
        addWidget(playersTabWidgets, excludePlayerField, 8 + halfWidth + 8, cy + 12);
        
        ButtonWidget excludeBtn = new ButtonWidget(0, 0, 32, 18, "+", () -> {
            String name = excludePlayerField.getText().trim();
            if (!name.isEmpty()) {
                payManager.addExcludedPlayers(name);
                excludePlayerField.setText("");
            }
        });
        addWidget(playersTabWidgets, excludeBtn, 8 + cw - 32, cy + 12);
        
        int listY = cy + 38;
        int listHeight = 60;
        
        manualList = new PlayerListWidget(0, 0, halfWidth, listHeight, "Manual");
        manualList.setPlayerProvider(() -> payManager.getPlayerListSample("manual", 1000));
        manualList.setOnRemove(name -> payManager.removeManualPlayers(new String[]{name}));
        manualList.setShowCount(true);
        addWidget(playersTabWidgets, manualList, 8, listY);
        
        excludeList = new PlayerListWidget(0, 0, halfWidth, listHeight, "Excluded");
        excludeList.setPlayerProvider(() -> payManager.getPlayerListSample("exclude", 1000));
        excludeList.setOnRemove(name -> payManager.removeExcludedPlayers(new String[]{name}));
        excludeList.setShowCount(true);
        addWidget(playersTabWidgets, excludeList, 8 + halfWidth + 8, listY);
        
        int onlineY = listY + listHeight + 12;
        
        onlineLabel = new LabelWidget(0, 0, cw - 80, "Online Players:");
        onlineLabel.setColor(Theme.TEXT_PRIMARY);
        addWidget(playersTabWidgets, onlineLabel, 8, onlineY);
        
        playerSearchField = new TextFieldWidget(0, 0, 76, 16, "Search...");
        playerSearchField.setMaxLength(16);
        playerSearchField.setOnChange(filter -> {
            playerSearchFilter = filter != null ? filter.toLowerCase() : "";
        });
        addWidget(playersTabWidgets, playerSearchField, 8 + cw - 76, onlineY - 2);
        
        tablistView = new PlayerListWidget(0, 0, cw, 160, "");
        tablistView.setPlayerProvider(this::getFilteredOnlinePlayers);
        tablistView.setShowCount(false);
        tablistView.setOnContextAction((player, action) -> {
            if (action == PlayerListWidget.ContextAction.EXCLUDE) {
                payManager.addExcludedPlayers(player);
            }
        });
        addWidget(playersTabWidgets, tablistView, 8, onlineY + 14);
    }
    
    private void initScanTab() {
        int cy = TITLE_BAR_HEIGHT + TAB_HEIGHT + 8;
        int cw = width - 16;
        
        scanIntervalSlider = new SliderWidget(0, 0, cw, "Scan interval", 
            payManager.getScanInterval(), 1, 2000, 1, payManager::setScanInterval);
        scanIntervalSlider.setSuffix("ms");
        addWidget(scanTabWidgets, scanIntervalSlider, 8, cy);
        
        int btnW = (cw - 8) / 3;
        startScanButton = new ButtonWidget(0, 0, btnW, 20, "Start Scan", this::onStartScanClicked);
        addWidget(scanTabWidgets, startScanButton, 8, cy + 32);
        
        ButtonWidget cancelScanButton = new ButtonWidget(0, 0, btnW, 20, "Cancel", () -> {
            payManager.stopTabScan();
            payManager.clearTabScanList();
            payManager.clearScanLogs();
        });
        addWidget(scanTabWidgets, cancelScanButton, 8 + btnW + 4, cy + 32);
        
        clearScanListButton = new ButtonWidget(0, 0, btnW, 20, "Clear List", () -> {
            payManager.clearTabScanList();
            payManager.clearScanLogs();
        });
        addWidget(scanTabWidgets, clearScanListButton, 8 + (btnW + 4) * 2, cy + 32);
        
        int halfBtnW = (cw - 4) / 2;
        ButtonWidget exportButton = new ButtonWidget(0, 0, halfBtnW, 20, "Export", payManager::exportPlayerList);
        addWidget(scanTabWidgets, exportButton, 8, cy + 56);
        
        ButtonWidget importButton = new ButtonWidget(0, 0, halfBtnW, 20, "Import", payManager::importPlayerList);
        addWidget(scanTabWidgets, importButton, 8 + halfBtnW + 4, cy + 56);
        
        dynamicSubdivisionCheckbox = new CheckboxWidget(0, 0, cw, "Dynamic Subdivision", 
            payManager.isDynamicSubdivisionEnabled(), enabled -> {
                payManager.setDynamicSubdivisionEnabled(enabled);
                ModConfig.getInstance().setDynamicSubdivisionEnabled(enabled);
            });
        addWidget(scanTabWidgets, dynamicSubdivisionCheckbox, 8, cy + 88);
        
        scanProgressBar = new ProgressBarWidget(0, 0, cw, "Scan");
        addWidget(scanTabWidgets, scanProgressBar, 8, cy + 112);
        
        LabelWidget scanLogLabel = new LabelWidget(0, 0, cw, "Scan Log:");
        scanLogLabel.setColor(Theme.TEXT_SECONDARY);
        addWidget(scanTabWidgets, scanLogLabel, 8, cy + 138);
        
        scanLogList = new PlayerListWidget(0, 0, cw, 141, "");
        scanLogList.setPlayerProvider(payManager::getScanLogs);
        scanLogList.setAutoScroll(true);
        scanLogList.setShowCount(false);
        addWidget(scanTabWidgets, scanLogList, 8, cy + 138);
    }
    
    private List<String> getTablistPlayerNames() {
        List<String> tabScan = payManager.getPlayerListSample("tabscan", 100000);
        if (!tabScan.isEmpty()) return tabScan;
        return payManager.getPlayerListSample("tablist", 100000);
    }
    
    private List<String> getFilteredOnlinePlayers() {
        List<String> source = getTablistPlayerNames();
        if (playerSearchFilter.isEmpty()) return source;
        
        List<String> startsWith = new ArrayList<>();
        List<String> contains = new ArrayList<>();
        
        for (String p : source) {
            String lower = p.toLowerCase();
            String searchableName = lower.startsWith(".") ? lower.substring(1) : lower;
            String searchableFilter = playerSearchFilter.startsWith(".") ? playerSearchFilter.substring(1) : playerSearchFilter;
            
            if (lower.startsWith(playerSearchFilter) || searchableName.startsWith(searchableFilter)) {
                startsWith.add(p);
            } else if (lower.contains(playerSearchFilter)) {
                contains.add(p);
            }
        }
        
        startsWith.addAll(contains);
        return startsWith;
    }
    
    private void onStartClicked() {
        String amount = amountField.getText().trim();
        if (amount.isEmpty()) {
            payManager.setLastError("Amount cannot be empty");
            errorDisplayUntil = System.currentTimeMillis() + 3000;
            return;
        }
        
        payManager.clearPaymentLogs();
        
        boolean auto = autoModeCheckbox.isChecked();
        long delay = payManager.getPaymentDelay();
        payManager.payAll(amount, delay, auto);
    }
    
    private void onPauseClicked() {
        if (payManager.isPaying() || payManager.isTabScanning()) {
            payManager.togglePause();
        }
    }
    
    private void onCancelClicked() {
        payManager.stopPaying();
        payManager.stopTabScan();
        payManager.clearTabScanList();
    }
    
    private void onStartScanClicked() {
        if (!payManager.isTabScanning()) {
            payManager.clearScanLogs();
            payManager.queryPlayersViaTabComplete(payManager.getScanInterval(), false);
        }
    }
    
    private void refreshSettings() {
        commandField.setText(payManager.getPayCommand());
        reverseSyntaxCheckbox.setChecked(payManager.isReverseSyntaxEnabled());
        doubleSendCheckbox.setChecked(payManager.isDoubleSendEnabled());
        doubleSendDelaySlider.setValue(payManager.getDoubleSendDelay());
        autoConfirmCheckbox.setChecked(payManager.getConfirmClickSlot() >= 0);
        confirmSlotField.setText(String.valueOf(Math.max(0, payManager.getConfirmClickSlot())));
        confirmDelaySlider.setValue(payManager.getConfirmClickDelay());
        amountField.setText("");
        tabScanEnabledCheckbox.setChecked(payManager.isTabScanEnabled());
        delaySlider.setValue(payManager.getPaymentDelay());
        dynamicSubdivisionCheckbox.setChecked(payManager.isDynamicSubdivisionEnabled());
    }
    
    private List<WidgetData> getActiveWidgets() {
        switch (activeTab) {
            case 0: return mainTabWidgets;
            case 1: return settingsTabWidgets;
            case 2: return playersTabWidgets;
            case 3: return scanTabWidgets;
            default: return mainTabWidgets;
        }
    }
    
    private float getScale() {
        // Use raw scale based on current width; no snapping
        return (float) width / BASE_WIDTH;
    }
    
    private static String cachedVersion = null;
    
    private static String getModVersion() {
        if (cachedVersion == null) {
            cachedVersion = FabricLoader.getInstance()
                .getModContainer("pay-everyone")
                .map(c -> "v" + c.getMetadata().getVersion().getFriendlyString())
                .orElse("");
        }
        return cachedVersion;
    }
    
    private static String getCancelKeyName() {
        try {
            if (PayEveryoneClient.getCancelPaymentKey() != null) {
                return PayEveryoneClient.getCancelPaymentKey().getTranslatedKeyMessage().getString();
            }
        } catch (Exception ignored) {}
        return "J";
    }
    
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible && !pinned) return;
        
        Minecraft mc = Minecraft.getInstance();
        float scale = getScale();
        
        int renderWidth = width;
        int renderHeight = height;
        
        int scaledMouseX = (int)((mouseX - x) / scale);
        int scaledMouseY = (int)((mouseY - y) / scale);
        
        RenderHelper.fill(graphics, x, y, x + renderWidth, y + renderHeight, Theme.BG_PRIMARY);
        
        RenderHelper.fill(graphics, x, y, x + renderWidth, y + 1, Theme.BORDER);
        RenderHelper.fill(graphics, x, y + renderHeight - 1, x + renderWidth, y + renderHeight, Theme.BORDER);
        RenderHelper.fill(graphics, x, y, x + 1, y + renderHeight, Theme.BORDER);
        RenderHelper.fill(graphics, x + renderWidth - 1, y, x + renderWidth, y + renderHeight, Theme.BORDER);
        
        int handleX = x + renderWidth - RESIZE_HANDLE_SIZE;
        int handleY = y + renderHeight - RESIZE_HANDLE_SIZE;
        RenderHelper.fill(graphics, handleX, handleY, x + renderWidth, y + renderHeight, Theme.BG_TERTIARY);
        for (int i = 0; i < 3; i++) {
            int offset = i * 3;
            RenderHelper.fill(graphics, handleX + RESIZE_HANDLE_SIZE - 3 - offset, handleY + RESIZE_HANDLE_SIZE - 1, 
                         handleX + RESIZE_HANDLE_SIZE - 2 - offset, handleY + RESIZE_HANDLE_SIZE, Theme.TEXT_SECONDARY);
            RenderHelper.fill(graphics,                          handleX + RESIZE_HANDLE_SIZE - 1, handleY + RESIZE_HANDLE_SIZE - 3 - offset,
                         handleX + RESIZE_HANDLE_SIZE, handleY + RESIZE_HANDLE_SIZE - 2 - offset, Theme.TEXT_SECONDARY);
        }
        
        RenderHelper.pushPose(graphics);
        RenderHelper.translate(graphics, x, y, 0);
        RenderHelper.scale(graphics, scale, scale, 1.0f);
        
        int ox = 0;
        int oy = 0;
        
        RenderHelper.fill(graphics, ox + 1, oy + 1, ox + BASE_WIDTH - 1, oy + TITLE_BAR_HEIGHT, Theme.TITLE_BAR);
        String titleText = "Pay Everyone " + getModVersion();
        RenderHelper.drawString(graphics, mc.font, titleText, ox + 6, oy + 4, Theme.TITLE_BAR_TEXT, false);
        
        int pinX = ox + BASE_WIDTH - 14;
        int pinY = oy + 2;
        int pinBg = isMouseOverPinScaled(scaledMouseX, scaledMouseY) ? Theme.BG_HOVER : Theme.BG_TERTIARY;
        RenderHelper.fill(graphics, pinX, pinY, pinX + 12, pinY + 12, pinBg);
        int dotColor = pinned ? Theme.PIN_ACTIVE : Theme.PIN_INACTIVE;
        RenderHelper.fill(graphics, pinX + 3, pinY + 3, pinX + 9, pinY + 9, dotColor);
        
        int hideX = pinX - 16;
        int hideY = oy + 2;
        int hideBg = isMouseOverHideScaled(scaledMouseX, scaledMouseY) ? Theme.BG_HOVER : Theme.BG_TERTIARY;
        RenderHelper.fill(graphics, hideX, hideY, hideX + 12, hideY + 12, hideBg);
        RenderHelper.fill(graphics, hideX + 3, hideY + 5, hideX + 9, hideY + 7, Theme.TEXT_SECONDARY);
        
        if (pinned) {
            String pinnedText = "Pinned";
            int pinnedWidth = mc.font.width(pinnedText);
            RenderHelper.drawString(graphics, mc.font, pinnedText, hideX - pinnedWidth - 4, oy + 4, Theme.WARNING, false);
        }
        
        int tabY = oy + TITLE_BAR_HEIGHT;
        int tabWidth = BASE_WIDTH / tabNames.length;
        for (int i = 0; i < tabNames.length; i++) {
            int tx = ox + i * tabWidth;
            int tabBg = (i == activeTab) ? Theme.TAB_ACTIVE : Theme.TAB_INACTIVE;
            RenderHelper.fill(graphics, tx, tabY, tx + tabWidth, tabY + TAB_HEIGHT, tabBg);
            int textColor = (i == activeTab) ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
            int textX = tx + (tabWidth - mc.font.width(tabNames[i])) / 2;
            RenderHelper.drawString(graphics, mc.font, tabNames[i], textX, tabY + 5, textColor, false);
        }
        
        int contentY = oy + TITLE_BAR_HEIGHT + TAB_HEIGHT;
        RenderHelper.fill(graphics, ox + 1, contentY, ox + BASE_WIDTH - 1, oy + BASE_HEIGHT - 1, Theme.BG_SECONDARY);
        
        for (WidgetData wd : getActiveWidgets()) {
            wd.widget.setPosition(wd.relX, wd.relY);
            wd.widget.setScreenTransform(x, y, scale, true);
            wd.widget.render(graphics, scaledMouseX, scaledMouseY, delta);
        }
        
        updateStatusDisplay();
        
        boolean showingWarning = false;
        if (payManager.isTabScanning()) {
            String warning = "Movement locked during Tabscan - [" + getCancelKeyName() + "] to Cancel";
            int warningWidth = mc.font.width(warning);
            int warningX = ox + (BASE_WIDTH - warningWidth) / 2;
            int warningY = oy + BASE_HEIGHT - 14;
            RenderHelper.drawString(graphics, mc.font, warning, warningX, warningY, 0xFFFF0000, false);
            showingWarning = true;
        } else if (payManager.isPaying()) {
            String warning = "[" + getCancelKeyName() + "] to Cancel";
            int warningWidth = mc.font.width(warning);
            int warningX = ox + (BASE_WIDTH - warningWidth) / 2;
            int warningY = oy + BASE_HEIGHT - 14;
            RenderHelper.drawString(graphics, mc.font, warning, warningX, warningY, Theme.WARNING, false);
            showingWarning = true;
        }
        
        if (!showingWarning) {
            String watermark = "Made by Aurick";
            RenderHelper.drawString(graphics, mc.font, watermark, ox + 4, oy + BASE_HEIGHT - 12, 0x44AAAAAA, false);
        }
        
        for (WidgetData wd : getActiveWidgets()) {
            if (wd.widget instanceof TextFieldWidget) {
                ((TextFieldWidget) wd.widget).renderSuggestionsOverlay(graphics, scaledMouseX, scaledMouseY);
            }
            if (wd.widget instanceof PlayerListWidget) {
                ((PlayerListWidget) wd.widget).renderContextMenu(graphics, scaledMouseX, scaledMouseY);
            }
        }
        
        RenderHelper.popPose(graphics);
    }
    
    private boolean isMouseOverPinScaled(int scaledMouseX, int scaledMouseY) {
        int pinX = BASE_WIDTH - 14;
        int pinY = 2;
        return scaledMouseX >= pinX && scaledMouseX < pinX + 12 && scaledMouseY >= pinY && scaledMouseY < pinY + 12;
    }
    
    private boolean isMouseOverHideScaled(int scaledMouseX, int scaledMouseY) {
        int hideX = BASE_WIDTH - 14 - 16;
        int hideY = 2;
        return scaledMouseX >= hideX && scaledMouseX < hideX + 12 && scaledMouseY >= hideY && scaledMouseY < hideY + 12;
    }
    
    private void updateStatusDisplay() {
        String lastError = payManager.getLastError();
        if (lastError != null && System.currentTimeMillis() < errorDisplayUntil) {
            statusLabel.setText(lastError);
            statusLabel.setColor(Theme.ERROR);
        } else if (payManager.isTabScanning()) {
            statusLabel.setText("Scanning...");
            statusLabel.setColor(Theme.WARNING);
        } else if (payManager.isPaying()) {
            if (payManager.isPaused()) {
                statusLabel.setText("Paused");
                statusLabel.setColor(Theme.WARNING);
            } else {
                statusLabel.setText("Paying...");
                statusLabel.setColor(Theme.ACCENT);
            }
        } else {
            statusLabel.setText("Ready");
            statusLabel.setColor(Theme.TEXT_SECONDARY);
        }
        
        if (payManager.isTabScanning()) {
            float scanProg = payManager.getScanProgress();
            progressBar.setProgress(scanProg);
            progressBar.setLabel("Scanning");
            scanProgressBar.setProgress(scanProg);
            scanProgressBar.setLabel("Scan");
        } else if (payManager.isPaying()) {
            float payProg = payManager.getPaymentProgress();
            progressBar.setProgress(payProg);
            progressBar.setLabel("Progress");
            scanProgressBar.setProgress(0);
            scanProgressBar.setLabel("Scan");
        } else {
            progressBar.setProgress(0);
            progressBar.setLabel("Progress");
            scanProgressBar.setProgress(0);
            scanProgressBar.setLabel("Scan");
        }
        
        List<String> onlinePlayers = payManager.getOnlinePlayers();
        List<String> tabScanList = payManager.getPlayerListSample("tabscan", 10000);
        List<String> manualList = payManager.getPlayerListSample("manual", 10000);
        
        String source;
        if (!manualList.isEmpty()) {
            source = "Manual";
        } else if (!tabScanList.isEmpty()) {
            source = "TabScan";
        } else {
            source = "Tab List";
        }
        infoLabel.setText("Players (" + source + "): " + onlinePlayers.size());
        
        onlineLabel.setText("Online (" + source + "): " + onlinePlayers.size());
        
        boolean isRunning = payManager.isPaying() || payManager.isTabScanning();
        startButton.setEnabled(!isRunning);
        pauseButton.setEnabled(isRunning);
        pauseButton.setText(payManager.isPaused() ? "Resume" : "Pause");
        
        startScanButton.setEnabled(!payManager.isTabScanning());
    }
    
    private boolean isMouseOverResize(double mouseX, double mouseY) {
        int renderW = getRenderedWidth();
        int renderH = getRenderedHeight();
        int handleX = x + renderW - RESIZE_HANDLE_SIZE;
        int handleY = y + renderH - RESIZE_HANDLE_SIZE;
        return mouseX >= handleX && mouseX < x + renderW && mouseY >= handleY && mouseY < y + renderH;
    }
    
    private int getRenderedWidth() {
        return width;
    }
    
    private int getRenderedHeight() {
        return height;
    }
    
    private double toScaledX(double mouseX) {
        return (mouseX - x) / getScale();
    }
    
    private double toScaledY(double mouseY) {
        return (mouseY - y) / getScale();
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        double scaledX = toScaledX(mouseX);
        double scaledY = toScaledY(mouseY);
        
        for (WidgetData wd : getActiveWidgets()) {
            if (wd.widget instanceof PlayerListWidget) {
                PlayerListWidget plw = (PlayerListWidget) wd.widget;
                if (plw.hasContextMenu()) {
                    plw.mouseClicked(scaledX, scaledY, button);
                    return true;
                }
            }
        }
        
        if (button == 0 && isMouseOverResize(mouseX, mouseY)) {
            resizing = true;
            resizeStartMouseX = (int) mouseX;
            resizeStartMouseY = (int) mouseY;
            resizeStartWidth = width;
            resizeStartHeight = height;
            return true;
        }
        
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        
        int pinX = BASE_WIDTH - 14;
        int pinY = 2;
        if (scaledX >= pinX && scaledX < pinX + 12 && scaledY >= pinY && scaledY < pinY + 12) {
            pinned = !pinned;
            return true;
        }
        
        int hideX = pinX - 16;
        int hideY = 2;
        if (scaledX >= hideX && scaledX < hideX + 12 && scaledY >= hideY && scaledY < hideY + 12) {
            PayEveryoneHud.getInstance().setManuallyHidden(true);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§e[Pay Everyone] GUI hidden. Use /payeveryone show to restore."), 
                    false);
            }
            return true;
        }
        
        if (button == 0 && scaledY >= 0 && scaledY < TITLE_BAR_HEIGHT) {
            dragging = true;
            dragOffsetX = (int) mouseX - x;
            dragOffsetY = (int) mouseY - y;
            return true;
        }
        
        if (scaledY >= TITLE_BAR_HEIGHT && scaledY < TITLE_BAR_HEIGHT + TAB_HEIGHT) {
            int tabWidth = BASE_WIDTH / tabNames.length;
            int clickedTab = (int)(scaledX / tabWidth);
            if (clickedTab >= 0 && clickedTab < tabNames.length) {
                activeTab = clickedTab;
                clearFocus();
                return true;
            }
        }
        
        for (WidgetData wd : getActiveWidgets()) {
            if (wd.widget.mouseClicked(scaledX, scaledY, button)) {
                clearFocusExcept(wd.widget);
                return true;
            }
        }
        
        clearFocus();
        return true;
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (resizing && button == 0) {
            resizing = false;
            return true;
        }
        
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        
        double widgetMouseX = toScaledX(mouseX);
        double widgetMouseY = toScaledY(mouseY);
        
        for (WidgetData wd : getActiveWidgets()) {
            if (wd.widget.mouseReleased(widgetMouseX, widgetMouseY, button)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (resizing) {
            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            
            int deltaXInt = (int)(mouseX - resizeStartMouseX);
            int deltaYInt = (int)(mouseY - resizeStartMouseY);
            
            int newWidth, newHeight;
            if (Math.abs(deltaXInt) > Math.abs(deltaYInt)) {
                newWidth = resizeStartWidth + deltaXInt;
                newHeight = (int)(newWidth / BASE_ASPECT_RATIO);
            } else {
                newHeight = resizeStartHeight + deltaYInt;
                newWidth = (int)(newHeight * BASE_ASPECT_RATIO);
            }
            
            if (newWidth < MIN_WIDTH) {
                newWidth = MIN_WIDTH;
                newHeight = (int)(newWidth / BASE_ASPECT_RATIO);
            }
            if (newHeight < MIN_HEIGHT) {
                newHeight = MIN_HEIGHT;
                newWidth = (int)(newHeight * BASE_ASPECT_RATIO);
            }
            if (newWidth > screenWidth - x) {
                newWidth = screenWidth - x;
                newHeight = (int)(newWidth / BASE_ASPECT_RATIO);
            }
            if (newHeight > screenHeight - y) {
                newHeight = screenHeight - y;
                newWidth = (int)(newHeight * BASE_ASPECT_RATIO);
            }
            
            width = newWidth;
            height = newHeight;
            return true;
        }
        
        if (dragging) {
            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            
            x = Math.max(0, Math.min(screenWidth - width, (int)mouseX - dragOffsetX));
            y = Math.max(0, Math.min(screenHeight - height, (int)mouseY - dragOffsetY));
            return true;
        }
        
        double widgetMouseX = toScaledX(mouseX);
        double widgetMouseY = toScaledY(mouseY);
        float scale = getScale();
        
        for (WidgetData wd : getActiveWidgets()) {
            if (wd.widget.mouseDragged(widgetMouseX, widgetMouseY, button, deltaX / scale, deltaY / scale)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;
        
        double widgetMouseX = toScaledX(mouseX);
        double widgetMouseY = toScaledY(mouseY);
        
        for (WidgetData wd : getActiveWidgets()) {
            if (wd.widget.mouseScrolled(widgetMouseX, widgetMouseY, amount)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (WidgetData wd : getActiveWidgets()) {
            if (wd.widget.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean charTyped(char chr, int modifiers) {
        for (WidgetData wd : getActiveWidgets()) {
            if (wd.widget.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return false;
    }
    
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        if (width > screenWidth) {
            width = screenWidth;
            height = (int)(width / BASE_ASPECT_RATIO);
        }
        if (height > screenHeight) {
            height = screenHeight;
            width = (int)(height * BASE_ASPECT_RATIO);
        }
        
        if (width < MIN_WIDTH) {
            width = MIN_WIDTH;
            height = (int)(width / BASE_ASPECT_RATIO);
        }
        
        int renderW = getRenderedWidth();
        int renderH = getRenderedHeight();
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + renderW > screenWidth) x = Math.max(0, screenWidth - renderW);
        if (y + renderH > screenHeight) y = Math.max(0, screenHeight - renderH);
        
        for (WidgetData wd : getActiveWidgets()) {
            wd.widget.tick();
        }
    }
    
    private void clearFocus() {
        for (WidgetData wd : mainTabWidgets) wd.widget.setFocused(false);
        for (WidgetData wd : settingsTabWidgets) wd.widget.setFocused(false);
        for (WidgetData wd : playersTabWidgets) wd.widget.setFocused(false);
        for (WidgetData wd : scanTabWidgets) wd.widget.setFocused(false);
    }
    
    private void clearFocusExcept(Widget except) {
        for (WidgetData wd : mainTabWidgets) {
            if (wd.widget != except) wd.widget.setFocused(false);
        }
        for (WidgetData wd : settingsTabWidgets) {
            if (wd.widget != except) wd.widget.setFocused(false);
        }
        for (WidgetData wd : playersTabWidgets) {
            if (wd.widget != except) wd.widget.setFocused(false);
        }
        for (WidgetData wd : scanTabWidgets) {
            if (wd.widget != except) wd.widget.setFocused(false);
        }
    }
    
    private boolean hasBeenPositioned = false;
    
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { 
        if (visible && !this.visible && !hasBeenPositioned) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getWindow() != null) {
                this.x = (mc.getWindow().getGuiScaledWidth() - width) / 2;
                this.y = (mc.getWindow().getGuiScaledHeight() - height) / 2;
                hasBeenPositioned = true;
            }
        }
        this.visible = visible; 
    }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    
    public boolean isMouseOver(double mouseX, double mouseY) {
        int renderW = getRenderedWidth();
        int renderH = getRenderedHeight();
        return mouseX >= x && mouseX < x + renderW && mouseY >= y && mouseY < y + renderH;
    }
}
