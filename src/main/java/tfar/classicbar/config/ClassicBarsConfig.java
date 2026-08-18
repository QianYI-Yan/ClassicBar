package tfar.classicbar.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import tfar.classicbar.ClassicBar;
import tfar.classicbar.EventHandler;
import tfar.classicbar.api.BarRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置门面：优先使用手动指定（/classicbar backend 命令）的后端，
 * 否则按运行时检测自动选择（cloth-config > 内存默认）。
 * 想切换测试不同配置库时，用命令或放入/移除对应库即可。
 */
public class ClassicBarsConfig {

  private static ConfigBackend backend;

  /** 客户端初始化时调用：选择后端并加载条配置 */
  public static void init() {
    backend = createBackend();
    ClassicBar.logger.info("Classic Bar 使用配置后端: {}", backend.getName());
    EventHandler.cacheConfigs();
  }

  /** 手动切换后端（/classicbar backend 命令调用），选择结果持久化到文件 */
  public static void switchBackend(String name) {
    saveForcedBackend(name);
    init();
  }

  private static ConfigBackend createBackend() {
    String forced = loadForcedBackend();
    if (forced != null) {
      switch (forced) {
        case "cloth-config" -> {
          if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return new ClothConfigBackend();
          }
        }
        case "memory" -> {
          return new MemoryConfigBackend();
        }
      }
    }
    // 自动检测：优先 cloth-config，否则内存默认
    if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
      return new ClothConfigBackend();
    }
    return new MemoryConfigBackend();
  }

  private static Path backendFile() {
    return FabricLoader.getInstance().getConfigDir().resolve(ClassicBar.MODID).resolve("backend.txt");
  }

  private static String loadForcedBackend() {
    Path file = backendFile();
    if (!Files.exists(file)) {
      return null;
    }
    try {
      return Files.readString(file).trim();
    } catch (IOException e) {
      return null;
    }
  }

  private static void saveForcedBackend(String name) {
    try {
      Path file = backendFile();
      file.getParent().toFile().mkdirs();
      Files.writeString(file, name);
    } catch (IOException e) {
      ClassicBar.logger.error("保存配置后端选择失败", e);
    }
  }

  /** 条颜色/预测叠加的过渡动画速度 */
  public static double getTransitionSpeed() {
    return backend.getTransitionSpeed();
  }

  /** 各条的渲染优先级（从上到下） */
  public static List<String> getPriority() {
    return backend.getPriority();
  }

  /**
   * 刷新优先级列表：仅在列表为空时填充注册顺序。
   * 注意：不能每次 cacheConfigs 都覆盖，否则用户配置的优先级永远不生效。
   */
  public static void reloadPriority() {
    if (backend.getPriority().isEmpty()) {
      backend.setPriority(new ArrayList<>(BarRegistry.REGISTRY.keySet()));
    }
  }

  /** 生成配置 GUI（供 ModMenu 或 /classicbar config 命令调用） */
  public static Screen createConfigScreen(Screen parent) {
    return backend.createConfigScreen(parent);
  }
}