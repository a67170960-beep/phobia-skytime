package ruhack.phobia.util.render;

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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;
import ruhack.phobia.system.modulesystem.impl.render.ShaderHands;

public final class ShaderHandsTrail {
   private static final int UNIFORM_SIZE = 80;
   private static final int DOWNSAMPLE = 2;
   private static final RenderPipeline FADE_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[0])
         .withLocation(Identifier.of("phobia", "pipeline/shader_hands_trail_fade"))
         .withVertexShader(Identifier.of("phobia", "post/shaderhands_trail_vertex"))
         .withFragmentShader(Identifier.of("phobia", "post/shaderhands_trail_fade_fragment"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("TrailData", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final RenderPipeline BLEND_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[0])
         .withLocation(Identifier.of("phobia", "pipeline/shader_hands_trail_blend"))
         .withVertexShader(Identifier.of("phobia", "post/shaderhands_trail_vertex"))
         .withFragmentShader(Identifier.of("phobia", "post/shaderhands_trail_blend_fragment"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[0])
         .withLocation(Identifier.of("phobia", "pipeline/shader_hands_trail_composite"))
         .withVertexShader(Identifier.of("phobia", "post/shaderhands_trail_vertex"))
         .withFragmentShader(Identifier.of("phobia", "post/shaderhands_trail_composite_fragment"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("TrailData", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static SimpleFramebuffer maskFramebuffer;
   public static final OutputTarget OUTPUT_TARGET = new OutputTarget(
      "phobia_shader_hands_trail", () -> (Framebuffer)(maskFramebuffer != null ? maskFramebuffer : MinecraftClient.getInstance().getFramebuffer())
   );
   private static final GpuTexture[] trailTextures = new GpuTexture[2];
   private static final GpuTextureView[] trailViews = new GpuTextureView[2];
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer dummyVertexBuffer;
   private static ByteBuffer uniformData;
   private static int width;
   private static int height;
   private static int readIndex;
   private static boolean framePrepared;
   private static long lastFrameNanos;
   private static float smoothDt = 0.016666668F;
   private static float smoothRise;
   private static float smoothSway;
   private static float smoothBurst;
   private static boolean wasSwinging;
   private static long lastSwingNanos = Long.MIN_VALUE;

   private ShaderHandsTrail() {
   }

   public static void beginFrame() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.getFramebuffer() != null) {
         ShaderHands settings = ShaderHands.getInstance();
         if (settings != null && settings.isTrailEnabled()) {
            ensureResources(client.getFramebuffer().textureWidth, client.getFramebuffer().textureHeight);
            long now = System.nanoTime();
            if (lastFrameNanos != 0L && now - lastFrameNanos > 250000000L) {
               clearTrailTextures();
               lastFrameNanos = 0L;
               smoothRise = 0.0F;
               smoothSway = 0.0F;
               smoothBurst = 0.0F;
            }

            if (lastFrameNanos != 0L) {
               compositeBehindHand(client);
            }

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            RenderPass ignored = encoder.createRenderPass(
               () -> "phobia:shader_hands_trail_mask_clear",
               maskFramebuffer.getColorAttachmentView(),
               OptionalInt.of(0),
               maskFramebuffer.getDepthAttachmentView(),
               OptionalDouble.of(1.0)
            );
            if (ignored != null) {
               ignored.close();
            }

            framePrepared = true;
         }
      }
   }

   public static void endFrame() {
      if (framePrepared && maskFramebuffer != null) {
         framePrepared = false;
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.getFramebuffer() != null) {
            ShaderHands settings = ShaderHands.getInstance();
            if (settings != null && settings.isTrailEnabled()) {
               long now = System.nanoTime();
               float rawDt = lastFrameNanos == 0L ? 0.016666668F : (float)(now - lastFrameNanos) / 1.0E9F;
               lastFrameNanos = now;
               if (rawDt <= 0.0F || rawDt > 0.05F) {
                  rawDt = smoothDt;
               }

               rawDt = Math.max(0.0069444445F, Math.min(0.016666668F, rawDt));
               smoothDt = smoothDt + (rawDt - smoothDt) * 0.08F;
               float time = (float)(now % 100000000000L) / 1.0E9F;
               float riseDelta = settings.trailRise.getValue() * smoothDt;
               float swayDelta = ((float)Math.sin(time * 3.6F) - (float)Math.sin((time - smoothDt) * 3.6F)) * settings.trailSway.getValue();
               float smoothing = 1.0F - (float)Math.exp(-smoothDt * 8.0F);
               smoothRise = smoothRise + (riseDelta - smoothRise) * smoothing;
               smoothSway = smoothSway + (swayDelta - smoothSway) * smoothing;
               boolean swinging = client.player != null && client.player.handSwinging;
               if (settings.trailBurst.isValue() && swinging && !wasSwinging) {
                  lastSwingNanos = now;
               }

               wasSwinging = swinging;
               float burst = 0.0F;
               if (settings.trailBurst.isValue() && lastSwingNanos != Long.MIN_VALUE) {
                  float burstAge = (float)(now - lastSwingNanos) / 1.0E9F;
                  burst = Math.max(0.0F, 1.0F - burstAge / 0.45F);
               } else if (!settings.trailBurst.isValue()) {
                  lastSwingNanos = Long.MIN_VALUE;
               }

               smoothBurst = smoothBurst + (burst - smoothBurst) * (1.0F - (float)Math.exp(-smoothDt * 6.0F));
               float fade = settings.trailFade.getValue() + smoothBurst * settings.trailBurstPower.getValue() * 0.012F;
               writeUniforms(
                  smoothSway,
                  smoothRise,
                  fade,
                  time,
                  smoothDt,
                  settings.trailTurbulence.getValue(),
                  settings.trailFlicker.getValue(),
                  width / 2.0F,
                  height / 2.0F,
                  settings.getPrimaryColor(),
                  settings.getSecondaryColor()
               );
               int writeIndex = 1 - readIndex;
               GpuSampler linear = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
               CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
               encoder.writeToBuffer(uniformBuffer.slice(), uniformData);
               RenderPass fadePass = encoder.createRenderPass(() -> "phobia:shader_hands_trail_fade", trailViews[writeIndex], OptionalInt.of(0));

               try {
                  fadePass.setPipeline(FADE_PIPELINE);
                  fadePass.setVertexBuffer(0, dummyVertexBuffer);
                  fadePass.bindTexture("Sampler0", trailViews[readIndex], linear);
                  fadePass.setUniform("TrailData", uniformBuffer.slice());
                  fadePass.draw(0, 6);
               } catch (Throwable var21) {
                  if (fadePass != null) {
                     try {
                        fadePass.close();
                     } catch (Throwable var19) {
                        var21.addSuppressed(var19);
                     }
                  }

                  throw var21;
               }

               if (fadePass != null) {
                  fadePass.close();
               }

               fadePass = encoder.createRenderPass(() -> "phobia:shader_hands_trail_inject", trailViews[writeIndex], OptionalInt.empty());

               try {
                  fadePass.setPipeline(BLEND_PIPELINE);
                  fadePass.setVertexBuffer(0, dummyVertexBuffer);
                  fadePass.bindTexture("Sampler0", maskFramebuffer.getColorAttachmentView(), linear);
                  fadePass.draw(0, 6);
               } catch (Throwable var20) {
                  if (fadePass != null) {
                     try {
                        fadePass.close();
                     } catch (Throwable var18) {
                        var20.addSuppressed(var18);
                     }
                  }

                  throw var20;
               }

               if (fadePass != null) {
                  fadePass.close();
               }

               readIndex = writeIndex;
            } else {
               reset();
            }
         }
      }
   }

   private static void compositeBehindHand(MinecraftClient client) {
      GpuSampler linear = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      RenderPass compositePass = encoder.createRenderPass(
         () -> "phobia:shader_hands_trail_behind_hand", client.getFramebuffer().getColorAttachmentView(), OptionalInt.empty()
      );

      try {
         compositePass.setPipeline(COMPOSITE_PIPELINE);
         compositePass.setVertexBuffer(0, dummyVertexBuffer);
         compositePass.bindTexture("Sampler0", trailViews[readIndex], linear);
         compositePass.setUniform("TrailData", uniformBuffer.slice());
         compositePass.draw(0, 6);
      } catch (Throwable var7) {
         if (compositePass != null) {
            try {
               compositePass.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (compositePass != null) {
         compositePass.close();
      }
   }

   private static void ensureResources(int framebufferWidth, int framebufferHeight) {
      if (dummyVertexBuffer == null) {
         ByteBuffer dummy = MemoryUtil.memAlloc(4);
         dummy.putInt(0).flip();
         dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "phobia:shader_hands_trail_vertex", 32, dummy);
         MemoryUtil.memFree(dummy);
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "phobia:shader_hands_trail_uniform", 136, 80L);
         uniformData = MemoryUtil.memAlloc(80);
      }

      if (maskFramebuffer == null || width != framebufferWidth || height != framebufferHeight) {
         releaseTextures();
         width = framebufferWidth;
         height = framebufferHeight;
         maskFramebuffer = new SimpleFramebuffer("phobia_shader_hands_mask", width, height, true);
         int trailWidth = Math.max(2, width / 2);
         int trailHeight = Math.max(2, height / 2);

         for (int i = 0; i < 2; i++) {
            int index = i;
            trailTextures[i] = RenderSystem.getDevice()
               .createTexture(() -> "phobia:shader_hands_trail_" + index, 12, TextureFormat.RGBA8, trailWidth, trailHeight, 1, 1);
            trailViews[i] = RenderSystem.getDevice().createTextureView(trailTextures[i]);
         }

         clearTrailTextures();
         readIndex = 0;
         lastFrameNanos = 0L;
         smoothDt = 0.016666668F;
         smoothRise = 0.0F;
         smoothSway = 0.0F;
         smoothBurst = 0.0F;
         wasSwinging = false;
         lastSwingNanos = Long.MIN_VALUE;
      }
   }

   private static void clearTrailTextures() {
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

      for (GpuTextureView view : trailViews) {
         if (view != null) {
            RenderPass ignored = encoder.createRenderPass(() -> "phobia:shader_hands_trail_clear", view, OptionalInt.of(0));
            if (ignored != null) {
               ignored.close();
            }
         }
      }
   }

   private static void writeUniforms(
      float offsetX,
      float offsetY,
      float fade,
      float time,
      float dt,
      float turbulence,
      float flicker,
      float textureWidth,
      float textureHeight,
      int primaryColor,
      int secondaryColor
   ) {
      uniformData.clear();
      uniformData.putFloat(offsetX).putFloat(offsetY).putFloat(fade).putFloat(time);
      uniformData.putFloat(dt).putFloat(turbulence).putFloat(flicker).putFloat(0.0F);
      uniformData.putFloat(textureWidth).putFloat(textureHeight).putFloat(0.0F).putFloat(0.0F);
      putColor(primaryColor);
      putColor(secondaryColor);
      uniformData.flip();
   }

   private static void putColor(int color) {
      uniformData.putFloat((color >> 16 & 0xFF) / 255.0F);
      uniformData.putFloat((color >> 8 & 0xFF) / 255.0F);
      uniformData.putFloat((color & 0xFF) / 255.0F);
      uniformData.putFloat(1.0F);
   }

   public static void reset() {
      framePrepared = false;
      if (trailViews[0] != null || trailViews[1] != null) {
         clearTrailTextures();
      }

      lastFrameNanos = 0L;
      smoothRise = 0.0F;
      smoothSway = 0.0F;
      smoothBurst = 0.0F;
      wasSwinging = false;
      lastSwingNanos = Long.MIN_VALUE;
   }

   private static void releaseTextures() {
      if (maskFramebuffer != null) {
         maskFramebuffer.delete();
         maskFramebuffer = null;
      }

      for (int i = 0; i < 2; i++) {
         if (trailViews[i] != null) {
            trailViews[i].close();
            trailViews[i] = null;
         }

         if (trailTextures[i] != null) {
            trailTextures[i].close();
            trailTextures[i] = null;
         }
      }
   }
}
