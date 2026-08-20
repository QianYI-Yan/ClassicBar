package tfar.classicbar.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.io.FileUtils;
import tfar.classicbar.ClassicBar;
import tfar.classicbar.EventHandler;
import tfar.classicbar.config.ClassicBarsConfig;
import tfar.classicbar.network.PacketHandler;
import tfar.classicbar.network.SyncState;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Fabric 客户端入口：注册客户端命令、网络接收器。
 * 注：HUD 渲染在 26.2 中已改为 mixin 注入 Hud.extractRenderState（fabric 的 HudRenderCallback 已移除）。
 */
public class ClassicBarClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 初始化配置后端并加载各条配置（否则 HUD 不显示任何条）
        ClassicBarsConfig.init();

        // 登录时检测服务器是否安装本模组（决定是否显示饱和度/消耗度叠加层）
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PacketHandler.presentOnServer = ClientPlayNetworking.canSend(PacketHandler.SYNC_ID);
        });

        ClientCommandRegistrationCallback.EVENT.register(ClassicBarClient::commands);
        PacketHandler.registerClientReceiver();
    }

    static void commands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(ClientCommands.literal(ClassicBar.MODID)
                .then(ClientCommands.literal("reload")
                        .executes(c -> {
                            EventHandler.cacheConfigs();
                            return 1;
                        })
                ).then(ClientCommands.literal("config")
                        .executes(c -> {
                            Minecraft.getInstance().setScreenAndShow(ClassicBarsConfig.createConfigScreen(null));
                            return 1;
                        })
                ).then(ClientCommands.literal("backend")
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("cloth-config");
                                    builder.suggest("memory");
                                    return builder.buildFuture();
                                })
                                .executes(c -> {
                                    String name = StringArgumentType.getString(c, "name");
                                    ClassicBarsConfig.switchBackend(name);
                                    c.getSource().sendFeedback(Component.literal("已切换配置后端: " + name));
                                    return 1;
                                })
                        )
                ).then(ClientCommands.literal("reset")
                        .executes(c -> {
                            Path folder = FabricLoader.getInstance().getConfigDir().resolve(ClassicBar.MODID);
                            try {
                                FileUtils.cleanDirectory(folder.toFile());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            EventHandler.cacheConfigs();
                            return 1;
                        })
                )
        );
    }

    public static Player getLocalPlayer() {
      return Minecraft.getInstance().player;
    }
}
