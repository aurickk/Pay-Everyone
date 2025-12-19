package pay.everyone.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public class PayManager {
    private static final PayManager INSTANCE = new PayManager();
    
    private static final int MAX_PLAYER_NAME_LENGTH = 16;
    private static final int MIN_PLAYER_NAME_LENGTH = 3;
    private static final Pattern VALID_PLAYER_NAME_PATTERN = Pattern.compile("^(\\.?[a-zA-Z0-9_][a-zA-Z0-9_]*)$");
    private static final Set<String> HARDCODED_EXCLUSIONS = Set.of("*", "**", "***", "all", "everyone", "@a", "@p", "@r", "@s");
    
    private static final String[] SCAN_PREFIXES = {
        "", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
        "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "_", ".",
    };
    
    private static final int COMMAND_CHECK_REQUEST_ID = 99998;
    private static final int TAB_SCAN_REQUEST_ID_BASE = 10000;
    private static final long COMMAND_CHECK_TIMEOUT_MS = 2000;
    private static final long CONFIRMATION_DELAY_MS = 500;
    public static final int MAX_CONTAINER_SLOT = 89;
    private static final long MAX_DELAY_MS = 60000;
    
    private final Set<String> excludedPlayers = Collections.synchronizedSet(new HashSet<>());
    private final List<String> tabScanPlayerList = new ArrayList<>();
    private final List<String> manualPlayerList = new ArrayList<>();
    private final Object playerListLock = new Object();
    private final Object paymentLock = new Object();
    private final Object scanLock = new Object();
    private final Set<Integer> processedRequestIds = Collections.synchronizedSet(new HashSet<>());
    
    private volatile boolean isPaying = false;
    private volatile boolean shouldStop = false;
    private volatile boolean isPaused = false;
    private volatile boolean isTabScanning = false;
    private volatile boolean debugMode = false;
    private volatile boolean scanCompleted = false;
    private volatile boolean isAutoScan = false;
    private volatile long scanInterval = 50;
    private volatile float scanProgress = 0.0f;
    private volatile float paymentProgress = 0.0f;
    private volatile String lastPaymentLog = "";
    private final List<String> paymentLogs = new ArrayList<>();
    private static final int MAX_PAYMENT_LOGS = 50;
    
    private volatile int confirmClickSlot = -1;
    private volatile long confirmClickDelay = 100;
    private volatile boolean doubleSend = false;
    private volatile long doubleSendDelay = 1000;
    private volatile boolean reverseSyntax = false;
    private volatile boolean tabScanEnabled = true;
    private volatile long paymentDelay = 1000;
    private volatile Thread paymentWorkerThread = null;
    
    private volatile String pendingAmount = null;
    private volatile long pendingDelay = 1000;
    private volatile boolean pendingAutoMode = false;
    private volatile Runnable pendingConfirmationCallback = null;
    private volatile String lastError = null;
    
    private volatile String payCommand = "pay";
    private volatile String lastDiscoveryMethod = "tab list";
    
    private volatile boolean waitingForCommandCheck = false;
    private volatile boolean commandQueryable = false;

    private PayManager() {}

    public static PayManager getInstance() { return INSTANCE; }
    
    public static long parseShortNumber(String input) throws NumberFormatException {
        if (input == null || input.isEmpty()) throw new NumberFormatException("Empty input");
        String s = input.trim().toLowerCase();
        long multiplier = 1;
        
        if (s.endsWith("t")) { multiplier = 1_000_000_000_000L; s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("b")) { multiplier = 1_000_000_000L; s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("m")) { multiplier = 1_000_000L; s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("k")) { multiplier = 1_000L; s = s.substring(0, s.length() - 1); }
        
        if (s.isEmpty()) throw new NumberFormatException("No number before suffix");
        return s.contains(".") ? (long)(Double.parseDouble(s) * multiplier) : Long.parseLong(s) * multiplier;
    }
    
    public static String formatShortNumber(long value) {
        if (value >= 1_000_000_000_000L) {
            double v = value / 1_000_000_000_000.0;
            return (v == (long) v) ? String.format("%dt", (long) v) : String.format("%.1ft", v);
        } else if (value >= 1_000_000_000L) {
            double v = value / 1_000_000_000.0;
            return (v == (long) v) ? String.format("%db", (long) v) : String.format("%.1fb", v);
        } else if (value >= 1_000_000L) {
            double v = value / 1_000_000.0;
            return (v == (long) v) ? String.format("%dm", (long) v) : String.format("%.1fm", v);
        } else if (value >= 1_000L) {
            double v = value / 1_000.0;
            return (v == (long) v) ? String.format("%dk", (long) v) : String.format("%.1fk", v);
        }
        return String.valueOf(value);
    }

    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean enabled) { this.debugMode = enabled; }
    public String getPayCommand() { return payCommand; }
    public String getLastError() { return lastError; }
    public void setLastError(String error) { lastError = error; }
    public void clearLastError() { lastError = null; }
    public String getLastDiscoveryMethod() { return lastDiscoveryMethod; }
    public boolean isTabScanning() { return isTabScanning; }
    public boolean isPaying() { return isPaying; }
    public boolean isPaused() { return isPaused; }
    public float getScanProgress() { return scanProgress; }
    public float getPaymentProgress() { return paymentProgress; }
    public String getLastPaymentLog() { return lastPaymentLog; }
    public List<String> getPaymentLogs() { synchronized (paymentLogs) { return new ArrayList<>(paymentLogs); } }
    
    private void addPaymentLog(String log) {
        synchronized (paymentLogs) {
            paymentLogs.add(log);
            if (paymentLogs.size() > MAX_PAYMENT_LOGS) {
                paymentLogs.remove(0);
            }
            lastPaymentLog = log;
        }
    }
    
    public void clearPaymentLogs() {
        synchronized (paymentLogs) {
            paymentLogs.clear();
            lastPaymentLog = "";
        }
    }
    
    private final List<String> scanLogs = new ArrayList<>();
    private static final int MAX_SCAN_LOGS = 100;
    
    public List<String> getScanLogs() { synchronized (scanLogs) { return new ArrayList<>(scanLogs); } }
    
    public void addScanLog(String log) {
        synchronized (scanLogs) {
            scanLogs.add(log);
            if (scanLogs.size() > MAX_SCAN_LOGS) {
                scanLogs.remove(0);
            }
        }
    }
    
    public void clearScanLogs() {
        synchronized (scanLogs) {
            scanLogs.clear();
        }
    }
    
    public void togglePause() {
        isPaused = !isPaused;
        if (debugMode) {
            PayEveryone.LOGGER.info("Pause toggled: {}", isPaused);
        }
    }
    
    public int getExclusionCount() { return excludedPlayers.size(); }
    public int getConfirmClickSlot() { return confirmClickSlot; }
    public long getConfirmClickDelay() { return confirmClickDelay; }
    public boolean isDoubleSendEnabled() { return doubleSend; }
    public long getDoubleSendDelay() { return doubleSendDelay; }
    public int getOnlinePlayerCount() { return getOnlinePlayers().size(); }
    public boolean isReverseSyntaxEnabled() { return reverseSyntax; }
    public boolean isTabScanEnabled() { return tabScanEnabled; }
    public boolean isScanCompleted() { return scanCompleted; }
    public int getManualPlayerCount() { synchronized (playerListLock) { return manualPlayerList.size(); } }
    
    public void setPayCommand(String command) {
        if (command == null) return;
        if (command.startsWith("/")) command = command.substring(1);
        String trimmed = command.trim();
        if (!trimmed.isEmpty()) this.payCommand = trimmed;
    }
    
    public void setConfirmClickSlot(int slotId) { this.confirmClickSlot = slotId; }
    public void setConfirmClickDelay(long delayMs) { this.confirmClickDelay = Math.max(50, Math.min(delayMs, 2000)); }
    public void setDoubleSend(boolean enabled) { this.doubleSend = enabled; }
    public void setDoubleSendDelay(long delayMs) { this.doubleSendDelay = Math.max(0, Math.min(delayMs, 10000)); }
    public void setReverseSyntax(boolean enabled) { this.reverseSyntax = enabled; }
    public void setTabScanEnabled(boolean enabled) { this.tabScanEnabled = enabled; }
    public long getPaymentDelay() { return paymentDelay; }
    public void setPaymentDelay(long delayMs) { this.paymentDelay = Math.max(0, Math.min(delayMs, 60000)); }
    public long getScanInterval() { return scanInterval; }
    public void setScanInterval(long intervalMs) { this.scanInterval = Math.max(50, Math.min(intervalMs, 10000)); }

    public boolean stopTabScan() {
        if (isTabScanning) {
            shouldStop = true;
            isPaused = false;
            isTabScanning = false;
            scanCompleted = true;
            synchronized (playerListLock) { tabScanPlayerList.clear(); }
            scanProgress = 0.0f;
            pendingAmount = null;
            pendingAutoMode = false;
            pendingConfirmationCallback = null;
            addScanLog("Scan cancelled");
            return true;
        }
        return false;
    }

    private boolean isValidPlayerName(String name) {
        if (name == null || name.isEmpty()) return false;
        String trimmed = name.trim();
        if (HARDCODED_EXCLUSIONS.contains(trimmed.toLowerCase())) return false;
        
        boolean isGeyserName = trimmed.startsWith(".");
        int minLength = isGeyserName ? 4 : MIN_PLAYER_NAME_LENGTH;
        int maxLength = isGeyserName ? 17 : MAX_PLAYER_NAME_LENGTH;
        
        if (trimmed.length() < minLength || trimmed.length() > maxLength) return false;
        return VALID_PLAYER_NAME_PATTERN.matcher(trimmed).matches();
    }
    
    private void sendPaymentCommand(String command, String playerName, String context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            LocalPlayer currentPlayer = minecraft.player;
            if (currentPlayer != null && currentPlayer.connection != null) {
                try {
                    currentPlayer.connection.send(new ServerboundChatCommandPacket(command));
                } catch (Exception e) {
                    try {
                        currentPlayer.connection.sendCommand(command);
                    } catch (Exception e2) {
                        PayEveryone.LOGGER.error("Failed to send payment command to " + playerName, e2);
                    }
                }
            }
        });
    }

    public void addExcludedPlayers(String... players) {
        for (String player : players) {
            String cleaned = player.trim();
            if (!cleaned.isEmpty()) excludedPlayers.add(cleaned.toLowerCase());
        }
    }

    public AddPlayersResult addManualPlayers(String... players) {
        List<String> added = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        synchronized (playerListLock) {
            for (String player : players) {
                String cleaned = player.trim();
                if (cleaned.isEmpty()) continue;
                if (manualPlayerList.contains(cleaned)) duplicates.add(cleaned);
                else { manualPlayerList.add(cleaned); added.add(cleaned); }
            }
        }
        return new AddPlayersResult(added, duplicates);
    }
    
    public static class AddPlayersResult {
        public final List<String> added;
        public final List<String> duplicates;
        public AddPlayersResult(List<String> added, List<String> duplicates) {
            this.added = added;
            this.duplicates = duplicates;
        }
    }

    public void clearManualPlayers() { synchronized (playerListLock) { manualPlayerList.clear(); } }

    public List<String> removeManualPlayers(String[] playerNames) {
        List<String> removed = new ArrayList<>();
        synchronized (playerListLock) {
            for (String name : playerNames) {
                String cleanName = name.trim();
                if (!cleanName.isEmpty() && manualPlayerList.remove(cleanName)) removed.add(cleanName);
            }
        }
        return removed;
    }

    public void clearExclusions() { excludedPlayers.clear(); }

    public List<String> removeExcludedPlayers(String[] playerNames) {
        List<String> removed = new ArrayList<>();
        for (String name : playerNames) {
            String cleanName = name.trim().toLowerCase();
            if (!cleanName.isEmpty() && excludedPlayers.remove(cleanName)) removed.add(name.trim());
        }
        return removed;
    }

    public void clearTabScanList() {
        synchronized (playerListLock) { tabScanPlayerList.clear(); }
        scanCompleted = false;
        scanProgress = 0.0f;
    }
    
    public void clearAllPlayerLists() {
        synchronized (playerListLock) { manualPlayerList.clear(); tabScanPlayerList.clear(); }
        excludedPlayers.clear();
        lastDiscoveryMethod = "tab list";
        pendingAmount = null;
    }
    
    /**
     * Reset only configurable settings (used by the Settings tab button).
     * Does not clear player lists, logs, or TabScan cache.
     */
    public void resetSettingsOnly() {
        confirmClickSlot = -1;
        confirmClickDelay = 100;
        doubleSend = false;
        doubleSendDelay = 1000;
        reverseSyntax = false;
        tabScanEnabled = true;
        payCommand = "pay";
        debugMode = false;
        scanInterval = 50;
        paymentDelay = 1000;
        lastError = null;
    }

    /**
     * Full reset used internally if needed (not wired to GUI button anymore).
     */
    public void resetAllSettings() {
        synchronized (playerListLock) { manualPlayerList.clear(); tabScanPlayerList.clear(); }
        excludedPlayers.clear();
        processedRequestIds.clear();
        synchronized (paymentLock) { isPaying = false; }
        shouldStop = false;
        resetSettingsOnly();
        pendingAmount = null;
        pendingAutoMode = false;
        pendingConfirmationCallback = null;
        lastDiscoveryMethod = "tab list";
        scanCompleted = false;
        scanProgress = 0.0f;
        paymentProgress = 0.0f;
        isAutoScan = false;
    }

    public void queryPlayersViaTabComplete() { queryPlayersViaTabComplete(250, false); }
    
    public void queryPlayersViaTabComplete(long intervalMs) { queryPlayersViaTabComplete(intervalMs, false); }
    
    public void queryPlayersViaTabComplete(long intervalMs, boolean isAuto) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.connection == null) return;
        
        if (isTabScanning) return; // Already scanning
        
        scanInterval = intervalMs;
        isTabScanning = true;
        isAutoScan = isAuto;
        isPaused = false;
        shouldStop = false;
        scanCompleted = false;
        scanProgress = 0.0f;
        processedRequestIds.clear();
        synchronized (playerListLock) { tabScanPlayerList.clear(); }
        
        clearScanLogs();
        addScanLog(String.format("Starting scan (%d prefixes, %dms interval)", SCAN_PREFIXES.length, scanInterval));
        if (debugMode) {
            PayEveryone.LOGGER.info("Starting tab scan ({} prefixes, {}ms interval)", SCAN_PREFIXES.length, scanInterval);
        }
        
        CompletableFuture.runAsync(this::runSequentialScan).exceptionally(t -> {
            PayEveryone.LOGGER.error("Tab scan failed", t);
            isTabScanning = false;
            scanCompleted = true;
            isPaused = false;
            return null;
        });
    }

    private void runSequentialScan() {
        Minecraft minecraft = Minecraft.getInstance();
        final String cmd = payCommand;
        final boolean isReverse = reverseSyntax;
        scanProgress = 0.0f;
        
        for (int i = 0; i < SCAN_PREFIXES.length && isTabScanning && !shouldStop; i++) {
            while (isPaused && isTabScanning && !shouldStop) {
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
            if (shouldStop || !isTabScanning) break;
            
            String prefix = SCAN_PREFIXES[i];
            int requestId = TAB_SCAN_REQUEST_ID_BASE + i;
            
            minecraft.execute(() -> {
                LocalPlayer player = minecraft.player;
                if (player != null && player.connection != null) {
                    String command = isReverse ? "/" + cmd + " 1 " + prefix : "/" + cmd + " " + prefix;
                    player.connection.send(new ServerboundCommandSuggestionPacket(requestId, command));
                }
            });
            
            scanProgress = (float)(i + 1) / SCAN_PREFIXES.length;
            
            try { Thread.sleep(scanInterval); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        finishScan();
    }

    private void finishScan() {
        synchronized (scanLock) { 
        if (scanCompleted) return;
            scanCompleted = !isAutoScan;
        }
        isTabScanning = false;
        isPaused = false;
        scanProgress = 1.0f;
        
        Minecraft minecraft = Minecraft.getInstance();
        int totalPlayers; synchronized (playerListLock) { totalPlayers = tabScanPlayerList.size(); }
        
        if (totalPlayers == 0) {
            lastDiscoveryMethod = "tab list";
            addScanLog("Scan complete - no players found via command");
        } else {
            lastDiscoveryMethod = "tabscan";
            addScanLog(String.format("Scan complete - found %d players", totalPlayers));
        }
        
        if (debugMode) {
            PayEveryone.LOGGER.info("Tab scan complete. Found {} players via {}", totalPlayers, lastDiscoveryMethod);
                }
        
        Runnable callback; synchronized (paymentLock) { callback = pendingConfirmationCallback; pendingConfirmationCallback = null; }
        if (callback != null) { minecraft.execute(callback); return; }
        
        if (pendingAmount != null) {
            String amount = pendingAmount; long delay = pendingDelay; boolean autoMode = pendingAutoMode;
            pendingAmount = null; pendingAutoMode = false;
            minecraft.execute(() -> payAll(amount, delay, autoMode));
        }
    }
    
    public CompletableFuture<Boolean> checkCommandQueryable() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        
        if (player == null || player.connection == null) { future.complete(false); return future; }
        
        waitingForCommandCheck = true;
        commandQueryable = false;
        
        final boolean isReverse = reverseSyntax;
        minecraft.execute(() -> {
            String checkCommand = isReverse ? "/" + payCommand + " 1 " : "/" + payCommand + " ";
            player.connection.send(new ServerboundCommandSuggestionPacket(COMMAND_CHECK_REQUEST_ID, checkCommand));
        });
        
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(COMMAND_CHECK_TIMEOUT_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            waitingForCommandCheck = false;
            future.complete(commandQueryable);
        }).exceptionally(t -> { waitingForCommandCheck = false; future.complete(false); return null; });
        
        return future;
    }
    
    public void startPaymentWithScan(String amountOrRange, long delayMs, Runnable onConfirmationReady) {
        if (isPaying || isTabScanning) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.connection == null) return;
        
        boolean hasManualPlayers, hasTabScanPlayers;
        synchronized (playerListLock) {
            hasManualPlayers = !manualPlayerList.isEmpty();
            hasTabScanPlayers = !tabScanPlayerList.isEmpty();
        }
        
        if (hasManualPlayers) { lastDiscoveryMethod = "manual add"; if (onConfirmationReady != null) onConfirmationReady.run(); return; }
        if (hasTabScanPlayers) { lastDiscoveryMethod = "tabscan (cached)"; if (onConfirmationReady != null) onConfirmationReady.run(); return; }
        
        pendingAmount = amountOrRange;
        pendingDelay = delayMs;
        
        if (!tabScanEnabled) {
            lastDiscoveryMethod = "tab list";
            if (onConfirmationReady != null) {
                CompletableFuture.runAsync(() -> {
                    try { Thread.sleep(CONFIRMATION_DELAY_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    minecraft.execute(onConfirmationReady);
                });
            }
            return;
        }
        
        checkCommandQueryable().thenAccept(queryable -> {
            if (queryable) {
                if (debugMode) {
                minecraft.execute(() -> {
                    LocalPlayer p = minecraft.player;
                        if (p != null) p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§6[Pay Everyone] Scanning players via /%s...", payCommand)), false);
                });
                }
                pendingConfirmationCallback = onConfirmationReady;
                lastDiscoveryMethod = "tabscan";
                queryPlayersViaTabComplete(250);
            } else {
                lastDiscoveryMethod = "tab list";
                if (onConfirmationReady != null) {
                    CompletableFuture.runAsync(() -> {
                        try { Thread.sleep(CONFIRMATION_DELAY_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        minecraft.execute(onConfirmationReady);
                    });
                }
            }
        });
    }

    public void handleTabCompletionResponse(int requestId, List<String> suggestions) {
        if (requestId == COMMAND_CHECK_REQUEST_ID && waitingForCommandCheck) {
            commandQueryable = true;
            waitingForCommandCheck = false;
            return;
        }
        
        if (!isTabScanning) return;
        
        boolean isOurRequest = (requestId >= TAB_SCAN_REQUEST_ID_BASE && requestId < TAB_SCAN_REQUEST_ID_BASE + SCAN_PREFIXES.length);
        if (!isOurRequest || !processedRequestIds.add(requestId)) return;
        
        int prefixIndex = requestId - TAB_SCAN_REQUEST_ID_BASE;
        String prefix = (prefixIndex >= 0 && prefixIndex < SCAN_PREFIXES.length) ? SCAN_PREFIXES[prefixIndex] : "?";
        String prefixDisplay = prefix.isEmpty() ? "(all)" : "'" + prefix + "'";
        
        synchronized (playerListLock) {
            int foundCount = 0;
            for (String suggestion : suggestions) {
                String cleaned = suggestion.trim().replaceAll("§[0-9a-fk-or]", "");
                if (isValidPlayerName(cleaned) && !tabScanPlayerList.contains(cleaned)) {
                    tabScanPlayerList.add(cleaned);
                    foundCount++;
        }
            }
            addScanLog(String.format("Prefix %s: +%d (total: %d)", prefixDisplay, foundCount, tabScanPlayerList.size()));
        }
    }

    public List<String> getPlayersForAutocomplete() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) return Collections.emptyList();

        String localPlayerName = VersionCompat.getProfileName(localPlayer.getGameProfile());
        Set<String> allPlayers = new HashSet<>();
        
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                String name = VersionCompat.getProfileName(info.getProfile());
                if (name != null && isValidPlayerName(name)) allPlayers.add(name);
            }
        }
        
        synchronized (playerListLock) { allPlayers.addAll(tabScanPlayerList); allPlayers.addAll(manualPlayerList); }
        allPlayers.remove(localPlayerName);
        return new ArrayList<>(allPlayers);
    }

    public boolean isExcluded(String playerName) { return excludedPlayers.contains(playerName.toLowerCase()); }

    public List<String> getPlayerListSample(String listType, int maxCount) {
        List<String> result = new ArrayList<>();
        switch (listType.toLowerCase()) {
            case "tabscan":
                synchronized (playerListLock) {
                    for (int i = 0; i < Math.min(maxCount, tabScanPlayerList.size()); i++) result.add(tabScanPlayerList.get(i));
                }
                break;
            case "manual": case "add":
                synchronized (playerListLock) {
                    for (int i = 0; i < Math.min(maxCount, manualPlayerList.size()); i++) result.add(manualPlayerList.get(i));
                }
                break;
            case "exclude":
                int count = 0;
                for (String player : excludedPlayers) { if (count++ >= maxCount) break; result.add(player); }
                break;
            case "tablist":
                Minecraft minecraft = Minecraft.getInstance();
                ClientPacketListener connection = minecraft.getConnection();
                if (connection != null) {
                    int i = 0;
                    for (PlayerInfo info : connection.getOnlinePlayers()) {
                        if (i++ >= maxCount) break;
                        String name = VersionCompat.getProfileName(info.getProfile());
                        if (name != null && isValidPlayerName(name)) result.add(name);
                    }
                }
                break;
        }
        return result;
    }

    public List<String> getOnlinePlayers() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) return Collections.emptyList();

        String localPlayerName = VersionCompat.getProfileName(localPlayer.getGameProfile());
        List<String> sourceList;
        
        synchronized (playerListLock) {
            if (!manualPlayerList.isEmpty()) {
                sourceList = new ArrayList<>(manualPlayerList);
            } else if (!tabScanPlayerList.isEmpty()) {
                sourceList = new ArrayList<>(tabScanPlayerList);
            } else {
                ClientPacketListener connection = minecraft.getConnection();
                if (connection == null) return Collections.emptyList();
                sourceList = new ArrayList<>();
                for (PlayerInfo playerInfo : connection.getOnlinePlayers()) {
                    String name = VersionCompat.getProfileName(playerInfo.getProfile());
                    if (name != null && isValidPlayerName(name)) sourceList.add(name);
                }
            }
        }
        
        return sourceList.stream()
                .filter(name -> !name.equals(localPlayerName))
                .filter(name -> !excludedPlayers.contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public boolean payAll(String amountOrRange, long delayMs) { return payAll(amountOrRange, delayMs, false); }
    
    public boolean payAll(String amountOrRange, long delayMs, boolean autoMode) {
        synchronized (paymentLock) { if (isPaying) return false; }

        if (delayMs < 0 || delayMs > MAX_DELAY_MS) {
            lastError = "Invalid delay (0-60000ms)";
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            lastError = "Player not available";
            return false;
        }

        boolean hasTabScanResults;
        synchronized (playerListLock) { hasTabScanResults = !tabScanPlayerList.isEmpty(); }
        if (tabScanEnabled && !scanCompleted && !isTabScanning && !hasTabScanResults) {
            pendingAmount = amountOrRange;
            pendingDelay = delayMs;
            pendingAutoMode = autoMode;
            addPaymentLog("Starting TabScan first...");
            queryPlayersViaTabComplete(scanInterval, true); // Auto-started scan (not cached)
            return true; // Will auto-continue after scan
        }

        List<String> playersToPay = new ArrayList<>(getOnlinePlayers());
        if (playersToPay.isEmpty()) {
            lastError = "No players to pay";
            return false;
        }
        
        Collections.shuffle(playersToPay);

        boolean isRange = amountOrRange.contains("-");
        long parsedMinAmount = 0, parsedMaxAmount = 0;
        
        if (isRange) {
            String[] parts = amountOrRange.split("-");
            if (parts.length != 2) {
                lastError = "Invalid range! Use: min-max";
                return false;
            }
            try {
                parsedMinAmount = parseShortNumber(parts[0]);
                parsedMaxAmount = parseShortNumber(parts[1]);
                if (parsedMinAmount < 1 || parsedMaxAmount < 1 || parsedMaxAmount < parsedMinAmount) {
                    lastError = "Invalid range! max >= min >= 1";
                    return false;
                }
            } catch (NumberFormatException e) {
                lastError = "Invalid range format!";
                return false;
            }
        } else {
            try {
                parsedMinAmount = parseShortNumber(amountOrRange);
                parsedMaxAmount = parsedMinAmount;
                if (parsedMinAmount < 1) {
                    lastError = "Amount must be >= 1";
                    return false;
                }
            } catch (NumberFormatException e) {
                lastError = "Invalid amount!";
                return false;
            }
        }
        
        int playerCount = playersToPay.size();
        if (autoMode) {
            parsedMinAmount = Math.max(1, parsedMinAmount / playerCount);
            parsedMaxAmount = Math.max(parsedMinAmount, parsedMaxAmount / playerCount);
        }

        final long minAmount = parsedMinAmount, maxAmount = parsedMaxAmount;
        final boolean finalIsRange = isRange;

        synchronized (paymentLock) { isPaying = true; }
        shouldStop = false;
        isPaused = false;
        paymentProgress = 0.0f;
        
        if (confirmClickSlot >= 0) {
            minecraft.execute(() -> {
                pay.everyone.mod.gui.PayEveryoneHud.getInstance().getWindow().setPinned(true);
            });
        }
        
        String amountDisplay = finalIsRange ? String.format("%d-%d", minAmount, maxAmount) : String.valueOf(minAmount);
        addPaymentLog(String.format("Starting: %d players, %s each", playersToPay.size(), amountDisplay));

        Random random = new Random();
        final int totalPlayers = playersToPay.size();
        CompletableFuture.runAsync(() -> {
            paymentWorkerThread = Thread.currentThread();
            try {
                for (int i = 0; i < totalPlayers; i++) {
                    while (isPaused && !shouldStop) {
                        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                    }
                    
                if (shouldStop) {
                        addPaymentLog("Stopped");
                    break;
                }

                String playerName = playersToPay.get(i);
                    long amount;
                    if (finalIsRange) {
                        long range = maxAmount - minAmount;
                        if (range <= 0) amount = minAmount;
                        else if (range > Long.MAX_VALUE / 2) {
                            amount = minAmount + (long)(Math.random() * range);
                            if (amount > maxAmount) amount = maxAmount;
                        } else amount = minAmount + random.nextLong(range + 1);
                    } else amount = minAmount;
                    
                String command = reverseSyntax 
                    ? String.format("%s %d %s", payCommand, amount, playerName)
                    : String.format("%s %s %d", payCommand, playerName, amount);
                
                    sendPaymentCommand(command, playerName, null);
                
                    if (doubleSend && !shouldStop) {
                        if (doubleSendDelay > 0) {
                        try { Thread.sleep(doubleSendDelay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                    }
                        if (!shouldStop) sendPaymentCommand(command, playerName, "double");
                                }

                    final int idx = i + 1;
                    final long amt = amount;
                    paymentProgress = (float) idx / totalPlayers;
                    addPaymentLog(String.format("[%d/%d] /%s %s %d", idx, totalPlayers, payCommand, playerName, amt));

                    if (i < totalPlayers - 1 && !shouldStop) {
                        try { Thread.sleep(paymentDelay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
            }

            if (!shouldStop) {
                    final int finalTotal = totalPlayers;
                minecraft.execute(() -> {
                        synchronized (paymentLock) { isPaying = false; }
                        paymentProgress = 1.0f;
                        addPaymentLog(String.format("Done! Paid %d players", finalTotal));
                        if (isAutoScan) {
                            synchronized (playerListLock) { tabScanPlayerList.clear(); }
                            isAutoScan = false;
                        }
                });
            } else {
                    minecraft.execute(() -> { 
                        synchronized (paymentLock) { isPaying = false; }
                        paymentProgress = 0.0f;
                        if (isAutoScan) {
                            synchronized (playerListLock) { tabScanPlayerList.clear(); }
                            isAutoScan = false;
                        }
                    });
                }
            } catch (Exception e) {
                PayEveryone.LOGGER.error("Payment failed", e);
                addPaymentLog("Error: Payment failed");
                minecraft.execute(() -> { synchronized (paymentLock) { isPaying = false; } paymentProgress = 0.0f; });
            } finally {
                paymentWorkerThread = null;
            }
        }).exceptionally(t -> {
            PayEveryone.LOGGER.error("Payment async task failed", t);
            addPaymentLog("Error: Async task failed");
            Minecraft.getInstance().execute(() -> { synchronized (paymentLock) { isPaying = false; } paymentProgress = 0.0f; });
            paymentWorkerThread = null;
            return null;
        });
        
        return true;
    }

    public void stopPaying() {
        if ((isPaying || isPaused) && !shouldStop) {
            addPaymentLog("Cancelled");
            shouldStop = true;
            isPaused = false;
            Thread t = paymentWorkerThread;
            if (t != null) {
                try { t.interrupt(); } catch (Throwable ignored) {}
            }
            if (debugMode) {
                PayEveryone.LOGGER.info("Payment stopped by user");
            }
        }
    }
    
    public boolean isAutoConfirmEnabled() { return confirmClickSlot >= 0 && isPaying; }
    
    public void handleContainerOpened(int containerId) {
        if (!isAutoConfirmEnabled()) return;
        
        final int slotToClick = confirmClickSlot;
        final long delay = confirmClickDelay;
        
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                if (!isAutoConfirmEnabled() || confirmClickSlot != slotToClick) return;
                LocalPlayer player = minecraft.player;
                if (player != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryMouseClick(containerId, slotToClick, 0, 
                        net.minecraft.world.inventory.ClickType.PICKUP, player);
                }
            });
        });
    }
}
