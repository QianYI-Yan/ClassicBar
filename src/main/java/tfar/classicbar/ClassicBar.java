package tfar.classicbar;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tfar.classicbar.api.colorprovider.ColorProviderSerializers;
import tfar.classicbar.network.PacketHandler;
import tfar.classicbar.network.SyncHandler;

/**
 * Fabric 模组主入口（双端加载）：负责服务端网络与数据同步逻辑。
 */
public class ClassicBar implements ModInitializer {

  public static final String MODID = "classicbar";

  public static final Logger logger = LogManager.getLogger();

  @Override
  public void onInitialize() {
    ColorProviderSerializers.init();
    PacketHandler.registerMessages();
    SyncHandler.register();
  }

}
