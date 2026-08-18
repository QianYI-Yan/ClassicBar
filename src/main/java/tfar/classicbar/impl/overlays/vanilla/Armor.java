package tfar.classicbar.impl.overlays.vanilla;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2i;
import tfar.classicbar.api.BarSettings;
import tfar.classicbar.impl.BarInfo;
import tfar.classicbar.impl.IconData;
import tfar.classicbar.impl.overlays.templates.StackingBarOverlay;
import tfar.classicbar.util.ModUtils;

import java.util.List;

/**
 * 护甲条：
 * 检测到 DAB（Detail Armor Bar，modid: detailab）激活时让位，
 * 不渲染护甲条/图标，只保留护甲数值显示，避免重复。
 */
public class Armor extends StackingBarOverlay {

    /** DAB（Detail Armor Bar Reconstructed）的 modid */
    private static final String DAB_MOD_ID = "detailab";

    /** 是否安装了 DAB（静态检测，客户端加载时确定） */
    private static final boolean DAB_ACTIVE = FabricLoader.getInstance().isModLoaded(DAB_MOD_ID);

    public static final BarInfo INFO = BarInfo.getBuilder("armor")
            .setShouldRender(player -> player.getArmorValue() >= 1)
            .setNumerator(Player::getArmorValue)
            .setDenominator(fixed(20f))
            .setIconData(new IconData(List.of(new Vector2i(43, 9)))).build();

    public Armor(BarSettings barSettings) {
        super(INFO, barSettings, CODEC);
    }

    public static final Codec<Armor> CODEC = RecordCodecBuilder.create(
            objectInstance -> codecStart(objectInstance)
                    .apply(objectInstance, Armor::new)
    );

    @Override
    public boolean render(Gui gui, GuiGraphics graphics, Player player, int vOffset) {
        if (DAB_ACTIVE) {
            // DAB 激活：让位，只保留护甲数值显示
            if (shouldRender(player)) {
                ModUtils.setupOverlayRenderState(true, false);
                if (getBarSettings().show_text()) {
                    renderText(graphics, player, vOffset);
                }
                return true;
            }
            return false;
        }
        return super.render(gui, graphics, player, vOffset);
    }
}