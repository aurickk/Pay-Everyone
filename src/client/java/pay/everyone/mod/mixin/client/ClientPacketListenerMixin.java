package pay.everyone.mod.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pay.everyone.mod.PayManager;
import pay.everyone.mod.VersionCompat;

import java.util.ArrayList;
import java.util.List;

/** Handles command suggestions and screen packets via VersionCompat. */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(at = @At("HEAD"), method = "handleCommandSuggestions", require = 0)
    private void onCommandSuggestions(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci) {
        try {
            // Use VersionCompat for cross-version packet access
            int requestId = VersionCompat.getPacketId(packet);
            List<String> playerNames = new ArrayList<>();
            
            List<ClientboundCommandSuggestionsPacket.Entry> suggestions = VersionCompat.getPacketSuggestions(packet);
            for (ClientboundCommandSuggestionsPacket.Entry entry : suggestions) {
                String text = VersionCompat.getSuggestionText(entry);
                if (text != null && !text.isEmpty()) {
                    playerNames.add(text);
                }
            }
            
            PayManager.getInstance().handleTabCompletionResponse(requestId, playerNames);
        } catch (Exception e) {
            // Silently ignore errors to prevent crashes across versions
            pay.everyone.mod.PayEveryone.LOGGER.debug("Error handling command suggestions", e);
        }
    }
    
    @Inject(at = @At("TAIL"), method = "handleOpenScreen", require = 0)
    private void onOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        try {
            if (PayManager.getInstance().isAutoConfirmEnabled()) {
                PayManager.getInstance().handleContainerOpened(packet.getContainerId());
            }
        } catch (Exception e) {
            // Silently ignore errors to prevent crashes across versions
            pay.everyone.mod.PayEveryone.LOGGER.debug("Error handling open screen", e);
        }
    }
}
