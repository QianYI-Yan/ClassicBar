package tfar.classicbar.impl.overlays.templates;

import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2i;
import tfar.classicbar.ClassicBar;
import tfar.classicbar.api.*;
import tfar.classicbar.config.ClassicBarsConfig;
import tfar.classicbar.impl.BarInfo;
import tfar.classicbar.util.HealthEffect;
import tfar.classicbar.util.ModUtils;

import java.util.Optional;

public abstract class BarOverlayImpl implements BarOverlay {

    //maximum width the bar can be
    public static final int WIDTH = 77;
    public static final int HEIGHT = 5;
    public static final int BAR_U = 2;
    public static final int BAR_V = 11;
    public static final ResourceLocation BAR = ResourceLocation.fromNamespaceAndPath(ClassicBar.MODID, "textures/gui/health.png");

    // 1.21.1 原版已移除 textures/gui/icons.png（改用独立 sprite），
    // 这里使用模组自带的 1.20.1 版 icons.png，保证各条 UV 坐标正确
    public static final ResourceLocation GUI_ICONS_LOCATION = ResourceLocation.fromNamespaceAndPath(ClassicBar.MODID, "textures/gui/icons.png");
    private final BarSettings barSettings;

    protected final boolean dependenciesMet;
    protected boolean errored;

    // 动画状态：条宽平滑过渡 + 淡入淡出（速度由 transition_speed 配置控制）
    protected double displayedWidth = -1;
    protected float displayedAlpha = 1.0f;

    protected final BarInfo barInfo;

    /**
     */
    protected static <T extends BarOverlayImpl> Products.P1<RecordCodecBuilder.Mu<T>, BarSettings> codecStart(
            RecordCodecBuilder.Instance<T> instance) {
        return instance.group(BarSettings.CODEC.fieldOf("bar_settings").forGetter(BarOverlayImpl::getBarSettings));
    }


    protected BarOverlayImpl(BarInfo barInfo, BarSettings barSettings) {
        this.barInfo = barInfo;
        this.barSettings = barSettings;
        dependenciesMet = barInfo.checkDependencies();
    }

    public static int getWidth(double d1, double d2) {
      double ratio = WIDTH * d1 / d2;
      return (int)Math.ceil(ratio);
    }

    public BarSettings getBarSettings() {
        return barSettings;
    }

    public final boolean shouldRender(Player player) {
        return canRender() && barInfo.shouldRender().test(player);
    }

    public final boolean canRender() {
        return !errored() && barSettings.enabled() && dependenciesMet;
    }

    @Override
    public final BarSide getSide() {
        return barSettings.side();
    }

    @Override
    public boolean render(Gui gui, GuiGraphics graphics, Player player, int vOffset) {
        boolean shouldShow = shouldRender(player);
        // 淡出动画：即使不应显示，只要透明度未完全归零就继续渲染直至完全消失
        if (!shouldShow && displayedAlpha <= 0.01f) {
            return false;
        }
        // 每帧更新动画状态（条宽平滑 + 透明度淡入淡出）
        updateAnimation(player, shouldShow);

        ModUtils.setupOverlayRenderState(true, false);
        renderBar(gui, graphics, player, vOffset);
        renderBarDecorations(gui, graphics, player, vOffset);
        Color.reset(graphics);//don't leak colors
        if (barSettings.show_text()) {
            renderText(graphics, player, vOffset);
        }
        if (barSettings.show_icon()) {
            // 图标统一用白色 + 动画透明度
            Color.WHITE.color2Gl(graphics, displayedAlpha);
            renderIcon(graphics, player, vOffset);
        }
        return true;
    }

    public void renderBar(Gui gui, GuiGraphics graphics, Player player, int vOffset) {
        renderSimpleBar(barSettings.colorProvider().getColor(player,barInfo.getRatio(player),0),graphics, player, vOffset);
    }

    public void renderBarDecorations(Gui gui, GuiGraphics graphics, Player player, int vOffset) {

    }

    public void renderText(GuiGraphics graphics, Player player, int vOffset) {
        int text = (int)barInfo.numerator().getValue(player);
        int xStart = graphics.guiWidth() / 2 + getIconOffset();
        int yStart = graphics.guiHeight() - vOffset;
        int color = barSettings.colorProvider().getColor(player,barInfo.getRatio(player) , 0).colorToText();
        // 应用淡出透明度到文本颜色
        int alpha = (int) (((color >>> 24) & 0xFF) * displayedAlpha);
        color = (alpha << 24) | (color & 0xFFFFFF);
        textHelper(graphics,xStart,yStart,text,color);
    }

