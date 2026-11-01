package ruhack.phobia.system.screensystem;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import ruhack.phobia.system.modulesystem.impl.rage.AutoSwap;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.rendersystem.render2d.ring.SegmentedRing2D;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.player.swap.MovementController;

public final class AutoSwapRingScreen extends Screen {
   private static final int RING_SIZE = 3;
   private static final float BASE_SIZE = 142.0F;
   private static final float BASE_THICKNESS = 31.0F;
   private static final float SECTOR_DEGREES = 106.0F;
   private static final float[] SECTOR_CENTER_ANGLES = new float[]{90.0F, -30.0F, -150.0F};
   private static final long OPEN_DURATION_NS = 180000000L;
   private static final long CLOSE_DURATION_NS = 130000000L;
   private final AutoSwap autoSwap;
   private final Screen parent;
   private final MovementController movement = new MovementController();
   private int hoveredSlot = -1;
   private int pendingSwapSlot = -1;
   private long openedAt;
   private long closingAt;
   private boolean closing;
   private float visualScale = 1.0F;

   public AutoSwapRingScreen(AutoSwap autoSwap, Screen parent) {
      super(Text.literal("AutoSwap"));
      this.autoSwap = autoSwap;
      this.parent = parent;
   }

   protected void init() {
      this.openedAt = System.nanoTime();
      this.closingAt = 0L;
      this.closing = false;
      this.pendingSwapSlot = -1;
      this.movement.restoreFromCurrent();
   }

