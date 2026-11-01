package ruhack.phobia.system.rendersystem.render2d.font.msdf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public class MsdfFont {
   private final String name;
   private final Map<Integer, MsdfGlyph> glyphs = new HashMap<>();
   private GpuTexture texture;
   private GpuTextureView textureView;
   private float lineHeight;
   private float ascender;
   private float descender;
   private float pxRange;
   private int atlasWidth;
   private int atlasHeight;
   private float emSize;
   private boolean loaded = false;

   public MsdfFont(String name) {
      this.name = name;
   }

   public void load(Identifier atlasId, Identifier jsonId) {
      if (!this.loaded) {
         try {
            this.loadAtlas(atlasId);
            this.loadMetrics(jsonId);
            this.loaded = true;
            System.out.println("[MSDF] Successfully loaded font: " + this.name);
         } catch (Exception e) {
            System.err.println("[MSDF] Failed to load font '" + this.name + "':");
            e.printStackTrace();
         }
      }
   }

   private void loadAtlas(Identifier id) throws Exception {
      Identifier resourceId = Identifier.of(id.getNamespace(), "fonts/" + id.getPath());

      try (InputStream is = ((Resource)MinecraftClient.getInstance().getResourceManager().getResource(resourceId).get()).getInputStream()) {
         NativeImage image = NativeImage.read(is);
         this.atlasWidth = image.getWidth();
         this.atlasHeight = image.getHeight();
         this.texture = RenderSystem.getDevice().createTexture(() -> "msdf:" + this.name, 5, TextureFormat.RGBA8, this.atlasWidth, this.atlasHeight, 1, 1);
         RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, image);
         this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
         image.close();
      }
   }

   private void loadMetrics(Identifier id) throws Exception {
      Identifier resourceId = Identifier.of(id.getNamespace(), "fonts/" + id.getPath());

      try (InputStream is = ((Resource)MinecraftClient.getInstance().getResourceManager().getResource(resourceId).get()).getInputStream()) {
         String json = new String(is.readAllBytes());
         JsonObject root = JsonParser.parseString(json).getAsJsonObject();
         JsonObject atlas = root.getAsJsonObject("atlas");
         this.pxRange = atlas.get("distanceRange").getAsFloat();
         boolean yOriginBottom = atlas.has("yOrigin") && "bottom".equals(atlas.get("yOrigin").getAsString());
         JsonObject metrics = root.getAsJsonObject("metrics");
         this.emSize = metrics.get("emSize").getAsFloat();
         this.lineHeight = metrics.get("lineHeight").getAsFloat();
         this.ascender = metrics.get("ascender").getAsFloat();
         this.descender = metrics.get("descender").getAsFloat();

         for (JsonElement el : root.getAsJsonArray("glyphs")) {
            JsonObject g = el.getAsJsonObject();
            int unicode = g.get("unicode").getAsInt();
            float advance = g.has("advance") ? g.get("advance").getAsFloat() : 0.0F;
            float pw = 0.0F;
            float ph = 0.0F;
            float u0 = 0.0F;
            float v0 = 0.0F;
            float u1 = 0.0F;
            float v1 = 0.0F;
            float bx = 0.0F;
            float by = 0.0F;
            if (g.has("atlasBounds")) {
               JsonObject ab = g.getAsJsonObject("atlasBounds");
               float left = ab.get("left").getAsFloat();
               float bottom = ab.get("bottom").getAsFloat();
               float right = ab.get("right").getAsFloat();
               float top = ab.get("top").getAsFloat();
               u0 = left / this.atlasWidth;
               u1 = right / this.atlasWidth;
               if (yOriginBottom) {
                  v0 = 1.0F - top / this.atlasHeight;
                  v1 = 1.0F - bottom / this.atlasHeight;
               } else {
                  v0 = top / this.atlasHeight;
                  v1 = bottom / this.atlasHeight;
               }
            }

            if (g.has("planeBounds")) {
               JsonObject pb = g.getAsJsonObject("planeBounds");
               float left = pb.get("left").getAsFloat();
               float bottom = pb.get("bottom").getAsFloat();
               float right = pb.get("right").getAsFloat();
               float top = pb.get("top").getAsFloat();
               bx = left;
               by = top;
               pw = right - left;
               ph = top - bottom;
            }

            this.glyphs.put(unicode, new MsdfGlyph(unicode, advance, 0.0F, 0.0F, pw, ph, u0, v0, u1, v1, bx, by));
         }

         System.out.println("[MSDF] Loaded font '" + this.name + "' with " + this.glyphs.size() + " glyphs");
      }
   }

   public MsdfGlyph getGlyph(int codepoint) {
      return this.glyphs.get(codepoint);
   }

   public GpuTextureView getTextureView() {
      return this.textureView;
   }

   public float getLineHeight() {
      return this.lineHeight;
   }

   public float getAscender() {
      return this.ascender;
   }

   public float getDescender() {
      return this.descender;
   }

   public float getPxRange() {
      return this.pxRange;
   }

   public float getEmSize() {
      return this.emSize;
   }

   public String getName() {
      return this.name;
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   public float getStringWidth(String text, float size) {
      float scale = size / this.emSize;
      float width = 0.0F;

      for (int i = 0; i < text.length(); i++) {
         MsdfGlyph glyph = this.glyphs.get(Integer.valueOf(text.charAt(i)));
         if (glyph != null) {
            width += glyph.advance * scale;
         }
      }

      return width;
   }

   public void shutdown() {
      if (this.textureView != null) {
         this.textureView.close();
         this.textureView = null;
      }

      if (this.texture != null) {
         this.texture.close();
         this.texture = null;
      }

      this.glyphs.clear();
      this.loaded = false;
   }
}
