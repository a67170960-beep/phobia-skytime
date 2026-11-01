package ruhack.phobia.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruhack.phobia.system.modulesystem.impl.render.Tags;
import ruhack.phobia.util.aim.neuro.NeuroController;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
   @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
   private void phobia$forceTag(T entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
      if (NeuroController.get().isRecordingTarget(entity)) {
         cir.setReturnValue(true);
      } else {
         Tags tags = Tags.getInstance();
         if (tags != null && tags.isState() && tags.shouldTag(entity)) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "updateRenderState", at = @At("TAIL"))
   private void phobia$updateTag(T entity, S state, float tickDelta, CallbackInfo ci) {
      if (NeuroController.get().isRecordingTarget(entity)) {
         state.displayName = entity.getDisplayName();
         state.nameLabelPos = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getLerpedYaw(tickDelta));
      }
   }
}
