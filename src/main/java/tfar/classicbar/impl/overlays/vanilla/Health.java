package tfar.classicbar.impl.overlays.vanilla;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import tfar.classicbar.api.BarSettings;
import tfar.classicbar.api.BarSide;
import tfar.classicbar.impl.BarInfo;
import tfar.classicbar.impl.overlays.templates.BarOverlayImpl;
import tfar.classicbar.api.Color;
import tfar.classicbar.util.HealthEffect;
import tfar.classicbar.util.ModUtils;

public class Health extends BarOverlayImpl {

  private double playerHealth = 0;
  private long healthUpdateCounter = 0;
  private double lastPlayerHealth = 0;

  public static final BarInfo INFO = BarInfo.createSimpleVanilla("health",
          player -> true,LivingEntity::getHealth, LivingEntity::getMaxHealth);

  public Health(BarSettings settings) {
    super(INFO,settings);
  }

  public static final Codec<Health> CODEC = RecordCodecBuilder.create(
          objectInstance -> codecStart(objectInstance).apply(objectInstance,Health::new)
  );

  @Override
  public Codec<? extends BarOverlayImpl> codec() {
    return CODEC;
  }

  @Override
  public void renderBar(Gui gui, GuiGraphics graphics, Player player, int vOffset) {
    int updateCounter = gui.getGuiTicks();

    double health = player.getHealth();
    double barWidth = displayedWidth;
    boolean highlight = healthUpdateCounter > (long) updateCounter && (healthUpdateCounter - (long) updateCounter) / 3 % 2 == 1;

    //player is damaged and resistant
    // 1.21.11 中 invulnerableTime/invulnerableDuration 已移除，用 hurtTime/hurtDuration（受伤动画时间）替代
    if (health < playerHealth && player.hurtTime > 0) {
      healthUpdateCounter = updateCounter + 20;
      lastPlayerHealth = playerHealth;
    } else if (health > playerHealth && player.hurtTime > 0) {
      healthUpdateCounter = updateCounter + 10;
      /* lastPlayerHealth = playerHealth;*/
    }
    playerHealth = health;
    double displayHealth = health + (lastPlayerHealth - health) * ((double) player.hurtTime / player.hurtDuration);

    int xStart = graphics.guiWidth() / 2 + getHOffset();
    int yStart = graphics.guiHeight() - vOffset;
    double maxHealth = player.getMaxHealth();


      //Bar background
   // renderBarBackground(graphics,player,screenWidth,screenHeight,vOffset,highlight);

    double f = xStart + (getSide() == BarSide.RIGHT ? WIDTH - barWidth : 0);

    //is the bar changing
    //Pass 1, draw bar portion
    //interpolate the bar
    if (displayHealth != health) {
      //reset to white
      if (displayHealth > health) {
        //draw interpolation
        double w = BarOverlayImpl.getWidth(displayHealth, maxHealth);
        double off = getSide() == BarSide.RIGHT ? w - barWidth : 0;
        //draw interpolation
        renderPartialBar(Color.WHITE,graphics,f + 2 - off, yStart + 2,w);
        //Health is increasing, IDK what to do here
      } else {/*
                  f = xStart + getWidth(health, maxHealth);
                  drawTexturedModalRect(f, yStart + 1, 1, 10, getWidth(health - displayHealth, maxHealth), 7, general.style, true, true);*/
      }
    }
    //draw portion of bar based on health remaining
   // Color primary = getBarSettings().colorProvider().getColor(player, ,0);

    renderSimpleBar(getBarSettings().colorProvider().getColor(player,barInfo.getRatio(player),0), graphics, player, vOffset,highlight);

    HealthEffect effect = getHealthEffect(player);

    //renderPartialBar(primary,graphics,f + 2, yStart + 2, barWidth);
    if (effect == HealthEffect.POISON) {
      //draw poison overlay（1.21.11 用 blit 颜色参数半透明绿色叠加）
      ModUtils.drawTexturedModalRect(getIconRL(),graphics,(int) f + 1, yStart + 1, 1, 36, (int) barWidth, 7, tintedArgb(Color.fromRGB(0, 255, 0).withAlpha(0.5f)));
    }
  }

  @Override
  public void renderIcon(GuiGraphics graphics, Player player, int vOffset) {
    HealthEffect effect = getHealthEffect(player);

    int xStart = graphics.guiWidth() / 2 + getIconOffset();
    int yStart = graphics.guiHeight() - vOffset;
    int i5 = (player.level().getLevelData().isHardcore()) ? 5 : 0;
    //Draw health icon
    //heart background
    ModUtils.drawTexturedModalRect(getIconRL(),graphics,xStart, yStart, 16, 9 * i5, 9, 9);
    //heart
    ModUtils.drawTexturedModalRect(getIconRL(),graphics,xStart, yStart, 36 + effect.i, 9 * i5, 9, 9);
  }
}