    public void renderIcon(GuiGraphics graphics, Player player, int vOffset) {
        renderSimpleIcon(graphics, vOffset);
    }

    public void renderSimpleIcon(GuiGraphics graphics, int vOffset) {
        int xStart = graphics.guiWidth() / 2 + getIconOffset();
        int yStart = graphics.guiHeight() - vOffset;

        for (int i = 0; i < barInfo.icon_data().uvs().size(); i++) {
            Vector2i uv = barInfo.icon_data().uvs().get(i);
            ModUtils.drawTexturedModalRect(getIconRL(),graphics,xStart, yStart, uv.x, uv.y, 9, 9);
        }
    }

    public int getHOffset() {
        return switch (getSide()){
            case LEFT -> -91;
            case RIGHT -> 10;
        };

    }

    public int getIconOffset() {
        return switch (getSide()) {
            case LEFT -> -101;
            case RIGHT -> 92;
        };
    }

    public static HealthEffect getHealthEffect(Player player) {
        HealthEffect effects = HealthEffect.NONE;//16
        if (player.hasEffect(MobEffects.POISON)) effects = HealthEffect.POISON;//evaluates to 52
        else if (player.hasEffect(MobEffects.WITHER)) effects = HealthEffect.WITHER;//evaluates to 88
        else if (player.isFullyFrozen()) effects = HealthEffect.FROZEN;
        return effects;
    }

    protected void renderBarBackground(GuiGraphics graphics, Player player, int vOffset) {
        renderBarBackground(graphics, player, vOffset,false);
    }

    public void renderFlashBarBackground(GuiGraphics graphics, Player player, int vOffset) {
        renderBarBackground(graphics, player, vOffset,true);
    }

    protected void renderBarBackground(GuiGraphics graphics, Player player,  int vOffset,boolean flash) {
        double barWidth = displayedWidth;
        int xStart = graphics.guiWidth() / 2 + getHOffset();
        if (isFitted() && getSide() == BarSide.RIGHT) {
            xStart += WIDTH - barWidth;
        }
        int yStart = graphics.guiHeight() - vOffset;
        // 背景槽用白色 + 动画透明度
        Color.WHITE.color2Gl(graphics, displayedAlpha);

        if (isFitted()) {
            drawScaledBarBackground(graphics, barWidth, xStart, yStart + 1,flash);
        } else renderFullBarBackground(graphics, xStart, yStart,flash);
    }

    private void drawScaledBarBackground(GuiGraphics stack, double barWidth, int x, int y, boolean flash) {
        switch (getSide()) {
            case LEFT -> {
                ModUtils.drawTexturedModalRect(BAR,stack,x, y - 1, 0, flash ? 18 : 0, (int) (barWidth + 2), 9);
                ModUtils.drawTexturedModalRect(BAR,stack, (int) (x + barWidth + 2), y - 1, WIDTH + 2, flash ? 18 : 0, 2, 9);
            }
            case RIGHT -> {
                ModUtils.drawTexturedModalRect(BAR,stack,x, y - 1, 0, flash ? 18 : 0, barWidth + 2, 9);
                ModUtils.drawTexturedModalRect(BAR,stack,x + barWidth + 2, y-1, WIDTH + 2, flash ? 18 : 0, 2, 9);
            }
        }
    }
    public void textHelper(GuiGraphics graphics,int xStart,int yStart,double stat, int color) {
        int i1 = (int) Math.floor(stat);
        int i2 = barSettings.show_icon() ? 1 : 0;

        switch (getSide()) {
            case LEFT -> {
                int i3 = Minecraft.getInstance().font.width(i1 + "");
                ModUtils.drawStringOnHUD(graphics, i1 + "", xStart - 9 * i2 - i3 + 5, yStart - 1, color);
            }
            case RIGHT -> {
                ModUtils.drawStringOnHUD(graphics, i1 + "", xStart + 9 * i2, yStart - 1, color);
            }
        }
    }

    protected int getXStartBar(int screenWidth,int barWidth){
        int xStart = screenWidth / 2 + getHOffset();
        if (getSide() == BarSide.RIGHT) xStart += (WIDTH - barWidth);
        return xStart;
    }

    private void renderFullBarBackground(GuiGraphics matrices, int xStart, int yStart, boolean flash) {
        renderFullBarBackground(matrices,xStart,yStart,flash
        ? 18 : 0);
    }

