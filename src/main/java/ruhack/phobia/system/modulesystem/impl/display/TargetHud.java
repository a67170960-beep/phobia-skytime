package ruhack.phobia.system.modulesystem.impl.display;

import java.util.Arrays;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import ruhack.phobia.system.dragsystem.PillHudElement;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.colorschem.ColorUtil;

public class TargetHud extends PillHudElement {
   private static TargetHud instance;
   private static final float TARGET_SCALE = 1.25F;
   private static final float RADIUS = 10.0F;
   private static final float PAD = 8.75F;
   private static final float HEAD = 22.5F;
   private static final float HEAD_RADIUS = 3.75F;
   private static final float GAP = 7.5F;
   private static final float TARGET_TEXT_SIZE = 9.375F;
   private static final float TARGET_UNIT_SIZE = 6.25F;
   private static final float LABEL_SIZE = 6.25F;
   private static final float FALLBACK_LETTER_SIZE = 11.25F;
   private static final float EMPTY_SLOT_TEXT_SIZE = 6.25F;
   private static final float HP_GAP = 7.5F;
   private static final float BAR_H = 3.125F;
   private static final float MIN_TEXT_BLOCK = 68.75F;
   private static final float SLOTS_GAP_Y = 5.0F;
   private static final float SLOT = 10.0F;
   private static final float SLOT_GAP = 5.625F;
   private static final float SLOTS_PAD = 7.5F;
   private static final float SLOTS_H = 17.5F;
   private static final EquipmentSlot[] SLOT_ORDER = new EquipmentSlot[]{
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
   };
   private float panelP = 0.0F;
   private float smoothWidth = -1.0F;
   private float smoothHealth = -1.0F;
   private long lastFrame = -1L;
   private String name = "";
   private String typeLabel = "";
   private Identifier skin;
   private float health;
   private float maxHealth = 20.0F;
   private final ItemStack[] equipment = new ItemStack[SLOT_ORDER.length];
   private int snapshotEntityId = Integer.MIN_VALUE;

   public TargetHud() {
      super("TargetHud", 160, 90, true);
      instance = this;
      Arrays.fill(this.equipment, ItemStack.EMPTY);
   }

   public static TargetHud getInstance() {
      return instance;
   }

   @Override
   public float getRoundingRadius() {
      return 10.0F;
   }

   @Override
   public boolean visible() {
      return this.mc.player != null && (this.currentTarget() != null || this.mc.currentScreen instanceof ChatScreen || this.panelP > 0.02F);
   }

   private LivingEntity currentTarget() {
      KillAura aura = KillAura.getInstance();
      return aura != null && aura.isState() && aura.getTarget() != null && aura.getTarget().isAlive() ? aura.getTarget() : null;
   }

   @Override
   public void drawDraggable(DrawContext context, int alpha) {
      if (this.mc.player != null) {
         long now = System.currentTimeMillis();
         float dt = this.lastFrame < 0L ? 16.0F : Math.min((float)(now - this.lastFrame), 100.0F);
         this.lastFrame = now;
         LivingEntity target = this.currentTarget();
         boolean chat = this.mc.currentScreen instanceof ChatScreen;
         if (target == null && chat) {
            target = this.mc.player;
         }

         if (target != null) {
            this.snapshot(target);
         }

         float targetP = target != null ? 1.0F : 0.0F;
         this.panelP = this.panelP + (targetP - this.panelP) * (1.0F - (float)Math.exp(-dt / 55.0F));
         this.panelP = MathHelper.clamp(this.panelP, 0.0F, 1.0F);
         if (target != null || !(this.panelP <= 0.02F)) {
            if (!this.name.isEmpty()) {
               float a = alpha / 255.0F * easeOutCubic(this.panelP);
               int accent = ColorUtil.multAlpha(ColorUtil.getClientColorAt((float)this.x / Math.max(1, Render2D.getFixedScaledWidth())), a);
               int textColor = ColorUtil.multAlpha(ColorUtil.rgba(236, 238, 245, 255), a);
               int labelColor = ColorUtil.multAlpha(ColorUtil.rgba(150, 152, 165, 255), a);
               int bgColor = ColorUtil.multAlpha(ColorUtil.rgba(11, 12, 18, Math.round(Hud.getInstance().backgroundAlpha.getValue())), a);
               int outlineColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 20), a);
               int trackColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 25), a);
               int slotEmptyColor = ColorUtil.multAlpha(ColorUtil.rgba(120, 122, 135, 255), a);
               int spriteTint = ColorUtil.rgba(255, 255, 255, (int)(a * 255.0F));
               MsdfFont font = MsdfManager.BOLD;
               if (this.smoothHealth < 0.0F) {
                  this.smoothHealth = this.health;
               }

