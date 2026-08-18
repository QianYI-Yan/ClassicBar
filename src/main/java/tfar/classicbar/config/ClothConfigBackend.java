package tfar.classicbar.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionResult;
import tfar.classicbar.EventHandler;

import java.util.List;

/**
 * cloth-config 后端：提供现代化的图形化配置界面（支持 ModMenu 入口）。
 */
public class ClothConfigBackend implements ConfigBackend {

    /** AutoConfig 只能注册一次，用静态标志避免热切换后端时重复注册报错 */
    private static boolean registered = false;

    private final ClothConfigData data;

    public ClothConfigBackend() {
        if (!registered) {
            AutoConfig.register(ClothConfigData.class, GsonConfigSerializer::new);
            // 配置保存后重载条列表，让修改立即生效（仅首次注册时挂监听，避免重复监听导致多次重载）
            AutoConfig.getConfigHolder(ClothConfigData.class).registerSaveListener((h, config) -> {
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
    public Screen createConfigScreen(Screen parent) {
        return AutoConfig.getConfigScreen(ClothConfigData.class, parent).get();
    }

    @Override
    public String getName() {
        return "cloth-config";
    }
}
