package ruhack.phobia.system.screensystem.clickgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import ruhack.phobia.system.rendersystem.render2d.Render2D;

public final class ClickGuiLayout {
   public static final float MIN_WINDOW_WIDTH_PX = 700.0F;
   public static final float MAX_WINDOW_WIDTH_PX = 800.0F;
   public static final float MIN_WINDOW_HEIGHT_PX = 480.0F;
   public static final float MAX_WINDOW_HEIGHT_PX = 550.0F;
   public static final float SIDEBAR_WIDTH_PX = 152.0F;
   public static final float HEADER_HEIGHT_PX = 44.0F;
   public static final float CONTENT_PADDING_PX = 12.0F;
   public static final float COLUMN_GAP_PX = 9.0F;
   public static final float CARD_HEIGHT_PX = 40.0F;
   public static final float CARD_GAP_PX = 8.0F;
   public static final float SETTINGS_WIDTH_PX = 230.0F;
   public static final float SETTINGS_MIN_HEIGHT_PX = 41.0F;
   public static final float CONFIG_POPUP_WIDTH_PX = 218.0F;
   public static final float WINDOW_RADIUS_PX = 15.0F;
   public static final float CARD_RADIUS_PX = 9.0F;
   public static final float POPUP_RADIUS_PX = 10.0F;
   public final float windowWidth;
   public final float windowHeight;
   public final float sidebarWidth;
   public final float headerHeight;
   public final float padding;
   public final float gap;
   public final float cardHeight;
   public final float cardGap;
   public final float settingsWidth;
   public final float configPopupWidth;
   public final float windowRadius;
   public final float cardRadius;
   public final float popupRadius;

   private ClickGuiLayout(float windowWidthPx, float windowHeightPx) {
      this.windowWidth = physicalToRender(windowWidthPx);
      this.windowHeight = physicalToRender(windowHeightPx);
      this.sidebarWidth = physicalToRender(152.0F);
      this.headerHeight = physicalToRender(44.0F);
      this.padding = physicalToRender(12.0F);
      this.gap = physicalToRender(9.0F);
      this.cardHeight = physicalToRender(40.0F);
      this.cardGap = physicalToRender(8.0F);
      this.settingsWidth = physicalToRender(230.0F);
      this.configPopupWidth = physicalToRender(218.0F);
      this.windowRadius = physicalToRender(15.0F);
      this.cardRadius = physicalToRender(9.0F);
      this.popupRadius = physicalToRender(10.0F);
   }

   public static ClickGuiLayout current() {
      Window window = MinecraftClient.getInstance().getWindow();
      float physicalWidth = clamp(window.getWidth() * 0.44F, 700.0F, 800.0F);
      float physicalHeight = clamp(window.getHeight() * 0.56F, 480.0F, 550.0F);
      physicalWidth = Math.min(physicalWidth, Math.max(320.0F, window.getWidth() - 32.0F));
      physicalHeight = Math.min(physicalHeight, Math.max(240.0F, window.getHeight() - 32.0F));
      return new ClickGuiLayout(physicalWidth, physicalHeight);
   }

   public static float physicalToRender(float physicalPixels) {
      Window window = MinecraftClient.getInstance().getWindow();
      float minecraftGuiUnits = physicalPixels / Math.max(1.0F, window.getScaleFactor());
      return minecraftGuiUnits * Render2D.getScaleFactor();
   }

   public static float renderToPhysical(float renderUnits) {
      Window window = MinecraftClient.getInstance().getWindow();
      return renderUnits / Math.max(0.01F, Render2D.getScaleFactor()) * window.getScaleFactor();
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
