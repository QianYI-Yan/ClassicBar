package tfar.classicbar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import tfar.classicbar.ClassicBar;
import tfar.classicbar.EventHandler;
import tfar.classicbar.api.BarOverlay;
import tfar.classicbar.api.BarRegistry;
import tfar.classicbar.api.BarSide;
import tfar.classicbar.api.Color;
import tfar.classicbar.api.colorprovider.ColorProvider;
import tfar.classicbar.api.colorprovider.DualColorProvider;
import tfar.classicbar.api.colorprovider.DualEffectColorProvider;
import tfar.classicbar.api.colorprovider.DualEffectMapColorProvider;
import tfar.classicbar.api.colorprovider.SingleColorProvider;
import tfar.classicbar.api.colorprovider.StackingColorProvider;
import tfar.classicbar.api.colorprovider.StackingEffectColorProvider;
import tfar.classicbar.api.colorprovider.TransitioningColorProvider;
import tfar.classicbar.api.colorprovider.TransitioningEffectColorProvider;
import tfar.classicbar.impl.overlays.templates.BarOverlayImpl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * cloth-config 后端：提供完整的图形化配置界面（支持 ModMenu 入口）。
 * <p>
 * 界面结构（使用 ConfigBuilder 手动构建，支持动态生成每个条的分类）：
 * <ul>
 *   <li>通用分类：过渡速度、渲染优先级（持久化到 config/classicbar.json）</li>
 *   <li>每个条一个分类：启用、位置、自适应、显示数值、显示图标、图标路径、颜色
 *       （持久化到 config/classicbar/&lt;条名&gt;.json 的 bar_settings 节点）</li>
 * </ul>
 */
public class ClothConfigBackend implements ConfigBackend {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** AutoConfig 只能注册一次，用静态标志避免热切换后端时重复注册报错 */
    private static boolean registered = false;

    private final ClothConfigData data;

    public ClothConfigBackend() {
        if (!registered) {
            AutoConfig.register(ClothConfigData.class, GsonConfigSerializer::new);
            // 配置保存后重载条列表，让修改立即生效（仅首次注册时挂监听）
            AutoConfig.getConfigHolder(ClothConfigData.class).registerSaveListener((holder, config) -> {
                EventHandler.cacheConfigs();
                return InteractionResult.SUCCESS;
            });
            registered = true;
        }
        this.data = AutoConfig.getConfigHolder(ClothConfigData.class).getConfig();
    }

    @Override
    public double getTransitionSpeed() {
        return data.transition_speed;
    }

    @Override
    public void setTransitionSpeed(double transitionSpeed) {
        data.transition_speed = transitionSpeed;
    }

    @Override
    public List<String> getPriority() {
        return data.priority;
    }

    @Override
    public void setPriority(List<String> priority) {
        data.priority.clear();
        data.priority.addAll(priority);
    }

    @Override
    public void save() {
        AutoConfig.getConfigHolder(ClothConfigData.class).save();
    }

