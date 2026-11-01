package ruhack.phobia.system.rendersystem.render2d.font.msdf;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class Msdf2D {
   private static final int MAX_GLYPHS = 128;
   private static final int MSDF_DATA_SIZE = 128;
   private static final int GLYPH_VECTORS = 3;
   private static final int GLYPH_DATA_SIZE = 6144;
   private static RenderPipeline pipeline;
   private static GpuBuffer msdfBuffer;
   private static GpuBuffer glyphBuffer;
   private static ByteBuffer msdfData;
   private static ByteBuffer glyphData;
   private static boolean initialized = false;
   private static boolean batching;
   private static MsdfFont batchFont;
   private static Matrix4f batchMatrix;
   private static float batchRotation;
   private static float batchZ;
   private static int batchGlyphCount;

   public static void init() {
      if (!initialized) {
         try {
            System.out.println("[MSDF] Initializing Msdf2D shaders and buffers...");
            pipeline = RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.of("phobia", "msdf"))
               .withVertexShader(Identifier.of("phobia", "msdf_vertex"))
               .withFragmentShader(Identifier.of("phobia", "msdf_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("MsdfData", UniformType.UNIFORM_BUFFER)
               .withUniform("GlyphData", UniformType.UNIFORM_BUFFER)
               .withSampler("Sampler0")
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            msdfBuffer = RenderSystem.getDevice().createBuffer(() -> "Msdf2D MsdfData", 136, 128L);
            glyphBuffer = RenderSystem.getDevice().createBuffer(() -> "Msdf2D GlyphData", 136, 6144L);
            msdfData = MemoryUtil.memAlloc(128);
            glyphData = MemoryUtil.memAlloc(6144);
            initialized = true;
         } catch (Exception e) {
            System.err.println("[MSDF] Failed to initialize Msdf2D: " + e.getMessage());
            e.printStackTrace();
         }
      }
   }

   public static void drawString(Matrix4f matrix, MsdfFont font, String text, float x, float y, float size, int color, float rotation, float z) {
      if (initialized && font != null && font.isLoaded() && !text.isEmpty()) {
         if (batching) {
            appendString(matrix, font, text, x, y, size, color, null, rotation, z);
         } else {
            float scale = size / font.getEmSize();
            float cursorX = x;
            float baseline = y + font.getAscender() * scale;
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            float a = (color >> 24 & 0xFF) / 255.0F;
            msdfData.clear();
            msdfData.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
            msdfData.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
            msdfData.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
            msdfData.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
            msdfData.putFloat(r).putFloat(g).putFloat(b).putFloat(a);
            msdfData.putFloat(font.getPxRange()).putFloat((float)Math.toRadians(rotation)).putFloat(0.0F).putFloat(0.0F);
            msdfData.putFloat(z).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
            msdfData.flip();
            glyphData.clear();
            int glyphCount = 0;

            for (int i = 0; i < text.length() && glyphCount < 128; i++) {
               int ch = text.charAt(i);
               MsdfGlyph glyph = font.getGlyph(ch);
               if (glyph == null) {
                  glyph = font.getGlyph(63);
                  if (glyph == null) {
                     continue;
                  }
               }

               if (glyph.width > 0.0F && glyph.height > 0.0F) {
                  float gx = cursorX + glyph.bearingX * scale;
                  float gy = baseline - glyph.bearingY * scale;
                  float gw = glyph.width * scale;
                  float gh = glyph.height * scale;
                  glyphData.putFloat(gx).putFloat(gy).putFloat(gw).putFloat(gh);
                  glyphData.putFloat(glyph.u0).putFloat(glyph.v0);
                  glyphData.putFloat(glyph.u1 - glyph.u0).putFloat(glyph.v1 - glyph.v0);
                  putColor(glyphData, color);
                  glyphCount++;
               }

               cursorX += glyph.advance * scale;
            }

            if (glyphCount != 0) {
               glyphData.flip();
               render(font, glyphCount);
            }
         }
      }
   }

   public static void drawStringColored(Matrix4f matrix, MsdfFont font, String text, float x, float y, float size, int[] colors, float z) {
      if (initialized && font != null && font.isLoaded() && !text.isEmpty() && colors.length != 0) {
         if (batching) {
            appendString(matrix, font, text, x, y, size, -1, colors, 0.0F, z);
         } else {
            float scale = size / font.getEmSize();
            float cursorX = x;
            float baseline = y + font.getAscender() * scale;
            prepareMsdfData(matrix, font, -1, 0.0F, z);
            glyphData.clear();
            int glyphCount = 0;

            for (int i = 0; i < text.length() && glyphCount < 128; i++) {
               int ch = text.charAt(i);
               MsdfGlyph glyph = font.getGlyph(ch);
               if (glyph == null) {
                  glyph = font.getGlyph(63);
                  if (glyph == null) {
                     continue;
                  }
               }

               if (glyph.width > 0.0F && glyph.height > 0.0F) {
                  int color = colors[i % colors.length];
                  float gx = cursorX + glyph.bearingX * scale;
                  float gy = baseline - glyph.bearingY * scale;
                  float gw = glyph.width * scale;
                  float gh = glyph.height * scale;
                  glyphData.putFloat(gx).putFloat(gy).putFloat(gw).putFloat(gh);
                  glyphData.putFloat(glyph.u0).putFloat(glyph.v0);
                  glyphData.putFloat(glyph.u1 - glyph.u0).putFloat(glyph.v1 - glyph.v0);
                  putColor(glyphData, color);
                  glyphCount++;
               }

               cursorX += glyph.advance * scale;
            }

            if (glyphCount != 0) {
               glyphData.flip();
               render(font, glyphCount);
            }
         }
      }
   }

   public static float drawGlyph(
      Matrix4f matrix, MsdfFont font, MsdfGlyph glyph, float cursorX, float baseline, float scale, int color, float rotation, float z
   ) {
      if (initialized && font != null && glyph != null) {
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         float a = (color >> 24 & 0xFF) / 255.0F;
         msdfData.clear();
         msdfData.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
         msdfData.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
         msdfData.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
         msdfData.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
         msdfData.putFloat(r).putFloat(g).putFloat(b).putFloat(a);
         msdfData.putFloat(font.getPxRange()).putFloat((float)Math.toRadians(rotation)).putFloat(0.0F).putFloat(0.0F);
         msdfData.putFloat(z).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         msdfData.flip();
         float gx = cursorX + glyph.bearingX * scale;
         float gy = baseline - glyph.bearingY * scale;
         float gw = glyph.width * scale;
         float gh = glyph.height * scale;
         glyphData.clear();
         glyphData.putFloat(gx).putFloat(gy).putFloat(gw).putFloat(gh);
         glyphData.putFloat(glyph.u0).putFloat(glyph.v0);
         glyphData.putFloat(glyph.u1 - glyph.u0).putFloat(glyph.v1 - glyph.v0);
         putColor(glyphData, color);
         glyphData.flip();
         render(font, 1);
         return glyph.advance * scale;
      } else {
         return 0.0F;
      }
   }

   private static void prepareMsdfData(Matrix4f matrix, MsdfFont font, int color, float rotation, float z) {
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float a = (color >> 24 & 0xFF) / 255.0F;
      msdfData.clear();
      msdfData.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      msdfData.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      msdfData.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      msdfData.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
      msdfData.putFloat(r).putFloat(g).putFloat(b).putFloat(a);
      msdfData.putFloat(font.getPxRange()).putFloat((float)Math.toRadians(rotation)).putFloat(0.0F).putFloat(0.0F);
      msdfData.putFloat(z).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      msdfData.flip();
   }

   private static void putColor(ByteBuffer buffer, int color) {
      buffer.putFloat((color >> 16 & 0xFF) / 255.0F);
      buffer.putFloat((color >> 8 & 0xFF) / 255.0F);
      buffer.putFloat((color & 0xFF) / 255.0F);
      buffer.putFloat((color >> 24 & 0xFF) / 255.0F);
   }

   public static void beginBatch() {
      if (batching) {
         flushBatch();
      }

      batching = true;
      resetBatch();
   }

   public static void endBatch() {
      if (batching) {
         flushBatch();
         batching = false;
         resetBatch();
      }
   }

   private static void appendString(
      Matrix4f matrix, MsdfFont font, String text, float x, float y, float size, int solidColor, int[] colors, float rotation, float z
   ) {
      prepareBatch(matrix, font, rotation, z);
      float scale = size / font.getEmSize();
      float cursorX = x;
      float baseline = y + font.getAscender() * scale;

      for (int i = 0; i < text.length(); i++) {
         int ch = text.charAt(i);
         MsdfGlyph glyph = font.getGlyph(ch);
         if (glyph == null) {
            glyph = font.getGlyph(63);
            if (glyph == null) {
               continue;
            }
         }

         if (glyph.width > 0.0F && glyph.height > 0.0F) {
            if (batchGlyphCount == 128) {
               flushBatch();
               prepareBatch(matrix, font, rotation, z);
            }

            float gx = cursorX + glyph.bearingX * scale;
            float gy = baseline - glyph.bearingY * scale;
            float gw = glyph.width * scale;
            float gh = glyph.height * scale;
            int color = colors == null ? solidColor : colors[i % colors.length];
            glyphData.putFloat(gx).putFloat(gy).putFloat(gw).putFloat(gh);
            glyphData.putFloat(glyph.u0).putFloat(glyph.v0);
            glyphData.putFloat(glyph.u1 - glyph.u0).putFloat(glyph.v1 - glyph.v0);
            putColor(glyphData, color);
            batchGlyphCount++;
         }

         cursorX += glyph.advance * scale;
      }
   }

   private static void prepareBatch(Matrix4f matrix, MsdfFont font, float rotation, float z) {
      boolean incompatible = batchGlyphCount > 0
         && (batchFont != font || batchMatrix != matrix || Float.compare(batchRotation, rotation) != 0 || Float.compare(batchZ, z) != 0);
      if (incompatible) {
         flushBatch();
      }

      if (batchGlyphCount == 0) {
         batchFont = font;
         batchMatrix = matrix;
         batchRotation = rotation;
         batchZ = z;
         glyphData.clear();
         prepareMsdfData(matrix, font, -1, rotation, z);
      }
   }

   private static void flushBatch() {
      if (batchGlyphCount != 0 && batchFont != null) {
         glyphData.flip();
         render(batchFont, batchGlyphCount);
         resetBatch();
      }
   }

   private static void resetBatch() {
      batchFont = null;
      batchMatrix = null;
      batchRotation = 0.0F;
      batchZ = 0.0F;
      batchGlyphCount = 0;
      if (glyphData != null) {
         glyphData.clear();
      }
   }

   private static void render(MsdfFont font, int glyphCount) {
      GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(msdfBuffer.slice(), msdfData);
      encoder.writeToBuffer(glyphBuffer.slice(), glyphData);
      RenderPass pass = encoder.createRenderPass(() -> "Msdf2D", framebuffer.getColorAttachmentView(), OptionalInt.empty());

      try {
         pass.setPipeline(pipeline);
         pass.setUniform("MsdfData", msdfBuffer);
         pass.setUniform("GlyphData", glyphBuffer);
         pass.bindTexture("Sampler0", font.getTextureView(), sampler);
         pass.draw(0, glyphCount * 6);
      } catch (Throwable var9) {
         if (pass != null) {
            try {
               pass.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (pass != null) {
         pass.close();
      }
   }

   public static void shutdown() {
      if (msdfBuffer != null) {
         msdfBuffer.close();
         msdfBuffer = null;
      }

      if (glyphBuffer != null) {
         glyphBuffer.close();
         glyphBuffer = null;
      }

      if (msdfData != null) {
         MemoryUtil.memFree(msdfData);
         msdfData = null;
      }

      if (glyphData != null) {
         MemoryUtil.memFree(glyphData);
         glyphData = null;
      }

      pipeline = null;
      initialized = false;
      batching = false;
      resetBatch();
   }
}
