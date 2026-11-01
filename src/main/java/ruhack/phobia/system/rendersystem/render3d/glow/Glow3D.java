package ruhack.phobia.system.rendersystem.render3d.glow;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
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
import net.minecraft.client.render.Camera;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ruhack.phobia.util.render.RenderBufferPool;

public class Glow3D {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer vertexBuffer;
   private static final int UNIFORM_SIZE = 128;
   private static final int MAX_GLOWS = 1024;
   private static final int VERTEX_SIZE = 24;
   private static final int GLOW_STRIDE = 9;
   private static final float[] QUAD_U = new float[]{-1.0F, 1.0F, 1.0F, -1.0F, 1.0F, -1.0F};
   private static final float[] QUAD_V = new float[]{-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F};
   private static final float[] glowData = new float[9216];
   private static int glowCount;
   private static double cameraX;
   private static double cameraY;
   private static double cameraZ;
   private static Matrix4f projectionMatrix = new Matrix4f();
   private static Matrix4f viewMatrix = new Matrix4f();
   private static final Matrix4f combinedMatrix = new Matrix4f();
   private static final Matrix4f inverseViewMatrix = new Matrix4f();
   private static boolean ignoreDepth = false;

   public static void init() {
      if (pipeline == null) {
         try {
            VertexFormat format = VertexFormat.builder()
               .add("inPosition", VertexFormatElement.POSITION)
               .add("inColor", VertexFormatElement.COLOR)
               .add("inUV", VertexFormatElement.UV)
               .build();
            pipeline = RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.of("phobia", "3d/glow3d"))
               .withVertexShader(Identifier.of("phobia", "3d/glow3d_vertex"))
               .withFragmentShader(Identifier.of("phobia", "3d/glow3d_fragment"))
               .withVertexFormat(format, DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.ADDITIVE)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withCull(false)
               .withDepthWrite(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Glow3D Uniforms", 136, 128L);
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Glow3D Vertices", 40, 147456L);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   public static void setMatrices(Matrix4f projection, Matrix4f view) {
      projectionMatrix.set(projection);
      viewMatrix.set(view);
   }

   public static Vec3d getCameraPos() {
      Camera camera = mc.gameRenderer.getCamera();
      return camera.getCameraPos();
   }

   public static void begin() {
      begin(false);
   }

   public static void begin(boolean ignore) {
      if (pipeline == null) {
         init();
      }

      glowCount = 0;
      ignoreDepth = ignore;
      Vec3d camera = getCameraPos();
      cameraX = camera.x;
      cameraY = camera.y;
      cameraZ = camera.z;
   }

   public static void glow(double x, double y, double z, float size, int color, float alpha) {
      glow(x, y, z, size, color, alpha, 1.0F);
   }

   public static void glow(double x, double y, double z, float size, int color, float alpha, float intensity) {
      add(x, y, z, size, color, alpha * intensity, 0.0F);
   }

   public static void core(double x, double y, double z, float size, int color, float alpha) {
      add(x, y, z, size * 1.15F, color, alpha * 1.15F, 1.0F);
   }

   private static void add(double x, double y, double z, float size, int color, float alpha, float core) {
      if (glowCount < 1024 && !(size <= 0.0F) && !(alpha <= 0.001F)) {
         int offset = glowCount++ * 9;
         glowData[offset] = (float)(x - cameraX);
         glowData[offset + 1] = (float)(y - cameraY);
         glowData[offset + 2] = (float)(z - cameraZ);
         glowData[offset + 3] = size;
         glowData[offset + 4] = (color >> 16 & 0xFF) / 255.0F;
         glowData[offset + 5] = (color >> 8 & 0xFF) / 255.0F;
         glowData[offset + 6] = (color & 0xFF) / 255.0F;
         glowData[offset + 7] = Math.min(1.0F, alpha);
         glowData[offset + 8] = core;
      }
   }

   public static void end() {
      if (pipeline != null && uniformBuffer != null && vertexBuffer != null) {
         if (glowCount != 0) {
            Matrix4f combined = combinedMatrix.set(projectionMatrix).mul(viewMatrix);
            Matrix4f viewInv = inverseViewMatrix.set(viewMatrix).invert();
            float rightX = viewInv.m00();
            float rightY = viewInv.m01();
            float rightZ = viewInv.m02();
            float upX = viewInv.m10();
            float upY = viewInv.m11();
            float upZ = viewInv.m12();
            ByteBuffer uniformData = RenderBufferPool.acquire(0, 128);
            putMatrix(uniformData, combined);
            uniformData.flip();
            ByteBuffer vertexData = RenderBufferPool.acquire(1, glowCount * 6 * 24);

            for (int i = 0; i < glowCount; i++) {
               int offset = i * 9;
               float cx = glowData[offset];
               float cy = glowData[offset + 1];
               float cz = glowData[offset + 2];
               float size = glowData[offset + 3];
               float r = glowData[offset + 4];
               float g = glowData[offset + 5];
               float b = glowData[offset + 6];
               float a = glowData[offset + 7];
               float isCore = glowData[offset + 8];
               float alphaWithCore = isCore > 0.5F ? Math.min(1.0F, a * 1.12F) : a;

               for (int v = 0; v < 6; v++) {
                  float u = QUAD_U[v];
                  float vv = QUAD_V[v];
                  float px = cx + (rightX * u + upX * vv) * size;
                  float py = cy + (rightY * u + upY * vv) * size;
                  float pz = cz + (rightZ * u + upZ * vv) * size;
                  vertexData.putFloat(px).putFloat(py).putFloat(pz);
                  vertexData.put((byte)(r * 255.0F)).put((byte)(g * 255.0F)).put((byte)(b * 255.0F)).put((byte)(alphaWithCore * 255.0F));
                  vertexData.putFloat(u).putFloat(vv);
               }
            }

            vertexData.flip();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(), uniformData);
            encoder.writeToBuffer(vertexBuffer.slice(), vertexData);
            Framebuffer framebuffer = mc.getFramebuffer();
            RenderPass pass = encoder.createRenderPass(
               () -> "Glow3D",
               framebuffer.getColorAttachmentView(),
               OptionalInt.empty(),
               framebuffer.getDepthAttachmentView(),
               ignoreDepth ? OptionalDouble.of(1.0) : OptionalDouble.empty()
            );

            try {
               pass.setPipeline(pipeline);
               pass.setUniform("Uniforms", uniformBuffer);
               pass.setVertexBuffer(0, vertexBuffer);
               pass.draw(0, glowCount * 6);
            } catch (Throwable var29) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable var28) {
                     var29.addSuppressed(var28);
                  }
               }

               throw var29;
            }

            if (pass != null) {
               pass.close();
            }

            glowCount = 0;
         }
      }
   }

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }

   public static void shutdown() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      if (vertexBuffer != null) {
         vertexBuffer.close();
         vertexBuffer = null;
      }
   }
}
