package tfar.classicbar.impl.overlays.templates;

import com.mojang.serialization.Codec;
import tfar.classicbar.api.BarOverlay;
import tfar.classicbar.api.BarSettings;
import tfar.classicbar.impl.BarInfo;

/**
 * 通用简单条模板（保留占位）：后续迁移第三方模组兼容时，
 * 通过子类工厂方法创建对应的条（如羽毛条）。
 */
public class SimpleBarOverlay extends BarOverlayImpl {

    private Codec<? extends SimpleBarOverlay> codec;

    public SimpleBarOverlay(BarSettings settings, BarInfo barInfo, Codec<? extends SimpleBarOverlay> codec) {
        super(barInfo, settings);
        this.codec = codec;
    }

    @Override
    public Codec<? extends BarOverlay> codec() {
        return codec;
    }
}
