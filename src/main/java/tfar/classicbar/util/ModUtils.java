package tfar.classicbar.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class ModUtils {

  public static void drawTexturedModalRect(ResourceLocation texture,GuiGraphics stack, double x, int y, int textureX, int textureY, double width, int height) {
    stack.blit(texture, (int) x, y, textureX, textureY, (int) width, height);
  }

  public static void drawStringOnHUD(GuiGraphics stack, String string, int xOffset, int yOffset, int color) {

    xOffset += 2;
    yOffset += 2;

    stack.drawString(Minecraft.getInstance().font,string, xOffset, yOffset, color,true);
  }

  /**
   * 设置 HUD 条渲染所需的混合/深度测试状态（原 ForgeGui.setupOverlayRenderState 的逻辑）。
   * @param blend 是否禁用深度测试
   * @param depthTest 是否启用分离混合函数
   */
  public static void setupOverlayRenderState(boolean blend, boolean depthTest) {
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.enableDepthTest();
    if (blend) {
      RenderSystem.disableDepthTest();
    }
    if (depthTest) {
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }
  }
}
