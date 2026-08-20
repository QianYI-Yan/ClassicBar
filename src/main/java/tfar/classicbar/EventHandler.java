package tfar.classicbar;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import tfar.classicbar.api.BarOverlay;
import tfar.classicbar.api.BarRegistry;
import tfar.classicbar.api.BarSide;
import tfar.classicbar.config.ClassicBarsConfig;

import java.io.*;
import java.util.*;

/**
 * 经典条 HUD 渲染器：通过 HudRenderCallback 在 HUD 末尾绘制所有已注册的条。
 * 原版血条/护甲/饥饿/氧气等已被 mixin 禁用，因此这里需要手动初始化布局偏移。
 */
public class EventHandler {

  private static final List<BarOverlay> registry = new ArrayList<>();

  // 布局偏移：左侧/右侧当前已堆叠的高度（原版 HUD 内部管理，被禁用后这里自行管理）
    private static int leftOffset = 39;
    private static int rightOffset = 39;
  public static void render(GuiGraphics matrices, float partialTick) {
    Minecraft mc = Minecraft.getInstance();
    Gui gui = mc.gui;
    Entity entity = mc.getCameraEntity();
    if (!(entity instanceof Player player)) return;
    if (player.getAbilities().instabuild || player.isSpectator()) return;

    // 原版 bars 已被 mixin 禁用，从物品栏上方的位置（原版血条高度 39px）向上堆叠
    leftOffset = 39;
    rightOffset = 39;

    for (BarOverlay overlay : registry) {
      BarSide side = overlay.getSide();
      try {
        if (overlay.render(gui, matrices, player, getOffset(side))) {
          increment(side, 10);
        }
      } catch (Throwable e) {
        ClassicBar.logger.error("禁用异常的 overlay {}", overlay.name());
        e.printStackTrace();
        overlay.setErrored();
      }
    }
  }

  public static void increment(BarSide side, int amount){
    switch (side) {
      case LEFT -> leftOffset += amount;
      case RIGHT -> rightOffset += amount;
    }
  }

  public static int getOffset(BarSide side) {
    return switch (side) {
      case RIGHT -> rightOffset;
      case LEFT -> leftOffset;
    };
  }

  public static void cacheConfigs() {
    BarRegistry.init();
    loadBarFiles();
    ClassicBarsConfig.reloadPriority();
    registry.sort(Comparator.comparingInt(o -> ClassicBarsConfig.getPriority().indexOf(o.name())));
  }

  public static void loadBarFiles() {
    registry.clear();
    FabricLoader.getInstance().getConfigDir().resolve(ClassicBar.MODID).toFile().mkdirs();
    Gson gson = new Gson();
    for (Map.Entry<String, BarOverlay> entry : BarRegistry.REGISTRY.entrySet()) {
      File file = FabricLoader.getInstance().getConfigDir().resolve(ClassicBar.MODID).resolve(entry.getKey() + ".json").toFile();
      if (!file.exists()) {
        try {
          tryWrite(gson, entry.getKey(), file);
          tryRead(gson, entry.getKey(), file);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        try {
          tryRead(gson, entry.getKey(), file);
        } catch (Exception e) {
          ClassicBar.logger.error("加载条文件失败 {}", entry.getKey(), e);
          e.printStackTrace();
          // 写入一个新文件
          try {
            tryWrite(gson, entry.getKey(), file);
            tryRead(gson, entry.getKey(), file);
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        }
      }
    }
  }

  static void tryWrite(Gson gson, String name, File file) throws IOException {
    try (JsonWriter writer = gson.newJsonWriter(new FileWriter(file))) {
      writer.setIndent("    ");

      BarOverlay overlay = BarRegistry.REGISTRY.get(name);
      Codec<BarOverlay> codec = (Codec<BarOverlay>) overlay.codec();

      JsonElement element = codec.encodeStart(JsonOps.INSTANCE, overlay).resultOrPartial(ClassicBar.logger::error).orElseThrow();
      gson.toJson(element, writer);
    }
  }

  static void tryRead(Gson gson, String name, File file) throws IOException {
    try (JsonReader reader = gson.newJsonReader(new FileReader(file))) {
      JsonObject json = gson.fromJson(reader, JsonObject.class);
      BarOverlay barOverlay = BarRegistry.REGISTRY.get(name).codec().parse(new Dynamic<>(JsonOps.INSTANCE, json)).result().orElseThrow();
      registry.add(barOverlay);
    }
  }
}