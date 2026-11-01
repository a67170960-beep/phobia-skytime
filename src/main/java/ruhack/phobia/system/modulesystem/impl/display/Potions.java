package ruhack.phobia.system.modulesystem.impl.display;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import ruhack.phobia.system.dragsystem.PillHudElement;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.render.scissor.Scissor;

public class Potions extends PillHudElement {
   private static final String ICON_HEADER = "\ue00e";
   private static final float RADIUS = 8.0F;
   private static final float PAD = 8.0F;
   private static final float HEADER_H = 10.0F;
   private static final float HEADER_GAP = 5.0F;
   private static final float HEADER_ICON = 7.0F;
   private static final float ROW_H = 11.5F;
   private static final float SPRITE_SIZE = 8.0F;
   private static final float NAME_BADGE_GAP = 10.0F;
   private static final float ROW_SLIDE = 5.0F;
   private static final float BADGE_H = 8.5F;
   private static final float BADGE_PAD = 3.0F;
   private static final float BADGE_RADIUS = 2.5F;
   private static final float BADGE_TEXT = 5.0F;
   private static final long ROW_ANIM_MS = 220L;
   private static final long TIMER_ANIM_MS = 280L;
   private static final float TIMER_SHIFT = 6.5F;
   private final Map<String, Potions.Row> rowMap = new LinkedHashMap<>();
   private final HashSet<String> liveKeys = new HashSet<>();
   private float panelP = 0.0F;
   private float smoothWidth = -1.0F;
   private long lastFrame = -1L;

   public Potions() {
      super("Potions", 6, 58, true);
   }

   @Override
   public float getRoundingRadius() {
      return 8.0F;
   }

