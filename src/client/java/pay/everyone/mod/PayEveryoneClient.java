package pay.everyone.mod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;*/
//?} else {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?}
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.command.v2.ClientCommands;*/
//?} else {
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
//?}
//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//? } else {
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//? }
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import pay.everyone.mod.gui.PayEveryoneHud;

public class PayEveryoneClient implements ClientModInitializer {
	private static KeyMapping cancelPaymentKey;
	
	public static KeyMapping getCancelPaymentKey() { return cancelPaymentKey; }
	
	@Override
	public void onInitializeClient() {
		PayManager.getInstance().clearAllPlayerLists();
		
		ModConfig config = ModConfig.getInstance();
		PayManager.getInstance().setDynamicSubdivisionEnabled(config.isDynamicSubdivisionEnabled());
		
		//? if >=26.1 {
		/*cancelPaymentKey = KeyMappingHelper.registerKeyMapping(VersionCompat.createKeyMapping(
			"key.payeveryone.cancel_payment",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			"category.payeveryone"
		));
		*///?} else {
		cancelPaymentKey = KeyBindingHelper.registerKeyBinding(VersionCompat.createKeyMapping(
			"key.payeveryone.cancel_payment",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			"category.payeveryone"
		));
		//?}
		
		//? if >=26.1 {
		/*HudElementRegistry.addLast(net.minecraft.resources.Identifier.parse("pay-everyone:hud"), (graphics, tickCounter) -> {
			if (!InputHandler.isInitialized()) {
				InputHandler.init();
			}
			PayEveryoneHud.getInstance().render(graphics, VersionCompat.getTickDelta(tickCounter));
		});
		*///?} elif >=1.21.6 {
		HudElementRegistry.addLast(net.minecraft.resources.ResourceLocation.parse("pay-everyone:hud"), (graphics, tickCounter) -> {
			if (!InputHandler.isInitialized()) {
				InputHandler.init();
			}
			PayEveryoneHud.getInstance().render(graphics, VersionCompat.getTickDelta(tickCounter));
		});
		//?} else {
		/*HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			if (!InputHandler.isInitialized()) {
				InputHandler.init();
			}
			PayEveryoneHud.getInstance().render(graphics, VersionCompat.getTickDelta(tickDelta));
		});
		*///?}
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			PayEveryoneHud.getInstance().tick();

			boolean textFieldFocused = PayEveryoneHud.getInstance().getWindow().hasFocusedTextField();
			
			while (cancelPaymentKey.consumeClick()) {
				if (textFieldFocused) continue;
				PayManager pm = PayManager.getInstance();
				if (pm.isPaying() || pm.isTabScanning()) {
					pm.stopPaying();
					pm.stopTabScan();
					pm.clearTabScanList();
					pm.forceResetRunningState();
					if (client.player != null) {
						//? if >=26.1 {
						/*client.player.sendSystemMessage(
							net.minecraft.network.chat.Component.literal("§e[Pay Everyone] Payment/Scan cancelled via keybind"));
						*///?} else {
						client.player.displayClientMessage(
							net.minecraft.network.chat.Component.literal("§e[Pay Everyone] Payment/Scan cancelled via keybind"), false);
						//?}
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
			//? if >=26.1 {
			/*dispatcher.register(ClientCommands.literal("payeveryone")
				.then(ClientCommands.literal("hide")
					.executes(context -> {
						PayEveryoneHud.getInstance().setManuallyHidden(true);
						PayEveryoneHud.getInstance().getWindow().setPinned(false);
						if (context.getSource().getPlayer() != null) {
							context.getSource().getPlayer().sendSystemMessage(
								net.minecraft.network.chat.Component.literal("§a[Pay Everyone] GUI hidden"));
						}
						return 1;
					}))
				.then(ClientCommands.literal("show")
					.executes(context -> {
						PayEveryoneHud.getInstance().setManuallyHidden(false);
						if (context.getSource().getPlayer() != null) {
							context.getSource().getPlayer().sendSystemMessage(
								net.minecraft.network.chat.Component.literal("§a[Pay Everyone] GUI shown"));
						}
						return 1;
					})));
			*///?} else {
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
			//?}
		});
	}
}
