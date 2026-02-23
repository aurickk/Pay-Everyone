package pay.everyone.mod.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
//? if >=1.21.6 {
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.InventoryMenu;
//?} else {
import net.minecraft.client.gui.screens.Screen;
//?}
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pay.everyone.mod.PayManager;
import pay.everyone.mod.gui.PayEveryoneHud;

@Mixin(InventoryScreen.class)
//? if >=1.21.6 {
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    protected InventoryScreenMixin() { super(null, null, null); }
//?} else {
public abstract class InventoryScreenMixin extends Screen {
    protected InventoryScreenMixin() { super(null); }
//?}
    
    @Inject(method = "init", at = @At("TAIL"))
    private void onInventoryInit(CallbackInfo ci) {
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
