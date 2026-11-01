package ruhack.phobia.mixin;

import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.modulesystem.impl.render.Chams;
import ruhack.phobia.system.modulesystem.impl.render.ShaderHands;
import ruhack.phobia.util.render.ClientPipelines;

@Mixin(OrderedRenderCommandQueueImpl.class)
public class MixinOrderedRenderCommandQueue {
   @Inject(method = "submitItem", at = @At("HEAD"), cancellable = true)
   private void onSubmitItem(
      MatrixStack matrices,
      ItemDisplayContext displayContext,
      int light,
      int overlay,
      int outlineColor,
      int[] tints,
      List<BakedQuad> quads,
      RenderLayer layer,
      Glint glint,
      CallbackInfo ci
   ) {
      ShaderHands shaderHands = ShaderHands.getInstance();
      if (shaderHands != null && shaderHands.isState() && !quads.isEmpty()) {
         boolean thirdPersonSelf = Chams.renderingSelf
            && (displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
         if (ShaderHands.firstPersonItemContext || displayContext.isFirstPerson() || thirdPersonSelf) {
            Sprite sprite = quads.get(0).sprite();
            if (sprite != null) {
               Identifier atlas = sprite.getAtlasId();
               OrderedRenderCommandQueueImpl self = (OrderedRenderCommandQueueImpl)(Object)this;
               if (shaderHands.isGlowMode()) {
                  boolean trailPass = shaderHands.isTrailEnabled() && (ShaderHands.firstPersonItemContext || displayContext.isFirstPerson());
                  RenderLayer haloLayer = ClientPipelines.SHADER_HANDS_HALO.apply(atlas);
                  shaderHands.forEachItemHaloOutlinePass(
                     quads.size(),
                     trailPass,
                     (haloLight, haloOverlay) -> self.getBatchingQueue(1)
                        .submitItem(matrices, displayContext, haloLight, haloOverlay, 0, tints, quads, haloLayer, Glint.NONE)
                  );
                  RenderLayer trailHaloLayer = trailPass ? ClientPipelines.SHADER_HANDS_TRAIL_HALO.apply(atlas) : null;
                  if (trailPass) {
                     shaderHands.forEachItemHaloOutlinePass(
                        quads.size(),
                        true,
                        (haloLight, haloOverlay) -> self.getBatchingQueue(1)
                           .submitItem(matrices, displayContext, haloLight, haloOverlay, 0, tints, quads, trailHaloLayer, Glint.NONE)
                     );
                  }

                  if (trailPass && shaderHands.isTrailModelEnabled()) {
                     self.getBatchingQueue(1)
                        .submitItem(
                           matrices,
                           displayContext,
                           shaderHands.packTrailModelColor(),
                           shaderHands.packTrailModelOverlay(),
                           0,
                           tints,
                           quads,
                           trailHaloLayer,
                           Glint.NONE
                        );
                  }

                  if (shaderHands.fillFraction() > 0.01F) {
                     self.getBatchingQueue(1)
                        .submitItem(
                           matrices,
                           displayContext,
                           light,
                           shaderHands.packBakedItemFill(),
                           0,
                           tints,
                           quads,
                           ClientPipelines.SHADER_HANDS_BAKED_ITEM_FILL.apply(atlas),
                           Glint.NONE
                        );
                     if (trailPass) {
                        self.getBatchingQueue(1)
                           .submitItem(
                              matrices,
                              displayContext,
                              light,
                              shaderHands.packBakedItemFill(),
                              0,
                              tints,
                              quads,
                              ClientPipelines.SHADER_HANDS_TRAIL_BAKED_ITEM_FILL.apply(atlas),
                              Glint.NONE
                           );
                     }
                  }
               } else {
                  ci.cancel();
                  self.getBatchingQueue(0)
                     .submitItem(
                        matrices,
                        displayContext,
                        shaderHands.packColor1(),
                        shaderHands.packColor2Speed(),
                        0,
                        tints,
                        quads,
                        ClientPipelines.SHADER_HANDS_GRADIENT.apply(atlas),
                        glint
                     );
               }
            }
         }
      }
   }

   @Inject(method = "submitModel", at = @At("HEAD"), cancellable = true)
   private void chamsModel(
      Model model,
      Object state,
      MatrixStack matrices,
      RenderLayer layer,
      int light,
      int overlay,
      int color,
      Sprite sprite,
      int outlineColor,
      CrumblingOverlayCommand crumbling,
      CallbackInfo ci
   ) {
      ShaderHands shaderHands = ShaderHands.getInstance();
      if (ShaderHands.firstPersonItemContext && shaderHands != null && shaderHands.isState()) {
         Identifier texture = ClientPipelines.layerTextureOf(layer);
         if (texture != null) {
            OrderedRenderCommandQueueImpl self = (OrderedRenderCommandQueueImpl)(Object)this;
            if (shaderHands.isGlowMode()) {
               boolean trailPass = shaderHands.isTrailEnabled();
               RenderLayer haloLayer = ClientPipelines.SHADER_HANDS_HALO.apply(texture);
               shaderHands.forEachHaloOutlinePass(
                  (haloLight, haloOverlay) -> self.getBatchingQueue(1)
                     .submitModel(model, state, matrices, haloLayer, haloLight, haloOverlay, color, sprite, 0, crumbling)
               );
               RenderLayer trailHaloLayer = trailPass ? ClientPipelines.SHADER_HANDS_TRAIL_HALO.apply(texture) : null;
               if (trailPass) {
                  shaderHands.forEachHaloOutlinePass(
                     (haloLight, haloOverlay) -> self.getBatchingQueue(1)
                        .submitModel(model, state, matrices, trailHaloLayer, haloLight, haloOverlay, color, sprite, 0, crumbling)
                  );
               }

               if (trailPass && shaderHands.isTrailModelEnabled()) {
                  self.getBatchingQueue(1)
                     .submitModel(
                        model,
                        state,
                        matrices,
                        trailHaloLayer,
                        shaderHands.packTrailModelColor(),
                        shaderHands.packTrailModelOverlay(),
                        color,
                        sprite,
                        0,
                        crumbling
                     );
               }

               if (shaderHands.fillFraction() > 0.01F) {
                  self.getBatchingQueue(1)
                     .submitModel(
                        model,
                        state,
                        matrices,
                        ClientPipelines.SHADER_HANDS_ITEMGLOW.apply(texture),
                        shaderHands.packItemFillColor(),
                        shaderHands.packColor2Speed(),
                        color,
                        sprite,
                        0,
                        crumbling
                     );
                  if (trailPass) {
                     self.getBatchingQueue(1)
                        .submitModel(
                           model,
                           state,
                           matrices,
                           ClientPipelines.SHADER_HANDS_TRAIL_ITEMGLOW.apply(texture),
                           shaderHands.packItemFillColor(),
                           shaderHands.packColor2Speed(),
                           color,
                           sprite,
                           0,
                           crumbling
                        );
                  }
               }
            } else {
               ci.cancel();
               self.getBatchingQueue(0)
                  .submitModel(
                     model,
                     state,
                     matrices,
                     ClientPipelines.SHADER_HANDS_GRADIENT.apply(texture),
                     shaderHands.packColor1(),
                     shaderHands.packColor2Speed(),
                     color,
                     sprite,
                     0,
                     crumbling
                  );
            }

            return;
         }
      }

      if (Chams.renderContext) {
         Identifier texture = ClientPipelines.layerTextureOf(layer);
         if (texture != null) {
            ci.cancel();
            OrderedRenderCommandQueueImpl self = (OrderedRenderCommandQueueImpl)(Object)this;
            self.getBatchingQueue(0)
               .submitModel(
                  model, state, matrices, ClientPipelines.CHAMS.apply(texture), Chams.visibleLight, Chams.visibleOverlay, -1, sprite, outlineColor, crumbling
               );
            if (Chams.wallContext) {
               self.getBatchingQueue(1)
                  .submitModel(
                     model, state, matrices, ClientPipelines.CHAMS_WALL.apply(texture), Chams.wallLight, Chams.wallOverlay, -1, sprite, outlineColor, crumbling
                  );
            }
         }
      }
   }

   @Inject(method = "submitModelPart", at = @At("HEAD"), cancellable = true)
   private void chamsModelPart(
      ModelPart part,
      MatrixStack matrices,
      RenderLayer layer,
      int light,
      int overlay,
      Sprite sprite,
      boolean sheeted,
      boolean hasGlint,
      int color,
      CrumblingOverlayCommand crumbling,
      int outlineColor,
      CallbackInfo ci
   ) {
      ShaderHands shaderHands = ShaderHands.getInstance();
      if (ShaderHands.firstPersonArmContext && shaderHands != null && shaderHands.isState()) {
         Identifier texture = ClientPipelines.layerTextureOf(layer);
         if (texture != null) {
            OrderedRenderCommandQueueImpl self = (OrderedRenderCommandQueueImpl)(Object)this;
            if (shaderHands.isGlowMode()) {
               boolean trailPass = shaderHands.isTrailEnabled();
               RenderLayer haloLayer = ClientPipelines.SHADER_HANDS_HALO.apply(texture);
               shaderHands.forEachHaloPass(
                  (haloLight, haloOverlay) -> self.getBatchingQueue(1)
                     .submitModelPart(part, matrices, haloLayer, haloLight, haloOverlay, sprite, false, false, -1, null, 0)
               );
               RenderLayer trailHaloLayer = trailPass ? ClientPipelines.SHADER_HANDS_TRAIL_HALO.apply(texture) : null;
               if (trailPass) {
                  shaderHands.forEachHaloPass(
                     (haloLight, haloOverlay) -> self.getBatchingQueue(1)
                        .submitModelPart(part, matrices, trailHaloLayer, haloLight, haloOverlay, sprite, false, false, -1, null, 0)
                  );
               }

               if (shaderHands.isTrailModelEnabled()) {
                  self.getBatchingQueue(1)
                     .submitModelPart(
                        part,
                        matrices,
                        trailHaloLayer,
                        shaderHands.packTrailModelColor(),
                        shaderHands.packTrailModelOverlay(),
                        sprite,
                        false,
                        false,
                        -1,
                        null,
                        0
                     );
               }
            } else {
               ci.cancel();
               self.getBatchingQueue(0)
                  .submitModelPart(
                     part,
                     matrices,
                     ClientPipelines.SHADER_HANDS_GRADIENT.apply(texture),
                     shaderHands.packColor1(),
                     shaderHands.packColor2Speed(),
                     sprite,
                     false,
                     false,
                     -1,
                     null,
                     0
                  );
            }

            return;
         }
      }

      if (Chams.renderContext) {
         Identifier texture = ClientPipelines.layerTextureOf(layer);
         if (texture != null) {
            ci.cancel();
            OrderedRenderCommandQueueImpl self = (OrderedRenderCommandQueueImpl)(Object)this;
            self.getBatchingQueue(0)
               .submitModelPart(
                  part, matrices, ClientPipelines.CHAMS.apply(texture), Chams.visibleLight, Chams.visibleOverlay, sprite, false, false, -1, null, 0
               );
            if (Chams.wallContext) {
               self.getBatchingQueue(1)
                  .submitModelPart(
                     part, matrices, ClientPipelines.CHAMS_WALL.apply(texture), Chams.wallLight, Chams.wallOverlay, sprite, false, false, -1, null, 0
                  );
            }
         }
      }
   }
}
