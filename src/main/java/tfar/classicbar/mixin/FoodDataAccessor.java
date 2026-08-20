package tfar.classicbar.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 26.2 的 FoodData 移除了 getExhaustionLevel()/setExhaustion()，
 * 这里用 accessor 访问 private 的 exhaustionLevel 字段以保留消耗度功能。
 */
@Mixin(FoodData.class)
public interface FoodDataAccessor {

    @Accessor("exhaustionLevel")
    float classicbar$getExhaustionLevel();

    @Accessor("exhaustionLevel")
    void classicbar$setExhaustionLevel(float value);
}
