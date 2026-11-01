package ruhack.phobia.system.rendersystem.render3d;

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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ruhack.phobia.util.render.RenderBufferPool;

public class Render3D {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer vertexBuffer;
   private static final int UNIFORM_SIZE = 256;
   private static final int MAX_VERTICES = 4096;
   private static final int VERTEX_SIZE = 16;
   private static final float[] vertices = new float[28672];
   private static final Matrix4f combinedMatrix = new Matrix4f();
   private static final Matrix4f identityMatrix = new Matrix4f();
   private static int vertexCount;
   private static boolean ignoreDepth = false;
   private static Matrix4f projectionMatrix = new Matrix4f();
   private static Matrix4f viewMatrix = new Matrix4f();

   public static void init() {
      if (pipeline == null) {
         try {
            VertexFormat format = VertexFormat.builder().add("inPosition", VertexFormatElement.POSITION).add("inColor", VertexFormatElement.COLOR).build();
            pipeline = RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.of("phobia", "line3d"))
               .withVertexShader(Identifier.of("phobia", "line3d_vertex"))
               .withFragmentShader(Identifier.of("phobia", "line3d_fragment"))
               .withVertexFormat(format, DrawMode.DEBUG_LINES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withCull(false)
               .withDepthWrite(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Render3D Uniforms", 136, 256L);
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Render3D Vertices", 40, 65536L);
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

   public static void begin(MatrixStack matrices) {
      begin(matrices, false);
   }

   public static void begin(MatrixStack matrices, boolean ignore) {
      if (pipeline == null) {
         init();
      }

      vertexCount = 0;
      combinedMatrix.set(projectionMatrix).mul(viewMatrix);
      ignoreDepth = ignore;
   }

   public static void begin() {
      begin(null, false);
   }

   public static void begin(boolean ignore) {
      begin(null, ignore);
   }

   public static void drawLineBox(double x, double y, double z, float size, int color, float alpha) {
      Vec3d cam = getCameraPos();
      float rx = (float)(x - cam.x);
      float ry = (float)(y - cam.y);
      float rz = (float)(z - cam.z);
      float h = size / 2.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      addLine(rx - h, ry - h, rz - h, rx + h, ry - h, rz - h, r, g, b, alpha);
      addLine(rx + h, ry - h, rz - h, rx + h, ry - h, rz + h, r, g, b, alpha);
      addLine(rx + h, ry - h, rz + h, rx - h, ry - h, rz + h, r, g, b, alpha);
      addLine(rx - h, ry - h, rz + h, rx - h, ry - h, rz - h, r, g, b, alpha);
      addLine(rx - h, ry + h, rz - h, rx + h, ry + h, rz - h, r, g, b, alpha);
      addLine(rx + h, ry + h, rz - h, rx + h, ry + h, rz + h, r, g, b, alpha);
      addLine(rx + h, ry + h, rz + h, rx - h, ry + h, rz + h, r, g, b, alpha);
      addLine(rx - h, ry + h, rz + h, rx - h, ry + h, rz - h, r, g, b, alpha);
      addLine(rx - h, ry - h, rz - h, rx - h, ry + h, rz - h, r, g, b, alpha);
      addLine(rx + h, ry - h, rz - h, rx + h, ry + h, rz - h, r, g, b, alpha);
      addLine(rx + h, ry - h, rz + h, rx + h, ry + h, rz + h, r, g, b, alpha);
      addLine(rx - h, ry - h, rz + h, rx - h, ry + h, rz + h, r, g, b, alpha);
   }

   public static void drawRotatedLineBox(double x, double y, double z, float size, float rotY, float rotX, int color, float alpha) {
      Vec3d cam = getCameraPos();
      float rx = (float)(x - cam.x);
      float ry = (float)(y - cam.y);
      float rz = (float)(z - cam.z);
      float h = size / 2.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float[][] corners = new float[][]{{-h, -h, -h}, {h, -h, -h}, {h, -h, h}, {-h, -h, h}, {-h, h, -h}, {h, h, -h}, {h, h, h}, {-h, h, h}};
      float cosY = (float)Math.cos(Math.toRadians(rotY));
      float sinY = (float)Math.sin(Math.toRadians(rotY));
      float cosX = (float)Math.cos(Math.toRadians(rotX));
      float sinX = (float)Math.sin(Math.toRadians(rotX));

      for (float[] corner : corners) {
         float cx = corner[0];
         float cy = corner[1];
         float cz = corner[2];
         float tempX = cx * cosY - cz * sinY;
         float tempZ = cx * sinY + cz * cosY;
         cx = tempX;
         cz = tempZ;
         float tempY = cy * cosX - cz * sinX;
         tempZ = cy * sinX + cz * cosX;
         cy = tempY;
         corner[0] = cx + rx;
         corner[1] = cy + ry;
         corner[2] = tempZ + rz;
      }

      int[][] edges = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

      for (int[] edge : edges) {
         float[] p1 = corners[edge[0]];
         float[] p2 = corners[edge[1]];
         addLine(p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], r, g, b, alpha);
      }
   }

   private static void addLine(float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
      addVertex(x1, y1, z1, r, g, b, a);
      addVertex(x2, y2, z2, r, g, b, a);
   }

   private static void addVertex(float x, float y, float z, float r, float g, float b, float a) {
      if (vertexCount < 4096) {
         int offset = vertexCount++ * 7;
         vertices[offset] = x;
         vertices[offset + 1] = y;
         vertices[offset + 2] = z;
         vertices[offset + 3] = r;
         vertices[offset + 4] = g;
         vertices[offset + 5] = b;
         vertices[offset + 6] = a;
      }
   }

   public static void end() {
      if (pipeline != null && uniformBuffer != null && vertexBuffer != null) {
         if (vertexCount != 0) {
            ByteBuffer uniformData = RenderBufferPool.acquire(0, 256);
            putMatrix(uniformData, combinedMatrix);
            putMatrix(uniformData, identityMatrix.identity());
            uniformData.flip();
            ByteBuffer vertexData = RenderBufferPool.acquire(1, vertexCount * 16);

            for (int i = 0; i < vertexCount; i++) {
               int offset = i * 7;
               vertexData.putFloat(vertices[offset]).putFloat(vertices[offset + 1]).putFloat(vertices[offset + 2]);
               vertexData.put((byte)(vertices[offset + 3] * 255.0F))
                  .put((byte)(vertices[offset + 4] * 255.0F))
                  .put((byte)(vertices[offset + 5] * 255.0F))
                  .put((byte)(vertices[offset + 6] * 255.0F));
            }

            vertexData.flip();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(), uniformData);
            encoder.writeToBuffer(vertexBuffer.slice(), vertexData);
            Framebuffer framebuffer = mc.getFramebuffer();
            RenderPass pass = encoder.createRenderPass(
               () -> "Render3D Lines",
               framebuffer.getColorAttachmentView(),
               OptionalInt.empty(),
               framebuffer.getDepthAttachmentView(),
               ignoreDepth ? OptionalDouble.of(1.0) : OptionalDouble.empty()
            );

            try {
               pass.setPipeline(pipeline);
               pass.setUniform("Uniforms", uniformBuffer);
               pass.setVertexBuffer(0, vertexBuffer);
               pass.draw(0, vertexCount);
            } catch (Throwable var8) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (pass != null) {
               pass.close();
            }

            vertexCount = 0;
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
