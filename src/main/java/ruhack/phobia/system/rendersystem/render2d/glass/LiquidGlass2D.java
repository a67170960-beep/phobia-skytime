package ruhack.phobia.system.rendersystem.render2d.glass;

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
import ruhack.phobia.system.rendersystem.render2d.blur.Kawase2D;

public class LiquidGlass2D {
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static final int UNIFORM_SIZE = 256;

   public static void init() {
      if (pipeline == null) {
         pipeline = RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.of("phobia", "liquidglass"))
            .withVertexShader(Identifier.of("phobia", "liquidglass_vertex"))
            .withFragmentShader(Identifier.of("phobia", "liquidglass_fragment"))
            .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build();
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "LiquidGlass2D Uniforms", 136, 256L);
      }
   }

   public static void draw(
      Matrix4f matrix,
      float x,
      float y,
      float width,
      float height,
      float[] radius,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null) {
         MinecraftClient mc = MinecraftClient.getInstance();
         Framebuffer fb = mc.getFramebuffer();
         float screenW = fb.textureWidth;
         float screenH = fb.textureHeight;
         ByteBuffer buffer = MemoryUtil.memAlloc(256);
         buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
         buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
         buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
         buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
         buffer.putFloat(x).putFloat(y).putFloat(width).putFloat(height);
         buffer.putFloat(screenW).putFloat(screenH);
         buffer.putFloat(0.0F).putFloat(0.0F);
         buffer.putFloat(radius[0]).putFloat(radius[1]).putFloat(radius[2]).putFloat(radius[3]);
         buffer.putFloat(0.5F);
         buffer.putFloat(squirt);
         buffer.putFloat(globalAlpha);
         buffer.putFloat(fresnelPower);
         buffer.putFloat((fresnelColor >> 16 & 0xFF) / 255.0F);
         buffer.putFloat((fresnelColor >> 8 & 0xFF) / 255.0F);
         buffer.putFloat((fresnelColor & 0xFF) / 255.0F);
         buffer.putFloat((fresnelColor >> 24 & 0xFF) / 255.0F);
         buffer.putFloat(baseAlpha);
         buffer.putInt(fresnelInvert ? 1 : 0);
         buffer.putFloat(fresnelMix);
         buffer.putFloat(distortStrength);
         buffer.putFloat(z);
         buffer.putFloat(0.0F);
         buffer.flip();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(uniformBuffer.slice(), buffer);
         MemoryUtil.memFree(buffer);
         GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
         GpuTextureView texView = Kawase2D.getBlurTextureView();
         if (texView == null) {
            texView = fb.getColorAttachmentView();
         }

         RenderPass pass = encoder.createRenderPass(() -> "LiquidGlass2D", fb.getColorAttachmentView(), OptionalInt.empty());

         try {
            pass.setPipeline(pipeline);
            pass.setUniform("Uniforms", uniformBuffer);
            pass.bindTexture("Sampler0", texView, sampler);
            pass.draw(0, 6);
         } catch (Throwable var28) {
            if (pass != null) {
               try {
                  pass.close();
               } catch (Throwable var27) {
                  var28.addSuppressed(var27);
               }
            }

            throw var28;
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
   }
}
