package tfar.classicbar.config;

import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * 配置后端抽象：支持多个配置库（cloth-config / 内存默认）。
 * 通过运行时检测自动选择后端，方便切换测试不同库的 GUI 效果。
 */
public interface ConfigBackend {

    /** 条颜色/预测叠加的过渡动画速度 */
    double getTransitionSpeed();

    void setTransitionSpeed(double transitionSpeed);

    /** 各条的渲染优先级（从上到下） */
    List<String> getPriority();

    void setPriority(List<String> priority);

    /** 保存配置到磁盘 */
    void save();

    /** 生成配置 GUI 屏幕；不支持 GUI 的后端返回 null */
    Screen createConfigScreen(Screen parent);

    /** 后端名称（用于日志） */
    String getName();
}
