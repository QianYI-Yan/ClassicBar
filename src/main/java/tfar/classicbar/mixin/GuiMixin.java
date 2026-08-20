package tfar.classicbar.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.classicbar.EventHandler;

/**
 * 26.2 中原版 HUD 渲染重构为「渲染状态提取」模型（GuiGraphics 移除，改为 GuiGraphicsExtractor），
 * 原版各条（血条/护甲/饥饿/氧气/坐骑血条）在 Hud.extractPlayerHealth 等私有方法中提取。
 * 这里注入 Hud：
 *  - 禁用原版血条/护甲/饥饿/氧气/坐骑血条的提取（由 ClassicBar 的自定义条替代）；
 *  - 在 extractRenderState 末尾追加 ClassicBar 的条渲染。
 */
@Mixin(Hud.class)
public class GuiMixin {

    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void classicbar$disablePlayerHealth(GuiGraphicsExtractor extractor, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "extractArmor", at = @At("HEAD"), cancellable = true)
    private static void classicbar$disableArmor(GuiGraphicsExtractor extractor, Player player, int i1, int i2, int i3, int i4, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "extractAirBubbles", at = @At("HEAD"), cancellable = true)
    private void classicbar$disableAir(GuiGraphicsExtractor extractor, Player player, int i1, int i2, int i3, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private void classicbar$disableFood(GuiGraphicsExtractor extractor, Player player, int i1, int i2, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "extractVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void classicbar$disableVehicleHealth(GuiGraphicsExtractor extractor, CallbackInfo ci) {
        ci.cancel();
    }

    /** 在所有原版 HUD 元素提取完毕后，追加 ClassicBar 的自定义条渲染 */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void classicbar$renderBars(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        EventHandler.render(extractor, deltaTracker);
    }
}