   public void tick() {
      this.movement.restoreFromCurrent();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      this.movement.restoreFromCurrent();
      this.autoSwap.validateRingItems();
      long now = System.nanoTime();
      float alpha;
      if (this.closing) {
         float progress = this.clamp01((float)(now - this.closingAt) / 1.3E8F);
         alpha = 1.0F - this.easeInCubic(progress);
         this.visualScale = 1.0F - this.easeInCubic(progress) * 0.16F;
         if (progress >= 1.0F) {
            this.finishClosing();
            return;
         }
      } else {
         float progress = this.clamp01((float)(now - this.openedAt) / 1.8E8F);
         alpha = this.easeOutCubic(progress);
         this.visualScale = 0.82F + this.easeOutBack(progress) * 0.18F;
      }

      float centerX = Render2D.getFixedScaledWidth() / 2.0F;
      float centerY = Render2D.getFixedScaledHeight() / 2.0F;
      float size = 142.0F * this.visualScale;
      float thickness = 31.0F * this.visualScale;
      this.hoveredSlot = this.closing ? -1 : this.findSector(Render2D.convertX(mouseX), Render2D.convertY(mouseY), centerX, centerY, size, thickness);
      int accent = ColorUtil.getClientColor();
      int[] colors = new int[3];

      for (int slot = 0; slot < 3; slot++) {
         if (!this.autoSwap.hasRingItem(slot)) {
            colors[slot] = ColorUtil.multAlpha(ColorUtil.rgba(21, 19, 27, 255), alpha * 0.72F);
         } else if (slot == this.hoveredSlot) {
            colors[slot] = ColorUtil.multAlpha(accent, alpha * 0.95F);
         } else if (this.autoSwap.isRingItemEquipped(slot)) {
            colors[slot] = ColorUtil.multAlpha(accent, alpha * 0.58F);
         } else {
            colors[slot] = ColorUtil.multAlpha(ColorUtil.rgba(52, 44, 65, 255), alpha);
         }
      }

      Matrix4f projection = Render2D.createProjection();
      Render2D.addOverrideTask(() -> SegmentedRing2D.draw(projection, centerX - size / 2.0F, centerY - size / 2.0F, size, thickness, 106.0F, 0.0F, colors));
      this.drawCenterText(context, centerX, centerY, alpha);

      for (int slot = 0; slot < 3; slot++) {
         this.drawSlot(context, slot, centerX, centerY, size, thickness, alpha);
      }
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   protected void applyBlur(DrawContext context) {
   }

   @Override
   public void renderInGameBackground(DrawContext context) {
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      if (this.closing) {
         return true;
      }

      float centerX = Render2D.getFixedScaledWidth() / 2.0F;
      float centerY = Render2D.getFixedScaledHeight() / 2.0F;
      int slot = this.findSector(
         Render2D.convertX((float)click.x()), Render2D.convertY((float)click.y()), centerX, centerY, 142.0F * this.visualScale, 31.0F * this.visualScale
      );
      if (slot < 0) {
         return true;
      }

      if (click.button() == 1) {
         this.autoSwap.clearRingItem(slot);
         return true;
      }

      if (click.button() != 0) {
         return true;
      }

      if (this.autoSwap.getRingItemStack(slot).isEmpty()) {
         if (this.client != null && this.client.player != null) {
            this.client.setScreen(new AutoSwapInventoryScreen(this.client.player, this.autoSwap, this, slot));
         }

         return true;
      } else if (!this.autoSwap.hasRingItem(slot)) {
         this.autoSwap.clearRingItem(slot);
         return true;
      } else {
         this.pendingSwapSlot = slot;
         this.startClosing();
         return true;
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      this.startClosing();
   }

   private void startClosing() {
      if (!this.closing) {
         this.closing = true;
         this.closingAt = System.nanoTime();
      }
   }

   private void finishClosing() {
      if (this.client != null) {
         int slot = this.pendingSwapSlot;
         this.pendingSwapSlot = -1;
         this.client.setScreen(this.parent);
         if (slot >= 0) {
            this.autoSwap.swapRingItem(slot);
         }
      }
   }

   private void drawCenterText(DrawContext context, float centerX, float centerY, float alpha) {
      if (Msdf.hasFonts() && MsdfManager.BOLD != null) {
         this.centeredText(
            context,
            "AutoSwap",
            centerX,
            centerY - 8.0F * this.visualScale,
            8.5F * this.visualScale,
            ColorUtil.multAlpha(ColorUtil.rgba(242, 239, 248, 255), alpha)
         );
         String caption = this.hoveredSlot < 0 ? "Выберите предмет" : this.autoSwap.getRingItemName(this.hoveredSlot);
         int captionColor = this.hoveredSlot >= 0 && this.autoSwap.hasRingItem(this.hoveredSlot)
            ? ColorUtil.getClientColor()
            : ColorUtil.rgba(144, 135, 158, 255);
         this.centeredText(context, caption, centerX, centerY + 3.0F * this.visualScale, 6.2F * this.visualScale, ColorUtil.multAlpha(captionColor, alpha));
      }
   }

   private void drawSlot(DrawContext context, int slot, float centerX, float centerY, float size, float thickness, float alpha) {
      if (!(alpha < 0.12F)) {
         double angle = Math.toRadians(SECTOR_CENTER_ANGLES[slot]);
         float radius = size / 2.0F - thickness / 2.0F;
         float itemCenterX = centerX + (float)Math.cos(angle) * radius;
         float itemCenterY = centerY + (float)Math.sin(angle) * radius;
         ItemStack stack = this.autoSwap.getRingItemStack(slot);
         if (stack.isEmpty()) {
            if (Msdf.hasFonts() && MsdfManager.BOLD != null) {
               this.centeredText(
                  context,
                  "+",
                  itemCenterX,
                  itemCenterY - 4.6F * this.visualScale,
                  9.0F * this.visualScale,
                  ColorUtil.multAlpha(slot == this.hoveredSlot ? ColorUtil.rgba(255, 255, 255, 255) : ColorUtil.rgba(176, 166, 190, 255), alpha)
               );
            }
         } else {
            float fixedItemSize = 18.0F * this.visualScale;
            float guiScale = Math.max(0.01F, Render2D.getScaleFactor());
            float guiSize = fixedItemSize / guiScale;
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(itemCenterX / guiScale - guiSize / 2.0F, itemCenterY / guiScale - guiSize / 2.0F);
            matrices.scale(guiSize / 16.0F, guiSize / 16.0F);
            context.drawItem(stack, 0, 0);
            matrices.popMatrix();
         }
      }
   }

   private void centeredText(DrawContext context, String text, float centerX, float y, float size, int color) {
      Msdf.text(context, MsdfManager.BOLD, text, centerX - Msdf.width(MsdfManager.BOLD, text, size) / 2.0F, y, size, color, true);
   }

   private int findSector(float mouseX, float mouseY, float centerX, float centerY, float size, float thickness) {
      float dx = mouseX - centerX;
      float dy = mouseY - centerY;
      float distance = (float)Math.sqrt(dx * dx + dy * dy);
      float outerRadius = size / 2.0F + 3.0F;
      float innerRadius = size / 2.0F - thickness - 3.0F;
      if (!(distance < innerRadius) && !(distance > outerRadius)) {
         float angle = (float)Math.toDegrees(Math.atan2(dy, dx));

         for (int slot = 0; slot < SECTOR_CENTER_ANGLES.length; slot++) {
            if (Math.abs(this.wrappedDegrees(angle - SECTOR_CENTER_ANGLES[slot])) <= 53.0F) {
               return slot;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private float wrappedDegrees(float degrees) {
      float wrapped = degrees % 360.0F;
      if (wrapped > 180.0F) {
         wrapped -= 360.0F;
      }

      if (wrapped < -180.0F) {
         wrapped += 360.0F;
      }

      return wrapped;
   }

   private float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private float easeOutCubic(float value) {
      float inverse = 1.0F - this.clamp01(value);
      return 1.0F - inverse * inverse * inverse;
   }

   private float easeInCubic(float value) {
      float clamped = this.clamp01(value);
      return clamped * clamped * clamped;
   }

   private float easeOutBack(float value) {
      float clamped = this.clamp01(value);
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float shifted = clamped - 1.0F;
      return 1.0F + c3 * shifted * shifted * shifted + c1 * shifted * shifted;
   }
}
