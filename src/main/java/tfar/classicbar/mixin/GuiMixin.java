package tfar.classicbar.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁用原版 HUD 的血条/护甲/饥饿/氧气/吸收（renderPlayerHealth）
 * 与坐骑血条（renderVehicleHealth），由 ClassicBar 的自定义条替代。
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void classicbar$disableVanillaBars(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void classicbar$disableVehicleHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
    }
}
