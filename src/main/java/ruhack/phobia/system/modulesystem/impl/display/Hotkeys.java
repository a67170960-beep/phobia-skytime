package ruhack.phobia.system.modulesystem.impl.display;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.dragsystem.PillHudElement;
import ruhack.phobia.system.modulesystem.ModuleRepository;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.string.KeyHelper;

public class Hotkeys extends PillHudElement {
   private static final String ICON_HEADER = "D";
   private static final float RADIUS = 8.0F;
   private static final float PAD = 8.0F;
   private static final float HEADER_H = 10.0F;
   private static final float HEADER_GAP = 5.0F;
   private static final float HEADER_ICON = 7.0F;
   private static final float ROW_H = 11.5F;
   private static final float ROW_ICON = 5.5F;
   private static final float NAME_BADGE_GAP = 10.0F;
   private static final float ROW_SLIDE = 5.0F;
   private static final float BADGE_H = 8.5F;
   private static final float BADGE_PAD = 3.0F;
   private static final float BADGE_RADIUS = 2.5F;
   private static final float BADGE_TEXT = 5.0F;
   private static final long ROW_ANIM_MS = 220L;
   private final Map<String, Hotkeys.Row> rowMap = new LinkedHashMap<>();
   private final HashSet<String> liveKeys = new HashSet<>();
   private float panelP = 0.0F;
   private float smoothWidth = -1.0F;
   private long lastFrame = -1L;

   private static String categoryIcon(ModuleCategory category) {
      return switch (category) {
         case OTHER -> "T";
         case RAGE -> "\ue002";
         case LEGIT -> "7";
         case MOVEMENT -> "C";
         case PLAYER -> "Y";
         case PVE -> "\ue007";
         case RENDER -> "a";
         case DISPLAY -> "\ue003";
         case ESP -> "X";
         case MISC -> "\ue005";
         case BINDS -> "\ue008";
         case CONFIG -> "\ue006";
      };
   }

   public Hotkeys() {
      super("Hotkeys", 6, 200, true);
   }

   @Override
   public float getRoundingRadius() {
      return 8.0F;
   }

   @Override
   public boolean visible() {
      return this.mc.player == null ? false : this.hasBinds() || this.mc.currentScreen instanceof ChatScreen || !this.rowMap.isEmpty() || this.panelP > 0.02F;
   }

   private ModuleRepository repository() {
      Initialization init = Initialization.getInstance();
      return init != null && init.getManager() != null ? init.getManager().getModuleRepository() : null;
   }

   private boolean hasBinds() {
      ModuleRepository repo = this.repository();
      if (repo == null) {
         return false;
      }

      for (ModuleStructure module : repo.modules()) {
         if (module.getKey() != -1 && module.getKey() != -1 && module.isState()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void drawDraggable(DrawContext context, int alpha) {
      if (this.mc.player != null) {
         long now = System.currentTimeMillis();
         float dt = this.lastFrame < 0L ? 16.0F : Math.min((float)(now - this.lastFrame), 100.0F);
         this.lastFrame = now;
         boolean chat = this.mc.currentScreen instanceof ChatScreen;
         MsdfFont font = MsdfManager.BOLD;
         MsdfFont icons = MsdfManager.ICONS;
         this.syncRows(now, chat);
         boolean hasContent = !this.rowMap.isEmpty();
         float targetP = hasContent ? 1.0F : 0.0F;
         this.panelP = this.panelP + (targetP - this.panelP) * (1.0F - (float)Math.exp(-dt / 55.0F));
         this.panelP = MathHelper.clamp(this.panelP, 0.0F, 1.0F);
         if (hasContent || !(this.panelP <= 0.02F)) {
            float a = alpha / 255.0F * easeOutCubic(this.panelP);
            int accent = ColorUtil.multAlpha(ColorUtil.getClientColorAt((float)this.x / Math.max(1, Render2D.getFixedScaledWidth())), a);
            int textColor = ColorUtil.multAlpha(ColorUtil.rgba(236, 238, 245, 255), a);
            int dimIconColor = ColorUtil.multAlpha(ColorUtil.rgba(120, 122, 135, 255), a);
            int dotColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 70), a);
            int bgColor = ColorUtil.multAlpha(ColorUtil.rgba(11, 12, 18, Math.round(Hud.getInstance().backgroundAlpha.getValue())), a);
            int outlineColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 20), a);
            int badgeColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 22), a);
            int badgeTextColor = ColorUtil.multAlpha(ColorUtil.rgba(225, 227, 235, 255), a);
            float headerIconSize = this.iconSize(icons, "D", 7.0F);
            float headerWidth = Msdf.width(icons, "D", headerIconSize) + this.dotBlockWidth() + Msdf.width(font, "Hotkeys", 7.5F);
            float targetWidth = headerWidth;
            float rowsPresence = 0.0F;
            float maxPresence = 0.0F;

