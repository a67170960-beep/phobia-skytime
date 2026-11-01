package ruhack.phobia.system.rendersystem.render2d.rectangle;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class Rectangle2D {
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static ByteBuffer uniformData;
   private static final int UNIFORM_SIZE = 256;

   public static void init() {
      if (pipeline == null) {
         try {
            pipeline = RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.of("phobia", "rectangle"))
               .withVertexShader(Identifier.of("phobia", "rectangle_vertex"))
               .withFragmentShader(Identifier.of("phobia", "rectangle_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Rectangle2D Uniforms", 136, 256L);
            uniformData = MemoryUtil.memAlloc(256);
         } catch (Exception var1) {
         }
      }
   }

   public static void draw(Matrix4f matrix, float x, float y, float width, float height, float tl, float tr, float br, float bl, float z, int... colors) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null) {
         ByteBuffer buffer = uniformData;
         buffer.clear();
         buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
         buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
         buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
         buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
         buffer.position(64);
         buffer.putFloat(x).putFloat(y).putFloat(width).putFloat(height);
         buffer.putFloat(br).putFloat(tr).putFloat(bl).putFloat(tl);
         buffer.putFloat(z);
         buffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);

         for (int i = 0; i < 9; i++) {
            int color = colorAt(colors, i);
            buffer.putFloat((color >> 16 & 0xFF) / 255.0F);
            buffer.putFloat((color >> 8 & 0xFF) / 255.0F);
            buffer.putFloat((color & 0xFF) / 255.0F);
            buffer.putFloat((color >> 24 & 0xFF) / 255.0F);
         }

         buffer.flip();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(uniformBuffer.slice(), buffer);
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         RenderPass pass = encoder.createRenderPass(() -> "Rectangle2D", framebuffer.getColorAttachmentView(), OptionalInt.empty());

         try {
            pass.setPipeline(pipeline);
            pass.setUniform("Uniforms", uniformBuffer);
            pass.draw(0, 6);
         } catch (Throwable var18) {
            if (pass != null) {
               try {
                  pass.close();
               } catch (Throwable var17) {
                  var18.addSuppressed(var17);
               }
            }

            throw var18;
         }

         if (pass != null) {
            pass.close();
         }
      }
   }

   private static int colorAt(int[] colors, int index) {
      return colors != null && colors.length != 0 ? colors[Math.min(index, colors.length - 1)] : -1;
   }

   public static void shutdown() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      if (uniformData != null) {
         MemoryUtil.memFree(uniformData);
         uniformData = null;
      }

      pipeline = null;
   }
}
