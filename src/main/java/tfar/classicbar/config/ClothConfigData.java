package tfar.classicbar.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import tfar.classicbar.api.BarRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * cloth-config 的配置数据类（AutoConfig + JSON 序列化）。
 * 配置文件位于 config/classicbar.json。
 */
@Config(name = "classicbar")
public class ClothConfigData implements ConfigData {

    /** 条颜色/预测叠加的过渡动画速度 */
    @ConfigEntry.Gui.Tooltip
    public double transition_speed = 3;

    /** 各条的渲染优先级（从上到下） */
    @ConfigEntry.Gui.Tooltip
    public List<String> priority = new ArrayList<>(BarRegistry.REGISTRY.keySet());
}
