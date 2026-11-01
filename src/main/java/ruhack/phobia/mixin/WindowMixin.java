package ruhack.phobia.mixin;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourcePack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
   private static final String[] ICON_PATHS = new String[]{
      "/assets/phobia/icons/icon_16x16.png",
      "/assets/phobia/icons/icon_32x32.png",
      "/assets/phobia/icons/icon_48x48.png",
      "/assets/phobia/icons/icon_64x64.png",
      "/assets/phobia/icons/icon_128x128.png",
      "/assets/phobia/icons/icon_256x256.png"
   };
   private static final String FALLBACK_ICON = "/assets/phobia/tray_icon.png";

   @Shadow
   @Final
   private long handle;

   @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true)
   private void onSetIcon(ResourcePack resourcePack, Icons icons, CallbackInfo ci) {
      ci.cancel();

      List<NativeImage> images = new ArrayList<>();
      for (String path : ICON_PATHS) {
         try (InputStream stream = WindowMixin.class.getResourceAsStream(path)) {
            if (stream != null) {
               images.add(NativeImage.read(stream));
            }
         } catch (Throwable ignored) {
         }
      }

      if (images.isEmpty()) {
         try (InputStream stream = WindowMixin.class.getResourceAsStream(FALLBACK_ICON)) {
            if (stream != null) {
               images.add(NativeImage.read(stream));
            }
         } catch (Throwable ignored) {
         }
      }

      if (images.isEmpty()) {
         return;
      }

      List<ByteBuffer> byteBuffers = new ArrayList<>(images.size());
      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         Buffer buffer = GLFWImage.malloc(images.size(), memoryStack);
         for (int i = 0; i < images.size(); i++) {
            NativeImage image = images.get(i);
            ByteBuffer byteBuffer = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);
            byteBuffers.add(byteBuffer);
            byteBuffer.asIntBuffer().put(image.copyPixelsAbgr());
            buffer.position(i);
            buffer.width(image.getWidth());
            buffer.height(image.getHeight());
            buffer.pixels(byteBuffer);
         }
         buffer.position(0);
         GLFW.glfwSetWindowIcon(this.handle, buffer);
      } catch (Throwable ignored) {
      } finally {
         for (ByteBuffer b : byteBuffers) {
            MemoryUtil.memFree(b);
         }
         for (NativeImage img : images) {
            img.close();
         }
      }
   }
}
