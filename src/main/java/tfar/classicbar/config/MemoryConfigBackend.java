package tfar.classicbar.config;

import net.minecraft.client.gui.screens.Screen;
import tfar.classicbar.api.BarRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 内存默认后端：无外部依赖的兜底实现，不提供 GUI。
 * 仅当未安装 cloth-config / YACL 时使用。
 */
public class MemoryConfigBackend implements ConfigBackend {

    private double transitionSpeed = 3;
    private final List<String> priority = new ArrayList<>(BarRegistry.REGISTRY.keySet());

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
        // 内存后端无需持久化
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return null;
    }

    @Override
    public String getName() {
        return "memory";
    }
}
