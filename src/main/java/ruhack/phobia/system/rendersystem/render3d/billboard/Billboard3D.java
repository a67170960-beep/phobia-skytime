package ruhack.phobia.system.rendersystem.render3d.billboard;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.Camera;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ruhack.phobia.util.render.RenderBufferPool;

public final class Billboard3D {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static final int UNIFORM_SIZE = 128;
   private static final int VERTEX_SIZE = 24;
   private static final int MAX_SPRITES = 2048;
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuBuffer vertexBuffer;
   private static Matrix4f projection = new Matrix4f();
   private static Matrix4f view = new Matrix4f();
   private static final double[] spriteX = new double[2048];
   private static final double[] spriteY = new double[2048];
   private static final double[] spriteZ = new double[2048];
   private static final float[] spriteSize = new float[2048];
   private static final float[] spriteRotation = new float[2048];
   private static final int[] spriteColor = new int[2048];
   private static final float[] spriteAlpha = new float[2048];
   private static final float[] QUAD_X = new float[]{-1.0F, 1.0F, 1.0F, -1.0F, 1.0F, -1.0F};
   private static final float[] QUAD_Y = new float[]{-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F};
   private static final float[] QUAD_U = new float[]{0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F};
   private static final float[] QUAD_V = new float[]{1.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F};
   private static final Matrix4f inverseView = new Matrix4f();
   private static final Matrix4f combined = new Matrix4f();
   private static int spriteCount;
   private static Identifier batchTexture;
   private static boolean batchIgnoreDepth;

   private Billboard3D() {
   }

   public static void setMatrices(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
      projection.set(projectionMatrix);
      view.set(viewMatrix);
   }

   public static void draw(Identifier texture, double x, double y, double z, float size, float rotation, int color, float alpha, boolean ignoreDepth) {
      begin(texture, ignoreDepth);
      sprite(x, y, z, size, rotation, color, alpha);
      end();
   }

   public static void begin(Identifier texture, boolean ignoreDepth) {
      init();
      spriteCount = 0;
      batchTexture = texture;
      batchIgnoreDepth = ignoreDepth;
   }

   public static void sprite(double x, double y, double z, float size, float rotation, int color, float alpha) {
      if (spriteCount < 2048) {
         spriteX[spriteCount] = x;
         spriteY[spriteCount] = y;
         spriteZ[spriteCount] = z;
         spriteSize[spriteCount] = size;
         spriteRotation[spriteCount] = rotation;
         spriteColor[spriteCount] = color;
         spriteAlpha[spriteCount] = alpha;
         spriteCount++;
      }
   }

   public static void end() {
      if (pipeline != null && uniformBuffer != null && vertexBuffer != null && batchTexture != null && spriteCount != 0) {
         Camera camera = MC.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getCameraPos();
         inverseView.set(view).invert();
         float rightX = inverseView.m00();
         float rightY = inverseView.m01();
         float rightZ = inverseView.m02();
         float upX = inverseView.m10();
         float upY = inverseView.m11();
         float upZ = inverseView.m12();
         ByteBuffer vertices = RenderBufferPool.acquire(1, spriteCount * 6 * 24);

         for (int sprite = 0; sprite < spriteCount; sprite++) {
            float cx = (float)(spriteX[sprite] - cameraPos.x);
            float cy = (float)(spriteY[sprite] - cameraPos.y);
            float cz = (float)(spriteZ[sprite] - cameraPos.z);
            float radians = (float)Math.toRadians(spriteRotation[sprite]);
            float cos = (float)Math.cos(radians);
            float sin = (float)Math.sin(radians);
            float half = spriteSize[sprite] * 0.5F;
            int color = spriteColor[sprite];
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            float a = Math.max(0.0F, Math.min(1.0F, spriteAlpha[sprite]));

            for (int vertex = 0; vertex < 6; vertex++) {
               float localX = (QUAD_X[vertex] * cos - QUAD_Y[vertex] * sin) * half;
               float localY = (QUAD_X[vertex] * sin + QUAD_Y[vertex] * cos) * half;
               vertices.putFloat(cx + rightX * localX + upX * localY);
               vertices.putFloat(cy + rightY * localX + upY * localY);
               vertices.putFloat(cz + rightZ * localX + upZ * localY);
               vertices.put((byte)(r * 255.0F)).put((byte)(g * 255.0F)).put((byte)(b * 255.0F)).put((byte)(a * 255.0F));
               vertices.putFloat(QUAD_U[vertex]).putFloat(QUAD_V[vertex]);
            }
         }

         vertices.flip();
         ByteBuffer uniforms = RenderBufferPool.acquire(0, 128);
         putMatrix(uniforms, combined.set(projection).mul(view));
         uniforms.flip();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(uniformBuffer.slice(), uniforms);
         encoder.writeToBuffer(vertexBuffer.slice(), vertices);
         Framebuffer framebuffer = MC.getFramebuffer();
         GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
         GpuTextureView textureView = MC.getTextureManager().getTexture(batchTexture).getGlTextureView();
         RenderPass pass = encoder.createRenderPass(
            () -> "TargetESP Billboard",
            framebuffer.getColorAttachmentView(),
            OptionalInt.empty(),
            framebuffer.getDepthAttachmentView(),
            batchIgnoreDepth ? OptionalDouble.of(1.0) : OptionalDouble.empty()
         );

         try {
            pass.setPipeline(pipeline);
            pass.setUniform("Uniforms", uniformBuffer);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.bindTexture("Sampler0", textureView, sampler);
            pass.draw(0, spriteCount * 6);
         } catch (Throwable var26) {
            if (pass != null) {
               try {
                  pass.close();
               } catch (Throwable var25) {
                  var26.addSuppressed(var25);
               }
            }

            throw var26;
         }

         if (pass != null) {
            pass.close();
         }

         spriteCount = 0;
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
            .withLocation(Identifier.of("phobia", "3d/billboard3d"))
            .withVertexShader(Identifier.of("phobia", "3d/billboard3d_vertex"))
            .withFragmentShader(Identifier.of("phobia", "3d/billboard3d_fragment"))
            .withVertexFormat(format, DrawMode.TRIANGLES)
            .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .withDepthWrite(false)
            .build();
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Billboard3D Uniforms", 136, 128L);
         vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Billboard3D Vertices", 40, 294912L);
      }
   }

   private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
      buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }
}
