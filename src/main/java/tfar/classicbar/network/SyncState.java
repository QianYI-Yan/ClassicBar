package tfar.classicbar.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import tfar.classicbar.client.ClassicBarClient;

public enum SyncState {
    SATURATION, EXHAUSTION;

    public void sendTo(ServerPlayer player, float value) {
        PacketHandler.sendToClient(new S2CValueSync(value, this), player);
    }

    public record S2CValueSync(float value, SyncState state) implements S2CModPacket {
        public S2CValueSync(FriendlyByteBuf buf) {
            this(buf.readFloat(), buf.readEnum(SyncState.class));
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeFloat(value);
            buf.writeEnum(state);
        }

        @Override
        public void handleClient() {
            Player player = ClassicBarClient.getLocalPlayer();
            if (player == null) return;
            switch (state) {
                case SATURATION -> player.getFoodData().setSaturation(value);
                case EXHAUSTION -> player.getFoodData().setExhaustion(value);
            }
        }
    }
}
