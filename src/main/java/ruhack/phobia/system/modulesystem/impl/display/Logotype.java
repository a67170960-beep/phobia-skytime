package ruhack.phobia.system.modulesystem.impl.display;

import com.adl.nativeprotect.User;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import ruhack.phobia.system.dragsystem.PillHudElement;
import ruhack.phobia.system.modulesystem.impl.render.NoRender;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.ServerUtil;
import ruhack.phobia.util.colorschem.ColorUtil;

public class Logotype extends PillHudElement {
   private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
   private static final Identifier LOGO_TEXTURE = Identifier.of("phobia", "phobia.png");
   private static final String ICON_USER = "\ue012";
   private static final String ICON_FPS = "U";
   private static final String ICON_PING = "3";
   private static final String ICON_TIME = "J";
   private static final String ICON_TPS = "n";
   private static final float LOGO_VISUAL = 9.0F;
   private static final int FIXED_X = 6;
   private static final int FIXED_Y = 6;
   private static final float BOSS_BAR_OFFSET_PIXELS = 38.0F;
   private static final float VANILLA_BOSS_BAR_WIDTH = 182.0F;
   private static final float VANILLA_BOSS_BAR_TOP = 3.0F;
   private static final float VANILLA_BOSS_BAR_BOTTOM = 17.0F;
   private String cachedProfileName;
   private String cachedTime = "00:00";
   private int cachedMinute = -1;

   public Logotype() {
      super("Logotype", 6, 6, true);
   }

