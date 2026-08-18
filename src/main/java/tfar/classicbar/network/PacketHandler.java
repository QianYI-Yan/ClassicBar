package tfar.classicbar.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import tfar.classicbar.ClassicBar;

public final class PacketHandler {

  public static final ResourceLocation SYNC_ID = new ResourceLocation(ClassicBar.MODID, "s2c_sync");

  /** 服务器是否安装本模组（客户端在登录时通过 canSend 检测） */
  public static boolean presentOnServer;

  /**
   * 服务端注册：注册 SYNC_ID 的接收器（空实现），
   * 使客户端能通过 ClientPlayNetworking.canSend 检测到服务器已安装本模组。
   * 注意：不能按 EnvType 判断——单机（集成服务器）时进程环境是 CLIENT，
   * 但集成服务器也需要注册接收器，否则 presentOnServer 恒为 false，
   * 导致饱和度/消耗度叠加层不显示。
   */
  public static void registerMessages() {
    ServerPlayNetworking.registerGlobalReceiver(SYNC_ID, (server, player, handler, buf, responseSender) -> {
    });
  }

  /** 客户端注册：接收服务器下发的数据同步包 */
  public static void registerClientReceiver() {
    ClientPlayNetworking.registerGlobalReceiver(SYNC_ID, (client, handler, buf, responseSender) -> {
      SyncState.S2CValueSync packet = new SyncState.S2CValueSync(buf);
      // 网络线程收到包，需切回主线程执行
      client.execute(packet::handleClient);
    });
  }

  public static <MSG extends S2CModPacket> void sendToClient(MSG msg, ServerPlayer player) {
    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    msg.encode(buf);
    ServerPlayNetworking.send(player, SYNC_ID, buf);
  }

  private PacketHandler() {}

}
