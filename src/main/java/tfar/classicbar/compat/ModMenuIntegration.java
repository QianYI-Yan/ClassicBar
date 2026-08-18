package tfar.classicbar.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import tfar.classicbar.config.ClassicBarsConfig;
import tfar.classicbar.config.ClothConfigBackend;
import tfar.classicbar.config.YaclConfigBackend;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ModMenu 集成：
 * - 主配置按钮：当前激活的配置后端
 * - 子设置页面：cloth-config 与 YACL 各注册一个，无论主后端是哪个都能直接打开对应的界面
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClassicBarsConfig::createConfigScreen;
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            factories.put("Cloth Config", parent -> new ClothConfigBackend().createConfigScreen(parent));
        }
        if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib")) {
            factories.put("YACL", parent -> new YaclConfigBackend().createConfigScreen(parent));
        }
        return factories;
    }
}
