package pay.everyone.mod.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pay.everyone.mod.PayManager;
import pay.everyone.mod.gui.PayEveryoneHud;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends Screen {
    protected CreativeInventoryScreenMixin() { super(null); }
    
    @Inject(method = "init", at = @At("TAIL"))
    private void onCreativeInit(CallbackInfo ci) {
        PayEveryoneHud.getInstance().setInventoryMode(true);
        PayEveryoneHud.getInstance().positionForInventory(this.width, this.height);
    }
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        PayEveryoneHud.getInstance().renderInScreen(graphics, mouseX, mouseY, delta);
    }
    
    @Override
    public void onClose() {
        if (PayManager.getInstance().isTabScanning()) {
            return;
        }
        PayEveryoneHud.getInstance().setInventoryMode(false);
        super.onClose();
    }
    
    @Override
    public void removed() {
        PayEveryoneHud.getInstance().setInventoryMode(false);
        super.removed();
    }
}
