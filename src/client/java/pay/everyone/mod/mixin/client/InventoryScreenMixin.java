package pay.everyone.mod.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
<<<<<<< HEAD
import net.minecraft.client.gui.screens.Screen;
=======
//? if >=1.21.6 {
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.InventoryMenu;
//?} else {
import net.minecraft.client.gui.screens.Screen;
//?}
>>>>>>> 230b532 (feat: migrate to stonecutter)
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pay.everyone.mod.PayManager;
import pay.everyone.mod.gui.PayEveryoneHud;

<<<<<<< HEAD
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends Screen {
    protected InventoryScreenMixin() { super(null); }
=======
/**
 * Consolidated InventoryScreenMixin for all Minecraft versions.
 * Input handling is done purely through GLFW callbacks in InputHandler.
 * This mixin only handles lifecycle (init/render/close) - NOT input events.
 */
@Mixin(InventoryScreen.class)
//? if >=1.21.6 {
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    protected InventoryScreenMixin() { super(null, null, null); }
//?} else {
public abstract class InventoryScreenMixin extends Screen {
    protected InventoryScreenMixin() { super(null); }
//?}
>>>>>>> 230b532 (feat: migrate to stonecutter)
    
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
<<<<<<< HEAD
    
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
            // Fallback for versions with different signature
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

=======
}
>>>>>>> 230b532 (feat: migrate to stonecutter)
