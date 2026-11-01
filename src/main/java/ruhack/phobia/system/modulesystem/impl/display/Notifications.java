package ruhack.phobia.system.modulesystem.impl.display;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import ruhack.phobia.system.dragsystem.PillHudElement;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.colorschem.ColorUtil;

public class Notifications extends PillHudElement {
   private static final String ICON_BELL = "M";
   private static final String ICON_CONFIG = String.valueOf('\ue00a');
   private static final float CONFIG_VISUAL = 8.0F;
   private static final long LIFETIME_MS = 2500L;
   private static final long ANIM_MS = 200L;
   private static final float STACK_GAP = 5.0F;
   private static final float SLIDE = 6.0F;
   private static final int MAX_TOASTS = 6;
   private static final float TOGGLE_W = 11.0F;
   private static final float TOGGLE_H = 6.5F;
   private static final float KNOB = 4.5F;
   private static final List<Notifications.Toast> TOASTS = new CopyOnWriteArrayList<>();
   private List<Notifications.Toast> demoToasts;
   private boolean demoShown;

   public static void info(String message) {
      push(new Notifications.Toast(Notifications.Type.INFO, List.of(new Notifications.Part(message, false))));
   }

   public static void module(String moduleName, boolean enabled) {
      push(
         new Notifications.Toast(
            enabled ? Notifications.Type.TOGGLE_ON : Notifications.Type.TOGGLE_OFF,
            List.of(
               new Notifications.Part("Функция ", false),
               new Notifications.Part(moduleName, true),
               new Notifications.Part(enabled ? " активирована" : " деактивирована", false)
            )
         )
      );
   }

   public static void config(String configName) {
      push(
         new Notifications.Toast(
            Notifications.Type.CONFIG,
            List.of(new Notifications.Part("Конфиг ", false), new Notifications.Part(configName, true), new Notifications.Part(" загружен", false))
         )
      );
   }

   private static void push(Notifications.Toast toast) {
      while (TOASTS.size() >= 6) {
         TOASTS.remove(0);
      }

      TOASTS.add(toast);
   }

   public Notifications() {
      super("Notifications", 0, 36, true);
   }

   @Override
   public void setX(int x) {
   }

   @Override
   public boolean visible() {
      return !TOASTS.isEmpty() || this.mc.currentScreen instanceof ChatScreen;
   }

   private List<Notifications.Toast> demoToasts() {
      if (this.demoToasts == null) {
         this.demoToasts = List.of(new Notifications.Toast(Notifications.Type.INFO, List.of(new Notifications.Part("Обычное уведомление", false)), true));
      }

      return this.demoToasts;
   }

