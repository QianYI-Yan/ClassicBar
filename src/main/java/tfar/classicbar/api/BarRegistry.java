package tfar.classicbar.api;

import net.minecraft.resources.Identifier;
import tfar.classicbar.api.colorprovider.*;
import tfar.classicbar.impl.overlays.templates.BarOverlayImpl;
import tfar.classicbar.impl.overlays.vanilla.*;

import java.util.LinkedHashMap;

public class BarRegistry {
    public static final LinkedHashMap<String, BarOverlay> REGISTRY = new LinkedHashMap<>();

    // 原版 HUD 元素 ID（记录用途：对应的原版显示会被本模组替换）
    public static final Identifier PLAYER_HEALTH = Identifier.fromNamespaceAndPath("minecraft", "player_health");
    public static final Identifier MOUNT_HEALTH = Identifier.fromNamespaceAndPath("minecraft", "mount_health");
    public static final Identifier FOOD_LEVEL = Identifier.fromNamespaceAndPath("minecraft", "food_level");
    public static final Identifier AIR_LEVEL = Identifier.fromNamespaceAndPath("minecraft", "air_level");
    public static final Identifier ARMOR_LEVEL = Identifier.fromNamespaceAndPath("minecraft", "armor_level");

    public static <B extends BarOverlay> void registerBar(B defaults) {
        if (defaults.dependenciesMet()) {// 依赖不满足的条不注册
            REGISTRY.put(defaults.name(), defaults);
        }
    }

    public static void init() {

    }

    static {
        registerBar(new Health(BarSettings.getBuilder()
                .setDisablesOverlay(PLAYER_HEALTH)
                .setColorProvider(TransitioningEffectColorProvider.DEFAULT).build()));
        registerBar(new MountHealth(BarSettings.getBuilder()
                .setDisablesOverlay(MOUNT_HEALTH)
                .setSide(BarSide.RIGHT).setColorProvider(TransitioningColorProvider.DEFAULT).build()));
        registerBar(new Food(BarSettings.getBuilder().setSide(BarSide.RIGHT)
                .setColorProvider(DualEffectColorProvider.FOOD)
                .setDisablesOverlay(FOOD_LEVEL)
                .build(),true,true,true));

        registerBar(new Air(BarSettings.getBuilder()
                .setDisablesOverlay(AIR_LEVEL)
                .setSide(BarSide.RIGHT)
                .setColorProvider(new SingleColorProvider(Color.hex2Color("#00E6E6")))
                .build()));
        registerBar(new Armor(BarSettings.getBuilder().setDisablesOverlay(ARMOR_LEVEL).fitted()
                .setColorProvider(StackingColorProvider.DEFAULT_ARMOR)
                .build()));
        registerBar(new Absorption(BarSettings.getBuilder()// health 已禁用吸收条
                .fitted()
                .setColorProvider(StackingEffectColorProvider.DEFAULT_ABSORPTION).build()));
        registerBar(new ArmorToughness(BarSettings.getBuilder().setSide(BarSide.RIGHT).fitted()
                .setColorProvider(StackingColorProvider.DEFAULT_ARMOR)
                .setIcon(BarOverlayImpl.BAR).build()));
    }
}
