package ruhack.phobia.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.EntityColorEvent;
import ruhack.phobia.system.modulesystem.impl.render.Chams;
import ruhack.phobia.util.render.ShaderPacking;
import ruhack.phobia.util.rotation.RotationController;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> implements IMinecraft {
   @Shadow
   @Nullable
   protected abstract RenderLayer getRenderLayer(S var1, boolean var2, boolean var3, boolean var4);

   @ModifyExpressionValue(method = "updateRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
   private float lerpAngleDegreesHook(
      float original, @Local(ordinal = 0, argsOnly = true) LivingEntity entity, @Local(ordinal = 0, argsOnly = true) float delta
   ) {
      RotationController controller = RotationController.INSTANCE;
      if (entity.equals(mc.player) && controller.getPreviousRotation().getYaw() != mc.player.getYaw() && !(mc.currentScreen instanceof HandledScreen)) {
         float prevYaw = controller.getPreviousRotation().getYaw();
         float currentYaw = controller.getRotation().getYaw();
         float effectiveDelta = delta;
         return MathHelper.lerp(effectiveDelta, prevYaw, currentYaw);
      } else {
         return original;
      }
   }

   @ModifyExpressionValue(method = "updateRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
   private float getLerpedPitchHook(float original, @Local(ordinal = 0, argsOnly = true) LivingEntity entity, @Local(ordinal = 0, argsOnly = true) float delta) {
      RotationController controller = RotationController.INSTANCE;
      if (entity.equals(mc.player) && controller.getPreviousRotation().getPitch() != mc.player.getPitch() && !(mc.currentScreen instanceof HandledScreen)) {
         float prevPitch = controller.getPreviousRotation().getPitch();
         float currentPitch = controller.getRotation().getPitch();
         float effectiveDelta = delta;
         return MathHelper.lerp(effectiveDelta, prevPitch, currentPitch);
      } else {
         return original;
      }
   }

   @Redirect(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;")
   )
   private RenderLayer renderLayerHook(
      LivingEntityRenderer<?, ?, ?> instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline
   ) {
      if (!translucent && state.width == 0.6F) {
         EntityColorEvent event = new EntityColorEvent(-1);
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            translucent = true;
         }
      }

      return this.getRenderLayer((S)state, showBody, translucent, showOutline);
   }

   @ModifyArg(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
      ),
      index = 6
   )
   private int modifyColor(int color, @Local(argsOnly = true) S renderState) {
      if (renderState.invisibleToPlayer) {
         EntityColorEvent event = new EntityColorEvent(color);
         EventManager.callEvent(event);
         return event.getColor();
      } else {
         return color;
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void chamsContextStart(
      LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci
   ) {
      Chams.renderingSelf = state instanceof PlayerEntityRenderState playerState && mc.player != null && playerState.id == mc.player.getId();
      Chams chams = Chams.getInstance();
      Chams.renderContext = chams != null && chams.isState() && chams.shouldApply(state);
      Chams.wallContext = Chams.renderContext && chams.throughWalls.isValue();
      if (Chams.renderContext) {
         Chams.visibleLight = ShaderPacking.packColorAlpha(chams.getPrimaryColor());
         Chams.visibleOverlay = ShaderPacking.packColorSpeedFill(chams.getSecondaryColor(), chams.speed.getValue(), chams.fillFraction());
         Chams.wallLight = ShaderPacking.packColorAlpha(chams.getPrimaryColor(), 0.45F);
         Chams.wallOverlay = Chams.visibleOverlay;
      }
   }

   @Inject(method = "render", at = @At("RETURN"))
   private void chamsContextEnd(
      LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci
   ) {
      Chams.renderContext = false;
      Chams.wallContext = false;
      Chams.renderingSelf = false;
   }
}
