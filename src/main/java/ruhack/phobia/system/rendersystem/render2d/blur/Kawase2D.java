package ruhack.phobia.system.rendersystem.render2d.blur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import ruhack.phobia.system.rendersystem.render2d.Render2D;

public class Kawase2D {
   private static final int DOWNSAMPLE_SCALE = 2;
   private static final int BUFFER_SIZE = 256;
   private static final RenderPipeline PIPELINE_BLUR = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation(Identifier.of("phobia", "pipeline/blur_pass"))
         .withVertexShader(Identifier.of("phobia", "blur_pass_vertex"))
         .withFragmentShader(Identifier.of("phobia", "blur_pass_fragment"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final RenderPipeline PIPELINE_FINAL = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation(Identifier.of("phobia", "pipeline/blur_final"))
         .withVertexShader(Identifier.of("phobia", "blur_final_vertex"))
         .withFragmentShader(Identifier.of("phobia", "blur_final_fragment"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f MODEL_OFFSET = new Vector3f(0.0F, 0.0F, 0.0F);
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer dummyVertexBuffer;
   private static ByteBuffer dataBuffer;
   private static GpuTexture copyTexture;
   private static GpuTextureView copyTextureView;
   private static GpuTexture[] pingPongTextures = new GpuTexture[2];
   private static GpuTextureView[] pingPongViews = new GpuTextureView[2];
   private static int lastWidth = 0;
   private static int lastHeight = 0;
   private static boolean initialized = false;
   private static long lastFrameTime = -1L;
   private static long managedFrameId;
   private static long preparedManagedFrameId = -1L;
   private static boolean managedFrame;
   private static int cachedBlurSrc = 0;
   private static float cachedStrength = 0.0F;
   private static final float[] BLUR_OFFSETS = new float[]{2.0F, 4.0F, 7.0F, 9.0F, 14.0F};

   public static void init() {
      if (!initialized) {
         dataBuffer = MemoryUtil.memAlloc(256);
         ByteBuffer dummyData = MemoryUtil.memAlloc(4);
         dummyData.putInt(0);
         dummyData.flip();
         dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "phobia:blur_dummy_vertex", 32, dummyData);
         MemoryUtil.memFree(dummyData);
         initialized = true;
      }
   }

   public static void beginFrame() {
      managedFrameId++;
      managedFrame = true;
   }

   public static void endFrame() {
      managedFrame = false;
   }

   private static void ensureTextures(int fbWidth, int fbHeight) {
      int blurWidth = fbWidth / 2;
      int blurHeight = fbHeight / 2;
      if (copyTexture == null || fbWidth != lastWidth || fbHeight != lastHeight) {
         if (copyTextureView != null) {
            copyTextureView.close();
            copyTextureView = null;
         }

         if (copyTexture != null) {
            copyTexture.close();
            copyTexture = null;
         }

         copyTexture = RenderSystem.getDevice().createTexture(() -> "phobia:blur_copy", 5, TextureFormat.RGBA8, fbWidth, fbHeight, 1, 1);
         copyTextureView = RenderSystem.getDevice().createTextureView(copyTexture);

         for (int i = 0; i < 2; i++) {
            if (pingPongViews[i] != null) {
               pingPongViews[i].close();
               pingPongViews[i] = null;
            }

            if (pingPongTextures[i] != null) {
               pingPongTextures[i].close();
               pingPongTextures[i] = null;
            }

            int idx = i;
            pingPongTextures[i] = RenderSystem.getDevice().createTexture(() -> "phobia:blur_pp_" + idx, 13, TextureFormat.RGBA8, blurWidth, blurHeight, 1, 1);
            pingPongViews[i] = RenderSystem.getDevice().createTextureView(pingPongTextures[i]);
         }

         lastWidth = fbWidth;
         lastHeight = fbHeight;
         lastFrameTime = -1L;
      }
   }

   public static void draw(Matrix4f matrix, float x, float y, float width, float height, float radius, float strength, float z) {
      draw(matrix, x, y, width, height, radius, radius, radius, radius, strength, z);
   }

   public static void draw(
      Matrix4f matrix, float x, float y, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, float strength, float z
   ) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.getFramebuffer() != null) {
         if (client.getFramebuffer().getColorAttachment() != null) {
            init();
            int fbWidth = client.getFramebuffer().textureWidth;
            int fbHeight = client.getFramebuffer().textureHeight;
            int blurWidth = fbWidth / 2;
            int blurHeight = fbHeight / 2;
            ensureTextures(fbWidth, fbHeight);
            long currentTime = System.nanoTime() / 16666666L;
            boolean strengthChanged = Math.abs(strength - cachedStrength) > 0.01F;
            boolean needsBlur = managedFrame ? preparedManagedFrameId != managedFrameId : currentTime != lastFrameTime || strengthChanged;
            GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
               .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            if (needsBlur) {
               encoder.copyTextureToTexture(client.getFramebuffer().getColorAttachment(), copyTexture, 0, 0, 0, 0, 0, fbWidth, fbHeight);
               prepareBlurData(fbWidth, fbHeight, blurWidth, blurHeight, 1.0F, strength);
               encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
               RenderPass downsample = encoder.createRenderPass(
                  () -> "phobia:blur_downsample", pingPongViews[0], OptionalInt.empty(), null, OptionalDouble.empty()
               );

               try {
                  downsample.setPipeline(PIPELINE_BLUR);
                  downsample.setVertexBuffer(0, dummyVertexBuffer);
                  downsample.bindTexture("Sampler0", copyTextureView, sampler);
                  RenderSystem.bindDefaultUniforms(downsample);
                  downsample.setUniform("DynamicTransforms", dynamicTransforms);
                  downsample.setUniform("BlurData", uniformBuffer);
                  downsample.draw(0, 6);
               } catch (Throwable var38) {
                  if (downsample != null) {
                     try {
                        downsample.close();
                     } catch (Throwable var35) {
                        var38.addSuppressed(var35);
                     }
                  }

                  throw var38;
               }

               if (downsample != null) {
                  downsample.close();
               }

               int iterations = BLUR_OFFSETS.length;
               float offsetScale = (float)Math.sqrt(Math.max(0.01F, blurVariance(strength)) / 346.0F);

               for (int i = 0; i < iterations; i++) {
                  int src = i % 2;
                  int dst = (i + 1) % 2;
                  float offset = BLUR_OFFSETS[i] * offsetScale;
                  int passIdx = i;
                  prepareBlurData(blurWidth, blurHeight, blurWidth, blurHeight, offset, 1.0F);
                  encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
                  RenderPass pass = encoder.createRenderPass(
                     () -> "phobia:blur_" + passIdx, pingPongViews[dst], OptionalInt.empty(), null, OptionalDouble.empty()
                  );

                  try {
                     pass.setPipeline(PIPELINE_BLUR);
                     pass.setVertexBuffer(0, dummyVertexBuffer);
                     pass.bindTexture("Sampler0", pingPongViews[src], sampler);
                     RenderSystem.bindDefaultUniforms(pass);
                     pass.setUniform("DynamicTransforms", dynamicTransforms);
                     pass.setUniform("BlurData", uniformBuffer);
                     pass.draw(0, 6);
                  } catch (Throwable var37) {
                     if (pass != null) {
                        try {
                           pass.close();
                        } catch (Throwable var34) {
                           var37.addSuppressed(var34);
                        }
                     }

                     throw var37;
                  }

                  if (pass != null) {
                     pass.close();
                  }
               }

               cachedBlurSrc = iterations % 2;
               lastFrameTime = currentTime;
               if (managedFrame) {
                  preparedManagedFrameId = managedFrameId;
               }

               cachedStrength = strength;
            }

            int scaledW = Render2D.getFixedScaledWidth();
            int scaledH = Render2D.getFixedScaledHeight();
            prepareFinalData(matrix, x, y, width, height, scaledW, scaledH, topLeft, topRight, bottomRight, bottomLeft, z);
            encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
            RenderPass finalPass = encoder.createRenderPass(() -> "phobia:blur_final", client.getFramebuffer().getColorAttachmentView(), OptionalInt.empty());

            try {
               finalPass.setPipeline(PIPELINE_FINAL);
               finalPass.setVertexBuffer(0, dummyVertexBuffer);
               finalPass.bindTexture("Sampler0", pingPongViews[cachedBlurSrc], sampler);
               RenderSystem.bindDefaultUniforms(finalPass);
               finalPass.setUniform("DynamicTransforms", dynamicTransforms);
               finalPass.setUniform("BlurData", uniformBuffer);
               finalPass.draw(0, 6);
            } catch (Throwable var36) {
               if (finalPass != null) {
                  try {
                     finalPass.close();
                  } catch (Throwable var33) {
                     var36.addSuppressed(var33);
                  }
               }

               throw var36;
            }

            if (finalPass != null) {
               finalPass.close();
            }
         }
      }
   }

   private static float blurVariance(float strength) {
      int oldIterations = Math.max(2, (int)(5.0F * strength));
      if (oldIterations == 2) {
         return 5.0F;
      } else {
         return oldIterations == 3 ? 9.0F : 9.0F * oldIterations - 18.0F;
      }
   }

   private static void prepareBlurData(int screenW, int screenH, int texW, int texH, float offset, float strength) {
      dataBuffer.clear();

      for (int i = 0; i < 16; i++) {
         dataBuffer.putFloat(0.0F);
      }

      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(screenW).putFloat(screenH);
      dataBuffer.putFloat(screenW).putFloat(screenH).putFloat(1.0F).putFloat(offset);
      dataBuffer.putFloat(texW).putFloat(texH).putFloat(1.0F).putFloat(strength);
      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.flip();
      ensureBuffer();
   }

   private static void prepareFinalData(
      Matrix4f matrix, float x, float y, float w, float h, int fbW, int fbH, float topLeft, float topRight, float bottomRight, float bottomLeft, float z
   ) {
      dataBuffer.clear();
      dataBuffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      dataBuffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      dataBuffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      dataBuffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
      dataBuffer.putFloat(x).putFloat(y).putFloat(w).putFloat(h);
      dataBuffer.putFloat(fbW).putFloat(fbH).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.putFloat(fbW).putFloat(fbH).putFloat(1.0F).putFloat(0.0F);
      dataBuffer.putFloat(topLeft).putFloat(topRight).putFloat(bottomRight).putFloat(bottomLeft);
      dataBuffer.putFloat(z).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.flip();
      ensureBuffer();
   }

   private static void ensureBuffer() {
      int size = dataBuffer.remaining();
      if (uniformBuffer == null || uniformBuffer.size() < size) {
         if (uniformBuffer != null) {
            uniformBuffer.close();
         }

         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "phobia:blur_uniform", 136, size);
      }
   }

   public static GpuTextureView getBlurTextureView() {
      return initialized && pingPongViews[cachedBlurSrc] != null ? pingPongViews[cachedBlurSrc] : null;
   }

   public static void shutdown() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      if (dummyVertexBuffer != null) {
         dummyVertexBuffer.close();
         dummyVertexBuffer = null;
      }

      if (dataBuffer != null) {
         MemoryUtil.memFree(dataBuffer);
         dataBuffer = null;
      }

      if (copyTextureView != null) {
         copyTextureView.close();
         copyTextureView = null;
      }

      if (copyTexture != null) {
         copyTexture.close();
         copyTexture = null;
      }

      for (int i = 0; i < 2; i++) {
         if (pingPongViews[i] != null) {
            pingPongViews[i].close();
            pingPongViews[i] = null;
         }

         if (pingPongTextures[i] != null) {
            pingPongTextures[i].close();
            pingPongTextures[i] = null;
         }
      }

      lastWidth = 0;
      lastHeight = 0;
      initialized = false;
      lastFrameTime = -1L;
      preparedManagedFrameId = -1L;
      managedFrame = false;
   }
}
