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
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && PayManager.getInstance().isTabScanning()) {
            return true;
        }
        if (PayEveryoneHud.getInstance().handleInventoryKey(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (PayEveryoneHud.getInstance().handleInventoryClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (PayEveryoneHud.getInstance().handleInventoryRelease(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (PayEveryoneHud.getInstance().handleInventoryDrag(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (PayEveryoneHud.getInstance().handleInventoryScroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        try {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        } catch (Throwable t) {
            return false;
        }
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (PayEveryoneHud.getInstance().handleInventoryChar(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
}

