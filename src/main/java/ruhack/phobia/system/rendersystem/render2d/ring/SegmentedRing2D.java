package ruhack.phobia.system.rendersystem.render2d.ring;

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

public final class SegmentedRing2D {
   private static final int UNIFORM_SIZE = 160;
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static ByteBuffer uniformData;

   private SegmentedRing2D() {
   }

   public static void init() {
      if (pipeline == null) {
         pipeline = RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.of("phobia", "segmented_ring"))
            .withVertexShader(Identifier.of("phobia", "segmented_ring_vertex"))
            .withFragmentShader(Identifier.of("phobia", "segmented_ring_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build();
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "SegmentedRing2D Uniforms", 136, 160L);
         uniformData = MemoryUtil.memAlloc(160);
      }
   }

   public static void draw(Matrix4f projection, float x, float y, float size, float thickness, float sectorDegrees, float z, int... colors) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null && uniformData != null) {
         ByteBuffer buffer = uniformData;
         buffer.clear();
         buffer.putFloat(projection.m00()).putFloat(projection.m01()).putFloat(projection.m02()).putFloat(projection.m03());
         buffer.putFloat(projection.m10()).putFloat(projection.m11()).putFloat(projection.m12()).putFloat(projection.m13());
         buffer.putFloat(projection.m20()).putFloat(projection.m21()).putFloat(projection.m22()).putFloat(projection.m23());
         buffer.putFloat(projection.m30()).putFloat(projection.m31()).putFloat(projection.m32()).putFloat(projection.m33());
         buffer.position(64);
         buffer.putFloat(x).putFloat(y).putFloat(size).putFloat(size);
         buffer.putFloat(thickness).putFloat(sectorDegrees).putFloat(z).putFloat(0.0F);
         buffer.position(96);

         for (int i = 0; i < 3; i++) {
            int color = colors.length == 0 ? -1 : colors[Math.min(i, colors.length - 1)];
            buffer.putFloat((color >> 16 & 0xFF) / 255.0F);
            buffer.putFloat((color >> 8 & 0xFF) / 255.0F);
            buffer.putFloat((color & 0xFF) / 255.0F);
            buffer.putFloat((color >> 24 & 0xFF) / 255.0F);
         }

         buffer.flip();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(uniformBuffer.slice(), buffer);
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         RenderPass pass = encoder.createRenderPass(() -> "SegmentedRing2D", framebuffer.getColorAttachmentView(), OptionalInt.empty());

         try {
            pass.setPipeline(pipeline);
            pass.setUniform("Uniforms", uniformBuffer);
            pass.draw(0, 6);
         } catch (Throwable var15) {
            if (pass != null) {
               try {
                  pass.close();
               } catch (Throwable var14) {
                  var15.addSuppressed(var14);
               }
            }

            throw var15;
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

      if (uniformData != null) {
         MemoryUtil.memFree(uniformData);
         uniformData = null;
      }

      pipeline = null;
   }
}
