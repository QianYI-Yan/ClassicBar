package tfar.classicbar.api;

import com.mojang.serialization.Codec;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public interface BarOverlay {

  BarSide getSide();

  boolean render(Gui gui, GuiGraphics graphics, Player player, int vOffset);

  boolean dependenciesMet();
  Optional<ResourceLocation> disablesOverlay();
  void setErrored();

  String name();
  Codec<? extends BarOverlay> codec();
}