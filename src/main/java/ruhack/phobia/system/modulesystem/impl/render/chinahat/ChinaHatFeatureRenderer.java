package ruhack.phobia.system.modulesystem.impl.render.chinahat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import ruhack.phobia.system.modulesystem.impl.render.ChinaHat;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.render.ClientPipelines;

public class ChinaHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final float PI2 = (float) (Math.PI * 2);
   private static final int CIRCLE_SEGMENTS = 720;
   private static final int OUTLINE_SEGMENTS = 360;
   private static final float[] CIRCLE_X = buildCircleCoordinates(720, 0.62F, true);
   private static final float[] CIRCLE_Z = buildCircleCoordinates(720, 0.62F, false);
   private static final float[] OUTLINE_X = buildCircleCoordinates(360, 0.55F, true);
   private static final float[] OUTLINE_Z = buildCircleCoordinates(360, 0.55F, false);

   public ChinaHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
      super(context);
   }

   public void render(MatrixStack matrixStack, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
      MinecraftClient mc = MinecraftClient.getInstance();
      ChinaHat chinaHat = ChinaHat.getInstance();
      if (chinaHat != null && chinaHat.isState()) {
         if (mc.player != null) {
            if (!mc.options.getPerspective().isFirstPerson()) {
               if (this.isLocalPlayer(state, mc)) {
                  matrixStack.push();
                  ((PlayerEntityModel)this.getContextModel()).head.applyTransform(matrixStack);
                  matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
                  matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                  float yOffset = 0.42F;
                  if (!mc.player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                     yOffset += 0.09F;
                  }

                  matrixStack.translate(0.0F, yOffset, 0.0F);
                  Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
                  int color1 = chinaHat.color1.getColor();
                  int color2 = chinaHat.color2.getColor();
                  float timeOffset = (float)System.currentTimeMillis() / 200.0F;
                  this.renderFlatHat(matrixStack, immediate, color1, color2, timeOffset);
                  this.renderOutline(matrixStack, immediate, color1, color2, timeOffset);
                  immediate.draw();
                  matrixStack.pop();
               }
            }
         }
      }
   }

   private boolean isLocalPlayer(PlayerEntityRenderState state, MinecraftClient mc) {
      try {
         if (state.id == mc.player.getId()) {
            return true;
         }
      } catch (Exception var5) {
      }

      try {
         if (state.playerName != null && mc.player.getName() != null) {
            return state.playerName.getString().equals(mc.player.getName().getString());
         }
      } catch (Exception var4) {
      }

      return false;
   }

   private void renderFlatHat(MatrixStack stack, VertexConsumerProvider provider, int color1, int color2, float timeOffset) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.CHINA_HAT);
      Matrix4f matrix = stack.peek().getPositionMatrix();
      float coneHeight = 0.3F;
      int alpha = 255;
      int centerColor = this.getGradientColor(0, 720, color1, color2, timeOffset);
      centerColor = ColorUtil.replAlpha(centerColor, alpha);
      consumer.vertex(matrix, 0.0F, coneHeight, 0.0F).color(centerColor);

      for (int i = 0; i <= 720; i++) {
         int color = this.getGradientColor(i, 720, color1, color2, timeOffset);
         color = ColorUtil.replAlpha(color, alpha);
         consumer.vertex(matrix, CIRCLE_X[i], 0.0F, CIRCLE_Z[i]).color(color);
      }

      for (int i = 720; i >= 0; i--) {
         int color = this.getGradientColor(i, 720, color1, color2, timeOffset);
         color = ColorUtil.replAlpha(color, alpha);
         consumer.vertex(matrix, CIRCLE_X[i], 0.0F, CIRCLE_Z[i]).color(color);
      }

      consumer.vertex(matrix, 0.0F, coneHeight, 0.0F).color(centerColor);
   }

   private void renderOutline(MatrixStack stack, VertexConsumerProvider provider, int color1, int color2, float timeOffset) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.CHINA_HAT_OUTLINE);
      Matrix4f matrix = stack.peek().getPositionMatrix();
      int outlineAlpha = 255;

      for (int i = 0; i <= 360; i++) {
         int color = this.getGradientColor(i * 2, 720, color1, color2, timeOffset);
         color = ColorUtil.replAlpha(color, outlineAlpha);
         consumer.vertex(matrix, OUTLINE_X[i], 0.0F, OUTLINE_Z[i]).color(color);
      }
   }

   private int getGradientColor(int index, int size, int color1, int color2, float timeOffset) {
      int adjustedIndex = (int)((index + timeOffset) % size);
      float progress = (float)adjustedIndex / size;
      return progress < 0.5F
         ? ColorUtil.interpolateColor(color1, color2, progress * 2.0F)
         : ColorUtil.interpolateColor(color2, color1, (progress - 0.5F) * 2.0F);
   }

   private static float[] buildCircleCoordinates(int segments, float width, boolean xAxis) {
      float[] coordinates = new float[segments + 1];

      for (int i = 0; i <= segments; i++) {
         float angle = i * (float) (Math.PI * 2) / segments;
         coordinates[i] = xAxis ? -MathHelper.sin(angle) * width : MathHelper.cos(angle) * width;
      }

      return coordinates;
   }
}
