package ruhack.phobia.system.rendersystem.render3d.fill;

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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ruhack.phobia.util.render.RenderBufferPool;

public class Fill3D {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer vertexBuffer;
   private static final int UNIFORM_SIZE = 256;
   private static final int MAX_VERTICES = 16384;
   private static final int VERTEX_SIZE = 16;
   private static final float[] vertices = new float[114688];
   private static int vertexCount;
   private static final Matrix4f combinedMatrix = new Matrix4f();
   private static final Matrix4f identityMatrix = new Matrix4f();
   private static final float[][] BOX_CORNERS = new float[8][3];
   private static final float[][] DIAMOND_CORNERS = new float[6][3];
   private static final int[][] BOX_FACES = new int[][]{{0, 1, 2, 3}, {4, 7, 6, 5}, {0, 4, 5, 1}, {2, 6, 7, 3}, {0, 3, 7, 4}, {1, 5, 6, 2}};
   private static final int[][] DIAMOND_FACES = new int[][]{{0, 2, 5}, {0, 5, 3}, {0, 3, 4}, {0, 4, 2}, {1, 5, 2}, {1, 3, 5}, {1, 4, 3}, {1, 2, 4}};
   private static Matrix4f projectionMatrix = new Matrix4f();
   private static Matrix4f viewMatrix = new Matrix4f();
   private static boolean ignoreDepth = false;

