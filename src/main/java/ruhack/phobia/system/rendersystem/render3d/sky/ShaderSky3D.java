package ruhack.phobia.system.rendersystem.render3d.sky;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ruhack.phobia.system.modulesystem.impl.render.ShaderSky;
import ruhack.phobia.util.render.RenderBufferPool;

public class ShaderSky3D {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int UNIFORM_SIZE = 112;
   private static final long START_NANOS = System.nanoTime();
   private static RenderPipeline auroraPipeline;
   private static RenderPipeline blackHolePipeline;
   private static RenderPipeline galaxyPipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer vertexBuffer;
   private static Matrix4f projectionMatrix = new Matrix4f();
   private static Matrix4f viewMatrix = new Matrix4f();
   private static final Matrix4f rotationViewMatrix = new Matrix4f();
   private static final Matrix4f inverseViewProjection = new Matrix4f();

   public static void setMatrices(Matrix4f projection, Matrix4f view) {
      projectionMatrix.set(projection);
      viewMatrix.set(view);
   }

   private static void init() {
      if (auroraPipeline == null) {
         try {
            VertexFormat format = VertexFormat.builder().add("inPosition", VertexFormatElement.POSITION).build();
            auroraPipeline = buildPipeline("sky_aurora", format);
            blackHolePipeline = buildPipeline("sky_blackhole", format);
            galaxyPipeline = buildPipeline("sky_galaxy", format);
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "ShaderSky3D Uniforms", 136, 112L);
            ByteBuffer vertexData = RenderBufferPool.acquire(1, 36);
            vertexData.putFloat(-1.0F).putFloat(-1.0F).putFloat(0.0F);
            vertexData.putFloat(3.0F).putFloat(-1.0F).putFloat(0.0F);
            vertexData.putFloat(-1.0F).putFloat(3.0F).putFloat(0.0F);
            vertexData.flip();
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "ShaderSky3D Vertices", 40, vertexData.remaining());
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(vertexBuffer.slice(), vertexData);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   private static RenderPipeline buildPipeline(String name, VertexFormat format) {
      return RenderPipeline.builder(new Snippet[0])
         .withLocation(Identifier.of("phobia", "3d/" + name))
         .withVertexShader(Identifier.of("phobia", "3d/sky_vertex"))
         .withFragmentShader(Identifier.of("phobia", "3d/" + name + "_fragment"))
         .withVertexFormat(format, DrawMode.TRIANGLES)
         .withUniform("SkyData", UniformType.UNIFORM_BUFFER)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build();
   }

   public static void render(ShaderSky module) {
      init();
      if (auroraPipeline != null && uniformBuffer != null && vertexBuffer != null) {
         RenderPipeline pipeline;
         if (module.mode.isSelected("Чёрная дыра")) {
            pipeline = blackHolePipeline;
         } else if (module.mode.isSelected("Галактика")) {
            pipeline = galaxyPipeline;
         } else {
            pipeline = auroraPipeline;
         }

         Matrix4f rotationView = rotationViewMatrix.set(viewMatrix).setTranslation(0.0F, 0.0F, 0.0F);
         Matrix4f invViewProj = inverseViewProjection.set(projectionMatrix).mul(rotationView).invert();
         float time = (float)((System.nanoTime() - START_NANOS) / 1.0E9) * module.speed.getValue();
         ByteBuffer uniformData = RenderBufferPool.acquire(0, 112);
         putMatrix(uniformData, invViewProj);
         putColor(uniformData, module.getPrimaryColor());
         putColor(uniformData, module.getSecondaryColor());
         uniformData.putFloat(time).putFloat(module.brightness.getValue()).putFloat(0.0F).putFloat(0.0F);
         uniformData.flip();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(uniformBuffer.slice(), uniformData);
         Framebuffer framebuffer = mc.getFramebuffer();
         if (framebuffer != null && framebuffer.getColorAttachmentView() != null && framebuffer.getDepthAttachmentView() != null) {
            RenderPass pass = encoder.createRenderPass(
               () -> "ShaderSky3D", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.empty()
            );

            try {
               pass.setPipeline(pipeline);
               pass.setUniform("SkyData", uniformBuffer);
               pass.setVertexBuffer(0, vertexBuffer);
               pass.draw(0, 3);
            } catch (Throwable var12) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (pass != null) {
               pass.close();
            }
         }
      }
   }

   private static void putColor(ByteBuffer buffer, int argb) {
      buffer.putFloat((argb >> 16 & 0xFF) / 255.0F)
         .putFloat((argb >> 8 & 0xFF) / 255.0F)
         .putFloat((argb & 0xFF) / 255.0F)
         .putFloat((argb >>> 24 & 0xFF) / 255.0F);
   }

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }
}
