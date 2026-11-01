package ruhack.phobia.system.rendersystem.render2d.gif;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ruhack.phobia.system.rendersystem.render2d.texture.Texture2D;

public class Gif2D {
   private static final Map<String, Gif2D.GifAnimation> CACHE = new HashMap<>();

   public static void init() {
   }

   public static void draw(Matrix4f matrix, float x, float y, float size, String path, int color, float radius, float z) {
      Gif2D.GifAnimation anim = CACHE.computeIfAbsent(path, Gif2D.GifAnimation::new);
      if (!anim.views.isEmpty()) {
         GpuTextureView frameView = anim.getCurrentFrame();
         if (frameView != null) {
            Texture2D.draw(matrix, x, y, size, frameView, color, radius, z);
         }
      }
   }

   public static void shutdown() {
      CACHE.values().forEach(Gif2D.GifAnimation::dispose);
      CACHE.clear();
   }

   private static Gif2D.FrameMeta readMeta(ImageReader reader, int index) {
      int delay = 100;
      int disposal = 0;
      int x = 0;
      int y = 0;

      try {
         IIOMetadata metadata = reader.getImageMetadata(index);
         IIOMetadataNode root = (IIOMetadataNode)metadata.getAsTree(metadata.getNativeMetadataFormatName());
         IIOMetadataNode gce = node(root, "GraphicControlExtension");
         if (gce != null) {
            delay = Math.max(20, parseInt(gce.getAttribute("delayTime"), 10) * 10);
            disposal = parseInt(gce.getAttribute("disposalMethod"), 0);
         }

         IIOMetadataNode desc = node(root, "ImageDescriptor");
         if (desc != null) {
            x = parseInt(desc.getAttribute("imageLeftPosition"), 0);
            y = parseInt(desc.getAttribute("imageTopPosition"), 0);
         }
      } catch (Exception var10) {
      }

      return new Gif2D.FrameMeta(delay, disposal, x, y);
   }

   private static IIOMetadataNode node(IIOMetadataNode root, String name) {
      for (int i = 0; i < root.getLength(); i++) {
         if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
            return (IIOMetadataNode)root.item(i);
         }
      }

      return null;
   }

   private static int parseInt(String value, int fallback) {
      if (value != null && !value.isEmpty()) {
         try {
            return Integer.parseInt(value);
         } catch (NumberFormatException e) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   private static void clear(Graphics2D g, int w, int h) {
      g.setComposite(AlphaComposite.Clear);
      g.fillRect(0, 0, w, h);
      g.setComposite(AlphaComposite.SrcOver);
   }

   private static NativeImage toNative(BufferedImage image, String path) {
      NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);

      for (int y = 0; y < image.getHeight(); y++) {
         for (int x = 0; x < image.getWidth(); x++) {
            int argb = image.getRGB(x, y);
            int a = argb >> 24 & 0xFF;
            int r = argb >> 16 & 0xFF;
            int g = argb >> 8 & 0xFF;
            int b = argb & 0xFF;
            if (a > 0 && keyBg(path, r, g, b)) {
               a = 0;
            }

            nativeImage.setColor(x, y, a << 24 | b << 16 | g << 8 | r);
         }
      }

      return nativeImage;
   }

   private static boolean keyBg(String path, int r, int g, int b) {
      if (path.contains("xz")) {
         return r > 252 && g > 252 && b > 252;
      }

      if (!path.contains("update")) {
         return false;
      }

      int max = Math.max(r, Math.max(g, b));
      int min = Math.min(r, Math.min(g, b));
      return max >= 228 && max - min < 28;
   }

   private record FrameMeta(int delay, int disposal, int x, int y) {
   }

   private static class GifAnimation {
      private final List<GpuTexture> textures = new ArrayList<>();
      private final List<GpuTextureView> views = new ArrayList<>();
      private final List<Integer> delays = new ArrayList<>();
      private int totalDuration = 0;

      GifAnimation(String path) {
         try {
            Identifier id = Identifier.of(path);
            String resourcePath = "/assets/" + id.getNamespace() + "/" + id.getPath();
            InputStream is = Gif2D.class.getResourceAsStream(resourcePath);
            if (is == null) {
               is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath.substring(1));
            }

            if (is == null) {
               return;
            }

            ImageInputStream stream = ImageIO.createImageInputStream(is);
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(stream);
            int count = reader.getNumImages(true);
            if (count <= 0) {
               reader.dispose();
               stream.close();
               is.close();
               return;
            }

            int canvasW = reader.getWidth(0);
            int canvasH = reader.getHeight(0);

            for (int i = 0; i < count; i++) {
               Gif2D.FrameMeta meta = Gif2D.readMeta(reader, i);
               BufferedImage frame = reader.read(i);
               BufferedImage canvas = new BufferedImage(canvasW, canvasH, 2);
               Graphics2D g = canvas.createGraphics();
               g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
               Gif2D.clear(g, canvasW, canvasH);
               g.drawImage(frame, meta.x, meta.y, null);
               g.dispose();
               this.upload(canvas, path, i);
               this.delays.add(meta.delay);
               this.totalDuration = this.totalDuration + meta.delay;
            }

            reader.dispose();
            stream.close();
            is.close();
         } catch (Exception var15) {
         }
      }

      private void upload(BufferedImage image, String path, int index) {
         NativeImage nativeImage = Gif2D.toNative(image, path);
         GpuTexture texture = RenderSystem.getDevice().createTexture(() -> "gif_" + index, 5, TextureFormat.RGBA8, image.getWidth(), image.getHeight(), 1, 1);
         RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, nativeImage);
         nativeImage.close();
         this.textures.add(texture);
         this.views.add(RenderSystem.getDevice().createTextureView(texture));
      }

      public GpuTextureView getCurrentFrame() {
         if (this.views.isEmpty()) {
            return null;
         }

         if (this.totalDuration <= 0) {
            return this.views.get(0);
         }

         long t = System.currentTimeMillis() % this.totalDuration;
         long acc = 0L;

         for (int i = 0; i < this.views.size(); i++) {
            acc += this.delays.get(i).intValue();
            if (t < acc) {
               return this.views.get(i);
            }
         }

         return this.views.get(this.views.size() - 1);
      }

      public void dispose() {
         this.views.forEach(GpuTextureView::close);
         this.textures.forEach(GpuTexture::close);
         this.views.clear();
         this.textures.clear();
      }
   }
}
