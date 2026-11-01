package ruhack.phobia.system.modulesystem.impl.display;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import ruhack.phobia.system.dragsystem.AbstractHudElement;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.impl.misc.FuntimeHelper;
import ruhack.phobia.system.modulesystem.impl.misc.HelperItemBind;
import ruhack.phobia.system.modulesystem.impl.misc.HolyworldHelper;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfGlyph;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.clickgui.ClickGui;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.istance.Instance;

public class BindPreview extends AbstractHudElement {
   private static BindPreview instance;
   private static final float CHIP_H = 22.0F;
   private static final float CHIP_MIN_W = 47.0F;
   private static final float CHIP_GAP = 5.0F;
   private static final float CHIP_PAD = 7.0F;
   private static final float CHIP_RADIUS = 8.0F;
   private static final float ITEM_SIZE = 9.0F;
   private static final float DOT_SIZE = 2.0F;
   private static final float INNER_GAP = 6.0F;
   private static final float KEY_SIZE = 7.0F;
   private static final float BLUR_STRENGTH = 8.0F;
   private static final long ANIMATION_MS = 220L;
   private final Map<String, BindPreview.Chip> chips = new LinkedHashMap<>();
   private final HashSet<String> liveKeys = new HashSet<>();

   public BindPreview() {
      super("BindPreview", 130, 200, 47, 22, true);
      instance = this;
   }

   public static BindPreview getInstance() {
      return instance;
   }

   @Override
   public float getRoundingRadius() {
      return 8.0F;
   }

   @Override
   public boolean visible() {
      return this.mc.player != null && (this.hasVisibleBinds() || this.mc.currentScreen instanceof ChatScreen || !this.chips.isEmpty());
   }

