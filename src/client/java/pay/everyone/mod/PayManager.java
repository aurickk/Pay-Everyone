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

public class PayManager {
    private static final PayManager INSTANCE = new PayManager();
    private final Set<String> excludedPlayers = new HashSet<>();
    
    /**
     * Parse a number string with optional suffix (k, m, b, t).
     * Examples: 4.9k = 4900, 500k = 500000, 2.4m = 2400000, 1b = 1000000000, 5t = 5000000000000
     */
    public static long parseShortNumber(String input) throws NumberFormatException {
        if (input == null || input.isEmpty()) throw new NumberFormatException("Empty input");
        
        String s = input.trim().toLowerCase();
        long multiplier = 1;
        
        if (s.endsWith("t")) {
            multiplier = 1_000_000_000_000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("b")) {
            multiplier = 1_000_000_000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("m")) {
            multiplier = 1_000_000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("k")) {
            multiplier = 1_000L;
            s = s.substring(0, s.length() - 1);
        }
        
        if (s.isEmpty()) throw new NumberFormatException("No number before suffix");
        
        if (s.contains(".")) {
            double value = Double.parseDouble(s);
            return (long) (value * multiplier);
        } else {
            return Long.parseLong(s) * multiplier;
        }
    }
    
    /**
     * Format a number with suffix for display (k, m, b, t).
     */
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
    private final List<String> tabScanPlayerList = new ArrayList<>();
    private final List<String> manualPlayerList = new ArrayList<>();
    private final Object playerListLock = new Object();
    private final Set<Integer> processedRequestIds = Collections.synchronizedSet(new HashSet<>());
    
    private static final String[] SCAN_PREFIXES = {
        "", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
        "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "_"
    };
    
    private boolean isPaying = false;
    private volatile boolean shouldStop = false;
    private volatile boolean isTabScanning = false;
    private volatile boolean debugMode = false;
    private volatile boolean scanCompleted = false;
    private volatile long scanInterval = 250;
    
    private volatile int confirmClickSlot = -1;
    private volatile long confirmClickDelay = 100;
    private volatile boolean doubleSend = false;
    private volatile long doubleSendDelay = 0;
    private volatile boolean reverseSyntax = false;
    private volatile boolean tabScanEnabled = true;
    
    private volatile String pendingAmount = null;
    private volatile long pendingDelay = 1000;
    private volatile boolean pendingAutoMode = false;
    private volatile Runnable pendingConfirmationCallback = null;
    
    private volatile String payCommand = "pay";
    private volatile String lastDiscoveryMethod = "tab list";
    
    private volatile boolean waitingForCommandCheck = false;
    private volatile boolean commandQueryable = false;
    private static final int COMMAND_CHECK_REQUEST_ID = 99998;

    private PayManager() {}

