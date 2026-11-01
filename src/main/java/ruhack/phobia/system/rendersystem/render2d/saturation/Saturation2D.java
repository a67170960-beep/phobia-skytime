package ruhack.phobia.system.rendersystem.render2d.saturation;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

public class Saturation2D {
   private static final Identifier PIPELINE_ID = Identifier.of("phobia", "pipeline/saturation");
   private static final Identifier VERTEX_SHADER = Identifier.of("phobia", "saturation_vertex");
   private static final Identifier FRAGMENT_SHADER = Identifier.of("phobia", "saturation_fragment");
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer dummyVertexBuffer;
   private static ByteBuffer dataBuffer;
   private static GpuTexture tempTexture;
   private static GpuTextureView tempTextureView;
   private static int lastWidth;
   private static int lastHeight;

   public static void init() {
      if (pipeline == null) {
         pipeline = RenderPipeline.builder(new Snippet[0])
            .withLocation(PIPELINE_ID)
            .withVertexShader(VERTEX_SHADER)
            .withFragmentShader(FRAGMENT_SHADER)
            .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
            .withUniform("SaturationData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
         dataBuffer = MemoryUtil.memAlloc(16);
         ByteBuffer dummyData = MemoryUtil.memAlloc(4);
         dummyData.putInt(0);
         dummyData.flip();
         dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "phobia:saturation_dummy_vertex", 40, dummyData);
         MemoryUtil.memFree(dummyData);
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "phobia:saturation_uniform", 136, 16L);
      }
   }

   private static void ensureTextures(int width, int height) {
      if (tempTexture == null || width != lastWidth || height != lastHeight) {
         if (tempTextureView != null) {
            tempTextureView.close();
         }

         if (tempTexture != null) {
            tempTexture.close();
         }

         tempTexture = RenderSystem.getDevice().createTexture(() -> "phobia:saturation_temp", 13, TextureFormat.RGBA8, width, height, 1, 1);
         tempTextureView = RenderSystem.getDevice().createTextureView(tempTexture);
         lastWidth = width;
         lastHeight = height;
      }
   }

   public static void apply(GpuTextureView targetView, GpuTextureView sourceView, float saturation) {
      if (pipeline == null) {
         init();
      }

      dataBuffer.clear();
      dataBuffer.putFloat(saturation);
      dataBuffer.flip();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
      GpuSampler linearSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      RenderPass renderPass = encoder.createRenderPass(() -> "phobia:saturation_pass", targetView, OptionalInt.empty());

      try {
         renderPass.setPipeline(pipeline);
         renderPass.setVertexBuffer(0, dummyVertexBuffer);
         renderPass.bindTexture("Sampler0", sourceView, linearSampler);
         renderPass.setUniform("SaturationData", uniformBuffer.slice());
         renderPass.draw(0, 6);
      } catch (Throwable var9) {
         if (renderPass != null) {
            try {
               renderPass.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (renderPass != null) {
         renderPass.close();
      }
   }

   public static void applyWithCopy(float saturation) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.getFramebuffer() != null) {
         int width = client.getFramebuffer().textureWidth;
         int height = client.getFramebuffer().textureHeight;
         ensureTextures(width, height);
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.copyTextureToTexture(client.getFramebuffer().getColorAttachment(), tempTexture, 0, 0, 0, 0, 0, width, height);
         apply(client.getFramebuffer().getColorAttachmentView(), tempTextureView, saturation);
      }
   }

   public static void shutdown() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
      }

      if (dummyVertexBuffer != null) {
         dummyVertexBuffer.close();
      }

      if (tempTextureView != null) {
         tempTextureView.close();
      }

      if (tempTexture != null) {
         tempTexture.close();
      }

      if (dataBuffer != null) {
         MemoryUtil.memFree(dataBuffer);
      }

      pipeline = null;
   }
}