    @Override
    public String getName() {
        return "cloth-config";
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("text.autoconfig.classicbar.title"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ================= 通用分类 =================
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("classicbar.config.category.general"));
        general.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("classicbar.config.transition_speed"), data.transition_speed)
                .setDefaultValue(3.0)
                // 下限 0.5：避免极小值导致动画系数趋近 0，条件不满足的条会因淡出过慢而"始终显示"
                .setMin(0.5)
                .setTooltip(Component.translatable("classicbar.config.transition_speed.tooltip"))
                .setSaveConsumer(value -> data.transition_speed = value)
                .build());
        general.addEntry(entryBuilder.startStrList(
                        Component.translatable("classicbar.config.priority"), new ArrayList<>(data.priority))
                .setDefaultValue(new ArrayList<>(BarRegistry.REGISTRY.keySet()))
                .setTooltip(Component.translatable("classicbar.config.priority.tooltip"))
                .setSaveConsumer(value -> {
                    data.priority.clear();
                    data.priority.addAll(value);
                })
                .build());

        // ================= 每个条一个分类 =================
        Map<String, JsonObject> barFiles = new LinkedHashMap<>();
        List<BarColorState> colorStates = new ArrayList<>();
        for (Map.Entry<String, BarOverlay> entry : BarRegistry.REGISTRY.entrySet()) {
            String name = entry.getKey();
            BarOverlay defaults = entry.getValue();
            // 读取当前条 JSON（文件不存在时用默认实例编码生成），界面修改直接写回该 JSON
            JsonObject barJson = loadBarJson(name, defaults);
            barFiles.put(name, barJson);
            JsonObject settings = barSettingsObject(barJson);

            ConfigCategory category = builder.getOrCreateCategory(barName(name));

            boolean enabled = getBool(settings, "enabled", true);
            category.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("classicbar.config.enabled"), enabled)
                    .setDefaultValue(enabled)
                    .setSaveConsumer(value -> settings.addProperty("enabled", value))
                    .build());

            BarSide side = getSide(settings);
            category.addEntry(entryBuilder.startEnumSelector(
                            Component.translatable("classicbar.config.side"), BarSide.class, side)
                    .setDefaultValue(side)
                    .setSaveConsumer(value -> settings.addProperty("side", value.getSerializedName()))
                    .build());

            boolean fitted = getBool(settings, "fitted", false);
            category.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("classicbar.config.fitted"), fitted)
                    .setDefaultValue(fitted)
                    .setSaveConsumer(value -> settings.addProperty("fitted", value))
                    .build());

            boolean showText = getBool(settings, "show_text", true);
            category.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("classicbar.config.show_text"), showText)
                    .setDefaultValue(showText)
                    .setSaveConsumer(value -> settings.addProperty("show_text", value))
                    .build());

            boolean showIcon = getBool(settings, "show_icon", true);
            category.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("classicbar.config.show_icon"), showIcon)
                    .setDefaultValue(showIcon)
                    .setSaveConsumer(value -> settings.addProperty("show_icon", value))
                    .build());

            String icon = getString(settings, "icon", "classicbar:textures/gui/icons.png");
            category.addEntry(entryBuilder.startStrField(
                            Component.translatable("classicbar.config.icon"), icon)
                    .setDefaultValue(icon)
                    .setSaveConsumer(value -> settings.addProperty("icon", value))
                    .build());

            // 颜色：允许 Alpha 的颜色选择器。默认值=默认渐变主色（cloth-config 右键可重置回默认渐变）。
            // 不设 saveConsumer（entry.save 会无条件调用，会覆盖渐变），改为在 savingRunnable 里
            // 对比「当前值 vs 打开时的值」，只有用户修改过才写回：
            //   值==默认主色 → 恢复默认完整渐变；其他值 → 写为纯色
            int originalColor = getCurrentColor(settings, defaults).colorToText();
            BarOverlayImpl defaultImpl = (BarOverlayImpl) defaults;
            ColorProvider defaultProvider = defaultImpl.getBarSettings().colorProvider();
            int defaultPrimaryColor = getPrimaryColor(defaultProvider).colorToText();
            var colorEntry = entryBuilder.startAlphaColorField(
                            Component.translatable("classicbar.config.color"), originalColor)
                    .setDefaultValue(defaultPrimaryColor)
                    .setTooltip(Component.translatable("classicbar.config.color.tooltip"))
                    .build();
            category.addEntry(colorEntry);
            colorStates.add(new BarColorState(settings, originalColor, defaultPrimaryColor,
                    defaultProvider, colorEntry::getValue));
        }

        builder.setSavingRunnable(() -> {
            // 颜色：仅当用户修改过（当前值 != 打开时的值）才写回
            for (BarColorState state : colorStates) {
                int current = state.currentColorSupplier.get();
                if (current == state.originalColor) {
                    continue; // 未修改，跳过（保持现状，不覆盖渐变）
                }
                if (current == state.defaultPrimaryColor) {
                    // 值等于默认主色（用户右键重置）→ 恢复默认完整渐变
                    ColorProvider.CODEC.encodeStart(JsonOps.INSTANCE, state.defaultProvider)
                            .result()
                            .ifPresent(element -> state.settings.add("color_provider", element));
                } else {
                    // 用户自定义颜色 → 写为纯色
                    state.settings.add("color_provider", singleColorProvider(current));
                }
            }
            // 写回所有条 JSON（其他字段的 saveConsumer 已把改动写进对应 JsonObject）
            for (Map.Entry<String, JsonObject> entry : barFiles.entrySet()) {
                writeBarJson(entry.getKey(), entry.getValue());
            }
            // 保存通用配置并重载 HUD
            AutoConfig.getConfigHolder(ClothConfigData.class).save();
            EventHandler.cacheConfigs();
        });

        return builder.build();
    }

    // ================= 辅助方法 =================

    private static File barFile(String name) {
        return FabricLoader.getInstance().getConfigDir().resolve(ClassicBar.MODID).resolve(name + ".json").toFile();
    }

    /** 读取条 JSON；文件不存在时用默认实例的 codec 编码生成一份 */
    private static JsonObject loadBarJson(String name, BarOverlay defaults) {
        File file = barFile(name);
        if (file.exists()) {
            try (JsonReader reader = new JsonReader(new FileReader(file))) {
                JsonElement element = GSON.fromJson(reader, JsonElement.class);
                if (element != null && element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
            } catch (IOException e) {
                ClassicBar.logger.error("读取条配置文件失败 {}", name, e);
            }
        }
        // 与 EventHandler.tryWrite 一致：codec 是 Codec<? extends BarOverlay>，需强转后才能编码默认实例
        @SuppressWarnings("unchecked")
        Codec<BarOverlay> codec = (Codec<BarOverlay>) defaults.codec();
        JsonElement generated = codec.encodeStart(JsonOps.INSTANCE, defaults).result().orElse(null);
        if (generated != null && generated.isJsonObject()) {
            return generated.getAsJsonObject();
        }
        return new JsonObject();
    }

    /** 写回条 JSON（4 空格缩进，与 EventHandler 的格式一致） */
    private static void writeBarJson(String name, JsonObject json) {
        try {
            File file = barFile(name);
            file.getParentFile().mkdirs();
            try (JsonWriter writer = GSON.newJsonWriter(new FileWriter(file))) {
                writer.setIndent("    ");
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            ClassicBar.logger.error("写入条配置文件失败 {}", name, e);
        }
    }

    /** 取出 bar_settings 节点；缺失时创建 */
    private static JsonObject barSettingsObject(JsonObject barJson) {
        JsonElement element = barJson.get("bar_settings");
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        JsonObject settings = new JsonObject();
        barJson.add("bar_settings", settings);
        return settings;
    }

    private static boolean getBool(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                ? element.getAsBoolean() : fallback;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static BarSide getSide(JsonObject settings) {
        JsonElement element = settings.get("side");
        if (element != null && element.isJsonPrimitive() && "right".equals(element.getAsString())) {
            return BarSide.RIGHT;
        }
        return BarSide.LEFT;
    }

    /** 读取当前 color_provider 的主色（用于颜色选择器的初始值） */
    private static Color getCurrentColor(JsonObject settings, BarOverlay defaults) {
        JsonElement element = settings.get("color_provider");
        if (element != null) {
            ColorProvider provider = ColorProvider.CODEC.parse(JsonOps.INSTANCE, element).result().orElse(null);
            if (provider != null) {
                return getPrimaryColor(provider);
            }
        }
        if (defaults instanceof BarOverlayImpl impl) {
            return getPrimaryColor(impl.getBarSettings().colorProvider());
        }
        return Color.WHITE;
    }

    /**
     * 提取颜色提供器的默认主色（不依赖 player，纯字段提取）：
     * 单色/双色取第一个颜色，堆叠取第一档，渐变取第一个颜色点，效果类取 normal 组第一个颜色。
     */
    private static Color getPrimaryColor(ColorProvider provider) {
        if (provider instanceof SingleColorProvider single) {
            return single.color();
        }
        if (provider instanceof DualColorProvider dual) {
            return dual.primary();
        }
        if (provider instanceof DualEffectColorProvider dualEffect) {
            return dualEffect.primary();
        }
        if (provider instanceof DualEffectMapColorProvider dualMap) {
            return dualMap.primary();
        }
        if (provider instanceof StackingColorProvider stacking && !stacking.colors().isEmpty()) {
            return stacking.colors().get(0);
        }
        if (provider instanceof StackingEffectColorProvider stackingEffect && !stackingEffect.normalColors().isEmpty()) {
            return stackingEffect.normalColors().get(0);
        }
        if (provider instanceof TransitioningColorProvider transitioning) {
            return transitioning.colors().values().stream().findFirst().orElse(Color.WHITE);
        }
        if (provider instanceof TransitioningEffectColorProvider transitioningEffect) {
            return transitioningEffect.normalColors().values().stream().findFirst().orElse(Color.WHITE);
        }
        return Color.WHITE;
    }

    /** 生成单色 color_provider 节点：{"type":"single","color":"#AARRGGBB"} */
    private static JsonObject singleColorProvider(int argb) {
        Color color = Color.fromRGBA((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
        JsonObject object = new JsonObject();
        object.addProperty("type", "single");
        object.addProperty("color", color.toHexString());
        return object;
    }

    /** 条分类名：优先使用翻译，缺失时显示原始条名（26.2 中 I18n.exists 已移除，改用 get 返回值回退判断） */
    private static Component barName(String name) {
        String key = "classicbar.config.bar." + name;
        return !I18n.get(key).equals(key) ? Component.translatable(key) : Component.literal(name);
    }

    /** 记录每个条的颜色编辑状态，用于保存时判断颜色是否被修改过 */
    private static class BarColorState {
        final JsonObject settings;
        final int originalColor;
        final int defaultPrimaryColor;
        final ColorProvider defaultProvider;
        final Supplier<Integer> currentColorSupplier;

        BarColorState(JsonObject settings, int originalColor, int defaultPrimaryColor,
                      ColorProvider defaultProvider, Supplier<Integer> currentColorSupplier) {
            this.settings = settings;
            this.originalColor = originalColor;
            this.defaultPrimaryColor = defaultPrimaryColor;
            this.defaultProvider = defaultProvider;
            this.currentColorSupplier = currentColorSupplier;
        }
    }
}
