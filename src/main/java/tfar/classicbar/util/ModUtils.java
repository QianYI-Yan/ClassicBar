package tfar.classicbar.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class ModUtils {

  /** 白色不透明（默认） */
  public static void drawTexturedModalRect(Identifier texture, GuiGraphics stack, double x, int y, int textureX, int textureY, double width, int height) {
    drawTexturedModalRect(texture, stack, (int) x, y, textureX, textureY, (int) width, height, 0xFFFFFFFF);
  }

  /**
   * 1.21.11 的 GuiGraphics 使用 RenderPipeline 渲染，颜色作为 blit 参数传入（ARGB）。
   * 纹理 health.png / icons.png 均为 256x256。
   */
  public static void drawTexturedModalRect(Identifier texture, GuiGraphics stack, int x, int y, int textureX, int textureY, int width, int height, int color) {
    stack.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, textureX, textureY, width, height, 256, 256, color);
  }

  public static void drawStringOnHUD(GuiGraphics stack, String string, int xOffset, int yOffset, int color) {

    xOffset += 2;
    yOffset += 2;

    stack.drawString(Minecraft.getInstance().font,string, xOffset, yOffset, color,true);
  }

  /**
   * 1.21.11+ GuiGraphics 渲染自管理混合/深度状态，无需手动设置（原 Forge 逻辑的 RenderSystem/GlStateManager 已移除）。
   */
  public static void setupOverlayRenderState(boolean blend, boolean depthTest) {
    // no-op：1.21.11 中 GuiGraphics.blit 自动管理渲染状态
  }
}
