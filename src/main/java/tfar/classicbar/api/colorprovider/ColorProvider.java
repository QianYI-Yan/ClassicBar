package tfar.classicbar.api.colorprovider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.player.Player;
import tfar.classicbar.api.Color;

public interface ColorProvider {

    Codec<ColorProvider> CODEC = Codec.STRING.xmap(ColorProviderSerializers.MAP::get, ColorProviderSerializer::name)
            .dispatch(ColorProvider::getSerializer, ColorProvider::providerCodec);

    //note, this CAN go above 1
    Color getColor(Player player,float ratio, int layer);
    ColorProviderSerializer<?> getSerializer();

    /** 把序列化器转为对应的 MapCodec（1.21.1 中 dispatch 的第二个参数要求返回 MapCodec） */
    static MapCodec<? extends ColorProvider> providerCodec(ColorProviderSerializer<?> serializer) {
        return serializer.codec();
    }
}
