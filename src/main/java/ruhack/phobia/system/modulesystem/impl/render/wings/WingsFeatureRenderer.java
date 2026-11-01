package ruhack.phobia.system.modulesystem.impl.render.wings;

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
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.impl.render.Wings;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.render.ClientPipelines;

public final class WingsFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final float[][] SHAPE = new float[][]{
      {0.05F, 0.05F}, {0.48F, -0.18F}, {0.88F, -0.05F}, {0.68F, 0.3F}, {1.02F, 0.48F}, {0.58F, 0.62F}, {0.72F, 0.92F}, {0.25F, 0.72F}, {0.05F, 0.35F}
   };

   public WingsFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
      super(context);
   }

   public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Wings wings = Wings.getInstance();
      if (wings != null && wings.isState() && mc.player != null && this.shouldRender(state, wings, mc)) {
         if (state.id != mc.player.getId() || !mc.options.getPerspective().isFirstPerson()) {
            matrices.push();
            ((PlayerEntityModel)this.getContextModel()).body.applyTransform(matrices);
            matrices.translate(0.0, 0.18, 0.18);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(8.0F));
            float flap = wings.animation.isValue() ? 8.0F + (float)Math.sin(System.currentTimeMillis() / 280.0) * 12.0F : 10.0F;
            Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
            this.renderWing(matrices, provider, -1.0F, -flap);
            this.renderWing(matrices, provider, 1.0F, flap);
            provider.draw();
            matrices.pop();
         }
      }
   }

   private boolean shouldRender(PlayerEntityRenderState state, Wings wings, MinecraftClient mc) {
      if (state.id == mc.player.getId()) {
         return wings.targets.isSelected("Себя");
      }

      String name = state.playerName == null ? "" : state.playerName.getString();
      return FriendUtils.isFriend(name) ? wings.targets.isSelected("Друзья") : wings.targets.isSelected("Игроки");
   }

   private void renderWing(MatrixStack matrices, VertexConsumerProvider provider, float side, float rotation) {
      matrices.push();
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      VertexConsumer fill = provider.getBuffer(ClientPipelines.CHINA_HAT);
      int center = ColorUtil.getClientColorAt(side < 0.0F ? 0.0F : 0.5F);
      fill.vertex(matrix, 0.0F, 0.0F, 0.0F).color(ColorUtil.replAlpha(center, 190));

      for (float[] point : SHAPE) {
         int color = ColorUtil.getClientColorAt(point[1] + (side < 0.0F ? 0.0F : 0.5F));
         fill.vertex(matrix, side * point[0], -point[1], 0.0F).color(ColorUtil.replAlpha(color, 190));
      }

      fill.vertex(matrix, side * SHAPE[0][0], -SHAPE[0][1], 0.0F).color(ColorUtil.replAlpha(center, 190));
      VertexConsumer outline = provider.getBuffer(ClientPipelines.CHINA_HAT_OUTLINE);

      for (float[] point : SHAPE) {
         outline.vertex(matrix, side * point[0], -point[1], 0.0F).color(ColorUtil.getClientColorAt(point[1]));
      }

      outline.vertex(matrix, side * SHAPE[0][0], -SHAPE[0][1], 0.0F).color(ColorUtil.getClientColorAt(0.0F));
      matrices.pop();
   }
}