   @Override
   public void drawDraggable(DrawContext context, int alpha) {
      this.y = 6;
      float a = alpha / 255.0F;
      int accent = ColorUtil.multAlpha(ColorUtil.getClientColorAt((float)this.x / Math.max(1, Render2D.getFixedScaledWidth())), a);
      int logoColor = accent;
      int textColor = ColorUtil.multAlpha(ColorUtil.rgba(236, 238, 245, 255), a);
      int unitColor = ColorUtil.multAlpha(ColorUtil.getClientColorAt((float)(this.x + this.width) / Math.max(1, Render2D.getFixedScaledWidth()), 200), a);
      int dotColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 70), a);
      int bgColor = ColorUtil.multAlpha(ColorUtil.rgba(11, 12, 18, Math.round(Hud.getInstance().backgroundAlpha.getValue())), a);
      int outlineColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 20), a);
      MsdfFont font = MsdfManager.BOLD;
      MsdfFont icons = MsdfManager.ICONS;
      String name = this.getProfileName();
      String fps = String.valueOf(this.mc.getCurrentFps());
      String ping = String.valueOf(this.getPing());
      LocalTime now = LocalTime.now();
      if (now.getMinute() != this.cachedMinute) {
         this.cachedMinute = now.getMinute();
         this.cachedTime = now.format(TIME_FORMAT);
      }

      String time = this.cachedTime;
      Hud hud = Hud.getInstance();
      boolean showTps = hud != null && hud.showTps.isValue();
      String tps = String.valueOf(Math.round(ServerUtil.TPS));
      float logoSize = 9.0F;
      float contentWidth = logoSize
         + this.dotBlockWidth()
         + this.segmentWidth(font, icons, "\ue012", name, null)
         + this.dotBlockWidth()
         + this.segmentWidth(font, icons, "U", fps, "FPS")
         + this.dotBlockWidth()
         + this.segmentWidth(font, icons, "3", ping, "MS")
         + this.dotBlockWidth()
         + this.segmentWidth(font, icons, "J", time, null);
      if (showTps) {
         contentWidth += this.dotBlockWidth() + this.segmentWidth(font, icons, "n", tps, "TPS");
      }

      this.width = (int)Math.ceil(contentWidth + 18.0F);
      this.height = 21;
      float viewportWidth = Render2D.getFixedScaledWidth() / Math.max(0.01F, Render2D.getContextScale());
      if (Render2D.hasUsableViewport()) {
         this.x = Math.max(6, Math.min(this.x, (int)Math.floor(viewportWidth - this.width - 6.0F)));
      }

      this.y = this.getVisualY();
      this.drawPill(context, bgColor, outlineColor);
      float cursor = this.x + 9.0F;
      Render2D.image(context, cursor, this.y + 10.5F - logoSize / 2.0F, logoSize, LOGO_TEXTURE, logoColor, 0.0F, false);
      cursor += logoSize;
      cursor = this.drawDot(context, cursor, dotColor);
      cursor = this.drawSegment(context, font, icons, cursor, "\ue012", name, null, accent, textColor, unitColor);
      cursor = this.drawDot(context, cursor, dotColor);
      cursor = this.drawSegment(context, font, icons, cursor, "U", fps, "FPS", accent, textColor, unitColor);
      cursor = this.drawDot(context, cursor, dotColor);
      cursor = this.drawSegment(context, font, icons, cursor, "3", ping, "MS", accent, textColor, unitColor);
      cursor = this.drawDot(context, cursor, dotColor);
      cursor = this.drawSegment(context, font, icons, cursor, "J", time, null, accent, textColor, unitColor);
      if (showTps) {
         cursor = this.drawDot(context, cursor, dotColor);
         this.drawSegment(context, font, icons, cursor, "n", tps, "TPS", accent, textColor, unitColor);
      }
   }

   @Override
   public void setY(int y) {
      super.setY(6);
   }

   @Override
   public int getY() {
      return this.getVisualY();
   }

   public int getVisualY() {
      return 6 + this.calculateBossBarOffset();
   }

   private int calculateBossBarOffset() {
      if (!this.intersectsVisibleBossBar()) {
         return 0;
      }

      float physicalPixelsPerHudUnit = 2.0F * Math.max(0.01F, Render2D.getContextScale());
      return Math.round(38.0F / physicalPixelsPerHudUnit);
   }

   private boolean intersectsVisibleBossBar() {
      if (this.mc.inGameHud != null && this.mc.getWindow() != null) {
         NoRender noRender = NoRender.getInstance();
         if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("BossBar")) {
            return false;
         }

         Map<UUID, ClientBossBar> bossBars = this.mc.inGameHud.getBossBarHud().bossBars;
         if (bossBars != null && !bossBars.isEmpty()) {
            float minecraftScale = this.mc.getWindow().getScaleFactor();
            float bossWidth = 182.0F;

            for (ClientBossBar bossBar : bossBars.values()) {
               bossWidth = Math.max(bossWidth, this.mc.textRenderer.getWidth(bossBar.getName()));
            }

            float bossLeft = this.mc.getWindow().getWidth() / 2.0F - bossWidth * minecraftScale / 2.0F;
            float bossRight = this.mc.getWindow().getWidth() / 2.0F + bossWidth * minecraftScale / 2.0F;
            float bossTop = 3.0F * minecraftScale;
            float bossBottom = 17.0F * minecraftScale;
            float hudPixelScale = 2.0F * Math.max(0.01F, Render2D.getContextScale());
            float watermarkLeft = this.x * hudPixelScale;
            float watermarkRight = (this.x + this.width) * hudPixelScale;
            float watermarkTop = 6.0F * hudPixelScale;
            float watermarkBottom = 27.0F * hudPixelScale;
            return watermarkRight > bossLeft && watermarkLeft < bossRight && watermarkBottom > bossTop && watermarkTop < bossBottom;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int getPing() {
      if (this.mc.getNetworkHandler() != null && this.mc.player != null) {
         PlayerListEntry entry = this.mc.getNetworkHandler().getPlayerListEntry(this.mc.player.getUuid());
         return entry != null ? Math.max(entry.getLatency(), 0) : 0;
      } else {
         return 0;
      }
   }

   private String getProfileName() {
      return "leaked by angelw1ngsov";
   }
}