            for (Hotkeys.Row row : this.rowMap.values()) {
               float p = this.presence(row, now);
               rowsPresence += p * 11.5F;
               maxPresence = Math.max(maxPresence, p);
               targetWidth = Math.max(targetWidth, this.rowWidth(font, icons, row));
            }

            targetWidth += 16.0F;
            if (this.smoothWidth < 0.0F) {
               this.smoothWidth = targetWidth;
            }

            this.smoothWidth = this.smoothWidth + (targetWidth - this.smoothWidth) * (1.0F - (float)Math.exp(-dt / 70.0F));
            this.width = (int)Math.ceil(this.smoothWidth);
            this.height = (int)Math.ceil(26.0F + (rowsPresence > 0.1F ? 5.0F * maxPresence + rowsPresence : 0.0F));
            Render2D.blur(context, this.x, this.y, this.width, this.height, 8.0F, 8.0F, false);
            Render2D.rect(context, this.x, this.y, this.width, this.height, 8.0F, bgColor, false);
            Render2D.outline(context, this.x, this.y, this.width, this.height, 8.0F, 0.6F, outlineColor, false);
            float headerCenter = this.y + 8.0F + 5.0F;
            float cursor = this.x + 8.0F;
            Msdf.text(context, icons, "D", cursor, this.glyphY(icons, "D", headerIconSize, headerCenter), headerIconSize, accent, false);
            cursor += Msdf.width(icons, "D", headerIconSize);
            cursor = this.drawDot(context, cursor, headerCenter, dotColor);
            Msdf.text(context, font, "Hotkeys", cursor, this.textY(font, 7.5F, headerCenter), 7.5F, textColor, false);
            float rowY = this.y + 8.0F + 10.0F + 5.0F * maxPresence;