               this.smoothHealth = this.smoothHealth + (this.health - this.smoothHealth) * (1.0F - (float)Math.exp(-dt / 90.0F));
               String hp = String.valueOf(Math.round(this.smoothHealth));
               float nameWidth = Msdf.width(font, this.name, 9.375F) + 7.5F + Msdf.width(font, hp, 9.375F) + 1.0F + Msdf.width(font, "hp", 6.25F);
               float textBlock = Math.max(68.75F, Math.max(nameWidth, Msdf.width(font, this.typeLabel, 6.25F)));
               float targetWidth = 38.75F + textBlock + 8.75F;
               if (this.smoothWidth < 0.0F) {
                  this.smoothWidth = targetWidth;
               }

               this.smoothWidth = this.smoothWidth + (targetWidth - this.smoothWidth) * (1.0F - (float)Math.exp(-dt / 70.0F));
               this.width = (int)Math.ceil(this.smoothWidth);
               this.height = (int)Math.ceil(62.5);
               float mainH = 40.0F;
               Render2D.blur(context, this.x, this.y, this.width, mainH, 10.0F, 8.0F, false);
               Render2D.rect(context, this.x, this.y, this.width, mainH, 10.0F, bgColor, false);
               Render2D.outline(context, this.x, this.y, this.width, mainH, 10.0F, 0.6F, outlineColor, false);
               float headX = this.x + 8.75F;
               float headY = this.y + 8.75F;
               if (this.skin != null) {
                  Render2D.imageRegion(context, headX, headY, 22.5F, 22.5F, this.skin, spriteTint, 3.75F, 0.125F, 0.125F, 0.25F, 0.25F, true, false);
                  Render2D.imageRegion(context, headX, headY, 22.5F, 22.5F, this.skin, spriteTint, 3.75F, 0.625F, 0.125F, 0.75F, 0.25F, true, false);
               } else {
                  Render2D.rect(context, headX, headY, 22.5F, 22.5F, 3.75F, ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 18), a), false);
                  String letter = this.name.isEmpty() ? "?" : this.name.substring(0, 1).toUpperCase();
                  Msdf.text(
                     context,
                     font,
                     letter,
                     headX + 11.25F - Msdf.width(font, letter, 11.25F) / 2.0F,
                     this.textY(font, 11.25F, headY + 11.25F),
                     11.25F,
                     accent,
                     false
                  );
               }

               float textX = this.x + 8.75F + 22.5F + 7.5F;
               Msdf.text(context, font, this.typeLabel, textX, this.y + 8.75F - 0.5F, 6.25F, labelColor, false);
               float nameY = this.textY(font, 9.375F, this.y + 8.75F + 11.25F);
               Msdf.text(context, font, this.name, textX, nameY, 9.375F, textColor, false);
               float hpX = textX + Msdf.width(font, this.name, 9.375F) + 7.5F;
               Msdf.text(context, font, hp, hpX, nameY, 9.375F, textColor, false);
               Msdf.text(context, font, "hp", hpX + Msdf.width(font, hp, 9.375F) + 1.0F, this.unitY(font, nameY), 6.25F, accent, false);
               float barY = this.y + 8.75F + 22.5F - 3.125F;
               float barW = textBlock;
               float fill = this.maxHealth > 0.0F ? MathHelper.clamp(this.smoothHealth / this.maxHealth, 0.0F, 1.0F) : 0.0F;
               Render2D.rect(context, textX, barY, barW, 3.125F, 1.5625F, trackColor, false);
               if (fill > 0.01F) {
                  Render2D.rect(context, textX, barY, Math.max(3.125F, barW * fill), 3.125F, 1.5625F, accent, false);
               }

               float slotsW = 15.0F + SLOT_ORDER.length * 10.0F + (SLOT_ORDER.length - 1) * 5.625F;
               float slotsX = this.x + (this.width - slotsW) / 2.0F;
               float slotsY = this.y + mainH + 5.0F;
               Render2D.blur(context, slotsX, slotsY, slotsW, 17.5F, 8.75F, 8.0F, false);
               Render2D.rect(context, slotsX, slotsY, slotsW, 17.5F, 8.75F, bgColor, false);
               Render2D.outline(context, slotsX, slotsY, slotsW, 17.5F, 8.75F, 0.6F, outlineColor, false);
               float slotX = slotsX + 7.5F;
               float slotCenter = slotsY + 8.75F;

               for (int i = 0; i < SLOT_ORDER.length; i++) {
                  if (this.equipment[i].isEmpty()) {
                     Msdf.text(
                        context,
                        font,
                        "x",
                        slotX + 5.0F - Msdf.width(font, "x", 6.25F) / 2.0F,
                        this.textY(font, 6.25F, slotCenter),
                        6.25F,
                        slotEmptyColor,
                        false
                     );
                  }

                  slotX += 15.625F;
               }
            }
         }
      }
   }

   private void snapshot(LivingEntity target) {
      if (this.snapshotEntityId != target.getId()) {
         this.snapshotEntityId = target.getId();
         this.name = target.getName().getString();
         this.typeLabel = target.getType().getName().getString();
         this.skin = target instanceof AbstractClientPlayerEntity player ? player.getSkin().body().texturePath() : null;
         Arrays.fill(this.equipment, ItemStack.EMPTY);
      }

      this.health = target.getHealth() + target.getAbsorptionAmount();
      this.maxHealth = Math.max(1.0F, target.getMaxHealth());

      for (int i = 0; i < SLOT_ORDER.length; i++) {
         ItemStack stack = target.getEquippedStack(SLOT_ORDER[i]);
         this.equipment[i] = stack == null ? ItemStack.EMPTY : stack;
      }
   }

   public boolean queueEquipmentModels(DrawContext context) {
      Hud hud = Hud.getInstance();
      if (hud != null && hud.isState() && hud.interfaceSettings.isSelected("TargetHud") && this.mc.player != null) {
         LivingEntity target = this.currentTarget();
         if (target == null && this.mc.currentScreen instanceof ChatScreen) {
            target = this.mc.player;
         }

         if (target == null) {
            return false;
         }

         this.snapshot(target);
         float mainH = 40.0F;
         float slotsW = 15.0F + SLOT_ORDER.length * 10.0F + (SLOT_ORDER.length - 1) * 5.625F;
         float slotsX = this.x + (this.width - slotsW) / 2.0F;
         float slotsY = this.y + mainH + 5.0F;
         float slotX = slotsX + 7.5F;
         float slotY = slotsY + 3.75F;
         float hudScale = hud.getInterfaceScale();
         boolean queued = false;

         for (ItemStack stack : this.equipment) {
            if (!stack.isEmpty()) {
               this.queueItemModel(context, stack, slotX, slotY, 10.0F, hudScale);
               queued = true;
            }

            slotX += 15.625F;
         }

         return queued;
      } else {
         return false;
      }
   }

   private void queueItemModel(DrawContext context, ItemStack stack, float x, float y, float size, float hudScale) {
      float logicalToGui = hudScale * 2.0F / Math.max(1, this.mc.getWindow().getScaleFactor());
      float modelScale = size * logicalToGui / 16.0F;
      Matrix3x2fStack matrices = context.getMatrices();
      matrices.pushMatrix();
      matrices.translate(x * logicalToGui, y * logicalToGui);
      matrices.scale(modelScale, modelScale);
      context.drawItem(stack, 0, 0);
      matrices.popMatrix();
   }

   private static float easeOutCubic(float t) {
      float inv = 1.0F - MathHelper.clamp(t, 0.0F, 1.0F);
      return 1.0F - inv * inv * inv;
   }
}