   public static void init() {
      if (pipeline == null) {
         try {
            VertexFormat format = VertexFormat.builder().add("inPosition", VertexFormatElement.POSITION).add("inColor", VertexFormatElement.COLOR).build();
            pipeline = RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.of("phobia", "3d/fill3d"))
               .withVertexShader(Identifier.of("phobia", "3d/fill3d_vertex"))
               .withFragmentShader(Identifier.of("phobia", "3d/fill3d_fragment"))
               .withVertexFormat(format, DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withCull(false)
               .withDepthWrite(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Fill3D Uniforms", 136, 256L);
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Fill3D Vertices", 40, 262144L);
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

      vertexCount = 0;
      combinedMatrix.set(projectionMatrix).mul(viewMatrix);
      ignoreDepth = ignore;
   }

   public static void box(double x, double y, double z, float size, int color, float alpha) {
      Vec3d cam = getCameraPos();
      float rx = (float)(x - cam.x);
      float ry = (float)(y - cam.y);
      float rz = (float)(z - cam.z);
      float h = size / 2.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      quad(rx - h, ry - h, rz - h, rx + h, ry - h, rz - h, rx + h, ry - h, rz + h, rx - h, ry - h, rz + h, r, g, b, alpha);
      quad(rx - h, ry + h, rz - h, rx - h, ry + h, rz + h, rx + h, ry + h, rz + h, rx + h, ry + h, rz - h, r, g, b, alpha);
      quad(rx - h, ry - h, rz - h, rx - h, ry + h, rz - h, rx + h, ry + h, rz - h, rx + h, ry - h, rz - h, r, g, b, alpha);
      quad(rx + h, ry - h, rz + h, rx + h, ry + h, rz + h, rx - h, ry + h, rz + h, rx - h, ry - h, rz + h, r, g, b, alpha);
      quad(rx - h, ry - h, rz + h, rx - h, ry + h, rz + h, rx - h, ry + h, rz - h, rx - h, ry - h, rz - h, r, g, b, alpha);
      quad(rx + h, ry - h, rz - h, rx + h, ry + h, rz - h, rx + h, ry + h, rz + h, rx + h, ry - h, rz + h, r, g, b, alpha);
   }

   public static void box(Box box, int color, float alpha) {
      Vec3d cam = getCameraPos();
      float minX = (float)(box.minX - cam.x);
      float minY = (float)(box.minY - cam.y);
      float minZ = (float)(box.minZ - cam.z);
      float maxX = (float)(box.maxX - cam.x);
      float maxY = (float)(box.maxY - cam.y);
      float maxZ = (float)(box.maxZ - cam.z);
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      quad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, alpha);
      quad(minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, alpha);
      quad(minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, alpha);
      quad(maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, r, g, b, alpha);
      quad(minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, r, g, b, alpha);
      quad(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, alpha);
   }

   public static void rotatedBox(double x, double y, double z, float size, float rotY, float rotX, int color, float alpha) {
      Vec3d cam = getCameraPos();
      float rx = (float)(x - cam.x);
      float ry = (float)(y - cam.y);
      float rz = (float)(z - cam.z);
      float h = size / 2.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float[][] corners = BOX_CORNERS;
      setCorner(corners[0], -h, -h, -h);
      setCorner(corners[1], h, -h, -h);
      setCorner(corners[2], h, -h, h);
      setCorner(corners[3], -h, -h, h);
      setCorner(corners[4], -h, h, -h);
      setCorner(corners[5], h, h, -h);
      setCorner(corners[6], h, h, h);
      setCorner(corners[7], -h, h, h);
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

      for (int[] face : BOX_FACES) {
         float[] p0 = corners[face[0]];
         float[] p1 = corners[face[1]];
         float[] p2 = corners[face[2]];
         float[] p3 = corners[face[3]];
         quad(p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], p3[0], p3[1], p3[2], r, g, b, alpha);
      }
   }

   public static void diamond(double x, double y, double z, float size, int color, float alpha) {
      Vec3d cam = getCameraPos();
      float rx = (float)(x - cam.x);
      float ry = (float)(y - cam.y);
      float rz = (float)(z - cam.z);
      float h = size / 2.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float[] top = new float[]{rx, ry + h, rz};
      float[] bottom = new float[]{rx, ry - h, rz};
      float[] front = new float[]{rx, ry, rz + h};
      float[] back = new float[]{rx, ry, rz - h};
      float[] left = new float[]{rx - h, ry, rz};
      float[] right = new float[]{rx + h, ry, rz};
      tri(top, front, right, r, g, b, alpha);
      tri(top, right, back, r, g, b, alpha);
      tri(top, back, left, r, g, b, alpha);
      tri(top, left, front, r, g, b, alpha);
      tri(bottom, right, front, r, g, b, alpha);
      tri(bottom, back, right, r, g, b, alpha);
      tri(bottom, left, back, r, g, b, alpha);
      tri(bottom, front, left, r, g, b, alpha);
   }

   public static void rotatedDiamond(double x, double y, double z, float size, float rotY, float rotX, int color, float alpha) {
      Vec3d cam = getCameraPos();
      float rx = (float)(x - cam.x);
      float ry = (float)(y - cam.y);
      float rz = (float)(z - cam.z);
      float h = size / 2.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float[][] corners = DIAMOND_CORNERS;
      setCorner(corners[0], 0.0F, h, 0.0F);
      setCorner(corners[1], 0.0F, -h, 0.0F);
      setCorner(corners[2], 0.0F, 0.0F, h);
      setCorner(corners[3], 0.0F, 0.0F, -h);
      setCorner(corners[4], -h, 0.0F, 0.0F);
      setCorner(corners[5], h, 0.0F, 0.0F);
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

      for (int[] face : DIAMOND_FACES) {
         tri(corners[face[0]], corners[face[1]], corners[face[2]], r, g, b, alpha);
      }
   }

   private static void tri(float[] p1, float[] p2, float[] p3, float r, float g, float b, float a) {
      addVertex(p1[0], p1[1], p1[2], r, g, b, a);
      addVertex(p2[0], p2[1], p2[2], r, g, b, a);
      addVertex(p3[0], p3[1], p3[2], r, g, b, a);
   }

   private static void quad(
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float r,
      float g,
      float b,
      float a
   ) {
      addVertex(x1, y1, z1, r, g, b, a);
      addVertex(x2, y2, z2, r, g, b, a);
      addVertex(x3, y3, z3, r, g, b, a);
      addVertex(x1, y1, z1, r, g, b, a);
      addVertex(x3, y3, z3, r, g, b, a);
      addVertex(x4, y4, z4, r, g, b, a);
   }

   private static void addVertex(float x, float y, float z, float r, float g, float b, float a) {
      if (vertexCount < 16384) {
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

   private static void setCorner(float[] corner, float x, float y, float z) {
      corner[0] = x;
      corner[1] = y;
      corner[2] = z;
   }

   public static void end() {
      if (pipeline != null && uniformBuffer != null && vertexBuffer != null) {
         if (vertexCount != 0) {
            ByteBuffer uniformData = RenderBufferPool.acquire(0, 256);
            putMatrix(uniformData, combinedMatrix);
            putMatrix(uniformData, identityMatrix);
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
               () -> "Fill3D",
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