   @Override
   public void drawDraggable(DrawContext context, int alpha) {
      if (this.mc.player != null) {
         long now = System.currentTimeMillis();
         this.syncChips(now, this.mc.currentScreen instanceof ChatScreen);
         if (!this.chips.isEmpty()) {
            MsdfFont font = MsdfManager.BOLD;
            float cursor = this.x;
            float contentWidth = 0.0F;
            int visibleCount = 0;

            for (BindPreview.Chip chip : this.chips.values()) {
               float presence = this.presence(chip, now);
               if (!(presence <= 0.01F)) {
                  float eased = easeOutCubic(presence);
                  float chipWidth = this.chipWidth(font, chip.key);
                  float animatedWidth = chipWidth * eased;
                  float drawX = cursor;
                  float centerY = this.y + 11.0F;
                  float opacity = alpha / 255.0F * eased;
                  int bgColor = ColorUtil.multAlpha(ColorUtil.rgba(11, 12, 18, Math.round(Hud.getInstance().backgroundAlpha.getValue())), opacity);
                  int outlineColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 22), opacity);
                  int keyColor = ColorUtil.multAlpha(ColorUtil.rgba(242, 243, 248, 255), opacity);
                  int dotColor = ColorUtil.multAlpha(ColorUtil.rgba(138, 140, 151, 155), opacity);
                  Render2D.blur(context, drawX, this.y, animatedWidth, 22.0F, 8.0F, 8.0F, false);
                  Render2D.rect(context, drawX, this.y, animatedWidth, 22.0F, 8.0F, bgColor, false);
                  Render2D.outline(context, drawX, this.y, animatedWidth, 22.0F, 8.0F, 0.6F, outlineColor, false);
                  if (animatedWidth > 30.55F) {
                     float keyWidth = Msdf.width(font, chip.key, 7.0F);
                     float groupWidth = 23.0F + keyWidth;
                     float itemX = drawX + (animatedWidth - groupWidth) / 2.0F;
                     float dotX = itemX + 9.0F + 6.0F;
                     Render2D.rect(context, dotX, centerY - 1.0F, 2.0F, 2.0F, 1.0F, dotColor, false);
                     float keyX = dotX + 2.0F + 6.0F;
                     Msdf.text(context, font, chip.key, keyX, this.textY(font, 7.0F, centerY), 7.0F, keyColor, false);
                  }

                  cursor += animatedWidth + 5.0F * eased;
                  contentWidth += animatedWidth;
                  if (visibleCount++ > 0) {
                     contentWidth += 5.0F * eased;
                  }
               }
            }

            this.width = Math.max(1, (int)Math.ceil(contentWidth));
            this.height = 22;
         }
      }
   }

   public boolean queueItemModels(DrawContext context) {
      Hud hud = Hud.getInstance();
      if (hud != null
         && hud.isState()
         && hud.interfaceSettings.isSelected("BindPreview")
         && this.mc.player != null
         && !this.chips.isEmpty()
         && (
            this.mc.currentScreen == null
               || this.mc.currentScreen instanceof ChatScreen
               || this.mc.currentScreen instanceof GameMenuScreen
               || this.mc.currentScreen instanceof ClickGui
         )) {
         long now = System.currentTimeMillis();
         MsdfFont font = MsdfManager.BOLD;
         float cursor = this.x;
         boolean queued = false;

         for (BindPreview.Chip chip : this.chips.values()) {
            float presence = this.presence(chip, now);
            if (!(presence <= 0.01F)) {
               float eased = easeOutCubic(presence);
               float fullWidth = this.chipWidth(font, chip.key);
               float animatedWidth = fullWidth * eased;
               if (animatedWidth > 30.55F && !chip.stack.isEmpty()) {
                  float keyWidth = Msdf.width(font, chip.key, 7.0F);
                  float groupWidth = 23.0F + keyWidth;
                  float itemX = cursor + (animatedWidth - groupWidth) / 2.0F;
                  float itemY = this.y + 6.5F;
                  if (!this.screenOccludes(itemX, itemY, 9.0F, 9.0F)) {
                     this.drawItem(context, chip.stack, itemX, itemY, 9.0F);
                     queued = true;
                  }
               }

               cursor += animatedWidth + 5.0F * eased;
            }
         }

         return queued;
      } else {
         return false;
      }
   }

   private boolean screenOccludes(float itemX, float itemY, float itemWidth, float itemHeight) {
      float hudScale = Math.max(0.01F, Hud.getInstance().getInterfaceScale());
      if (this.mc.currentScreen instanceof ClickGui) {
         return ClickGui.INSTANCE.overlapsPanel(itemX * hudScale, itemY * hudScale, itemWidth * hudScale, itemHeight * hudScale);
      }

      if (!(this.mc.currentScreen instanceof GameMenuScreen)) {
         return false;
      }

      float screenWidth = Render2D.getFixedScaledWidth() / hudScale;
      float screenHeight = Render2D.getFixedScaledHeight() / hudScale;
      float menuWidth = 240.0F / hudScale;
      float menuTop = screenHeight * 0.18F;
      float menuBottom = screenHeight * 0.82F;
      float menuLeft = (screenWidth - menuWidth) / 2.0F;
      float menuRight = menuLeft + menuWidth;
      return itemX < menuRight && itemX + itemWidth > menuLeft && itemY < menuBottom && itemY + itemHeight > menuTop;
   }

   private void syncChips(long now, boolean chat) {
      this.liveKeys.clear();
      this.addHelper(now, Instance.get(HolyworldHelper.class));
      this.addHelper(now, Instance.get(FuntimeHelper.class));
      if (this.liveKeys.isEmpty() && chat) {
         this.updateChip(now, "demo_hw", Items.NETHER_STAR.getDefaultStack(), "X");
         this.updateChip(now, "demo_ft_1", Items.ENDER_EYE.getDefaultStack(), "V");
         this.updateChip(now, "demo_ft_2", Items.FIRE_CHARGE.getDefaultStack(), "B");
      }

      Iterator<Entry<String, BindPreview.Chip>> iterator = this.chips.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<String, BindPreview.Chip> entry = iterator.next();
         if (!this.liveKeys.contains(entry.getKey())) {
            BindPreview.Chip chip = entry.getValue();
            if (chip.disappearAt < 0L) {
               float p = this.presence(chip, now);
               chip.disappearAt = now - (long)((1.0F - p) * 220.0F);
            } else if (now - chip.disappearAt > 220L) {
               iterator.remove();
            }
         }
      }
   }

   private void addHelper(long now, ModuleStructure helper) {
      if (helper != null && helper.isState()) {
         List<HelperItemBind> binds;
         if (helper instanceof HolyworldHelper holyworld) {
            binds = holyworld.getItemBinds();
         } else {
            if (!(helper instanceof FuntimeHelper funtime)) {
               return;
            }

            binds = funtime.getItemBinds();
         }

         for (HelperItemBind entry : binds) {
            if (entry.bind().getKey() != -1) {
               String id = entry.source() + ":" + entry.bind().getName();
               this.updateChip(now, id, entry.item().getDefaultStack(), this.formatKey(entry.bind().getKeyDisplayName()));
            }
         }
      }
   }

   private boolean hasVisibleBinds() {
      return this.helperHasBinds(Instance.get(HolyworldHelper.class)) || this.helperHasBinds(Instance.get(FuntimeHelper.class));
   }

   private boolean helperHasBinds(ModuleStructure helper) {
      if (helper != null && helper.isState()) {
         List<HelperItemBind> binds;
         if (helper instanceof HolyworldHelper holyworld) {
            binds = holyworld.getItemBinds();
         } else if (helper instanceof FuntimeHelper funtime) {
            binds = funtime.getItemBinds();
         } else {
            binds = List.of();
         }
         for (HelperItemBind entry : binds) {
            if (entry.bind().getKey() != -1) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void updateChip(long now, String id, ItemStack stack, String key) {
      this.liveKeys.add(id);
      BindPreview.Chip chip = this.chips.get(id);
      if (chip == null) {
         chip = new BindPreview.Chip();
         chip.appearAt = now;
         this.chips.put(id, chip);
      } else if (chip.disappearAt >= 0L) {
         float p = this.presence(chip, now);
         chip.appearAt = now - (long)(p * 220.0F);
         chip.disappearAt = -1L;
      }

      chip.stack = stack;
      chip.key = key;
   }

   private float chipWidth(MsdfFont font, String key) {
      float natural = 30.0F + Msdf.width(font, key, 7.0F) + 7.0F;
      return Math.max(47.0F, natural);
   }

   private void drawItem(DrawContext context, ItemStack stack, float itemX, float itemY, float size) {
      float hudScale = Hud.getInstance().getInterfaceScale();
      float logicalToGui = hudScale * 2.0F / Math.max(1, this.mc.getWindow().getScaleFactor());
      float modelScale = size * logicalToGui / 16.0F;
      Matrix3x2fStack matrices = context.getMatrices();
      matrices.pushMatrix();
      matrices.translate(itemX * logicalToGui, itemY * logicalToGui);
      matrices.scale(modelScale, modelScale);
      context.drawItem(stack, 0, 0);
      matrices.popMatrix();
   }

   private float textY(MsdfFont font, float size, float centerY) {
      MsdfGlyph glyph = font.getGlyph(48);
      if (glyph != null && !(glyph.height <= 0.0F)) {
         float scale = size / font.getEmSize();
         return centerY - (font.getAscender() - glyph.bearingY + glyph.height / 2.0F) * scale;
      } else {
         return centerY - font.getLineHeight() * size / 2.0F;
      }
   }

   private float presence(BindPreview.Chip chip, long now) {
      return chip.disappearAt >= 0L ? 1.0F - Math.min(1.0F, (float)(now - chip.disappearAt) / 220.0F) : Math.min(1.0F, (float)(now - chip.appearAt) / 220.0F);
   }

   private String formatKey(String key) {
      String formatted = key.toUpperCase(Locale.US).replace("NUMPAD", "NUM").replace(" ", "");
      return "`".equals(formatted) ? "GRAVE" : formatted;
   }

   private static float easeOutCubic(float value) {
      float inverse = 1.0F - MathHelper.clamp(value, 0.0F, 1.0F);
      return 1.0F - inverse * inverse * inverse;
   }

   private static final class Chip {
      ItemStack stack = ItemStack.EMPTY;
      String key;
      long appearAt;
      long disappearAt = -1L;
   }
}
