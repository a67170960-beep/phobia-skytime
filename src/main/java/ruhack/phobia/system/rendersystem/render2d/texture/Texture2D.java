package ruhack.phobia.system.rendersystem.render2d.texture;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class Texture2D {
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static ByteBuffer uniformData;
   private static final int UNIFORM_SIZE = 256;

   public static void init() {
      if (pipeline == null) {
         pipeline = RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.of("phobia", "texture"))
            .withVertexShader(Identifier.of("phobia", "texture_vertex"))
            .withFragmentShader(Identifier.of("phobia", "texture_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build();
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture2D Uniforms", 136, 256L);
         uniformData = MemoryUtil.memAlloc(256);
      }
   }

   public static void draw(Matrix4f matrix, float x, float y, float size, GpuTextureView textureView, int color, float radius, float z) {
      drawRegionInternal(matrix, x, y, size, size, textureView, color, radius, z, 0.0F, 0.0F, 1.0F, 1.0F, false, false);
   }

   public static void drawMask(Matrix4f matrix, float x, float y, float size, GpuTextureView textureView, int color, float radius, float z) {
      drawRegionInternal(matrix, x, y, size, size, textureView, color, radius, z, 0.0F, 0.0F, 1.0F, 1.0F, false, true);
   }

   public static void drawRegion(
      Matrix4f matrix,
      float x,
      float y,
      float width,
      float height,
      GpuTextureView textureView,
      int color,
      float radius,
      float z,
      float u0,
      float v0,
      float u1,
      float v1,
      boolean nearest
   ) {
      drawRegionInternal(matrix, x, y, width, height, textureView, color, radius, z, u0, v0, u1, v1, nearest, false);
   }

   private static void drawRegionInternal(
      Matrix4f matrix,
      float x,
      float y,
      float width,
      float height,
      GpuTextureView textureView,
      int color,
      float radius,
      float z,
      float u0,
      float v0,
      float u1,
      float v1,
      boolean nearest,
      boolean alphaMask
   ) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null && textureView != null) {
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         float a = (color >> 24 & 0xFF) / 255.0F;
         ByteBuffer buffer = uniformData;
         buffer.clear();
         buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
         buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
         buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
         buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
         buffer.position(64);
         buffer.putFloat(x).putFloat(y).putFloat(width).putFloat(height);
         buffer.putFloat(r).putFloat(g).putFloat(b).putFloat(a);
         buffer.putFloat(radius).putFloat(u0).putFloat(v0).putFloat(u1 - u0);
         buffer.putFloat(z).putFloat(v1 - v0).putFloat(alphaMask ? 1.0F : 0.0F).putFloat(0.0F);
         buffer.flip();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(uniformBuffer.slice(), buffer);
         GpuSampler sampler = RenderSystem.getSamplerCache().get(nearest ? FilterMode.NEAREST : FilterMode.LINEAR, false);
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         RenderPass pass = encoder.createRenderPass(() -> "Texture2D", framebuffer.getColorAttachmentView(), OptionalInt.empty());

         try {
            pass.setPipeline(pipeline);
            pass.setUniform("Uniforms", uniformBuffer);
            pass.bindTexture("Sampler0", textureView, sampler);
            pass.draw(0, 6);
         } catch (Throwable var27) {
            if (pass != null) {
               try {
                  pass.close();
               } catch (Throwable var26) {
                  var27.addSuppressed(var26);
               }
            }

            throw var27;
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
   }
}
