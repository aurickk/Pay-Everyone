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
	private static KeyMapping toggleGuiKey;
	private static KeyMapping cancelPaymentKey;
	
	public static KeyMapping getToggleGuiKey() { return toggleGuiKey; }
	public static KeyMapping getCancelPaymentKey() { return cancelPaymentKey; }
	
	@Override
	public void onInitializeClient() {
		PayManager.getInstance().clearAllPlayerLists();
		
		toggleGuiKey = KeyBindingHelper.registerKeyBinding(VersionCompat.createKeyMapping(
			"key.payeveryone.toggle_gui",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			"category.payeveryone"
		));
		
		cancelPaymentKey = KeyBindingHelper.registerKeyBinding(VersionCompat.createKeyMapping(
			"key.payeveryone.cancel_payment",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_J,  // Default to J key (next to K)
			"category.payeveryone"
		));
		
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			// Initialize input handler on first render (when GLFW context is ready)
			if (!InputHandler.isInitialized()) {
				InputHandler.init();
			}
			PayEveryoneHud.getInstance().render(graphics, VersionCompat.getTickDelta(tickDelta));
		});
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			PayEveryoneHud.getInstance().tick();
			
			while (toggleGuiKey.consumeClick()) {
				PayEveryoneHud.getInstance().toggle();
			}
			
			while (cancelPaymentKey.consumeClick()) {
				PayManager pm = PayManager.getInstance();
				if (pm.isPaying() || pm.isTabScanning()) {
					pm.stopPaying();
					pm.stopTabScan();
					pm.clearTabScanList();
					// Show chat feedback
					if (client.player != null) {
						client.player.displayClientMessage(
							net.minecraft.network.chat.Component.literal("§e[Pay Everyone] Payment/Scan cancelled via keybind"), false);
					}
				}
			}
		});
		
		// On world/server join & disconnect, clear logical player lists
		// but KEEP the GUI window instance so size/position persist
		// during this game session. The JVM restart will naturally reset
		// everything back to defaults on full game restart.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			PayManager.getInstance().clearAllPlayerLists();
		});
		
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			PayManager.getInstance().clearAllPlayerLists();
		});

		// /payeveryone client-side command to open the GUI
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("payeveryone")
				.executes(context -> {
					PayEveryoneHud.getInstance().toggle();
					return 1;
				}));
		});
	}
}
