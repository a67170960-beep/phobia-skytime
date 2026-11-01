package ruhack.phobia.mixin;

import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ruhack.phobia.util.interfaces.IBuiltChunkAnimator;

@Mixin(BuiltChunk.class)
public class MixinBuiltChunk implements IBuiltChunkAnimator {
   @Unique
   private float animation = 100.0F;

   @Override
   public float getAnimation() {
      return this.animation;
   }

   @Override
   public void setAnimation(float value) {
      this.animation = value;
   }
}
