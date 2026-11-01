package ruhack.phobia.system.modulesystem.impl.render.boobs;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import ruhack.phobia.system.modulesystem.impl.render.Boobs;

public class BoobsFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final Map<Integer, BoobsFeatureRenderer.PhysicsState> PHYSICS = new HashMap<>();

   public BoobsFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
      super(context);
   }

   public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Boobs boobs = Boobs.getInstance();
      if (boobs != null && boobs.isState() && mc.player != null && !state.spectator && !state.invisibleToPlayer) {
         boolean local = state.id == mc.player.getId();
         if (local || !boobs.display.isSelected("На себе")) {
            if (!local || !mc.options.getPerspective().isFirstPerson()) {
               PlayerEntity player = getPlayer(mc, state.id);
               if (player == null || boobs.showInArmor.isValue() || player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) {
                  float size = boobs.size.getValue() / 100.0F;
                  if (!(size <= 0.001F) && state.skinTextures != null && state.skinTextures.body() != null) {
                     BoobsFeatureRenderer.PhysicsState physics = PHYSICS.computeIfAbsent(state.id, ignored -> new BoobsFeatureRenderer.PhysicsState());
                     physics.update(player, state, boobs);
                     Identifier skin = state.skinTextures.body().texturePath();
                     Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
                     VertexConsumer consumer = immediate.getBuffer(RenderLayers.entityTranslucent(skin));
                     matrices.push();
                     ((PlayerEntityModel)this.getContextModel()).body.applyTransform(matrices);
                     float width = 0.19F + size * 0.09F;
                     float height = 0.18F + size * 0.09F;
                     float depth = 0.07F + size * 0.16F;
                     float separation = 0.012F + boobs.cleavage.getValue() * 0.075F;
                     float centerY = 0.22F + boobs.yOffset.getValue() * 0.075F + physics.renderOffsetY;
                     float frontZ = -0.126F - boobs.zOffset.getValue() * 0.055F;
                     float shiftX = boobs.xOffset.getValue() * 0.065F;
                     if (boobs.uniboob.isValue()) {
                        matrices.push();
                        matrices.translate(shiftX, centerY, frontZ);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotation(physics.rotation));
                        drawBox(matrices, consumer, light, -width, -height * 0.5F, -depth, width, height * 0.5F, 0.0F, 20, 20, 28, 26);
                        matrices.pop();
                     } else {
                        this.renderSide(matrices, consumer, light, -1, shiftX, centerY, frontZ, width, height, depth, separation, physics);
                        this.renderSide(matrices, consumer, light, 1, shiftX, centerY, frontZ, width, height, depth, separation, physics);
                     }

                     matrices.pop();
                     immediate.draw();
                  }
               }
            }
         }
      }
   }

   private void renderSide(
      MatrixStack matrices,
      VertexConsumer consumer,
      int light,
      int side,
      float shiftX,
      float centerY,
      float frontZ,
      float width,
      float height,
      float depth,
      float separation,
      BoobsFeatureRenderer.PhysicsState physics
   ) {
      float centerX = shiftX + side * (width * 0.5F + separation * 0.5F);
      matrices.push();
      matrices.translate(centerX, centerY + physics.renderOffsetY * (side < 0 ? 0.08F : -0.08F), frontZ);
      matrices.multiply(RotationAxis.POSITIVE_X.rotation(physics.rotation));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotation(side * -0.055F));
      int u0 = side < 0 ? 20 : 24;
      int u1 = side < 0 ? 24 : 28;
      drawBox(matrices, consumer, light, -width * 0.5F, -height * 0.5F, -depth, width * 0.5F, height * 0.5F, 0.0F, u0, 20, u1, 26);
      matrices.pop();
   }

   private static void drawBox(
      MatrixStack matrices,
      VertexConsumer consumer,
      int light,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      int pixelU0,
      int pixelV0,
      int pixelU1,
      int pixelV1
   ) {
      float u0 = pixelU0 / 64.0F;
      float v0 = pixelV0 / 64.0F;
      float u1 = pixelU1 / 64.0F;
      float v1 = pixelV1 / 64.0F;
      Entry entry = matrices.peek();
      Matrix4f matrix = entry.getPositionMatrix();
      quad(consumer, entry, matrix, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, u0, v0, u1, v1, 0.0F, 0.0F, -1.0F, light);
      quad(consumer, entry, matrix, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, u0, v0, u1, v1, 0.0F, 0.0F, 1.0F, light);
      quad(consumer, entry, matrix, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, u0, v0, u1, v1, -1.0F, 0.0F, 0.0F, light);
      quad(consumer, entry, matrix, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, u0, v0, u1, v1, 1.0F, 0.0F, 0.0F, light);
      quad(consumer, entry, matrix, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, u0, v0, u1, v1, 0.0F, -1.0F, 0.0F, light);
      quad(consumer, entry, matrix, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, u0, v0, u1, v1, 0.0F, 1.0F, 0.0F, light);
   }

   private static void quad(
      VertexConsumer consumer,
      Entry entry,
      Matrix4f matrix,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz,
      float u0,
      float v0,
      float u1,
      float v1,
      float nx,
      float ny,
      float nz,
      int light
   ) {
      vertex(consumer, entry, matrix, ax, ay, az, u0, v0, nx, ny, nz, light);
      vertex(consumer, entry, matrix, bx, by, bz, u1, v0, nx, ny, nz, light);
      vertex(consumer, entry, matrix, cx, cy, cz, u1, v1, nx, ny, nz, light);
      vertex(consumer, entry, matrix, dx, dy, dz, u0, v1, nx, ny, nz, light);
   }

   private static void vertex(
      VertexConsumer consumer, Entry entry, Matrix4f matrix, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light
   ) {
      consumer.vertex(matrix, x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, nx, ny, nz);
   }

   private static PlayerEntity getPlayer(MinecraftClient mc, int id) {
      if (mc.world == null) {
         return null;
      } else {
         return mc.world.getEntityById(id) instanceof PlayerEntity player ? player : null;
      }
   }

   private static final class PhysicsState {
      private double lastY = Double.NaN;
      private long lastTime;
      private float offsetY;
      private float renderOffsetY;
      private float velocity;
      private float rotation;

      private void update(PlayerEntity player, PlayerEntityRenderState state, Boobs boobs) {
         long now = System.nanoTime();
         float dt = this.lastTime == 0L ? 0.016666668F : Math.min(0.05F, (float)(now - this.lastTime) / 1.0E9F);
         this.lastTime = now;
         float target = 0.0F;
         if (boobs.physics.isValue()) {
            double y = player == null ? Double.NaN : player.getY();
            double verticalSpeed = !Double.isNaN(y) && !Double.isNaN(this.lastY) ? (y - this.lastY) / Math.max(dt, 0.001F) : 0.0;
            this.lastY = y;
            float walk = MathHelper.sin(state.limbSwingAnimationProgress * 0.75F) * state.limbSwingAmplitude * 0.035F;
            target = (float)MathHelper.clamp(-verticalSpeed * 0.018 + walk, -0.09, 0.09) * boobs.bounce.getValue();
            float stiffness = MathHelper.lerp(boobs.floppy.getValue(), 24.0F, 8.0F);
            this.velocity = this.velocity + (target - this.offsetY) * stiffness * dt;
            this.velocity = this.velocity * (float)Math.pow(0.12F + boobs.floppy.getValue() * 0.72F, dt * 10.0F);
            this.offsetY = this.offsetY + this.velocity * dt * 8.0F;
         } else {
            this.offsetY = MathHelper.lerp(Math.min(1.0F, dt * 12.0F), this.offsetY, 0.0F);
            this.velocity = 0.0F;
         }

         float breath = boobs.breathing.isValue() ? MathHelper.sin(state.age * 0.12F) * 0.012F : 0.0F;
         this.renderOffsetY = MathHelper.clamp(this.offsetY + breath, -0.12F, 0.12F);
         this.rotation = MathHelper.clamp(-this.renderOffsetY * 2.8F, -0.24F, 0.24F);
      }
   }
}
