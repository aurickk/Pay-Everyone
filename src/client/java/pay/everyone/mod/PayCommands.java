package pay.everyone.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PayCommands {
    private static final PayManager payManager = PayManager.getInstance();
    private static String pendingAmount = null;
    private static long pendingDelay = 1000;
    private static boolean pendingAutoMode = false;
    
    private static final SuggestionProvider<FabricClientCommandSource> PLAYER_SUGGESTIONS = (context, builder) -> {
        List<String> players = payManager.getPlayersForAutocomplete();
        String input = builder.getRemaining();
        String[] parts = input.split("\\s+");
        String lastPart = parts.length > 0 ? parts[parts.length - 1].toLowerCase() : "";
        
        if (input.endsWith(" ") || input.isEmpty()) {
            lastPart = "";
        }
        
        int startPos = builder.getStart();
        if (parts.length > 1 || (parts.length == 1 && input.endsWith(" "))) {
            int lastSpaceIndex = input.lastIndexOf(' ');
            if (lastSpaceIndex >= 0) {
                startPos = builder.getStart() + lastSpaceIndex + 1;
            }
        }
        
        var newBuilder = builder.createOffset(startPos);
        for (String player : players) {
            if (player.toLowerCase().startsWith(lastPart) && !payManager.isExcluded(player)) {
                newBuilder.suggest(player);
            }
        }
        return newBuilder.buildFuture();
    };
    
    private static int checkNotBusy(CommandContext<FabricClientCommandSource> context) {
        if (payManager.isPaying()) {
            context.getSource().sendError(Component.literal("§c[Pay Everyone] Payment in progress! Use '/payall stop' first."));
            return 0;
        }
        if (payManager.isTabScanning()) {
            context.getSource().sendError(Component.literal("§c[Pay Everyone] Tab scan in progress! Use '/payall tabscan stop'."));
            return 0;
        }
        return 1;
    }
    
    private static void showConfirmationAndPay(FabricClientCommandSource source, String amountOrRange, long delayMs, boolean autoMode) {
        int playerCount = payManager.getOnlinePlayerCount();
        int excludedCount = payManager.getExclusionCount();
        int addedCount = payManager.getManualPlayerCount();
        String method = payManager.getLastDiscoveryMethod();
        String payCommand = payManager.getPayCommand();
        int confirmSlot = payManager.getConfirmClickSlot();
        boolean doubleSend = payManager.isDoubleSendEnabled();
        long doubleSendDelay = payManager.getDoubleSendDelay();
        boolean reverseSyntax = payManager.isReverseSyntaxEnabled();
        
        source.sendFeedback(Component.literal("§6========== Payment Confirmation =========="));
        source.sendFeedback(Component.literal(String.format("§e  Player source: §f%s", method)));
        String syntaxExample = reverseSyntax 
            ? String.format("/%s <amount> <player>", payCommand)
            : String.format("/%s <player> <amount>", payCommand);
        source.sendFeedback(Component.literal(String.format("§e  Pay command: §f/%s §7(%s)", payCommand, syntaxExample)));
        source.sendFeedback(Component.literal(String.format("§e  Players to pay: §f%d", playerCount)));
        source.sendFeedback(Component.literal(String.format("§e  Players excluded: §f%d", excludedCount)));
        source.sendFeedback(Component.literal(String.format("§e  Players added: §f%d", addedCount)));
        source.sendFeedback(Component.literal(String.format("§e  Auto-confirm slot: §f%s", confirmSlot >= 0 ? String.valueOf(confirmSlot) : "disabled")));
        
        if (doubleSend) {
            String delayText = doubleSendDelay > 0 ? String.format("(%dms delay)", doubleSendDelay) : "(no delay)";
            source.sendFeedback(Component.literal(String.format("§e  Double send: §fenabled %s", delayText)));
        } else {
            source.sendFeedback(Component.literal("§e  Double send: §fdisabled"));
        }
        source.sendFeedback(Component.literal(String.format("§e  Reverse syntax: §f%s", reverseSyntax ? "enabled" : "disabled")));
        
        if (autoMode && playerCount > 0) {
            try {
                long totalAmount = PayManager.parseShortNumber(amountOrRange);
                long perPlayerAmount = totalAmount / playerCount;
                source.sendFeedback(Component.literal(String.format("§e  Total amount: §f%s §7(%s)", amountOrRange, PayManager.formatShortNumber(totalAmount))));
                source.sendFeedback(Component.literal(String.format("§e  Per player: §f%s §7(%s ÷ %d)", PayManager.formatShortNumber(perPlayerAmount), PayManager.formatShortNumber(totalAmount), playerCount)));
            } catch (NumberFormatException e) {
                source.sendFeedback(Component.literal(String.format("§e  Amount: §f%s §7(auto mode)", amountOrRange)));
            }
        } else {
            try {
                long amount = PayManager.parseShortNumber(amountOrRange);
                if (!amountOrRange.matches("\\d+")) {
                    source.sendFeedback(Component.literal(String.format("§e  Amount: §f%s §7(%s)", amountOrRange, PayManager.formatShortNumber(amount))));
                } else {
                    source.sendFeedback(Component.literal(String.format("§e  Amount: §f%s", amountOrRange)));
                }
            } catch (NumberFormatException e) {
                source.sendFeedback(Component.literal(String.format("§e  Amount: §f%s", amountOrRange)));
            }
        }
        source.sendFeedback(Component.literal(String.format("§e  Delay: §f%dms", delayMs)));
        source.sendFeedback(Component.literal("§6=========================================="));
        
        if (playerCount == 0) {
            source.sendError(Component.literal("§c[Pay Everyone] No players to pay!"));
            pendingAmount = null;
            pendingAutoMode = false;
            return;
        }
        
        source.sendFeedback(Component.literal("§a[Pay Everyone] Type §f/payall confirm §ato continue."));
        pendingAmount = amountOrRange;
        pendingDelay = delayMs;
        pendingAutoMode = autoMode;
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("payall")
                .then(ClientCommandManager.argument("amount", StringArgumentType.string())
                        .executes(context -> {
                            if (checkNotBusy(context) == 0) return 0;
                            String amountOrRange = StringArgumentType.getString(context, "amount");
                            payManager.startPaymentWithScan(amountOrRange, 1000, () -> 
                                showConfirmationAndPay(context.getSource(), amountOrRange, 1000, false));
                            return 1;
                        })
                        .then(ClientCommandManager.literal("auto")
                                .executes(context -> {
                                    if (checkNotBusy(context) == 0) return 0;
                                    String amountOrRange = StringArgumentType.getString(context, "amount");
                                    payManager.startPaymentWithScan(amountOrRange, 1000, () -> 
                                        showConfirmationAndPay(context.getSource(), amountOrRange, 1000, true));
                                    return 1;
                                })
                                .then(ClientCommandManager.argument("delay", LongArgumentType.longArg(0))
                                        .executes(context -> {
                                            if (checkNotBusy(context) == 0) return 0;
                                            String amountOrRange = StringArgumentType.getString(context, "amount");
                                            long delay = LongArgumentType.getLong(context, "delay");
                                            payManager.startPaymentWithScan(amountOrRange, delay, () -> 
                                                showConfirmationAndPay(context.getSource(), amountOrRange, delay, true));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.argument("delay", LongArgumentType.longArg(0))
                                .executes(context -> {
                                    if (checkNotBusy(context) == 0) return 0;
                                    String amountOrRange = StringArgumentType.getString(context, "amount");
                                    long delay = LongArgumentType.getLong(context, "delay");
                                    payManager.startPaymentWithScan(amountOrRange, delay, () -> 
                                        showConfirmationAndPay(context.getSource(), amountOrRange, delay, false));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("confirm")
                        .executes(context -> {
                            if (pendingAmount == null) {
                                context.getSource().sendError(Component.literal("§c[Pay Everyone] No pending payment. Run '/payall <amount>' first."));
                                return 0;
                            }
                            if (payManager.isPaying()) {
                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Payment already in progress!"));
                                return 0;
                            }
                            String amount = pendingAmount;
                            long delay = pendingDelay;
                            boolean autoMode = pendingAutoMode;
                            pendingAmount = null;
                            pendingAutoMode = false;
                            
                            if (!payManager.payAll(amount, delay, autoMode)) {
                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Failed to start payment."));
                                return 0;
                            }
                            return 1;
                        }))
                .then(ClientCommandManager.literal("command")
                        .executes(context -> {
                            context.getSource().sendFeedback(Component.literal(
                                String.format("§6[Pay Everyone] Current pay command: §f/%s", payManager.getPayCommand())));
                            context.getSource().sendFeedback(Component.literal("§7  Use '/payall command <command>' to change it"));
                            return 1;
                        })
                        .then(ClientCommandManager.argument("cmd", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String cmd = StringArgumentType.getString(context, "cmd");
                                    payManager.setPayCommand(cmd);
                                    context.getSource().sendFeedback(Component.literal(
                                        String.format("§a[Pay Everyone] Pay command set to: §f/%s", payManager.getPayCommand())));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("exclude")
                        .then(ClientCommandManager.argument("players", StringArgumentType.greedyString())
                                .suggests(PLAYER_SUGGESTIONS)
                                .executes(context -> {
                                    String playersArg = StringArgumentType.getString(context, "players");
                                    String[] players = playersArg.split("\\s+");
                                    if (players.length == 0 || (players.length == 1 && players[0].isEmpty())) {
                                        context.getSource().sendError(Component.literal("§c[Pay Everyone] Specify at least one player"));
                                        return 0;
                                    }
                                    payManager.addExcludedPlayers(players);
                                    context.getSource().sendFeedback(Component.literal(
                                        String.format("§a[Pay Everyone] Excluded %d player(s): %s", players.length, String.join(", ", players))));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("clear")
                        .then(ClientCommandManager.literal("exclude")
                                .executes(context -> {
                                    payManager.clearExclusions();
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Cleared all exclusions"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("add")
                                .executes(context -> {
                                    payManager.clearManualPlayers();
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Cleared manually added players"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("tabscan")
                                .executes(context -> {
                                    payManager.clearTabScanList();
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Cleared tab scan results"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("all")
                                .executes(context -> {
                                    payManager.clearExclusions();
                                    payManager.clearManualPlayers();
                                    payManager.clearTabScanList();
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Cleared all lists"));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("players", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String playersArg = StringArgumentType.getString(context, "players");
                                    String[] players = playersArg.split("[, ]+");
                                    if (players.length == 0 || (players.length == 1 && players[0].isEmpty())) {
                                        context.getSource().sendError(Component.literal("§c[Pay Everyone] Specify at least one player"));
                                        return 0;
                                    }
                                    PayManager.AddPlayersResult result = payManager.addManualPlayers(players);
                                    if (!result.added.isEmpty()) {
                                        context.getSource().sendFeedback(Component.literal(
                                            String.format("§a[Pay Everyone] Added %d player(s): %s", result.added.size(), String.join(", ", result.added))));
                                    }
                                    if (!result.duplicates.isEmpty()) {
                                        context.getSource().sendFeedback(Component.literal(
                                            String.format("§e[Pay Everyone] Skipped %d duplicate(s): %s", result.duplicates.size(), String.join(", ", result.duplicates))));
                                    }
                                    if (result.added.isEmpty() && result.duplicates.isEmpty()) {
                                        context.getSource().sendError(Component.literal("§c[Pay Everyone] No valid player names"));
                                        return 0;
                                    }
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("stop")
                        .executes(context -> {
                            boolean stopped = false;
                            if (payManager.isPaying()) {
                                payManager.stopPaying();
                                context.getSource().sendFeedback(Component.literal("§c[Pay Everyone] Stopping payment..."));
                                stopped = true;
                            }
                            if (payManager.isTabScanning()) {
                                payManager.stopTabScan();
                                context.getSource().sendFeedback(Component.literal("§c[Pay Everyone] Stopping tab scan..."));
                                stopped = true;
                            }
                            if (pendingAmount != null) {
                                pendingAmount = null;
                                pendingAutoMode = false;
                                context.getSource().sendFeedback(Component.literal("§c[Pay Everyone] Pending payment cancelled."));
                                stopped = true;
                            }
                            if (!stopped) {
                                context.getSource().sendFeedback(Component.literal("§e[Pay Everyone] Nothing to stop."));
                            }
                            return 1;
                        }))
                .then(ClientCommandManager.literal("tabscan")
                        .then(ClientCommandManager.literal("on")
                                .executes(context -> {
                                    payManager.setTabScanEnabled(true);
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Tab scan enabled! /payall will use tab scan when available."));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("off")
                                .executes(context -> {
                                    payManager.setTabScanEnabled(false);
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Tab scan disabled! /payall will skip tab scan and use tab list."));
                                    return 1;
                                }))
                        .executes(context -> {
                            boolean isEnabled = payManager.isTabScanEnabled();
                            if (isEnabled) {
                                context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Tab scan: §aENABLED"));
                                context.getSource().sendFeedback(Component.literal("§7  /payall will use tab scan when available"));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall tabscan off' to disable"));
                            } else {
                                context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Tab scan: §cDISABLED"));
                                context.getSource().sendFeedback(Component.literal("§7  /payall will skip tab scan and use tab list"));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall tabscan on' to enable"));
                            }
                            
                            if (payManager.isTabScanning()) {
                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Tab scan already in progress!"));
                                return 0;
                            }
                            context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] ⚠ Stay still during tab scan!"));
                            payManager.queryPlayersViaTabComplete();
                            return 1;
                        })
                        .then(ClientCommandManager.argument("interval", LongArgumentType.longArg(50, 5000))
                                .executes(context -> {
                                    if (payManager.isTabScanning()) {
                                        context.getSource().sendError(Component.literal("§c[Pay Everyone] Tab scan already in progress!"));
                                        return 0;
                                    }
                                    context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] ⚠ Stay still during tab scan!"));
                                    payManager.queryPlayersViaTabComplete(LongArgumentType.getLong(context, "interval"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("stop")
                                .executes(context -> {
                                    if (payManager.stopTabScan()) {
                                        context.getSource().sendFeedback(Component.literal("§c[Pay Everyone] Tab scan stopped."));
                                    } else {
                                        context.getSource().sendFeedback(Component.literal("§e[Pay Everyone] No tab scan in progress."));
                                    }
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("debug")
                                .executes(context -> {
                                    boolean newState = !payManager.isDebugMode();
                                    payManager.setDebugMode(newState);
                                    context.getSource().sendFeedback(Component.literal(
                                        String.format("§6[Pay Everyone] Debug: %s", newState ? "§aENABLED" : "§cDISABLED")));
                                    return 1;
                                })
                                .then(ClientCommandManager.literal("true")
                                        .executes(context -> {
                                            payManager.setDebugMode(true);
                                            context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Debug: §aENABLED"));
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("false")
                                        .executes(context -> {
                                            payManager.setDebugMode(false);
                                            context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Debug: §cDISABLED"));
                                            return 1;
                                        }))))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.literal("exclude")
                                .then(ClientCommandManager.argument("players", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String playersArg = StringArgumentType.getString(context, "players");
                                            String[] players = playersArg.split("\\s+");
                                            if (players.length == 0 || (players.length == 1 && players[0].isEmpty())) {
                                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Specify at least one player"));
                                                return 0;
                                            }
                                            List<String> removed = payManager.removeExcludedPlayers(players);
                                            if (removed.isEmpty()) {
                                                context.getSource().sendFeedback(Component.literal("§e[Pay Everyone] None found in exclusion list"));
                                            } else {
                                                context.getSource().sendFeedback(Component.literal(
                                                    String.format("§a[Pay Everyone] Removed %d from exclusions: %s", removed.size(), String.join(", ", removed))));
                                            }
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("players", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String playersArg = StringArgumentType.getString(context, "players");
                                            String[] players = playersArg.split("\\s+");
                                            if (players.length == 0 || (players.length == 1 && players[0].isEmpty())) {
                                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Specify at least one player"));
                                                return 0;
                                            }
                                            List<String> removed = payManager.removeManualPlayers(players);
                                            if (removed.isEmpty()) {
                                                context.getSource().sendFeedback(Component.literal("§e[Pay Everyone] None found in add list"));
                                            } else {
                                                context.getSource().sendFeedback(Component.literal(
                                                    String.format("§a[Pay Everyone] Removed %d from add list: %s", removed.size(), String.join(", ", removed))));
                                            }
                                            return 1;
                                        }))))
                .then(ClientCommandManager.literal("list")
                        .executes(context -> {
                            for (String line : payManager.getDebugPlayerLists().split("\n")) {
                                context.getSource().sendFeedback(Component.literal(line));
                            }
                            return 1;
                        })
                        .then(ClientCommandManager.literal("tabscan")
                                .executes(context -> {
                                    List<String> players = payManager.getPlayerListSample("tabscan", 200);
                                    context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Tab Scan Players (max 200):"));
                                    context.getSource().sendFeedback(Component.literal(players.isEmpty() ? "§7  (empty)" : "§f  " + String.join(", ", players)));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("add")
                                .executes(context -> {
                                    List<String> players = payManager.getPlayerListSample("add", 200);
                                    context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Manually Added (max 200):"));
                                    context.getSource().sendFeedback(Component.literal(players.isEmpty() ? "§7  (empty)" : "§f  " + String.join(", ", players)));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("exclude")
                                .executes(context -> {
                                    List<String> players = payManager.getPlayerListSample("exclude", 200);
                                    context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Excluded (max 200):"));
                                    context.getSource().sendFeedback(Component.literal(players.isEmpty() ? "§7  (empty)" : "§f  " + String.join(", ", players)));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("tablist")
                                .executes(context -> {
                                    List<String> players = payManager.getPlayerListSample("tablist", 200);
                                    context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Tab List (max 200):"));
                                    context.getSource().sendFeedback(Component.literal(players.isEmpty() ? "§7  (empty)" : "§f  " + String.join(", ", players)));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("confirmclickslot")
                        .executes(context -> {
                            int currentSlot = payManager.getConfirmClickSlot();
                            if (currentSlot < 0) {
                                context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Auto-confirm: §cDISABLED"));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall confirmclickslot <slot>' to enable"));
                            } else {
                                context.getSource().sendFeedback(Component.literal(
                                    String.format("§6[Pay Everyone] Auto-confirm: §aENABLED §7(slot %d, %dms)", currentSlot, payManager.getConfirmClickDelay())));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall confirmclickslot off' to disable"));
                            }
                            return 1;
                        })
                        .then(ClientCommandManager.literal("off")
                                .executes(context -> {
                                    payManager.setConfirmClickSlot(-1);
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Auto-confirm disabled"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.argument("slotid", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    int slotId = IntegerArgumentType.getInteger(context, "slotid");
                                    payManager.setConfirmClickSlot(slotId);
                                    context.getSource().sendFeedback(Component.literal(
                                        String.format("§a[Pay Everyone] Auto-confirm slot %d enabled", slotId)));
                                    return 1;
                                })
                                .then(ClientCommandManager.argument("delay", LongArgumentType.longArg(50, 2000))
                                        .executes(context -> {
                                            int slotId = IntegerArgumentType.getInteger(context, "slotid");
                                            long delay = LongArgumentType.getLong(context, "delay");
                                            payManager.setConfirmClickSlot(slotId);
                                            payManager.setConfirmClickDelay(delay);
                                            context.getSource().sendFeedback(Component.literal(
                                                String.format("§a[Pay Everyone] Auto-confirm: slot %d, %dms delay", slotId, delay)));
                                            return 1;
                                        }))))
                .then(ClientCommandManager.literal("doublesend")
                        .executes(context -> {
                            boolean isEnabled = payManager.isDoubleSendEnabled();
                            long delay = payManager.getDoubleSendDelay();
                            if (isEnabled) {
                                String delayText = delay > 0 ? String.format("(%dms delay)", delay) : "(no delay)";
                                context.getSource().sendFeedback(Component.literal(String.format("§6[Pay Everyone] Double send: §aENABLED §7%s", delayText)));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall doublesend off' to disable"));
                            } else {
                                context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Double send: §cDISABLED"));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall doublesend <delay>' to enable"));
                            }
                            return 1;
                        })
                        .then(ClientCommandManager.literal("off")
                                .executes(context -> {
                                    payManager.setDoubleSend(false);
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Double send disabled"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.argument("delay", LongArgumentType.longArg(0))
                                .executes(context -> {
                                    long delay = LongArgumentType.getLong(context, "delay");
                                    payManager.setDoubleSend(true);
                                    payManager.setDoubleSendDelay(delay);
                                    String msg = delay > 0 
                                        ? String.format("§a[Pay Everyone] Double send enabled (%dms delay)", delay)
                                        : "§a[Pay Everyone] Double send enabled (no delay)";
                                    context.getSource().sendFeedback(Component.literal(msg));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("reversesyntax")
                        .executes(context -> {
                            boolean isEnabled = payManager.isReverseSyntaxEnabled();
                            if (isEnabled) {
                                context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Reverse syntax: §aENABLED"));
                                context.getSource().sendFeedback(Component.literal("§7  Payment format: /<command> <amount> <player>"));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall reversesyntax off' to disable"));
                            } else {
                                context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Reverse syntax: §cDISABLED"));
                                context.getSource().sendFeedback(Component.literal("§7  Payment format: /<command> <player> <amount>"));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall reversesyntax on' to enable"));
                            }
                            return 1;
                        })
                        .then(ClientCommandManager.literal("on")
                                .executes(context -> {
                                    payManager.setReverseSyntax(true);
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Reverse syntax enabled! Format: /<command> <amount> <player>"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("off")
                                .executes(context -> {
                                    payManager.setReverseSyntax(false);
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Reverse syntax disabled! Format: /<command> <player> <amount>"));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("reset")
                        .executes(context -> {
                            payManager.resetAllSettings();
                            context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] All settings have been reset to default."));
                            return 1;
                        })));
    }
}
