package tfar.classicbar.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import tfar.classicbar.mixin.FoodDataAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 同步饱和度（原版 MC 只在归零时同步）与消耗度（原版 MC 完全不同步）。
 */
public final class SyncHandler {

  private SyncHandler() {}

  // 原版 MC
  private static final Map<UUID, Float> lastSaturationLevels = new HashMap<>();
  private static final Map<UUID, Float> lastExhaustionLevels = new HashMap<>();

  public static void register() {
    // Fabric 无独立的玩家 tick 事件，在服务端 tick 中遍历所有在线玩家
    ServerTickEvents.END_SERVER_TICK.register(server -> {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
        syncVanillaData(player);
      }
    });

    // 玩家登出时清理缓存数据
    ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
      ServerPlayer player = handler.getPlayer();
      if (player != null) {
        onPlayerLoggedOut(player);
      }
    });
  }

  private static void syncVanillaData(ServerPlayer player) {
    UUID uuid = player.getUUID();
    Float lastSaturationLevel = lastSaturationLevels.get(uuid);
    Float lastExhaustionLevel = lastExhaustionLevels.get(uuid);

    float saturationLevel = player.getFoodData().getSaturationLevel();
    if (lastSaturationLevel == null || lastSaturationLevel != saturationLevel) {
      SyncState.SATURATION.sendTo(player, saturationLevel);
      lastSaturationLevels.put(uuid, saturationLevel);
    }

    float exhaustionLevel = ((FoodDataAccessor) player.getFoodData()).classicbar$getExhaustionLevel();
    if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f) {
      SyncState.EXHAUSTION.sendTo(player, exhaustionLevel);
      lastExhaustionLevels.put(uuid, exhaustionLevel);
    }
  }

  public static void onPlayerLoggedOut(ServerPlayer player) {
    UUID uuid = player.getUUID();
    lastSaturationLevels.remove(uuid);
    lastExhaustionLevels.remove(uuid);
  }
}