    public void renderFullBarBackground(GuiGraphics matrices, int xStart, int yStart,int vOffset) {
        ModUtils.drawTexturedModalRect(BAR,matrices, xStart, yStart, 0, vOffset, WIDTH + 4, 9);
    }

    public void renderFullBar(Color color,GuiGraphics matrices, int xStart, int yStart) {
        renderPartialBar(color,matrices,xStart,yStart,WIDTH);
    }

    protected void renderSimpleBar(Color color, GuiGraphics graphics, Player player, int vOffset) {
        renderSimpleBar(color,graphics,player,vOffset,false);
    }

    protected void renderSimpleBar(Color color, GuiGraphics graphics, Player player, int vOffset,boolean highlight) {
        int barWidth = (int) Math.ceil(displayedWidth);
        int xStart = getXStartBar(graphics.guiWidth(),barWidth);
        int yStart = graphics.guiHeight() - vOffset;

        //Bar background
        renderBarBackground(graphics,player, vOffset,highlight);
        //draw portion of bar based on feathers amount
        renderPartialBar(color,graphics,xStart+2,yStart+2,barWidth);
    }

    public void renderPartialBar(Color color,GuiGraphics matrices, double xStart, int yStart,double barWidth) {
        color.color2Gl(matrices, displayedAlpha);
        ModUtils.drawTexturedModalRect(BAR,matrices, xStart, yStart, BAR_U, BAR_V, barWidth, HEIGHT);
    }

    /** 每帧更新条宽平滑过渡与透明度淡入淡出 */
    private void updateAnimation(Player player, boolean shouldShow) {
        // transition_speed <= 0 表示禁用动画：直接跳到目标状态（立即显示/隐藏，不产生卡住问题）
        if (animationFactor() <= 0) {
            displayedWidth = getBarWidth(player);
            displayedAlpha = shouldShow ? 1.0f : 0.0f;
            return;
        }
        double target = getBarWidth(player);
        if (displayedWidth < 0) {
            displayedWidth = target;
        } else {
            displayedWidth += (target - displayedWidth) * animationFactor();
            if (Math.abs(target - displayedWidth) < 0.1) {
                displayedWidth = target;
            }
        }
        float targetAlpha = shouldShow ? 1.0f : 0.0f;
        displayedAlpha += (targetAlpha - displayedAlpha) * (float) animationFactor();
        if (displayedAlpha < 0.01f) displayedAlpha = 0.0f;
        if (displayedAlpha > 0.99f) displayedAlpha = 1.0f;
    }

    /** 动画速度系数：基于 transition_speed 配置（值越大动画越快）。
     *  关键：当 transition_speed 为 0 时禁用动画（立即显示/隐藏）；
     *  当为极小正数时也需保证淡出不至于过慢——否则 alpha 每帧几乎不衰减，
     *  条件不满足的条会"始终显示"（看起来像 bug）。
     *  这里强制动画系数 ≥ MIN_FACTOR，保证淡出最多约 0.5 秒内完成（否则透明度永远降不到 0）。 */
    private static final double MIN_ANIMATION_FACTOR = 1 - Math.exp(-0.5); // ≈ 0.393，约 0.5 秒内完成淡出

    private static double animationFactor() {
        double speed = ClassicBarsConfig.getTransitionSpeed();
        if (speed <= 0) {
            return 0; // 禁用动画
        }
        return Math.max(1 - Math.exp(-speed * 0.1), MIN_ANIMATION_FACTOR);
    }

    public ResourceLocation getIconRL() {
        return barSettings.icon();
    }

    public final int getBarWidth(Player player) {
        return (int) Math.ceil(WIDTH* barInfo.getRatio(player));
    }

    @FunctionalInterface
    public interface Numerator {
        float getValue(Player player);
    }

    @FunctionalInterface
    public interface Denominator {
        float getValue(Player player);
    }

    @Override
    public boolean dependenciesMet() {
        return dependenciesMet;
    }

    protected static Denominator fixed(float value) {
        return p -> value;
    }

    public final boolean isFitted() {
        return barSettings.fitted();
    }
    @Override
    public final String name() {
        return barInfo.name();
    }

    public final boolean errored() {
        return errored;
    }

    public void setErrored() {
        this.errored = true;
    }

    @Override
    public final Optional<ResourceLocation> disablesOverlay() {
        return barSettings.disablesOverlay();
    }
}
