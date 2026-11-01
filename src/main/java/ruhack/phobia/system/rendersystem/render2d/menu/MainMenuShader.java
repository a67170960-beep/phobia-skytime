package ruhack.phobia.system.rendersystem.render2d.menu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class MainMenuShader {
   private static final int UNIFORM_SIZE = 32;
   private static final long START_TIME = System.nanoTime();
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer dummyVertexBuffer;
   private static ByteBuffer uniformData;

   private MainMenuShader() {
   }

   public static void render(float mouseX, float mouseY) {
      MinecraftClient client = MinecraftClient.getInstance();
      Framebuffer framebuffer = client.getFramebuffer();
      if (framebuffer != null && framebuffer.textureWidth > 0 && framebuffer.textureHeight > 0) {
         if (pipeline == null) {
            init();
         }

         float time = (float)(System.nanoTime() - START_TIME) * 1.0E-9F;
         uniformData.clear();
         uniformData.putFloat(framebuffer.textureWidth);
         uniformData.putFloat(framebuffer.textureHeight);
         uniformData.putFloat(time);
         uniformData.putFloat((float)framebuffer.textureWidth / framebuffer.textureHeight);
         uniformData.putFloat(mouseX);
         uniformData.putFloat(mouseY);
         uniformData.putFloat(0.0F);
         uniformData.putFloat(0.0F);
         uniformData.flip();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.clearColorTexture(framebuffer.getColorAttachment(), -16053490);
         encoder.writeToBuffer(uniformBuffer.slice(), uniformData);
         RenderPass pass = encoder.createRenderPass(() -> "Phobia Main Menu Background", framebuffer.getColorAttachmentView(), OptionalInt.empty());

         try {
            pass.setPipeline(pipeline);
            pass.setUniform("MenuData", uniformBuffer);
            pass.setVertexBuffer(0, dummyVertexBuffer);
            pass.draw(0, 3);
         } catch (Throwable var10) {
            if (pass != null) {
               try {
                  pass.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (pass != null) {
            pass.close();
         }
      }
   }

   private static void init() {
      pipeline = RenderPipeline.builder(new Snippet[0])
         .withLocation(Identifier.of("phobia", "pipeline/main_menu"))
         .withVertexShader(Identifier.of("phobia", "menu/main_menu_vertex"))
         .withFragmentShader(Identifier.of("phobia", "menu/main_menu_fragment"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("MenuData", UniformType.UNIFORM_BUFFER)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build();
      uniformData = MemoryUtil.memAlloc(32);
      uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Phobia Main Menu Uniforms", 136, 32L);
      ByteBuffer dummy = MemoryUtil.memAlloc(4);
      dummy.putInt(0).flip();
      dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Phobia Main Menu Dummy Vertex", 40, dummy);
      MemoryUtil.memFree(dummy);
   }

   public static void shutdown() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
      }

      if (dummyVertexBuffer != null) {
         dummyVertexBuffer.close();
      }

      if (uniformData != null) {
         MemoryUtil.memFree(uniformData);
      }

      uniformBuffer = null;
      dummyVertexBuffer = null;
      uniformData = null;
      pipeline = null;
   }
}
