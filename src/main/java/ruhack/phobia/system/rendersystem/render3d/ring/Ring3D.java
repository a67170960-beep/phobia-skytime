package ruhack.phobia.system.rendersystem.render3d.ring;

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

public final class Ring3D {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int UNIFORM_SIZE = 128;
   private static final int VERTEX_SIZE = 12;
   private static final int MAX_RINGS = 8;
   private static RenderPipeline pipeline;
   private static GpuBuffer[] uniformBuffers;
   private static GpuBuffer vertexBuffer;
   private static Matrix4f projection = new Matrix4f();
   private static Matrix4f view = new Matrix4f();
   private static final float[] rings = new float[72];
   private static final float[] QUAD_U = new float[]{-1.0F, 1.0F, 1.0F, -1.0F, 1.0F, -1.0F};
   private static final float[] QUAD_V = new float[]{-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F};
   private static final Matrix4f inverseView = new Matrix4f();
   private static final Matrix4f combined = new Matrix4f();
   private static int ringCount;
   private static boolean batchIgnoreDepth;

   private Ring3D() {
   }

   public static void setMatrices(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
      projection.set(projectionMatrix);
      view.set(viewMatrix);
   }

   public static void begin(boolean ignoreDepth) {
      init();
      ringCount = 0;
      batchIgnoreDepth = ignoreDepth;
   }

   public static void ring(double x, double y, double z, float radius, float tubeRadius, int color, float alpha) {
      if (ringCount < 8) {
         Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
         int offset = ringCount++ * 9;
         rings[offset] = (float)(x - cam.x);
         rings[offset + 1] = (float)(y - cam.y);
         rings[offset + 2] = (float)(z - cam.z);
         rings[offset + 3] = radius;
         rings[offset + 4] = tubeRadius;
         rings[offset + 5] = (color >> 16 & 0xFF) / 255.0F;
         rings[offset + 6] = (color >> 8 & 0xFF) / 255.0F;
         rings[offset + 7] = (color & 0xFF) / 255.0F;
         rings[offset + 8] = Math.max(0.0F, Math.min(1.0F, alpha));
      }
   }

   public static void end() {
      if (pipeline != null && uniformBuffers != null && vertexBuffer != null && ringCount != 0) {
         inverseView.set(view).invert();
         float rightX = inverseView.m00();
         float rightY = inverseView.m01();
         float rightZ = inverseView.m02();
         float upX = inverseView.m10();
         float upY = inverseView.m11();
         float upZ = inverseView.m12();
         combined.set(projection).mul(view);
         float time = (float)(System.currentTimeMillis() % 3600000L) / 1000.0F;
         int count = ringCount;
         ByteBuffer vertices = RenderBufferPool.acquire(1, count * 6 * 12);
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

         for (int i = 0; i < count; i++) {
            int offset = i * 9;
            float cx = rings[offset];
            float cy = rings[offset + 1];
            float cz = rings[offset + 2];
            float radius = rings[offset + 3];
            float tube = rings[offset + 4];
            float half = radius + tube * 8.0F;

            for (int vertex = 0; vertex < 6; vertex++) {
               float u = QUAD_U[vertex];
               float v = QUAD_V[vertex];
               vertices.putFloat(cx + (rightX * u + upX * v) * half);
               vertices.putFloat(cy + (rightY * u + upY * v) * half);
               vertices.putFloat(cz + (rightZ * u + upZ * v) * half);
            }

            ByteBuffer uniforms = RenderBufferPool.acquire(0, 128);
            putMatrix(uniforms, combined);
            uniforms.putFloat(time).putFloat(radius).putFloat(tube).putFloat(rings[offset + 8]);
            uniforms.putFloat(cx).putFloat(cy).putFloat(cz).putFloat(0.0F);
            uniforms.putFloat(rings[offset + 5]).putFloat(rings[offset + 6]).putFloat(rings[offset + 7]).putFloat(1.0F);
            uniforms.flip();
            encoder.writeToBuffer(uniformBuffers[i].slice(), uniforms);
         }

         vertices.flip();
         encoder.writeToBuffer(vertexBuffer.slice(), vertices);
         Framebuffer framebuffer = mc.getFramebuffer();

         for (int i = 0; i < count; i++) {
            RenderPass pass = encoder.createRenderPass(
               () -> "TargetESP Ring",
               framebuffer.getColorAttachmentView(),
               OptionalInt.empty(),
               framebuffer.getDepthAttachmentView(),
               batchIgnoreDepth && i == 0 ? OptionalDouble.of(1.0) : OptionalDouble.empty()
            );

            try {
               pass.setPipeline(pipeline);
               pass.setUniform("Uniforms", uniformBuffers[i]);
               pass.setVertexBuffer(0, vertexBuffer);
               pass.draw(i * 6, 6);
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
         }

         ringCount = 0;
      } else {
         ringCount = 0;
      }
   }

   private static void init() {
      if (pipeline == null) {
         VertexFormat format = VertexFormat.builder().add("inPosition", VertexFormatElement.POSITION).build();
         pipeline = RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.of("phobia", "3d/ring3d"))
            .withVertexShader(Identifier.of("phobia", "3d/ring3d_vertex"))
            .withFragmentShader(Identifier.of("phobia", "3d/ring3d_fragment"))
            .withVertexFormat(format, DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.ADDITIVE)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .withDepthWrite(false)
            .build();
         uniformBuffers = new GpuBuffer[8];

         for (int i = 0; i < 8; i++) {
            int index = i;
            uniformBuffers[i] = RenderSystem.getDevice().createBuffer(() -> "Ring3D Uniforms " + index, 136, 128L);
         }

         vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Ring3D Vertices", 40, 576L);
      }
   }

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }
}
