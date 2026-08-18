package tfar.classicbar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tfar.classicbar.ClassicBar;
import tfar.classicbar.EventHandler;
import tfar.classicbar.api.BarRegistry;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * YACL 后端：提供现代化的配置界面（vanilla 风格）。
 * YACL 只负责 GUI，配置数据持久化到 config/classicbar/yacl_settings.json。
 */
public class YaclConfigBackend implements ConfigBackend {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private double transitionSpeed = 3;
    private final List<String> priority = new ArrayList<>(BarRegistry.REGISTRY.keySet());

    public YaclConfigBackend() {
        load();
    }

    @Override
    public double getTransitionSpeed() {
        return transitionSpeed;
    }

    @Override
    public void setTransitionSpeed(double transitionSpeed) {
        this.transitionSpeed = transitionSpeed;
    }

    @Override
    public List<String> getPriority() {
        return priority;
    }

    @Override
    public void setPriority(List<String> priority) {
        this.priority.clear();
        this.priority.addAll(priority);
    }

    @Override
    public void save() {
        File file = settingsFile().toFile();
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            Settings settings = new Settings(transitionSpeed, priority);
            GSON.toJson(settings, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void load() {
        File file = settingsFile().toFile();
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Settings settings = GSON.fromJson(reader, Settings.class);
            if (settings != null) {
                transitionSpeed = settings.transitionSpeed;
                if (settings.priority != null) {
                    priority.clear();
                    priority.addAll(settings.priority);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path settingsFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(ClassicBar.MODID).resolve("yacl_settings.json");
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Classic Bar"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("General"))
                        .option(Option.<Double>createBuilder()
                                .name(Component.literal("Transition Speed"))
                                .description(OptionDescription.of(Component.literal("条颜色/预测叠加的过渡动画速度")))
                                .binding(3.0, () -> transitionSpeed, v -> transitionSpeed = v)
                                .controller(option -> DoubleSliderControllerBuilder.create(option).range(0.0, 50.0).step(0.1))
                                .listener((opt, val) -> {
                                    save();
                                    EventHandler.cacheConfigs();
                                })
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.literal("Priority"))
                                .description(OptionDescription.of(Component.literal("渲染优先级，按条名逗号分隔")))
                                .binding(String.join(",", priority), () -> String.join(",", priority), this::parsePriority)
                                .controller(option -> StringControllerBuilder.create(option))
                                .listener((opt, val) -> {
                                    save();
                                    EventHandler.cacheConfigs();
                                })
                                .build())
                        .build())
                .save(this::save)
                .build()
                .generateScreen(parent);
    }

    private void parsePriority(String value) {
        priority.clear();
        for (String s : value.split(",")) {
            if (!s.isBlank()) {
                priority.add(s.trim());
            }
        }
    }

    @Override
    public String getName() {
        return "yacl";
    }

    /** YACL 配置的 JSON 持久化结构 */
    private record Settings(double transitionSpeed, List<String> priority) {
    }
}
