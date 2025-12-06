package pay.everyone.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PayCommands {
    private static final PayManager payManager = PayManager.getInstance();
    
    // Pending payment info for confirmation
    private static String pendingAmount = null;
    private static long pendingDelay = 1000;
    
    // Suggestion provider for player names (for exclude command)
    private static final SuggestionProvider<FabricClientCommandSource> PLAYER_SUGGESTIONS = (context, builder) -> {
        List<String> players = payManager.getPlayersForAutocomplete();
        String input = builder.getRemaining();
        
        // Handle space-separated player names - get the last partial name being typed
        String[] parts = input.split("\\s+");
        String lastPart = parts.length > 0 ? parts[parts.length - 1].toLowerCase() : "";
        
        // If input ends with space, we're starting a new name
        if (input.endsWith(" ") || input.isEmpty()) {
            lastPart = "";
        }
        
        // Calculate the start position for suggestions
        int startPos = builder.getStart();
        if (parts.length > 1 || (parts.length == 1 && input.endsWith(" "))) {
            // Find where the last word starts
            int lastSpaceIndex = input.lastIndexOf(' ');
            if (lastSpaceIndex >= 0) {
                startPos = builder.getStart() + lastSpaceIndex + 1;
            }
        }
        
        // Create a new builder at the correct position
        var newBuilder = builder.createOffset(startPos);
        
        for (String player : players) {
            if (player.toLowerCase().startsWith(lastPart) && !payManager.isExcluded(player)) {
                newBuilder.suggest(player);
            }
        }
        return newBuilder.buildFuture();
    };
    
    /**
     * Show the confirmation dialog and then run payment if user confirms
     */
    private static void showConfirmationAndPay(FabricClientCommandSource source, String amountOrRange, long delayMs) {
        int playerCount = payManager.getOnlinePlayerCount();
        int excludedCount = payManager.getExclusionCount();
        int addedCount = payManager.getManualPlayerCount();
        String method = payManager.getLastDiscoveryMethod();
        String payCommand = payManager.getPayCommand();
        int confirmSlot = payManager.getConfirmClickSlot();
        boolean doubleSend = payManager.isDoubleSendEnabled();
        
        // Show confirmation info
        source.sendFeedback(Component.literal("§6========== Payment Confirmation =========="));
        source.sendFeedback(Component.literal(String.format("§e  Player source: §f%s", method)));
        source.sendFeedback(Component.literal(String.format("§e  Pay command: §f/%s", payCommand)));
        source.sendFeedback(Component.literal(String.format("§e  Players to pay: §f%d", playerCount)));
        source.sendFeedback(Component.literal(String.format("§e  Players excluded: §f%d", excludedCount)));
        source.sendFeedback(Component.literal(String.format("§e  Players added: §f%d", addedCount)));
        source.sendFeedback(Component.literal(String.format("§e  Auto-confirm slot: §f%s", confirmSlot >= 0 ? String.valueOf(confirmSlot) : "disabled")));
        source.sendFeedback(Component.literal(String.format("§e  Double send: §f%s", doubleSend ? "enabled" : "disabled")));
        source.sendFeedback(Component.literal(String.format("§e  Amount: §f%s", amountOrRange)));
        source.sendFeedback(Component.literal(String.format("§e  Delay: §f%dms", delayMs)));
        source.sendFeedback(Component.literal("§6=========================================="));
        
        if (playerCount == 0) {
            source.sendError(Component.literal("§c[Pay Everyone] No players to pay!"));
            pendingAmount = null;
            return;
        }
        
        source.sendFeedback(Component.literal("§a[Pay Everyone] Type §f/payall confirm §ato continue."));
        
        // Store pending payment info
        pendingAmount = amountOrRange;
        pendingDelay = delayMs;
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // Register /payall command with subcommands
        dispatcher.register(ClientCommandManager.literal("payall")
                // Main payment command: /payall <amount> [delay] (delay defaults to 1000ms)
                .then(ClientCommandManager.argument("amount", StringArgumentType.string())
                        // /payall <amount> - uses default delay of 1000ms
                        .executes(context -> {
                            if (payManager.isPaying()) {
                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Payment process is already in progress! Use '/payall stop' to stop it first."));
                                return 0;
                            }
                            if (payManager.isTabScanning()) {
                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Tab scan is in progress! Wait for it to complete or use '/payall tabscan stop'."));
                                return 0;
                            }
                            String amountOrRange = StringArgumentType.getString(context, "amount");
                            
                            // Start the process: check command, scan if available, then show confirmation
                            payManager.startPaymentWithScan(amountOrRange, 1000, () -> {
                                showConfirmationAndPay(context.getSource(), amountOrRange, 1000);
                            });
                            return 1;
                        })
                        // /payall <amount> <delay> - custom delay
                        .then(ClientCommandManager.argument("delay", LongArgumentType.longArg(0))
                                .executes(context -> {
                                    if (payManager.isPaying()) {
                                        context.getSource().sendError(Component.literal("§c[Pay Everyone] Payment process is already in progress! Use '/payall stop' to stop it first."));
                                        return 0;
                                    }
                                    if (payManager.isTabScanning()) {
                                        context.getSource().sendError(Component.literal("§c[Pay Everyone] Tab scan is in progress! Wait for it to complete or use '/payall tabscan stop'."));
                                        return 0;
                                    }
                                    String amountOrRange = StringArgumentType.getString(context, "amount");
                                    long delay = LongArgumentType.getLong(context, "delay");
                                    
                                    // Start the process: check command, scan if available, then show confirmation
                                    payManager.startPaymentWithScan(amountOrRange, delay, () -> {
                                        showConfirmationAndPay(context.getSource(), amountOrRange, delay);
                                    });
                                    return 1;
                                })))
                // Subcommand: /payall confirm - confirm pending payment
                .then(ClientCommandManager.literal("confirm")
                        .executes(context -> {
                            if (pendingAmount == null) {
                                context.getSource().sendError(Component.literal("§c[Pay Everyone] No pending payment to confirm. Run '/payall <amount>' first."));
                                return 0;
                            }
                            if (payManager.isPaying()) {
                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Payment already in progress!"));
                                return 0;
                            }
                            
                            String amount = pendingAmount;
                            long delay = pendingDelay;
                            pendingAmount = null; // Clear pending
                            
                            boolean success = payManager.payAll(amount, delay);
                            if (!success) {
                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Failed to start payment process."));
                                return 0;
                            }
                            return 1;
                        }))
                // Subcommand: /payall command <command> - set custom pay command
                .then(ClientCommandManager.literal("command")
                        .executes(context -> {
                            String currentCommand = payManager.getPayCommand();
                            context.getSource().sendFeedback(Component.literal(
                                String.format("§6[Pay Everyone] Current pay command: §f/%s", currentCommand)
                            ));
                            context.getSource().sendFeedback(Component.literal(
                                "§7  Use '/payall command <command>' to change it"
                            ));
                            return 1;
                        })
                        .then(ClientCommandManager.argument("cmd", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String cmd = StringArgumentType.getString(context, "cmd");
                                    payManager.setPayCommand(cmd);
                                    context.getSource().sendFeedback(Component.literal(
                                        String.format("§a[Pay Everyone] Pay command set to: §f/%s", payManager.getPayCommand())
                                    ));
                                    return 1;
                                })))
                // Subcommand: /payall exclude <players>
                .then(ClientCommandManager.literal("exclude")
                        .then(ClientCommandManager.argument("players", StringArgumentType.greedyString())
                                .suggests(PLAYER_SUGGESTIONS)
                                .executes(context -> {
                                    String playersArg = StringArgumentType.getString(context, "players");
                                    String[] players = playersArg.split("\\s+");
                                    
                                    if (players.length == 0 || (players.length == 1 && players[0].isEmpty())) {
                                        context.getSource().sendError(Component.literal("§c[Pay Everyone] Please specify at least one player name"));
                                        return 0;
                                    }

                                    payManager.addExcludedPlayers(players);
                                    context.getSource().sendFeedback(Component.literal(
                                        String.format("§a[Pay Everyone] Added %d player(s) to exclusion list: %s", 
                                            players.length, String.join(", ", players))
                                    ));
                                    return 1;
                                })))
                // Subcommand: /payall clear - with subcommands
                .then(ClientCommandManager.literal("clear")
                        // /payall clear exclude - clear excluded players
                        .then(ClientCommandManager.literal("exclude")
                                .executes(context -> {
                                    payManager.clearExclusions();
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Cleared all exclusions"));
                                    return 1;
                                }))
                        // /payall clear add - clear manually added players
                        .then(ClientCommandManager.literal("add")
                                .executes(context -> {
                                    payManager.clearManualPlayers();
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Cleared manually added players"));
                                    return 1;
                                }))
                        // /payall clear tabscan - clear tabscan results
                        .then(ClientCommandManager.literal("tabscan")
                                .executes(context -> {
                                    payManager.clearTabScanList();
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Cleared tab scan results"));
                                    return 1;
                                }))
                        // /payall clear all - clear everything
                        .then(ClientCommandManager.literal("all")
                                .executes(context -> {
                                    payManager.clearExclusions();
                                    payManager.clearManualPlayers();
                                    payManager.clearTabScanList();
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Cleared all lists (exclusions, manual, tabscan)"));
                                    return 1;
                                })))
                // Subcommand: /payall add <players>
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("players", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String playersArg = StringArgumentType.getString(context, "players");
                                    String[] players = playersArg.split("[, ]+"); // Split by comma or space
                                    
                                    if (players.length == 0 || (players.length == 1 && players[0].isEmpty())) {
                                        context.getSource().sendError(Component.literal("§c[Pay Everyone] Please specify at least one player name"));
                                        return 0;
                                    }

                                    PayManager.AddPlayersResult result = payManager.addManualPlayers(players);
                                    
                                    if (!result.added.isEmpty()) {
                                        context.getSource().sendFeedback(Component.literal(
                                            String.format("§a[Pay Everyone] Added %d player(s) to payment list: %s", 
                                                result.added.size(), String.join(", ", result.added))
                                        ));
                                    }
                                    
                                    if (!result.duplicates.isEmpty()) {
                                        context.getSource().sendFeedback(Component.literal(
                                            String.format("§e[Pay Everyone] Skipped %d player(s) already on list: %s", 
                                                result.duplicates.size(), String.join(", ", result.duplicates))
                                        ));
                                    }
                                    
                                    if (result.added.isEmpty() && result.duplicates.isEmpty()) {
                                        context.getSource().sendError(Component.literal("§c[Pay Everyone] No valid player names provided"));
                                        return 0;
                                    }
                                    
                                    return 1;
                                })))
                // Subcommand: /payall stop
                .then(ClientCommandManager.literal("stop")
                        .executes(context -> {
                            boolean stopped = false;
                            if (payManager.isPaying()) {
                                payManager.stopPaying();
                                context.getSource().sendFeedback(Component.literal("§c[Pay Everyone] Stopping payment process..."));
                                stopped = true;
                            }
                            if (payManager.isTabScanning()) {
                                payManager.stopTabScan();
                                context.getSource().sendFeedback(Component.literal("§c[Pay Everyone] Stopping tab scan..."));
                                stopped = true;
                            }
                            if (pendingAmount != null) {
                                pendingAmount = null;
                                context.getSource().sendFeedback(Component.literal("§c[Pay Everyone] Pending payment cancelled."));
                                stopped = true;
                            }
                            if (!stopped) {
                                context.getSource().sendFeedback(Component.literal("§e[Pay Everyone] Nothing to stop."));
                            }
                            return 1;
                        }))
                // Subcommand: /payall tabscan [interval] - scan players via tab completion
                .then(ClientCommandManager.literal("tabscan")
                        .executes(context -> {
                            if (payManager.isTabScanning()) {
                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Tab scan already in progress! Use '/payall tabscan stop' to stop it."));
                                return 0;
                            }
                            context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] ⚠ WARNING: Stay still during tab scan!"));
                            payManager.queryPlayersViaTabComplete(); // Default 500ms
                            return 1;
                        })
                        .then(ClientCommandManager.argument("interval", LongArgumentType.longArg(50, 5000))
                                .executes(context -> {
                                    if (payManager.isTabScanning()) {
                                        context.getSource().sendError(Component.literal("§c[Pay Everyone] Tab scan already in progress! Use '/payall tabscan stop' to stop it."));
                                        return 0;
                                    }
                                    context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] ⚠ WARNING: Stay still during tab scan!"));
                                    long interval = LongArgumentType.getLong(context, "interval");
                                    payManager.queryPlayersViaTabComplete(interval);
                                    return 1;
                                }))
                        // /payall tabscan stop - stop tab scan in progress
                        .then(ClientCommandManager.literal("stop")
                                .executes(context -> {
                                    if (payManager.stopTabScan()) {
                                        context.getSource().sendFeedback(Component.literal("§c[Pay Everyone] Tab scan stopped."));
                                    } else {
                                        context.getSource().sendFeedback(Component.literal("§e[Pay Everyone] No tab scan in progress."));
                                    }
                                    return 1;
                                }))
                        // /payall tabscan debug - toggle debug mode for tabscan
                        .then(ClientCommandManager.literal("debug")
                                .executes(context -> {
                                    boolean newState = !payManager.isDebugMode();
                                    payManager.setDebugMode(newState);
                                    context.getSource().sendFeedback(Component.literal(
                                        String.format("§6[Pay Everyone] Tab scan debug: %s", newState ? "§aENABLED" : "§cDISABLED")
                                    ));
                                    return 1;
                                })
                                .then(ClientCommandManager.literal("true")
                                        .executes(context -> {
                                            payManager.setDebugMode(true);
                                            context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Tab scan debug: §aENABLED"));
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("false")
                                        .executes(context -> {
                                            payManager.setDebugMode(false);
                                            context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Tab scan debug: §cDISABLED"));
                                            return 1;
                                        }))))
                // Subcommand: /payall remove - remove players from lists
                .then(ClientCommandManager.literal("remove")
                        // /payall remove exclude <players> - remove from exclusion list
                        .then(ClientCommandManager.literal("exclude")
                                .then(ClientCommandManager.argument("players", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String playersArg = StringArgumentType.getString(context, "players");
                                            String[] players = playersArg.split("\\s+");
                                            
                                            if (players.length == 0 || (players.length == 1 && players[0].isEmpty())) {
                                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Please specify at least one player name"));
                                                return 0;
                                            }

                                            List<String> removed = payManager.removeExcludedPlayers(players);
                                            if (removed.isEmpty()) {
                                                context.getSource().sendFeedback(Component.literal(
                                                    "§e[Pay Everyone] None of the specified players were in the exclusion list"
                                                ));
                                            } else {
                                                context.getSource().sendFeedback(Component.literal(
                                                    String.format("§a[Pay Everyone] Removed %d player(s) from exclusion list: %s", 
                                                        removed.size(), String.join(", ", removed))
                                                ));
                                            }
                                            return 1;
                                        })))
                        // /payall remove add <players> - remove from manual add list
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("players", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String playersArg = StringArgumentType.getString(context, "players");
                                            String[] players = playersArg.split("\\s+");
                                            
                                            if (players.length == 0 || (players.length == 1 && players[0].isEmpty())) {
                                                context.getSource().sendError(Component.literal("§c[Pay Everyone] Please specify at least one player name"));
                                                return 0;
                                            }

                                            List<String> removed = payManager.removeManualPlayers(players);
                                            if (removed.isEmpty()) {
                                                context.getSource().sendFeedback(Component.literal(
                                                    "§e[Pay Everyone] None of the specified players were in the add list"
                                                ));
                                            } else {
                                                context.getSource().sendFeedback(Component.literal(
                                                    String.format("§a[Pay Everyone] Removed %d player(s) from add list: %s", 
                                                        removed.size(), String.join(", ", removed))
                                                ));
                                            }
                                            return 1;
                                        }))))
                // Subcommand: /payall list - list players (200 max)
                .then(ClientCommandManager.literal("list")
                        .executes(context -> {
                            // Show debug info
                            String debugInfo = payManager.getDebugPlayerLists();
                            for (String line : debugInfo.split("\n")) {
                                context.getSource().sendFeedback(Component.literal(line));
                            }
                            return 1;
                        })
                        // /payall list tabscan - list tabscan players
                        .then(ClientCommandManager.literal("tabscan")
                                .executes(context -> {
                                    List<String> players = payManager.getPlayerListSample("tabscan", 200);
                                    context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Tab Scan Players (showing up to 200):"));
                                    if (players.isEmpty()) {
                                        context.getSource().sendFeedback(Component.literal("§7  No players in tab scan list. Run '/payall tabscan' first."));
                                    } else {
                                        context.getSource().sendFeedback(Component.literal("§f  " + String.join(", ", players)));
                                    }
                                    return 1;
                                }))
                        // /payall list add - list manually added players
                        .then(ClientCommandManager.literal("add")
                                .executes(context -> {
                                    List<String> players = payManager.getPlayerListSample("add", 200);
                                    context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Manually Added Players (showing up to 200):"));
                                    if (players.isEmpty()) {
                                        context.getSource().sendFeedback(Component.literal("§7  No manually added players. Use '/payall add <players>' to add."));
                                    } else {
                                        context.getSource().sendFeedback(Component.literal("§f  " + String.join(", ", players)));
                                    }
                                    return 1;
                                }))
                        // /payall list exclude - list excluded players
                        .then(ClientCommandManager.literal("exclude")
                                .executes(context -> {
                                    List<String> players = payManager.getPlayerListSample("exclude", 200);
                                    context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Excluded Players (showing up to 200):"));
                                    if (players.isEmpty()) {
                                        context.getSource().sendFeedback(Component.literal("§7  No excluded players."));
                                    } else {
                                        context.getSource().sendFeedback(Component.literal("§f  " + String.join(", ", players)));
                                    }
                                    return 1;
                                }))
                        // /payall list tablist - list tab list players
                        .then(ClientCommandManager.literal("tablist")
                                .executes(context -> {
                                    List<String> players = payManager.getPlayerListSample("tablist", 200);
                                    context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Tab List Players (showing up to 200):"));
                                    if (players.isEmpty()) {
                                        context.getSource().sendFeedback(Component.literal("§7  No players in tab list."));
                                    } else {
                                        context.getSource().sendFeedback(Component.literal("§f  " + String.join(", ", players)));
                                    }
                                    return 1;
                                })))
                // Subcommand: /payall confirmclickslot - auto-click confirmation menu slot
                .then(ClientCommandManager.literal("confirmclickslot")
                        // Show current status when no argument
                        .executes(context -> {
                            int currentSlot = payManager.getConfirmClickSlot();
                            if (currentSlot < 0) {
                                context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Auto-confirm: §cDISABLED"));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall confirmclickslot <slotid>' to enable"));
                            } else {
                                context.getSource().sendFeedback(Component.literal(
                                    String.format("§6[Pay Everyone] Auto-confirm: §aENABLED §7(slot %d, %dms delay)", 
                                        currentSlot, payManager.getConfirmClickDelay())
                                ));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall confirmclickslot off' to disable"));
                            }
                            return 1;
                        })
                        // /payall confirmclickslot off - disable auto-confirm
                        .then(ClientCommandManager.literal("off")
                                .executes(context -> {
                                    payManager.setConfirmClickSlot(-1);
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Auto-confirm disabled"));
                                    return 1;
                                }))
                        // /payall confirmclickslot <slotid> - set slot to click
                        .then(ClientCommandManager.argument("slotid", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    int slotId = IntegerArgumentType.getInteger(context, "slotid");
                                    payManager.setConfirmClickSlot(slotId);
                                    context.getSource().sendFeedback(Component.literal(
                                        String.format("§a[Pay Everyone] Auto-confirm enabled! Will click slot %d on confirmation menus.", slotId)
                                    ));
                                    return 1;
                                })
                                // /payall confirmclickslot <slotid> <delay> - set slot and delay
                                .then(ClientCommandManager.argument("delay", LongArgumentType.longArg(50, 2000))
                                        .executes(context -> {
                                            int slotId = IntegerArgumentType.getInteger(context, "slotid");
                                            long delay = LongArgumentType.getLong(context, "delay");
                                            payManager.setConfirmClickSlot(slotId);
                                            payManager.setConfirmClickDelay(delay);
                                            context.getSource().sendFeedback(Component.literal(
                                                String.format("§a[Pay Everyone] Auto-confirm enabled! Slot: %d, Delay: %dms", slotId, delay)
                                            ));
                                            return 1;
                                        }))))
                // Subcommand: /payall doublesend - send payment command twice
                .then(ClientCommandManager.literal("doublesend")
                        // Show current status when no argument
                        .executes(context -> {
                            boolean isEnabled = payManager.isDoubleSendEnabled();
                            if (isEnabled) {
                                context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Double send: §aENABLED"));
                                context.getSource().sendFeedback(Component.literal("§7  Payment commands will be sent twice to the same player"));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall doublesend off' to disable"));
                            } else {
                                context.getSource().sendFeedback(Component.literal("§6[Pay Everyone] Double send: §cDISABLED"));
                                context.getSource().sendFeedback(Component.literal("§7  Use '/payall doublesend on' to enable"));
                            }
                            return 1;
                        })
                        // /payall doublesend on - enable double send
                        .then(ClientCommandManager.literal("on")
                                .executes(context -> {
                                    payManager.setDoubleSend(true);
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Double send enabled! Payment commands will be sent twice."));
                                    return 1;
                                }))
                        // /payall doublesend off - disable double send
                        .then(ClientCommandManager.literal("off")
                                .executes(context -> {
                                    payManager.setDoubleSend(false);
                                    context.getSource().sendFeedback(Component.literal("§a[Pay Everyone] Double send disabled"));
                                    return 1;
                                }))));
    }
}
