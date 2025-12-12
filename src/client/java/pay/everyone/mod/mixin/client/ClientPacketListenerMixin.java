package pay.everyone.mod.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pay.everyone.mod.PayManager;

import java.util.ArrayList;
import java.util.List;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(at = @At("HEAD"), method = "handleCommandSuggestions")
    private void onCommandSuggestions(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci) {
        int requestId = packet.id();
        List<String> playerNames = new ArrayList<>();
        
        for (ClientboundCommandSuggestionsPacket.Entry entry : packet.suggestions()) {
            String text = entry.text();
            if (text != null && !text.isEmpty()) {
                playerNames.add(text);
            }
        }
        
        PayManager.getInstance().handleTabCompletionResponse(requestId, playerNames);
    }
    
    @Inject(at = @At("TAIL"), method = "handleOpenScreen")
    private void onOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (PayManager.getInstance().isAutoConfirmEnabled()) {
            PayManager.getInstance().handleContainerOpened(packet.getContainerId());
        }
    }
}
