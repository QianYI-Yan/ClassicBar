package tfar.classicbar.api;

import com.mojang.serialization.Codec;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public interface BarOverlay {

  BarSide getSide();

  boolean render(Gui gui, GuiGraphicsExtractor graphics, Player player, int vOffset);

  boolean dependenciesMet();
  Optional<Identifier> disablesOverlay();
  void setErrored();

  String name();
  Codec<? extends BarOverlay> codec();
}