            for (Hotkeys.Row row : this.rowMap.values()) {
               float p = this.presence(row, now);
               if (!(p <= 0.01F)) {
                  float rowAlpha = a * easeOutCubic(p);
                  int rowText = ColorUtil.multAlpha(textColor, easeOutCubic(p));
                  int rowIcon = ColorUtil.multAlpha(row.enabled ? accent : dimIconColor, easeOutCubic(p));
                  int rowDot = ColorUtil.multAlpha(dotColor, easeOutCubic(p));
                  float center = rowY + 11.5F * p / 2.0F;
                  float cx = this.x + 8.0F - (1.0F - easeOutCubic(p)) * 5.0F;
                  float iconSize = this.iconSize(icons, row.icon, 5.5F);
                  Msdf.text(context, icons, row.icon, cx, this.glyphY(icons, row.icon, iconSize, center), iconSize, rowIcon, false);
                  cx += Msdf.width(icons, row.icon, iconSize);
                  cx = this.drawDot(context, cx, center, rowDot);
                  Msdf.text(context, font, row.name, cx, this.textY(font, 7.5F, center), 7.5F, rowText, false);
                  float badgeTextWidth = Msdf.width(font, row.key, 5.0F);
                  float badgeWidth = badgeTextWidth + 6.0F;
                  float badgeX = this.x + this.width - 8.0F - badgeWidth;
                  Render2D.rect(context, badgeX, center - 4.25F, badgeWidth, 8.5F, 2.5F, ColorUtil.multAlpha(badgeColor, easeOutCubic(p)), false);
                  Msdf.text(
                     context, font, row.key, badgeX + 3.0F, this.textY(font, 5.0F, center), 5.0F, ColorUtil.multAlpha(badgeTextColor, easeOutCubic(p)), false
                  );
                  rowY += 11.5F * p;
               }
            }
         }
      }
   }

   private void syncRows(long now, boolean chat) {
      this.liveKeys.clear();
      ModuleRepository repo = this.repository();
      if (repo != null) {
         for (ModuleStructure module : repo.modules()) {
            int key = module.getKey();
            if (key != -1 && key != -1 && module.isState()) {
               this.updateRow(now, module.getName(), this.formatKey(key), true, categoryIcon(module.getCategory()));
            }
         }
      }

      if (this.liveKeys.isEmpty() && chat) {
         this.updateRow(now, "AttackAura", "X", true, categoryIcon(ModuleCategory.RAGE));
         this.updateRow(now, "NoWeb", "NUM0", true, categoryIcon(ModuleCategory.PLAYER));
         this.updateRow(now, "Speed", "B", true, categoryIcon(ModuleCategory.MOVEMENT));
      }

      Iterator<Entry<String, Hotkeys.Row>> it = this.rowMap.entrySet().iterator();

      while (it.hasNext()) {
         Entry<String, Hotkeys.Row> entry = it.next();
         if (!this.liveKeys.contains(entry.getKey())) {
            Hotkeys.Row row = entry.getValue();
            if (row.disappearAt < 0L) {
               float p = this.presence(row, now);
               row.disappearAt = now - (long)((1.0F - p) * 220.0F);
            } else if (now - row.disappearAt > 220L) {
               it.remove();
            }
         }
      }
   }

   private void updateRow(long now, String name, String keyName, boolean enabled, String icon) {
      this.liveKeys.add(name);
      Hotkeys.Row row = this.rowMap.get(name);
      if (row == null) {
         row = new Hotkeys.Row();
         row.appearAt = now;
         this.rowMap.put(name, row);
      } else if (row.disappearAt >= 0L) {
         float p = this.presence(row, now);
         row.appearAt = now - (long)(p * 220.0F);
         row.disappearAt = -1L;
      }

      row.name = name;
      row.key = keyName;
      row.enabled = enabled;
      row.icon = icon;
   }

   private String formatKey(int key) {
      String name = KeyHelper.getKeyName(key).toUpperCase(Locale.US);
      return name.replace("NUMPAD", "NUM").replace(" ", "");
   }

   private float presence(Hotkeys.Row row, long now) {
      return row.disappearAt >= 0L ? 1.0F - Math.min(1.0F, (float)(now - row.disappearAt) / 220.0F) : Math.min(1.0F, (float)(now - row.appearAt) / 220.0F);
   }

   private float rowWidth(MsdfFont font, MsdfFont icons, Hotkeys.Row row) {
      float iconSize = this.iconSize(icons, row.icon, 5.5F);
      return Msdf.width(icons, row.icon, iconSize) + this.dotBlockWidth() + Msdf.width(font, row.name, 7.5F) + 10.0F + Msdf.width(font, row.key, 5.0F) + 6.0F;
   }

   private static float easeOutCubic(float t) {
      float inv = 1.0F - MathHelper.clamp(t, 0.0F, 1.0F);
      return 1.0F - inv * inv * inv;
   }

   private static final class Row {
      String name;
      String key;
      String icon;
      boolean enabled;
      long appearAt;
      long disappearAt = -1L;
   }
}
