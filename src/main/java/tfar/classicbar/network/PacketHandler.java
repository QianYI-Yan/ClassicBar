package tfar.classicbar.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import tfar.classicbar.ClassicBar;

public final class PacketHandler {

  public static final Identifier SYNC_ID = Identifier.fromNamespaceAndPath(ClassicBar.MODID, "s2c_sync");

  /** 服务器是否安装本模组（客户端在登录时通过 canSend 检测） */
  public static boolean presentOnServer;

  /**
   * 服务端注册：注册 S2CValueSync 的 payload 类型并挂一个空实现接收器，
   * 使客户端能通过 ClientPlayNetworking.canSend 检测到服务器已安装本模组。
   * 注意：不能按 EnvType 判断——单机（集成服务器）时进程环境是 CLIENT，
   * 但集成服务器也需要注册接收器，否则 presentOnServer 恒为 false，
   * 导致饱和度/消耗度叠加层不显示。
   */
  public static void registerMessages() {
    // 同一包类型需注册到两个方向：
    // - clientboundPlay：客户端接收（registerClientReceiver 使用）
    // - serverboundPlay：服务端能注册空接收器，使客户端 canSend 能检测到服务器已安装本模组
    // 注：fabric 26.2 中 playS2C/playC2S 改名为 clientboundPlay/serverboundPlay
    PayloadTypeRegistry.clientboundPlay().register(SyncState.S2CValueSync.TYPE, SyncState.S2CValueSync.STREAM_CODEC);
    PayloadTypeRegistry.serverboundPlay().register(SyncState.S2CValueSync.TYPE, SyncState.S2CValueSync.STREAM_CODEC);
    ServerPlayNetworking.registerGlobalReceiver(SyncState.S2CValueSync.TYPE, (payload, context) -> {
    });
  }

  /** 客户端注册：接收服务器下发的数据同步包 */
  public static void registerClientReceiver() {
    ClientPlayNetworking.registerGlobalReceiver(SyncState.S2CValueSync.TYPE, (payload, context) -> {
      // 网络线程收到包，需切回主线程执行
      context.client().execute(payload::handleClient);
    });
  }

  public static <MSG extends S2CModPacket & CustomPacketPayload> void sendToClient(MSG msg, ServerPlayer player) {
    ServerPlayNetworking.send(player, msg);
  }

  private PacketHandler() {}

}
