package tfar.classicbar.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import tfar.classicbar.client.ClassicBarClient;
import tfar.classicbar.mixin.FoodDataAccessor;

public enum SyncState {
    SATURATION, EXHAUSTION;

    public void sendTo(ServerPlayer player, float value) {
        PacketHandler.sendToClient(new S2CValueSync(value, this), player);
    }

    /**
     * 饱和度/消耗度同步包：1.21.1 网络改用 CustomPacketPayload + StreamCodec 注册。
     * 同时实现 S2CModPacket（handleClient 在客户端主线程处理）与 CustomPacketPayload。
     */
    public record S2CValueSync(float value, SyncState state) implements S2CModPacket, CustomPacketPayload {

        /** 包类型标识（复用 SYNC_ID） */
        public static final Type<S2CValueSync> TYPE = new Type<>(PacketHandler.SYNC_ID);

        /** 流式编解码器：编码写 float + 枚举，解码逆向读取 */
        public static final StreamCodec<FriendlyByteBuf, S2CValueSync> STREAM_CODEC = StreamCodec.of(
                S2CValueSync::encode,
                buf -> new S2CValueSync(buf.readFloat(), buf.readEnum(SyncState.class))
        );

        public S2CValueSync(FriendlyByteBuf buf) {
            this(buf.readFloat(), buf.readEnum(SyncState.class));
        }

        public static void encode(FriendlyByteBuf buf, S2CValueSync packet) {
            buf.writeFloat(packet.value());
            buf.writeEnum(packet.state());
        }

        public void encode(FriendlyByteBuf buf) {
            encode(buf, this);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @Override
        public void handleClient() {
            Player player = ClassicBarClient.getLocalPlayer();
            if (player == null) return;
            switch (state) {
                case SATURATION -> player.getFoodData().setSaturation(value);
                // 1.21.11 无 setExhaustion，用 accessor 写回 exhaustionLevel
                case EXHAUSTION -> ((FoodDataAccessor) player.getFoodData()).classicbar$setExhaustionLevel(value);
            }
        }
    }
}