   @Override
   public boolean visible() {
      return this.mc.player == null
         ? false
         : !this.mc.player.getStatusEffects().isEmpty() || this.mc.currentScreen instanceof ChatScreen || !this.rowMap.isEmpty() || this.panelP > 0.02F;
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
         this.panelP = Math.max(0.0F, Math.min(1.0F, this.panelP));
         if (hasContent || !(this.panelP <= 0.02F)) {
            float a = alpha / 255.0F * easeOutCubic(this.panelP);
            int accent = ColorUtil.multAlpha(ColorUtil.getClientColorAt((float)this.x / Math.max(1, Render2D.getFixedScaledWidth())), a);
            int textColor = ColorUtil.multAlpha(ColorUtil.rgba(236, 238, 245, 255), a);
            int amplifierColor = ColorUtil.multAlpha(ColorUtil.rgba(150, 152, 165, 255), a);
            int dotColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 70), a);
            int bgColor = ColorUtil.multAlpha(ColorUtil.rgba(11, 12, 18, Math.round(Hud.getInstance().backgroundAlpha.getValue())), a);
            int outlineColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 20), a);
            float headerIconSize = this.iconSize(icons, "\ue00e", 7.0F);
            float headerWidth = Msdf.width(icons, "\ue00e", headerIconSize) + this.dotBlockWidth() + Msdf.width(font, "Effects", 7.5F);
            float targetWidth = headerWidth;
            float rowsPresence = 0.0F;
            float maxPresence = 0.0F;

            for (Potions.Row row : this.rowMap.values()) {
               float p = this.presence(row, now);
               rowsPresence += p * 11.5F;
               maxPresence = Math.max(maxPresence, p);
               targetWidth = Math.max(targetWidth, this.rowWidth(font, row));
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
            Msdf.text(context, icons, "\ue00e", cursor, this.glyphY(icons, "\ue00e", headerIconSize, headerCenter), headerIconSize, accent, false);
            cursor += Msdf.width(icons, "\ue00e", headerIconSize);
            cursor = this.drawDot(context, cursor, headerCenter, dotColor);
            Msdf.text(context, font, "Effects", cursor, this.textY(font, 7.5F, headerCenter), 7.5F, textColor, false);
            float rowY = this.y + 8.0F + 10.0F + 5.0F * maxPresence;

            for (Potions.Row row : this.rowMap.values()) {
               float p = this.presence(row, now);
               if (!(p <= 0.01F)) {
                  this.drawRow(context, font, row, rowY, p, now, a, textColor, amplifierColor, dotColor);
                  rowY += 11.5F * p;
               }
            }
         }
      }
   }

   private void syncRows(long now, boolean chat) {
      this.liveKeys.clear();
      Collection<StatusEffectInstance> effects = this.mc.player.getStatusEffects();
      if (!effects.isEmpty()) {
         for (StatusEffectInstance effect : effects) {
            Identifier sprite = InGameHud.getEffectTexture(effect.getEffectType());
            String key = sprite.toString();
            String name = ((StatusEffect)effect.getEffectType().value()).getName().getString();
            String level = effect.getAmplifier() > 0 ? String.valueOf(effect.getAmplifier() + 1) : null;
            this.updateRow(now, key, this.texture(sprite), name, level, this.formatDuration(effect));
         }
      } else if (chat) {
         this.updateRow(now, "demo_speed", this.texture(Identifier.ofVanilla("mob_effect/speed")), "Speed", "2", "1:23");
         this.updateRow(now, "demo_haste", this.texture(Identifier.ofVanilla("mob_effect/haste")), "Haste", null, "0:56");
         this.updateRow(now, "demo_invis", this.texture(Identifier.ofVanilla("mob_effect/invisibility")), "Invisibility", null, "**:**");
      }

      Iterator<Entry<String, Potions.Row>> it = this.rowMap.entrySet().iterator();

      while (it.hasNext()) {
         Entry<String, Potions.Row> entry = it.next();
         if (!this.liveKeys.contains(entry.getKey())) {
            Potions.Row row = entry.getValue();
            if (row.disappearAt < 0L) {
               float p = this.presence(row, now);
               row.disappearAt = now - (long)((1.0F - p) * 220.0F);
            } else if (now - row.disappearAt > 220L) {
               it.remove();
            }
         }
      }
   }

   private void updateRow(long now, String key, Identifier texture, String name, String level, String time) {
      this.liveKeys.add(key);
      Potions.Row row = this.rowMap.get(key);
      if (row == null) {
         row = new Potions.Row();
         row.appearAt = now;
         this.rowMap.put(key, row);
      } else if (row.disappearAt >= 0L) {
         float p = this.presence(row, now);
         row.appearAt = now - (long)(p * 220.0F);
         row.disappearAt = -1L;
      }

      row.texture = texture;
      row.name = name;
      row.level = level;
      if (row.time != null && !row.time.equals(time)) {
         row.prevTime = row.time;
         row.timeSwitchAt = now;
      }

      row.time = time;
   }

   private void drawRow(
      DrawContext context, MsdfFont font, Potions.Row row, float rowY, float p, long now, float panelAlpha, int textColor, int amplifierColor, int dotColor
   ) {
      float rowAlpha = panelAlpha * easeOutCubic(p);
      int rowText = ColorUtil.multAlpha(textColor, easeOutCubic(p));
      int rowAmplifier = ColorUtil.multAlpha(amplifierColor, easeOutCubic(p));
      int rowDot = ColorUtil.multAlpha(dotColor, easeOutCubic(p));
      int spriteTint = ColorUtil.rgba(255, 255, 255, (int)(rowAlpha * 255.0F));
      float center = rowY + 11.5F * p / 2.0F;
      float cursor = this.x + 8.0F - (1.0F - easeOutCubic(p)) * 5.0F;
      Render2D.image(context, cursor, center - 4.0F, 8.0F, row.texture, spriteTint, 0.0F, false);
      cursor += 8.0F;
      cursor = this.drawDot(context, cursor, center, rowDot);
      Msdf.text(context, font, row.name, cursor, this.textY(font, 7.5F, center), 7.5F, rowText, false);
      cursor += Msdf.width(font, row.name, 7.5F);
      if (row.level != null) {
         Msdf.text(context, font, row.level, cursor + 2.5F, this.textY(font, 7.5F, center), 7.5F, rowAmplifier, false);
      }

      this.drawBadge(context, font, row, center, now, rowAlpha);
   }

   private void drawBadge(DrawContext context, MsdfFont font, Potions.Row row, float center, long now, float rowAlpha) {
      int badgeColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 22), rowAlpha);
      int badgeTextColor = ColorUtil.multAlpha(ColorUtil.rgba(225, 227, 235, 255), rowAlpha);
      boolean switching = row.timeSwitchAt >= 0L && now - row.timeSwitchAt < 280L && row.prevTime != null;
      if (row.timeSwitchAt >= 0L && !switching) {
         row.timeSwitchAt = -1L;
         row.prevTime = null;
      }

      float textWidth = Msdf.width(font, row.time, 5.0F);
      if (switching) {
         textWidth = Math.max(textWidth, Msdf.width(font, row.prevTime, 5.0F));
      }

      float badgeWidth = textWidth + 6.0F;
      float badgeX = this.x + this.width - 8.0F - badgeWidth;
      float badgeY = center - 4.25F;
      Render2D.rect(context, badgeX, badgeY, badgeWidth, 8.5F, 2.5F, badgeColor, false);
      if (!switching) {
         Msdf.text(context, font, row.time, badgeX + 3.0F, this.textY(font, 5.0F, center), 5.0F, badgeTextColor, false);
      } else {
         float t = (float)(now - row.timeSwitchAt) / 280.0F;
         float eased = easeOutBack(t);
         float staticY = this.textY(font, 5.0F, center);
         float oldY = this.textY(font, 5.0F, center + eased * 6.5F);
         float newY = this.textY(font, 5.0F, center + (eased - 1.0F) * 6.5F);
         int oldColor = ColorUtil.multAlpha(badgeTextColor, 1.0F - t);
         int newColor = ColorUtil.multAlpha(badgeTextColor, t);
         Scissor.push(badgeX, badgeY, badgeWidth, 8.5);
         if (row.prevTime.length() != row.time.length()) {
            Msdf.text(context, font, row.prevTime, badgeX + 3.0F, oldY, 5.0F, oldColor, false);
            Msdf.text(context, font, row.time, badgeX + 3.0F, newY, 5.0F, newColor, false);
         } else {
            float charX = badgeX + 3.0F;

            for (int i = 0; i < row.time.length(); i++) {
               String oldCh = String.valueOf(row.prevTime.charAt(i));
               String newCh = String.valueOf(row.time.charAt(i));
               if (oldCh.equals(newCh)) {
                  Msdf.text(context, font, newCh, charX, staticY, 5.0F, badgeTextColor, false);
               } else {
                  Msdf.text(context, font, oldCh, charX, oldY, 5.0F, oldColor, false);
                  Msdf.text(context, font, newCh, charX, newY, 5.0F, newColor, false);
               }

               charX += Msdf.width(font, newCh, 5.0F);
            }
         }

         Scissor.pop();
      }
   }

   private float presence(Potions.Row row, long now) {
      return row.disappearAt >= 0L ? 1.0F - Math.min(1.0F, (float)(now - row.disappearAt) / 220.0F) : Math.min(1.0F, (float)(now - row.appearAt) / 220.0F);
   }

   private float rowWidth(MsdfFont font, Potions.Row row) {
      float w = 8.0F + this.dotBlockWidth() + Msdf.width(font, row.name, 7.5F);
      if (row.level != null) {
         w += 2.5F + Msdf.width(font, row.level, 7.5F);
      }

      return w + (10.0F + Msdf.width(font, row.time, 5.0F) + 6.0F);
   }

   private Identifier texture(Identifier sprite) {
      return Identifier.of(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
   }

   private String formatDuration(StatusEffectInstance effect) {
      if (effect.isInfinite()) {
         return "**:**";
      }

      int seconds = effect.getDuration() / 20;
      return String.format("%d:%02d", seconds / 60, seconds % 60);
   }

   private static float easeOutCubic(float t) {
      float inv = 1.0F - Math.max(0.0F, Math.min(1.0F, t));
      return 1.0F - inv * inv * inv;
   }

   private static float easeOutBack(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      float c1 = 1.3F;
      float c3 = c1 + 1.0F;
      float d = t - 1.0F;
      return 1.0F + c3 * d * d * d + c1 * d * d;
   }

   private static final class Row {
      Identifier texture;
      String name;
      String level;
      String time;
      String prevTime;
      long appearAt;
      long disappearAt = -1L;
      long timeSwitchAt = -1L;
   }
}