    public static PayManager getInstance() { return INSTANCE; }
    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean enabled) { this.debugMode = enabled; }
    public String getPayCommand() { return payCommand; }
    public String getLastDiscoveryMethod() { return lastDiscoveryMethod; }
    public boolean isTabScanning() { return isTabScanning; }
    public boolean isPaying() { return isPaying; }
    public int getExclusionCount() { return excludedPlayers.size(); }
    public int getConfirmClickSlot() { return confirmClickSlot; }
    public long getConfirmClickDelay() { return confirmClickDelay; }
    public boolean isDoubleSendEnabled() { return doubleSend; }
    public long getDoubleSendDelay() { return doubleSendDelay; }
    public int getOnlinePlayerCount() { return getOnlinePlayers().size(); }
    
    public void setPayCommand(String command) {
        if (command.startsWith("/")) command = command.substring(1);
        this.payCommand = command.trim();
    }
    
    public void setConfirmClickSlot(int slotId) { this.confirmClickSlot = slotId; }
    public void setConfirmClickDelay(long delayMs) { this.confirmClickDelay = Math.max(50, delayMs); }
    public void setDoubleSend(boolean enabled) { this.doubleSend = enabled; }
    public void setDoubleSendDelay(long delayMs) { this.doubleSendDelay = Math.max(0, delayMs); }
    
    public void setReverseSyntax(boolean enabled) { this.reverseSyntax = enabled; }
    public boolean isReverseSyntaxEnabled() { return reverseSyntax; }
    
    public void setTabScanEnabled(boolean enabled) { this.tabScanEnabled = enabled; }
    public boolean isTabScanEnabled() { return tabScanEnabled; }

    public boolean stopTabScan() {
        if (isTabScanning) {
            isTabScanning = false;
            scanCompleted = true;
            return true;
        }
        return false;
    }

    public void addExcludedPlayers(String... players) {
        for (String player : players) {
            excludedPlayers.add(player.toLowerCase());
        }
    }

    public AddPlayersResult addManualPlayers(String... players) {
        List<String> added = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        synchronized (playerListLock) {
            for (String player : players) {
                String cleaned = player.trim();
                if (!cleaned.isEmpty()) {
                    if (manualPlayerList.contains(cleaned)) {
                        duplicates.add(cleaned);
                    } else {
                        manualPlayerList.add(cleaned);
                        added.add(cleaned);
                    }
                }
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

    public void clearManualPlayers() {
        synchronized (playerListLock) { manualPlayerList.clear(); }
    }

    public List<String> removeManualPlayers(String[] playerNames) {
        List<String> removed = new ArrayList<>();
        synchronized (playerListLock) {
            for (String name : playerNames) {
                String cleanName = name.trim();
                if (!cleanName.isEmpty() && manualPlayerList.remove(cleanName)) {
                    removed.add(cleanName);
                }
            }
        }
        return removed;
    }

    public int getManualPlayerCount() {
        synchronized (playerListLock) { return manualPlayerList.size(); }
    }

    public void clearExclusions() { excludedPlayers.clear(); }

    public List<String> removeExcludedPlayers(String[] playerNames) {
        List<String> removed = new ArrayList<>();
        for (String name : playerNames) {
            String cleanName = name.trim().toLowerCase();
            if (!cleanName.isEmpty() && excludedPlayers.remove(cleanName)) {
                removed.add(name.trim());
            }
        }
        return removed;
    }

    public void clearTabScanList() {
        synchronized (playerListLock) { tabScanPlayerList.clear(); }
    }
    
    public void clearAllPlayerLists() {
        synchronized (playerListLock) {
            manualPlayerList.clear();
            tabScanPlayerList.clear();
        }
        excludedPlayers.clear();
        lastDiscoveryMethod = "tab list";
        pendingAmount = null;
    }
    
    public void resetAllSettings() {
        synchronized (playerListLock) {
            manualPlayerList.clear();
            tabScanPlayerList.clear();
        }
        excludedPlayers.clear();
        
        confirmClickSlot = -1;
        confirmClickDelay = 100;
        doubleSend = false;
        doubleSendDelay = 0;
        reverseSyntax = false;
        tabScanEnabled = true;
        payCommand = "pay";
        debugMode = false;
        scanInterval = 250;
        pendingAmount = null;
        pendingAutoMode = false;
        lastDiscoveryMethod = "tab list";
    }

    public void queryPlayersViaTabComplete() { queryPlayersViaTabComplete(250); }
    
    public void queryPlayersViaTabComplete(long intervalMs) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        
        if (player == null || player.connection == null) return;
        
        if (isTabScanning) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§c[Pay Everyone] Tab scan already in progress..."), false);
            return;
        }
        
        scanInterval = intervalMs;
        isTabScanning = true;
        scanCompleted = false;
        processedRequestIds.clear();
        
        synchronized (playerListLock) { tabScanPlayerList.clear(); }
        
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            String.format("§e[Pay Everyone] Starting tab scan (%d prefixes, %dms interval)...", 
                SCAN_PREFIXES.length, scanInterval)), false);
        
        CompletableFuture.runAsync(this::runSequentialScan);
    }

    private String getPrefixRangeDisplay(int startIndex, int endIndex) {
        if (startIndex < 0 || startIndex >= SCAN_PREFIXES.length) return "";
        endIndex = Math.min(endIndex, SCAN_PREFIXES.length - 1);
        String start = SCAN_PREFIXES[startIndex].isEmpty() ? "∅" : SCAN_PREFIXES[startIndex];
        String end = SCAN_PREFIXES[endIndex].isEmpty() ? "∅" : SCAN_PREFIXES[endIndex];
        return start.equals(end) ? "[" + start + "]" : "[" + start + "-" + end + "]";
    }

    private void runSequentialScan() {
        Minecraft minecraft = Minecraft.getInstance();
        final String currentPayCommand = payCommand;
        final boolean isReverse = reverseSyntax;
        int rangeStartCount = 0;
        
        for (int i = 0; i < SCAN_PREFIXES.length && isTabScanning; i++) {
            final int index = i;
            String prefix = SCAN_PREFIXES[i];
            int requestId = 10000 + i;
            
            if (i % 5 == 0) {
                synchronized (playerListLock) { rangeStartCount = tabScanPlayerList.size(); }
            }
            
            minecraft.execute(() -> {
                LocalPlayer player = minecraft.player;
                if (player != null && player.connection != null) {
                    String command = isReverse 
                        ? "/" + currentPayCommand + " 1 " + prefix
                        : "/" + currentPayCommand + " " + prefix;
                    player.connection.send(new ServerboundCommandSuggestionPacket(requestId, command));
                    
                    if (debugMode) {
                        String debugPrefix = prefix.isEmpty() ? "(empty)" : prefix;
                        String debugCmd = isReverse ? String.format("/%s 1 %s", currentPayCommand, debugPrefix) : String.format("/%s %s", currentPayCommand, debugPrefix);
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§8[Debug] [%d/%d] Sent: %s", index + 1, SCAN_PREFIXES.length, debugCmd)), false);
                    }
                }
            });
            
            try { Thread.sleep(scanInterval); } catch (InterruptedException e) { break; }
            
            if (i % 5 == 0 || i == SCAN_PREFIXES.length - 1) {
                final int progress = (i + 1) * 100 / SCAN_PREFIXES.length;
                final int currentCount;
                synchronized (playerListLock) { currentCount = tabScanPlayerList.size(); }
                final int rangeFound = currentCount - rangeStartCount;
                final int rangeStart = (i / 5) * 5;
                final int rangeEnd = Math.min(rangeStart + 4, SCAN_PREFIXES.length - 1);
                final String rangeDisplay = getPrefixRangeDisplay(rangeStart, rangeEnd);
                
                minecraft.execute(() -> {
                    LocalPlayer player = minecraft.player;
                    if (player != null) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§e[Pay Everyone] Scanning %s... %d%% - Found %d players", 
                                rangeDisplay, progress, rangeFound)), false);
                    }
                });
            }
        }
        finishScan();
    }

    private void finishScan() {
        if (scanCompleted) return;
        scanCompleted = true;
        isTabScanning = false;
        
        Minecraft minecraft = Minecraft.getInstance();
        int totalPlayers;
        synchronized (playerListLock) { totalPlayers = tabScanPlayerList.size(); }
        
        if (totalPlayers == 0) {
            lastDiscoveryMethod = "tab list";
            minecraft.execute(() -> {
                LocalPlayer player = minecraft.player;
                if (player != null) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§e[Pay Everyone] Tab scan found no players. Using tab list..."), false);
                }
            });
        } else {
            lastDiscoveryMethod = "tabscan";
            minecraft.execute(() -> {
                LocalPlayer player = minecraft.player;
                if (player != null) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        String.format("§a[Pay Everyone] Tab scan complete! Found %d players.", totalPlayers)), false);
                }
            });
        }
        
        if (pendingConfirmationCallback != null) {
            Runnable callback = pendingConfirmationCallback;
            pendingConfirmationCallback = null;
            minecraft.execute(callback);
            return;
        }
        
        if (pendingAmount != null) {
            String amount = pendingAmount;
            long delay = pendingDelay;
            boolean autoMode = pendingAutoMode;
            pendingAmount = null;
            pendingAutoMode = false;
            minecraft.execute(() -> payAll(amount, delay, autoMode));
        }
    }
    
    public CompletableFuture<Boolean> checkCommandQueryable() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        
        if (player == null || player.connection == null) {
            future.complete(false);
            return future;
        }
        
        waitingForCommandCheck = true;
        commandQueryable = false;
        
        final boolean isReverse = reverseSyntax;
        minecraft.execute(() -> {
            String checkCommand = isReverse 
                ? "/" + payCommand + " 1 "
                : "/" + payCommand + " ";
            player.connection.send(new ServerboundCommandSuggestionPacket(COMMAND_CHECK_REQUEST_ID, checkCommand));
        });
        
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            waitingForCommandCheck = false;
            future.complete(commandQueryable);
        });
        
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
        
        if (hasManualPlayers) {
            lastDiscoveryMethod = "manual add";
            if (onConfirmationReady != null) onConfirmationReady.run();
            return;
        }
        
        if (hasTabScanPlayers) {
            lastDiscoveryMethod = "tabscan (cached)";
            if (onConfirmationReady != null) onConfirmationReady.run();
            return;
        }
        
        pendingAmount = amountOrRange;
        pendingDelay = delayMs;
        
        if (!tabScanEnabled) {
            lastDiscoveryMethod = "tab list";
            minecraft.execute(() -> {
                LocalPlayer p = minecraft.player;
                if (p != null) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        String.format("§e[Pay Everyone] Tab scan disabled. Using tab list...", payCommand)), false);
                }
            });
            if (onConfirmationReady != null) {
                CompletableFuture.runAsync(() -> {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    minecraft.execute(onConfirmationReady);
                });
            }
            return;
        }
        
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            String.format("§e[Pay Everyone] Checking if /%s can be queried...", payCommand)), false);
        
        checkCommandQueryable().thenAccept(queryable -> {
            if (queryable) {
                minecraft.execute(() -> {
                    LocalPlayer p = minecraft.player;
                    if (p != null) {
                        p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§6[Pay Everyone] ⚠ STAY STILL! Scanning players via /%s...", payCommand)), false);
                    }
                });
                pendingConfirmationCallback = onConfirmationReady;
                lastDiscoveryMethod = "tabscan";
                queryPlayersViaTabComplete(250);
            } else {
                lastDiscoveryMethod = "tab list";
                minecraft.execute(() -> {
                    LocalPlayer p = minecraft.player;
                    if (p != null) {
                        p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§e[Pay Everyone] /%s cannot be queried. Using tab list...", payCommand)), false);
                    }
                });
                if (onConfirmationReady != null) {
                    CompletableFuture.runAsync(() -> {
                        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
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
        
        boolean isOurRequest = (requestId >= 10000 && requestId < 10000 + SCAN_PREFIXES.length);
        if (!isOurRequest || !processedRequestIds.add(requestId)) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        
        int prefixIndex = requestId - 10000;
        String prefix = (prefixIndex >= 0 && prefixIndex < SCAN_PREFIXES.length) 
            ? (SCAN_PREFIXES[prefixIndex].isEmpty() ? "(empty)" : SCAN_PREFIXES[prefixIndex]) 
            : "(unknown)";
        
        int newPlayers, afterCount;
        synchronized (playerListLock) {
            int beforeCount = tabScanPlayerList.size();
            for (String suggestion : suggestions) {
                String cleaned = suggestion.trim().replaceAll("§[0-9a-fk-or]", "");
                if (!cleaned.isEmpty() && !tabScanPlayerList.contains(cleaned)) {
                    tabScanPlayerList.add(cleaned);
                }
            }
            afterCount = tabScanPlayerList.size();
            newPlayers = afterCount - beforeCount;
        }
        
        if (player != null && debugMode) {
            final String finalPrefix = prefix;
            final int finalNew = newPlayers, finalTotal = afterCount, finalSuggestions = suggestions.size();
            minecraft.execute(() -> {
                LocalPlayer p = minecraft.player;
                if (p != null) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        String.format("§7[Debug] '%s': %d suggestions, +%d new (total: %d)", 
                            finalPrefix, finalSuggestions, finalNew, finalTotal)), false);
                }
            });
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
                if (name != null) allPlayers.add(name);
            }
        }
        
        synchronized (playerListLock) {
            allPlayers.addAll(tabScanPlayerList);
            allPlayers.addAll(manualPlayerList);
        }
        
        allPlayers.remove(localPlayerName);
        return new ArrayList<>(allPlayers);
    }

    public boolean isExcluded(String playerName) {
        return excludedPlayers.contains(playerName.toLowerCase());
    }

    public String getDebugPlayerLists() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        int tabListCount = connection != null ? connection.getOnlinePlayers().size() : 0;
        
        synchronized (playerListLock) {
            return String.format(
                "§6=== Pay Everyone Debug ===\n§e Tab List: §f%d\n§e Tab Scan: §f%d\n§e Manual: §f%d\n§e Excluded: §f%d\n§e Total (payment): §f%d",
                tabListCount, tabScanPlayerList.size(), manualPlayerList.size(), excludedPlayers.size(), getOnlinePlayers().size());
        }
    }

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
                        if (name != null) result.add(name);
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
                    if (name != null) sourceList.add(name);
                }
            }
        }
        
        return sourceList.stream()
                .filter(name -> !name.equals(localPlayerName))
                .filter(name -> !excludedPlayers.contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public boolean payAll(String amountOrRange, long delayMs) {
        return payAll(amountOrRange, delayMs, false);
    }
    
    public boolean payAll(String amountOrRange, long delayMs, boolean autoMode) {
        if (isPaying) return false;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return false;

        List<String> playersToPay = new ArrayList<>(getOnlinePlayers());
        if (playersToPay.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Pay Everyone] No players to pay!"), false);
            return false;
        }
        
        Collections.shuffle(playersToPay);

        boolean isRange = amountOrRange.contains("-");
        long parsedMinAmount = 0, parsedMaxAmount = 0;
        
        if (isRange) {
            String[] parts = amountOrRange.split("-");
            if (parts.length != 2) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Pay Everyone] Invalid range! Use: min-max"), false);
                return false;
            }
            try {
                parsedMinAmount = parseShortNumber(parts[0]);
                parsedMaxAmount = parseShortNumber(parts[1]);
                if (parsedMinAmount < 1 || parsedMaxAmount < parsedMinAmount) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Pay Everyone] Invalid range values!"), false);
                    return false;
                }
            } catch (NumberFormatException e) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Pay Everyone] Invalid range format! Use: 1k-5k, 100-500, etc."), false);
                return false;
            }
        } else {
            try {
                parsedMinAmount = parseShortNumber(amountOrRange);
                parsedMaxAmount = parsedMinAmount;
                if (parsedMinAmount < 1) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Pay Everyone] Amount must be >= 1"), false);
                    return false;
                }
            } catch (NumberFormatException e) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Pay Everyone] Invalid amount! Use: 500, 4.9k, 2.5m, 1b, etc."), false);
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

        isPaying = true;
        shouldStop = false;
        
        String amountDisplay = finalIsRange ? String.format("%d-%d", minAmount, maxAmount) : String.valueOf(minAmount);
        String autoModeText = autoMode ? String.format(" (auto: %s ÷ %d)", amountOrRange, playerCount) : "";
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            String.format("§a[Pay Everyone] Paying %d players %s%s with %dms delay", 
                playersToPay.size(), amountDisplay, autoModeText, delayMs)), false);

        Random random = new Random();
        CompletableFuture.runAsync(() -> {
            final int[] paidCount = {0};
            for (int i = 0; i < playersToPay.size(); i++) {
                if (shouldStop) {
                    minecraft.execute(() -> player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        String.format("§c[Pay Everyone] Stopped! Paid %d/%d", paidCount[0], playersToPay.size())), false));
                    break;
                }

                String playerName = playersToPay.get(i);
                long amount = finalIsRange ? (minAmount + random.nextLong(maxAmount - minAmount + 1)) : minAmount;
                String command = reverseSyntax 
                    ? String.format("%s %d %s", payCommand, amount, playerName)
                    : String.format("%s %s %d", payCommand, playerName, amount);
                
                minecraft.execute(() -> {
                    if (player.connection != null) {
                        try {
                            player.connection.send(new ServerboundChatCommandPacket(command));
                        } catch (Exception e) {
                            player.connection.sendCommand(command);
                        }
                    }
                });
                
                if (doubleSend) {
                    if (doubleSendDelay > 0 && !shouldStop) {
                        try { Thread.sleep(doubleSendDelay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                    }
                    if (!shouldStop) {
                        minecraft.execute(() -> {
                            if (player.connection != null) {
                                try {
                                    player.connection.send(new ServerboundChatCommandPacket(command));
                                } catch (Exception e) {
                                    player.connection.sendCommand(command);
                                }
                            }
                        });
                    }
                }

                paidCount[0]++;
                final int currentIndex = i + 1;
                final long finalAmount = amount;
                final boolean isReverse = reverseSyntax;
                minecraft.execute(() -> {
                    String cmdDisplay = isReverse 
                        ? String.format("/%s %d %s", payCommand, finalAmount, playerName)
                        : String.format("/%s %s %d", payCommand, playerName, finalAmount);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        String.format("§e[Pay Everyone] %s (%d/%d)", cmdDisplay, currentIndex, playersToPay.size())), false);
                });

                if (i < playersToPay.size() - 1 && !shouldStop) {
                    try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
            }

            if (!shouldStop) {
                minecraft.execute(() -> {
                    isPaying = false;
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        String.format("§a[Pay Everyone] Done! Paid %d players.", playersToPay.size())), false);
                });
            } else {
                minecraft.execute(() -> isPaying = false);
            }
        });
        
        return true;
    }

    public void stopPaying() {
        if (isPaying && !shouldStop) {
            shouldStop = true;
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[Pay Everyone] Stopping..."), false);
            }
        }
    }
    
    public boolean isAutoConfirmEnabled() {
        return confirmClickSlot >= 0 && isPaying;
    }
    
    public void handleContainerOpened(int containerId) {
        if (!isAutoConfirmEnabled()) return;
        
        final int slotToClick = confirmClickSlot;
        final long delay = confirmClickDelay;
        
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(delay); } catch (InterruptedException e) { return; }
            
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                LocalPlayer player = minecraft.player;
                if (player != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryMouseClick(containerId, slotToClick, 0, 
                        net.minecraft.world.inventory.ClickType.PICKUP, player);
                    
                    if (debugMode) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            String.format("§7[Debug] Auto-clicked slot %d", slotToClick)), false);
                    }
                }
            });
        });
    }
}
