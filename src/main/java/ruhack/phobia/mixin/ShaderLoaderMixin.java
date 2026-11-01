package ruhack.phobia.mixin;

import com.mojang.blaze3d.shaders.ShaderType;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderLoader.class)
public abstract class ShaderLoaderMixin {
   @Inject(method = "getSource", at = @At("HEAD"), cancellable = true)
   private void onGetSource(Identifier id, ShaderType type, CallbackInfoReturnable<String> cir) {
      if ("phobia".equals(id.getNamespace())) {
         String ext = type == ShaderType.VERTEX ? ".vsh" : ".fsh";
         String path = "/assets/phobia/shaders/" + id.getPath() + ext;
         try (InputStream is = ShaderLoaderMixin.class.getResourceAsStream(path)) {
            if (is != null) {
               cir.setReturnValue(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
         } catch (Throwable ignored) {
         }
      }
   }
}
