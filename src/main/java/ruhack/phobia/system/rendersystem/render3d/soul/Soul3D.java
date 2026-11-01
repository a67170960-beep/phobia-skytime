package ruhack.phobia.system.rendersystem.render3d.soul;

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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ruhack.phobia.util.render.RenderBufferPool;

public final class Soul3D {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int UNIFORM_SIZE = 128;
   private static final int VERTEX_SIZE = 24;
   private static final int MAX_SOULS = 1024;
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer vertexBuffer;
   private static Matrix4f projection = new Matrix4f();
   private static Matrix4f view = new Matrix4f();
   private static final float[] souls = new float[9216];
   private static final float[] QUAD_U = new float[]{-1.0F, 1.0F, 1.0F, -1.0F, 1.0F, -1.0F};
   private static final float[] QUAD_V = new float[]{-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F};
   private static final Matrix4f inverseView = new Matrix4f();
   private static final Matrix4f combined = new Matrix4f();
   private static int soulCount;
   private static double cameraX;
   private static double cameraY;
   private static double cameraZ;
   private static boolean batchIgnoreDepth;

   private Soul3D() {
   }

   public static void setMatrices(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
      projection.set(projectionMatrix);
      view.set(viewMatrix);
   }

   public static void begin(boolean ignoreDepth) {
      init();
      soulCount = 0;
      batchIgnoreDepth = ignoreDepth;
      Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
      cameraX = camera.x;
      cameraY = camera.y;
      cameraZ = camera.z;
   }

   public static void soul(double x, double y, double z, float size, int color, float alpha, int seed) {
      if (soulCount < 1024) {
         int offset = soulCount++ * 9;
         souls[offset] = (float)(x - cameraX);
         souls[offset + 1] = (float)(y - cameraY);
         souls[offset + 2] = (float)(z - cameraZ);
         souls[offset + 3] = size;
         souls[offset + 4] = (color >> 16 & 0xFF) / 255.0F;
         souls[offset + 5] = (color >> 8 & 0xFF) / 255.0F;
         souls[offset + 6] = (color & 0xFF) / 255.0F;
         souls[offset + 7] = Math.max(0.0F, Math.min(1.0F, alpha));
         souls[offset + 8] = seed;
      }
   }

   public static void end() {
      if (pipeline != null && uniformBuffer != null && vertexBuffer != null && soulCount != 0) {
         inverseView.set(view).invert();
         float rightX = inverseView.m00();
         float rightY = inverseView.m01();
         float rightZ = inverseView.m02();
         float upX = inverseView.m10();
         float upY = inverseView.m11();
         float upZ = inverseView.m12();
         ByteBuffer vertices = RenderBufferPool.acquire(1, soulCount * 6 * 24);

         for (int i = 0; i < soulCount; i++) {
            int offset = i * 9;
            float cx = souls[offset];
            float cy = souls[offset + 1];
            float cz = souls[offset + 2];
            float size = souls[offset + 3];
            byte r = (byte)(souls[offset + 4] * 255.0F);
            byte g = (byte)(souls[offset + 5] * 255.0F);
            byte b = (byte)(souls[offset + 6] * 255.0F);
            byte a = (byte)(souls[offset + 7] * 255.0F);
            float uvBase = souls[offset + 8] * 4.0F;

            for (int vertex = 0; vertex < 6; vertex++) {
               float u = QUAD_U[vertex];
               float v = QUAD_V[vertex];
               vertices.putFloat(cx + (rightX * u + upX * v) * size);
               vertices.putFloat(cy + (rightY * u + upY * v) * size);
               vertices.putFloat(cz + (rightZ * u + upZ * v) * size);
               vertices.put(r).put(g).put(b).put(a);
               vertices.putFloat(uvBase + u).putFloat(v);
            }
         }

         vertices.flip();
         ByteBuffer uniforms = RenderBufferPool.acquire(0, 128);
         putMatrix(uniforms, combined.set(projection).mul(view));
         float time = (float)(System.currentTimeMillis() % 3600000L) / 1000.0F;
         uniforms.putFloat(time).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         uniforms.flip();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(uniformBuffer.slice(), uniforms);
         encoder.writeToBuffer(vertexBuffer.slice(), vertices);
         Framebuffer framebuffer = mc.getFramebuffer();
         RenderPass pass = encoder.createRenderPass(
            () -> "TargetESP Souls",
            framebuffer.getColorAttachmentView(),
            OptionalInt.empty(),
            framebuffer.getDepthAttachmentView(),
            batchIgnoreDepth ? OptionalDouble.of(1.0) : OptionalDouble.empty()
         );

         try {
            pass.setPipeline(pipeline);
            pass.setUniform("Uniforms", uniformBuffer);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.draw(0, soulCount * 6);
         } catch (Throwable var22) {
            if (pass != null) {
               try {
                  pass.close();
               } catch (Throwable var21) {
                  var22.addSuppressed(var21);
               }
            }

            throw var22;
         }

         if (pass != null) {
            pass.close();
         }

         soulCount = 0;
      } else {
         soulCount = 0;
      }
   }

   private static void init() {
      if (pipeline == null) {
         VertexFormat format = VertexFormat.builder()
            .add("inPosition", VertexFormatElement.POSITION)
            .add("inColor", VertexFormatElement.COLOR)
            .add("inUV", VertexFormatElement.UV)
            .build();
         pipeline = RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.of("phobia", "3d/soul3d"))
            .withVertexShader(Identifier.of("phobia", "3d/soul3d_vertex"))
            .withFragmentShader(Identifier.of("phobia", "3d/soul3d_fragment"))
            .withVertexFormat(format, DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.ADDITIVE)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .withDepthWrite(false)
            .build();
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Soul3D Uniforms", 136, 128L);
         vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Soul3D Vertices", 40, 147456L);
      }
   }

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }
}