   @Override
   public void drawDraggable(DrawContext context, int alpha) {
      long now = System.currentTimeMillis();
      MsdfFont font = MsdfManager.BOLD;
      MsdfFont icons = MsdfManager.ICONS;
      boolean chat = this.mc.currentScreen instanceof ChatScreen;
      List<Notifications.Toast> source = TOASTS;
      if (TOASTS.isEmpty() && chat) {
         source = this.demoToasts();
         if (!this.demoShown) {
            this.demoShown = true;

            for (Notifications.Toast toast : source) {
               toast.createdAt = now;
               toast.curY = Float.NaN;
            }
         }
      } else if (!chat) {
         this.demoShown = false;
      }

      float centerX = Render2D.getFixedScaledWidth() / (2.0F * Math.max(0.01F, Render2D.getContextScale()));
      float stackY = this.y;
      float maxW = 0.0F;

      for (Notifications.Toast toast : source) {
         if (!toast.sticky && toast.removeAt < 0L && now - toast.createdAt >= 2500L) {
            toast.removeAt = now;
         }

         float p = this.presence(toast, now);
         if (toast.removeAt < 0L || !(p <= 0.001F)) {
            float eased = easeOutCubic(p);
            float a = alpha / 255.0F * eased;
            int accent = ColorUtil.multAlpha(ColorUtil.getClientColorAt((float)this.x / Math.max(1, Render2D.getFixedScaledWidth())), a);
            int textColor = ColorUtil.multAlpha(ColorUtil.rgba(236, 238, 245, 255), a);
            int dotColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 70), a);
            int bgColor = ColorUtil.multAlpha(ColorUtil.rgba(11, 12, 18, Math.round(Hud.getInstance().backgroundAlpha.getValue())), a);
            int outlineColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 20), a);
            if (toast.iconWidth < 0.0F) {
               toast.iconWidth = switch (toast.type) {
                  case INFO -> Msdf.width(icons, "M", this.iconSize(icons, "M", 6.5F));
                  case TOGGLE_ON, TOGGLE_OFF -> 11.0F;
                  case CONFIG -> Msdf.width(icons, ICON_CONFIG, this.iconSize(icons, ICON_CONFIG, 8.0F));
               };
            }

            if (toast.textWidth < 0.0F) {
               toast.textWidth = 0.0F;

               for (Notifications.Part part : toast.parts) {
                  toast.textWidth = toast.textWidth + Msdf.width(font, part.text(), 7.5F);
               }
            }

            float iconWidth = toast.iconWidth;
            float textWidth = toast.textWidth;
            float toastW = 9.0F + iconWidth + this.dotBlockWidth() + textWidth + 9.0F;
            float toastX = centerX - toastW / 2.0F;
            maxW = Math.max(maxW, toastW);
            if (Float.isNaN(toast.curY)) {
               toast.curY = stackY;
            }

            toast.curY = toast.curY + (stackY - toast.curY) * 0.25F;
            float toastY = toast.curY - (1.0F - eased) * 6.0F;
            Render2D.blur(context, toastX, toastY, toastW, 21.0F, 10.5F, 8.0F, false);
            Render2D.rect(context, toastX, toastY, toastW, 21.0F, 10.5F, bgColor, false);
            Render2D.outline(context, toastX, toastY, toastW, 21.0F, 10.5F, 0.6F, outlineColor, false);
            float center = toastY + 10.5F;
            float cursor = toastX + 9.0F;
            switch (toast.type) {
               case INFO: {
                  float size = this.iconSize(icons, "M", 6.5F);
                  Msdf.text(context, icons, "M", cursor, this.glyphY(icons, "M", size, center), size, accent, false);
                  break;
               }
               case TOGGLE_ON:
                  this.drawToggle(context, cursor, center, true, accent, a);
                  break;
               case TOGGLE_OFF:
                  this.drawToggle(context, cursor, center, false, accent, a);
                  break;
               case CONFIG: {
                  float size = this.iconSize(icons, ICON_CONFIG, 8.0F);
                  Msdf.text(context, icons, ICON_CONFIG, cursor, this.glyphY(icons, ICON_CONFIG, size, center), size, accent, false);
               }
            }

            cursor += iconWidth;
            cursor = this.drawDot(context, cursor, center, dotColor);
            float textY = this.textY(font, 7.5F, center);

            for (Notifications.Part part : toast.parts) {
               Msdf.text(context, font, part.text(), cursor, textY, 7.5F, part.accent() ? accent : textColor, false);
               cursor += Msdf.width(font, part.text(), 7.5F);
            }

            stackY += 26.0F * eased;
         } else if (!toast.sticky) {
            TOASTS.remove(toast);
         }
      }

      this.width = (int)Math.max(60.0F, maxW);
      this.height = (int)Math.max(21.0F, stackY - this.y - 5.0F);
      this.x = (int)(centerX - this.width / 2.0F);
   }

   private void drawToggle(DrawContext context, float x, float centerY, boolean on, int accent, float alpha) {
      float trackY = centerY - 3.25F;
      int trackColor = on ? accent : ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 45), alpha);
      Render2D.rect(context, x, trackY, 11.0F, 6.5F, 3.25F, trackColor, false);
      float inset = 1.0F;
      float knobX = on ? x + 11.0F - inset - 4.5F : x + inset;
      int knobColor = ColorUtil.multAlpha(ColorUtil.rgba(240, 240, 248, on ? 255 : 160), alpha);
      Render2D.rect(context, knobX, trackY + inset, 4.5F, 4.5F, 2.25F, knobColor, false);
   }

   private float presence(Notifications.Toast toast, long now) {
      return toast.removeAt >= 0L ? 1.0F - Math.min(1.0F, (float)(now - toast.removeAt) / 200.0F) : Math.min(1.0F, (float)(now - toast.createdAt) / 200.0F);
   }

   private static float easeOutCubic(float t) {
      float inv = 1.0F - MathHelper.clamp(t, 0.0F, 1.0F);
      return 1.0F - inv * inv * inv;
   }

   private record Part(String text, boolean accent) {
   }

   private static final class Toast {
      final Notifications.Type type;
      final List<Notifications.Part> parts;
      final boolean sticky;
      long createdAt;
      long removeAt = -1L;
      float curY = Float.NaN;
      float textWidth = -1.0F;
      float iconWidth = -1.0F;

      Toast(Notifications.Type type, List<Notifications.Part> parts) {
         this(type, parts, false);
      }

      Toast(Notifications.Type type, List<Notifications.Part> parts, boolean sticky) {
         this.type = type;
         this.parts = parts;
         this.sticky = sticky;
         this.createdAt = System.currentTimeMillis();
      }
   }

   private enum Type {
      INFO,
      TOGGLE_ON,
      TOGGLE_OFF,
      CONFIG;
   }
}
