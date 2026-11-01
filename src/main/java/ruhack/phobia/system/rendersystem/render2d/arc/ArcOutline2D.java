package ruhack.phobia.system.rendersystem.render2d.arc;

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

public class ArcOutline2D {
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static final int UNIFORM_SIZE = 160;

   public static void init() {
      if (pipeline == null) {
         try {
            pipeline = RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.of("phobia", "arc_outline"))
               .withVertexShader(Identifier.of("phobia", "arc_outline_vertex"))
               .withFragmentShader(Identifier.of("phobia", "arc_outline_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "ArcOutline2D Uniforms", 136, 160L);
         } catch (Exception e) {
            System.err.println("[ArcOutline2D] Failed to init: " + e.getMessage());
         }
      }
   }

   public static void draw(
      Matrix4f matrix,
      float x,
      float y,
      float size,
      float arcThickness,
      float degree,
      float rotation,
      float outlineThickness,
      int fillColor,
      int outlineColor,
      float z
   ) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null) {
         float fr = (fillColor >> 16 & 0xFF) / 255.0F;
         float fg = (fillColor >> 8 & 0xFF) / 255.0F;
         float fb = (fillColor & 0xFF) / 255.0F;
         float fa = (fillColor >> 24 & 0xFF) / 255.0F;
         float or = (outlineColor >> 16 & 0xFF) / 255.0F;
         float og = (outlineColor >> 8 & 0xFF) / 255.0F;
         float ob = (outlineColor & 0xFF) / 255.0F;
         float oa = (outlineColor >> 24 & 0xFF) / 255.0F;
         ByteBuffer buffer = MemoryUtil.memAlloc(160);
         buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
         buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
         buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
         buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
         buffer.position(64);
         buffer.putFloat(x).putFloat(y).putFloat(size).putFloat(size);
         buffer.putFloat(size).putFloat(arcThickness).putFloat(degree).putFloat(rotation);
         buffer.putFloat(z).putFloat(outlineThickness).putFloat(0.0F).putFloat(0.0F);
         buffer.putFloat(fr).putFloat(fg).putFloat(fb).putFloat(fa);
         buffer.putFloat(or).putFloat(og).putFloat(ob).putFloat(oa);
         buffer.flip();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(uniformBuffer.slice(), buffer);
         MemoryUtil.memFree(buffer);
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         RenderPass pass = encoder.createRenderPass(() -> "ArcOutline2D", framebuffer.getColorAttachmentView(), OptionalInt.empty());

         try {
            pass.setPipeline(pipeline);
            pass.setUniform("Uniforms", uniformBuffer);
            pass.draw(0, 6);
         } catch (Throwable var26) {
            if (pass != null) {
               try {
                  pass.close();
               } catch (Throwable var25) {
                  var26.addSuppressed(var25);
               }
            }

            throw var26;
         }

         if (pass != null) {
            pass.close();
         }
      }
   }

   public static void shutdown() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      pipeline = null;
   }
}
