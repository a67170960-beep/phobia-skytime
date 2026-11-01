package ruhack.phobia.system.rendersystem.render3d.watercaustic;

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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ruhack.phobia.util.render.RenderBufferPool;

public class WaterCaustic3D {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer waterDataBuffer;
   private static GpuBuffer vertexBuffer;
   private static final int UNIFORM_SIZE = 128;
   private static final int WATER_DATA_SIZE = 80;
   private static final int MAX_VERTICES = 16384;
   private static final int VERTEX_SIZE = 24;
   private static final float[] vertices = new float[81920];
   private static final Matrix4f combinedMatrix = new Matrix4f();
   private static final Matrix4f identityMatrix = new Matrix4f();
   private static int vertexCount;
   private static Matrix4f projectionMatrix = new Matrix4f();
   private static Matrix4f viewMatrix = new Matrix4f();

   public static void init() {
      if (pipeline == null) {
         try {
            VertexFormat format = VertexFormat.builder()
               .add("inPosition", VertexFormatElement.POSITION)
               .add("inColor", VertexFormatElement.COLOR)
               .add("inUV", VertexFormatElement.UV)
               .build();
            pipeline = RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.of("phobia", "3d/watercaustic"))
               .withVertexShader(Identifier.of("phobia", "3d/watercaustic_vertex"))
               .withFragmentShader(Identifier.of("phobia", "3d/watercaustic_fragment"))
               .withVertexFormat(format, DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withUniform("WaterData", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withCull(false)
               .withDepthWrite(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "WaterCaustic3D Uniforms", 136, 128L);
            waterDataBuffer = RenderSystem.getDevice().createBuffer(() -> "WaterCaustic3D Data", 136, 80L);
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "WaterCaustic3D Vertices", 40, 393216L);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   public static void setMatrices(Matrix4f projection, Matrix4f view) {
      projectionMatrix.set(projection);
      viewMatrix.set(view);
   }

   public static void begin() {
      if (pipeline == null) {
         init();
      }

      vertexCount = 0;
   }

   public static void box(Box box) {
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
      float rx1 = (float)(x1 - cam.x);
      float ry1 = (float)(y1 - cam.y);
      float rz1 = (float)(z1 - cam.z);
      float rx2 = (float)(x2 - cam.x);
      float ry2 = (float)(y2 - cam.y);
      float rz2 = (float)(z2 - cam.z);
      quad(rx1, ry1, rz1, rx2, ry1, rz1, rx2, ry1, rz2, rx1, ry1, rz2);
      quad(rx1, ry2, rz1, rx1, ry2, rz2, rx2, ry2, rz2, rx2, ry2, rz1);
      quad(rx1, ry1, rz1, rx1, ry2, rz1, rx2, ry2, rz1, rx2, ry1, rz1);
      quad(rx1, ry1, rz2, rx2, ry1, rz2, rx2, ry2, rz2, rx1, ry2, rz2);
      quad(rx1, ry1, rz1, rx1, ry1, rz2, rx1, ry2, rz2, rx1, ry2, rz1);
      quad(rx2, ry1, rz1, rx2, ry2, rz1, rx2, ry2, rz2, rx2, ry1, rz2);
   }

   private static void quad(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
      addVertex(x1, y1, z1, 0.0F, 0.0F);
      addVertex(x2, y2, z2, 1.0F, 0.0F);
      addVertex(x3, y3, z3, 1.0F, 1.0F);
      addVertex(x1, y1, z1, 0.0F, 0.0F);
      addVertex(x3, y3, z3, 1.0F, 1.0F);
      addVertex(x4, y4, z4, 0.0F, 1.0F);
   }

   private static void addVertex(float x, float y, float z, float u, float v) {
      if (vertexCount < 16384) {
         int offset = vertexCount++ * 5;
         vertices[offset] = x;
         vertices[offset + 1] = y;
         vertices[offset + 2] = z;
         vertices[offset + 3] = u;
         vertices[offset + 4] = v;
      }
   }

   public static void end(int color, float alpha, float time, float scale) {
      end(color, 16777215, alpha, time, scale);
   }

   public static void end(int color, int patternColor, float alpha, float time, float scale) {
      if (pipeline != null && vertexCount != 0 && uniformBuffer != null && vertexBuffer != null) {
         ByteBuffer uniformData = RenderBufferPool.acquire(0, 128);
         putMatrix(uniformData, combinedMatrix.set(projectionMatrix).mul(viewMatrix));
         putMatrix(uniformData, identityMatrix.identity());
         uniformData.flip();
         ByteBuffer waterData = RenderBufferPool.acquire(1, 80);
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         waterData.putFloat(r).putFloat(g).putFloat(b).putFloat(alpha);
         float pr = (patternColor >> 16 & 0xFF) / 255.0F;
         float pg = (patternColor >> 8 & 0xFF) / 255.0F;
         float pb = (patternColor & 0xFF) / 255.0F;
         waterData.putFloat(pr).putFloat(pg).putFloat(pb).putFloat(scale);
         waterData.putFloat(time);
         waterData.flip();
         ByteBuffer vertexData = RenderBufferPool.acquire(2, vertexCount * 24);

         for (int i = 0; i < vertexCount; i++) {
            int offset = i * 5;
            vertexData.putFloat(vertices[offset]).putFloat(vertices[offset + 1]).putFloat(vertices[offset + 2]);
            vertexData.putInt(-1);
            vertexData.putFloat(vertices[offset + 3]).putFloat(vertices[offset + 4]);
         }

         vertexData.flip();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(uniformBuffer.slice(), uniformData);
         encoder.writeToBuffer(waterDataBuffer.slice(), waterData);
         encoder.writeToBuffer(vertexBuffer.slice(), vertexData);
         Framebuffer framebuffer = mc.getFramebuffer();
         RenderPass pass = encoder.createRenderPass(
            () -> "WaterCaustic3D", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.empty()
         );

         try {
            pass.setPipeline(pipeline);
            pass.setUniform("Uniforms", uniformBuffer);
            pass.setUniform("WaterData", waterDataBuffer);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.draw(0, vertexCount);
         } catch (Throwable var20) {
            if (pass != null) {
               try {
                  pass.close();
               } catch (Throwable var19) {
                  var20.addSuppressed(var19);
               }
            }

            throw var20;
         }

         if (pass != null) {
            pass.close();
         }

         vertexCount = 0;
      }
   }

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }
}
