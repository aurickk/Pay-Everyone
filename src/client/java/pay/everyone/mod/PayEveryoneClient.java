package pay.everyone.mod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import pay.everyone.mod.gui.PayEveryoneHud;

public class PayEveryoneClient implements ClientModInitializer {
	private static KeyMapping cancelPaymentKey;
	
	public static KeyMapping getCancelPaymentKey() { return cancelPaymentKey; }
	
	@Override
	public void onInitializeClient() {
		PayManager.getInstance().clearAllPlayerLists();
		
		cancelPaymentKey = KeyBindingHelper.registerKeyBinding(VersionCompat.createKeyMapping(
			"key.payeveryone.cancel_payment",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_J,
			"category.payeveryone"
		));
		
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			if (!InputHandler.isInitialized()) {
				InputHandler.init();
			}
			PayEveryoneHud.getInstance().render(graphics, VersionCompat.getTickDelta(tickDelta));
		});
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			PayEveryoneHud.getInstance().tick();
			
			while (cancelPaymentKey.consumeClick()) {
				PayManager pm = PayManager.getInstance();
				if (pm.isPaying() || pm.isTabScanning()) {
					pm.stopPaying();
					pm.stopTabScan();
					pm.clearTabScanList();
					if (client.player != null) {
						client.player.displayClientMessage(
							net.minecraft.network.chat.Component.literal("§e[Pay Everyone] Payment/Scan cancelled via keybind"), false);
					}
				}
			}
		});
		
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			PayManager.getInstance().clearAllPlayerLists();
		});
		
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			PayManager.getInstance().clearAllPlayerLists();
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("payeveryone")
				.then(ClientCommandManager.literal("hide")
					.executes(context -> {
						PayEveryoneHud.getInstance().setManuallyHidden(true);
						PayEveryoneHud.getInstance().getWindow().setPinned(false);
						if (context.getSource().getPlayer() != null) {
							context.getSource().getPlayer().displayClientMessage(
								net.minecraft.network.chat.Component.literal("§a[Pay Everyone] GUI hidden"), false);
						}
						return 1;
					}))
				.then(ClientCommandManager.literal("show")
					.executes(context -> {
						PayEveryoneHud.getInstance().setManuallyHidden(false);
						if (context.getSource().getPlayer() != null) {
							context.getSource().getPlayer().displayClientMessage(
								net.minecraft.network.chat.Component.literal("§a[Pay Everyone] GUI shown"), false);
						}
						return 1;
					})));
		});
	}
}
