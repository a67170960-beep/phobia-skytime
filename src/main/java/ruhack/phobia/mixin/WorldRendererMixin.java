package ruhack.phobia.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.modulesystem.impl.render.CustomModels;
import ruhack.phobia.system.modulesystem.impl.render.NoRender;
import ruhack.phobia.system.modulesystem.impl.render.ShaderESP;
import ruhack.phobia.system.modulesystem.impl.render.ShaderSky;
import ruhack.phobia.system.rendersystem.render3d.sky.ShaderSky3D;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin implements IMinecraft {
   @Inject(method = "fillEntityRenderStates", at = @At("TAIL"))
   private void phobia$enableCustomModelOutlinePostProcess(
      Camera camera, Frustum frustum, RenderTickCounter tickCounter, WorldRenderState state, CallbackInfo ci
   ) {
      ShaderESP shaderESP = ShaderESP.getInstance();
      if (shaderESP != null && shaderESP.isState() && mc.player != null && CustomModels.appliesTo(mc.player) && shaderESP.shouldOutline(mc.player)) {
         state.hasOutline = true;
      }
   }

   @Inject(method = "hasBlindnessOrDarkness", at = @At("HEAD"), cancellable = true)
   private void onHasBlindnessOrDarkness(Camera camera, CallbackInfoReturnable<Boolean> cir) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState()) {
         if (camera.getFocusedEntity() instanceof LivingEntity livingEntity) {
            boolean hasBlindness = livingEntity.hasStatusEffect(StatusEffects.BLINDNESS);
            boolean hasDarkness = livingEntity.hasStatusEffect(StatusEffects.DARKNESS);
            if (noRender.modeSetting.isSelected("Bad Effects") && hasBlindness && !hasDarkness) {
               cir.setReturnValue(false);
            }

            if (noRender.modeSetting.isSelected("Darkness") && hasDarkness && !hasBlindness) {
               cir.setReturnValue(false);
            }

            if (noRender.modeSetting.isSelected("Bad Effects") && noRender.modeSetting.isSelected("Darkness")) {
               cir.setReturnValue(false);
            }
         }
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void onWorldRenderStart(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f frustumProjection,
      Matrix4f projectionMatrix,
      GpuBufferSlice fog,
      Vector4f fogColor,
      boolean shouldRenderSky,
      CallbackInfo ci
   ) {
      ShaderSky3D.setMatrices(projectionMatrix, positionMatrix);
   }

   @Inject(method = "method_62215", at = @At("HEAD"), cancellable = true)
   private static void onRenderSkyPass(GpuBufferSlice fog, SkyRenderState skyState, SkyRendering skyRendering, CallbackInfo ci) {
      ShaderSky shaderSky = ShaderSky.getInstance();
      if (shaderSky != null && shaderSky.isState()) {
         RenderSystem.setShaderFog(fog);
         ShaderSky3D.render(shaderSky);
         ci.cancel();
      }
   }
}
