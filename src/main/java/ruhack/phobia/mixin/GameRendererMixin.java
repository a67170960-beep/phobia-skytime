package ruhack.phobia.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.render.fog.FogRenderer.FogType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.dragsystem.Drag;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.DrawEvent;
import ruhack.phobia.system.eventsystem.impl.WorldRenderEvent;
import ruhack.phobia.system.modulesystem.impl.display.BindPreview;
import ruhack.phobia.system.modulesystem.impl.display.TargetHud;
import ruhack.phobia.system.modulesystem.impl.player.FreeCam;
import ruhack.phobia.system.modulesystem.impl.player.NoEntityTrace;
import ruhack.phobia.system.modulesystem.impl.render.Ambience;
import ruhack.phobia.system.modulesystem.impl.render.AspectRatio;
import ruhack.phobia.system.modulesystem.impl.render.NoRender;
import ruhack.phobia.system.modulesystem.impl.render.ShaderHands;
import ruhack.phobia.system.modulesystem.impl.render.ShulkerPreview;
import ruhack.phobia.system.modulesystem.impl.render.Tags;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.saturation.Saturation2D;
import ruhack.phobia.system.rendersystem.render3d.Render3D;
import ruhack.phobia.system.rendersystem.render3d.billboard.Billboard3D;
import ruhack.phobia.system.rendersystem.render3d.fill.Fill3D;
import ruhack.phobia.system.rendersystem.render3d.glow.Glow3D;
import ruhack.phobia.system.rendersystem.render3d.line.Line3D;
import ruhack.phobia.system.rendersystem.render3d.ring.Ring3D;
import ruhack.phobia.system.rendersystem.render3d.soul.Soul3D;
import ruhack.phobia.system.rendersystem.render3d.watercaustic.WaterCaustic3D;
import ruhack.phobia.system.screensystem.AutoSwapRingScreen;
import ruhack.phobia.system.screensystem.BuffRingScreen;
import ruhack.phobia.system.screensystem.MainMenuScreen;
import ruhack.phobia.system.screensystem.MainMenuUpdateScreen;
import ruhack.phobia.system.screensystem.TagPlayerMenuScreen;
import ruhack.phobia.system.screensystem.clickgui.ClickGui;
import ruhack.phobia.util.render.ShaderHandsTrail;
import ruhack.phobia.util.render.WorldToScreen;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.rotation.raycast.RaytracingUtil;
import ruhack.phobia.util.string.PlayerInteractionHelper;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   private GuiRenderer guiRenderer;
   @Shadow
   @Final
   private FogRenderer fogRenderer;

   @ModifyReturnValue(method = "getBasicProjectionMatrix", at = @At("RETURN"))
   private Matrix4f phobia$aspectRatio(Matrix4f matrix) {
      AspectRatio aspect = AspectRatio.getInstance();
      if (aspect != null && aspect.isState()) {
         matrix.m00(matrix.m11() / Math.max(0.1F, aspect.getRatio()));
      }

      return matrix;
   }

   @Inject(method = "renderWorld", at = @At("HEAD"))
   private void phobia$prepareShaderHandsFrame(RenderTickCounter tickCounter, CallbackInfo ci) {
      ShaderHands shaderHands = ShaderHands.getInstance();
      if (shaderHands != null && shaderHands.isState()) {
         shaderHands.refreshRenderCache();
      }
   }

   @Inject(
      method = "renderWorld",
      at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand")
   )
   private void hookWorldRender(
      RenderTickCounter tickCounter,
      CallbackInfo ci,
      @Local(ordinal = 0) float tickDelta,
      @Local(ordinal = 0) Matrix4f projection,
      @Local(ordinal = 1) Matrix4f view,
      @Local MatrixStack matrixStack
   ) {
      if (this.client.world != null && this.client.player != null) {
         Render3D.setMatrices(projection, view);
         WorldToScreen.update(projection, view, tickDelta);
         Line3D.setMatrices(projection, view);
         Fill3D.setMatrices(projection, view);
         Glow3D.setMatrices(projection, view);
         Billboard3D.setMatrices(projection, view);
         Soul3D.setMatrices(projection, view);
         Ring3D.setMatrices(projection, view);
         WaterCaustic3D.setMatrices(projection, view);
         Ambience ambience = Ambience.getInstance();
         if (ambience != null && ambience.isState()) {
            Saturation2D.applyWithCopy(ambience.getSaturationFactor());
         }

         EventManager.callEvent(new WorldRenderEvent(matrixStack, tickDelta));
         ShaderHands shaderHands = ShaderHands.getInstance();
         if (shaderHands != null && shaderHands.isTrailEnabled()) {
            ShaderHandsTrail.beginFrame();
         }
      }
   }

   @Inject(method = "renderWorld", at = @At("TAIL"))
   private void phobia$compositeShaderHandsTrail(RenderTickCounter tickCounter, CallbackInfo ci) {
      ShaderHands shaderHands = ShaderHands.getInstance();
      if (shaderHands != null && shaderHands.isTrailEnabled()) {
         ShaderHandsTrail.endFrame();
      }
   }

   @Inject(method = "updateCrosshairTarget", at = @At("HEAD"), cancellable = true)
   private void updateCrosshairTargetHook(float tickProgress, CallbackInfo ci) {
      NoEntityTrace noEntityTrace = NoEntityTrace.getInstance();
      if (noEntityTrace != null && noEntityTrace.shouldIgnoreEntityTrace()) {
         Entity entity = this.client.getCameraEntity();
         if (entity != null && this.client.world != null && this.client.player != null) {
            double range = Math.max(this.client.player.getBlockInteractionRange(), this.client.player.getEntityInteractionRange());
            this.client.crosshairTarget = entity.raycast(range, tickProgress, false);
            this.client.targetedEntity = null;
            ci.cancel();
         }
      }
   }

   @Inject(
      method = "updateCrosshairTarget",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getCrosshairTarget(FLnet/minecraft/entity/Entity;)Lnet/minecraft/util/hit/HitResult;"),
      cancellable = true
   )
   private void onUpdateTargetedEntity(float tickDelta, CallbackInfo ci) {
      if (!PlayerInteractionHelper.nullCheck()) {
         FreeCam freeCam = FreeCam.getInstance();
         if (freeCam.isState()) {
            Profilers.get().pop();
            this.client.crosshairTarget = RaytracingUtil.raycast(freeCam.pos, 4.5, AngleUtil.cameraAngle(), false);
            ci.cancel();
         }
      }
   }

   @Inject(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = Shift.AFTER)
   )
   private void onAfterGuiRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext drawContext) {
      if (!this.client.options.hudHidden) {
         boolean overlayScreen = this.isOverlayScreen();
         boolean mainMenu = this.isMainMenuScreen();
         if (!overlayScreen && !mainMenu) {
            EventManager.callEvent(new DrawEvent(drawContext, tickCounter.getDynamicDeltaTicks()));
         }

         if (this.client.currentScreen == null || this.client.currentScreen instanceof ChatScreen) {
            if (this.client.currentScreen == null) {
               Drag.onDraw(drawContext, 0, 0, tickCounter.getDynamicDeltaTicks(), false);
            }

            Render2D.beginBlurFrame();

            try {
               Render2D.renderOverrides(drawContext);
            } finally {
               Render2D.endBlurFrame();
            }
         }

         TargetHud targetHud = TargetHud.getInstance();
         BindPreview bindPreview = BindPreview.getInstance();
         ShulkerPreview shulkerPreview = ShulkerPreview.getInstance();
         Tags tags = Tags.getInstance();
         boolean tagsItemsQueued = !overlayScreen && !mainMenu && tags != null && tags.queueEquipmentModels(drawContext);
         boolean itemModelsQueued = !overlayScreen && !mainMenu && targetHud != null && targetHud.queueEquipmentModels(drawContext);
         if (tagsItemsQueued) {
            itemModelsQueued = true;
         }

         if (!overlayScreen && !mainMenu && bindPreview != null && bindPreview.queueItemModels(drawContext)) {
            itemModelsQueued = true;
         }

         if (!overlayScreen && !mainMenu && shulkerPreview != null && shulkerPreview.queueWorldPreviews(drawContext)) {
            itemModelsQueued = true;
         }

         if (itemModelsQueued) {
            this.guiRenderer.render(this.fogRenderer.getFogBuffer(FogType.NONE));
         }
      }
   }

   @Inject(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = Shift.BEFORE)
   )
   private void onBeforeGuiRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext drawContext) {
      boolean overlayScreen = this.isOverlayScreen();
      if (overlayScreen && !this.client.options.hudHidden) {
         EventManager.callEvent(new DrawEvent(drawContext, tickCounter.getDynamicDeltaTicks()));
         Drag.onDraw(drawContext, 0, 0, tickCounter.getDynamicDeltaTicks(), false);
         Render2D.beginBlurFrame();

         try {
            Render2D.renderOverrides(drawContext);
         } finally {
            Render2D.endBlurFrame();
         }

         Tags tags = Tags.getInstance();
         if (tags != null) {
            tags.queueEquipmentModels(drawContext);
         }
      } else if (this.isMainMenuScreen() && !this.client.options.hudHidden) {
         Render2D.beginBlurFrame();

         try {
            Render2D.renderOverrides(drawContext);
         } finally {
            Render2D.endBlurFrame();
         }
      }

      if (overlayScreen && !this.client.options.hudHidden) {
         TargetHud targetHud = TargetHud.getInstance();
         BindPreview bindPreview = BindPreview.getInstance();
         if (targetHud != null) {
            targetHud.queueEquipmentModels(drawContext);
         }

         if ((this.client.currentScreen instanceof GameMenuScreen || this.client.currentScreen instanceof ClickGui) && bindPreview != null) {
            bindPreview.queueItemModels(drawContext);
         }
      }
   }

   @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
   private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Damage")) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(method = "renderWorld", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
   private float onNauseaDistortion(float original) {
      NoRender noRender = NoRender.getInstance();
      return noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Nausea") ? 0.0F : original;
   }

   @Inject(method = "renderBlur", at = @At("HEAD"), cancellable = true)
   private void onRenderBlur(CallbackInfo ci) {
      if (this.client.currentScreen instanceof ClickGui
         || this.client.currentScreen instanceof TagPlayerMenuScreen
         || this.client.currentScreen instanceof AutoSwapRingScreen
         || this.client.currentScreen instanceof BuffRingScreen) {
         ci.cancel();
      }
   }

   private boolean isMainMenuScreen() {
      return this.client.currentScreen instanceof MainMenuScreen || this.client.currentScreen instanceof MainMenuUpdateScreen;
   }

   private boolean isOverlayScreen() {
      return this.client.currentScreen instanceof HandledScreen
         || this.client.currentScreen instanceof GameMenuScreen
         || this.client.currentScreen instanceof ClickGui
         || this.client.currentScreen instanceof TagPlayerMenuScreen
         || this.client.currentScreen instanceof AutoSwapRingScreen
         || this.client.currentScreen instanceof BuffRingScreen;
   }
}
