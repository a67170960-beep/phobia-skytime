package ruhack.phobia.util.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderSetup.OutlineMode;
import net.minecraft.client.render.RenderSetup.TextureSpec;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ClientPipelines {
   private static final Set<RenderLayer> CUSTOM_MODEL_OUTLINE_LAYERS = Collections.newSetFromMap(new IdentityHashMap<>());
   public static final Function<Identifier, RenderLayer> CUSTOM_MODEL_OUTLINE = Util.memoize(
      texture -> {
         RenderLayer layer = RenderLayer.of(
            "phobia_custom_model_outline",
            RenderSetup.builder(RenderPipelines.OUTLINE_CULL)
               .texture("Sampler0", texture)
               .outputTarget(OutputTarget.OUTLINE_TARGET)
               .outlineMode(OutlineMode.IS_OUTLINE)
               .build()
         );
         CUSTOM_MODEL_OUTLINE_LAYERS.add(layer);
         return layer;
      }
   );
   public static final RenderPipeline ROMB_ESP_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/wtex")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.LIGHTNING)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> ROMB_ESP = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(ROMB_ESP_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(1536).build();
      return RenderLayer.of("wtex", setup);
   });
   public static final RenderPipeline GHOSTS_ESP_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/wtex")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.LIGHTNING)
         .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> GHOSTS_ESP = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(GHOSTS_ESP_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(1536).build();
      return RenderLayer.of("wtex", setup);
   });
   public static final RenderPipeline CHAIN_ESP_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/wtex")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.LIGHTNING)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> CHAIN_ESP = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(CHAIN_ESP_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(1536).build();
      return RenderLayer.of("wtex", setup);
   });
   public static final RenderPipeline CRYSTAL_FILLED_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/crystal_filled")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final RenderLayer CRYSTAL_FILLED = RenderLayer.of(
      "crystal_filled", RenderSetup.builder(CRYSTAL_FILLED_PIPELINE).translucent().expectedBufferSize(8192).build()
   );
   public static final RenderPipeline CRYSTAL_GLOW_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/crystal_glow")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.LIGHTNING)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final RenderLayer CRYSTAL_GLOW = RenderLayer.of(
      "crystal_glow", RenderSetup.builder(CRYSTAL_GLOW_PIPELINE).translucent().expectedBufferSize(4096).build()
   );
   public static final RenderPipeline BLOOM_ESP_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/bloom_esp")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.LIGHTNING)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> BLOOM_ESP = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(BLOOM_ESP_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(2048).build();
      return RenderLayer.of("bloom_esp", setup);
   });
   public static final RenderPipeline CHINA_HAT_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/china_hat")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(true)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.TRIANGLE_FAN)
         .build()
   );
   public static final RenderLayer CHINA_HAT = RenderLayer.of(
      "china_hat", RenderSetup.builder(CHINA_HAT_PIPELINE).translucent().expectedBufferSize(8192).build()
   );
   public static final RenderPipeline CHINA_HAT_OUTLINE_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/china_hat_outline")
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(true)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINE_STRIP)
         .build()
   );
   public static final RenderLayer CHINA_HAT_OUTLINE = RenderLayer.of(
      "china_hat_outline", RenderSetup.builder(CHINA_HAT_OUTLINE_PIPELINE).translucent().expectedBufferSize(4096).build()
   );
   public static final RenderPipeline SHADER_HANDS_HALO_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/shader_hands_halo")
         .withVertexShader(Identifier.of("phobia", "3d/shaderhands_halo_vertex"))
         .withFragmentShader(Identifier.of("phobia", "3d/shaderhands_halo_fragment"))
         .withSampler("Sampler0")
         .withUniform("Globals", UniformType.UNIFORM_BUFFER)
         .withBlend(BlendFunction.LIGHTNING)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> SHADER_HANDS_HALO = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(SHADER_HANDS_HALO_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(1536).build();
      return RenderLayer.of("shader_hands_halo", setup);
   });
   public static final Function<Identifier, RenderLayer> SHADER_HANDS_TRAIL_HALO = Util.memoize(
      texture -> {
         RenderSetup setup = RenderSetup.builder(SHADER_HANDS_HALO_PIPELINE)
            .texture("Sampler0", texture)
            .outputTarget(ShaderHandsTrail.OUTPUT_TARGET)
            .translucent()
            .expectedBufferSize(1536)
            .build();
         return RenderLayer.of("shader_hands_trail_halo", setup);
      }
   );
   public static final RenderPipeline SHADER_HANDS_GRADIENT_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/shader_hands_gradient")
         .withVertexShader(Identifier.of("phobia", "3d/shaderhands_vertex"))
         .withFragmentShader(Identifier.of("phobia", "3d/shaderhands_gradient_fragment"))
         .withSampler("Sampler0")
         .withUniform("Globals", UniformType.UNIFORM_BUFFER)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(true)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> SHADER_HANDS_GRADIENT = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(SHADER_HANDS_GRADIENT_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(1536).build();
      return RenderLayer.of("shader_hands_gradient", setup);
   });
   public static final RenderPipeline SHADER_HANDS_ITEMGLOW_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/shader_hands_itemglow")
         .withVertexShader(Identifier.of("phobia", "3d/shaderhands_vertex"))
         .withFragmentShader(Identifier.of("phobia", "3d/shaderhands_itemglow_fragment"))
         .withSampler("Sampler0")
         .withUniform("Globals", UniformType.UNIFORM_BUFFER)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> SHADER_HANDS_ITEMGLOW = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(SHADER_HANDS_ITEMGLOW_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(1536).build();
      return RenderLayer.of("shader_hands_itemglow", setup);
   });
   public static final Function<Identifier, RenderLayer> SHADER_HANDS_TRAIL_ITEMGLOW = Util.memoize(
      texture -> {
         RenderSetup setup = RenderSetup.builder(SHADER_HANDS_ITEMGLOW_PIPELINE)
            .texture("Sampler0", texture)
            .outputTarget(ShaderHandsTrail.OUTPUT_TARGET)
            .translucent()
            .expectedBufferSize(1536)
            .build();
         return RenderLayer.of("shader_hands_trail_itemglow", setup);
      }
   );
   public static final RenderPipeline SHADER_HANDS_BAKED_ITEM_FILL_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/shader_hands_baked_item_fill")
         .withVertexShader(Identifier.of("phobia", "3d/shaderhands_baked_item_fill_vertex"))
         .withFragmentShader(Identifier.of("phobia", "3d/shaderhands_itemglow_fragment"))
         .withSampler("Sampler0")
         .withUniform("Globals", UniformType.UNIFORM_BUFFER)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> SHADER_HANDS_BAKED_ITEM_FILL = Util.memoize(
      texture -> {
         RenderSetup setup = RenderSetup.builder(SHADER_HANDS_BAKED_ITEM_FILL_PIPELINE)
            .texture("Sampler0", texture)
            .translucent()
            .expectedBufferSize(1536)
            .build();
         return RenderLayer.of("shader_hands_baked_item_fill", setup);
      }
   );
   public static final Function<Identifier, RenderLayer> SHADER_HANDS_TRAIL_BAKED_ITEM_FILL = Util.memoize(
      texture -> {
         RenderSetup setup = RenderSetup.builder(SHADER_HANDS_BAKED_ITEM_FILL_PIPELINE)
            .texture("Sampler0", texture)
            .outputTarget(ShaderHandsTrail.OUTPUT_TARGET)
            .translucent()
            .expectedBufferSize(1536)
            .build();
         return RenderLayer.of("shader_hands_trail_baked_item_fill", setup);
      }
   );
   public static final Function<Identifier, RenderLayer> CHAMS = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(SHADER_HANDS_GRADIENT_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(4096).build();
      return RenderLayer.of("chams", setup);
   });
   public static final RenderPipeline CHAMS_WALL_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/chams_wall")
         .withVertexShader(Identifier.of("phobia", "3d/shaderhands_vertex"))
         .withFragmentShader(Identifier.of("phobia", "3d/shaderhands_gradient_fragment"))
         .withSampler("Sampler0")
         .withUniform("Globals", UniformType.UNIFORM_BUFFER)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.GREATER_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> CHAMS_WALL = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(CHAMS_WALL_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(4096).build();
      return RenderLayer.of("chams_wall", setup);
   });
   private static final Map<RenderLayer, Optional<Identifier>> LAYER_TEXTURE_CACHE = new HashMap<>();
   public static final RenderPipeline WORLD_PARTICLES_COLOR_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
         .withLocation(Identifier.of("rich", "world_particles_color"))
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withBlend(BlendFunction.LIGHTNING)
         .build()
   );
   public static final RenderLayer WORLD_PARTICLES_QUADS = RenderLayer.of(
      "world_particles_cube", RenderSetup.builder(WORLD_PARTICLES_COLOR_PIPELINE).translucent().expectedBufferSize(2048).build()
   );
   public static final RenderPipeline WORLD_PARTICLES_LINES_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
         .withLocation(Identifier.of("rich", "world_particles_lines"))
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withBlend(BlendFunction.LIGHTNING)
         .build()
   );
   public static final RenderLayer WORLD_PARTICLES_LINES = RenderLayer.of(
      "world_particles_lines", RenderSetup.builder(WORLD_PARTICLES_LINES_PIPELINE).translucent().expectedBufferSize(2048).build()
   );
   public static final RenderPipeline WORLD_PARTICLES_GLOW_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_TEX_COLOR_SNIPPET})
         .withLocation(Identifier.of("rich", "world_particles_glow"))
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withBlend(BlendFunction.LIGHTNING)
         .withSampler("Sampler0")
         .build()
   );
   public static final Function<Identifier, RenderLayer> WORLD_PARTICLES_GLOW = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(WORLD_PARTICLES_GLOW_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(2048).build();
      return RenderLayer.of("world_particles_glow", setup);
   });
   public static final RenderPipeline GUI_ARROW_BLEND_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
         .withLocation("pipeline/gui_arrow_blend")
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(BlendFunction.LIGHTNING)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withCull(false)
         .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
         .build()
   );
   public static final Function<Identifier, RenderLayer> GUI_ARROW_BLEND = Util.memoize(texture -> {
      RenderSetup setup = RenderSetup.builder(GUI_ARROW_BLEND_PIPELINE).texture("Sampler0", texture).translucent().expectedBufferSize(256).build();
      return RenderLayer.of("gui_arrow_blend", setup);
   });

   public static boolean isCustomModelOutline(RenderLayer layer) {
      return CUSTOM_MODEL_OUTLINE_LAYERS.contains(layer);
   }

   public static Identifier layerTextureOf(RenderLayer layer) {
      return LAYER_TEXTURE_CACHE.computeIfAbsent(layer, l -> {
         TextureSpec spec = (TextureSpec)l.renderSetup.textures.get("Sampler0");
         return spec != null ? Optional.of(spec.location) : Optional.empty();
      }).orElse(null);
   }
}
