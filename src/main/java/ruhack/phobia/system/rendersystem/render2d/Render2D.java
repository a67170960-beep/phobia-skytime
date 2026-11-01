package ruhack.phobia.system.rendersystem.render2d;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ruhack.phobia.system.rendersystem.render2d.arc.Arc2D;
import ruhack.phobia.system.rendersystem.render2d.arc.ArcOutline2D;
import ruhack.phobia.system.rendersystem.render2d.blur.Kawase2D;
import ruhack.phobia.system.rendersystem.render2d.colored.ColorPicker2D;
import ruhack.phobia.system.rendersystem.render2d.colored.Hue2D;
import ruhack.phobia.system.rendersystem.render2d.gif.Gif2D;
import ruhack.phobia.system.rendersystem.render2d.glass.LiquidGlass2D;
import ruhack.phobia.system.rendersystem.render2d.glow.Glow2D;
import ruhack.phobia.system.rendersystem.render2d.glow.GlowOutline2D;
import ruhack.phobia.system.rendersystem.render2d.line.TaperedLine2D;
import ruhack.phobia.system.rendersystem.render2d.outline.Outline2D;
import ruhack.phobia.system.rendersystem.render2d.rectangle.Rectangle2D;
import ruhack.phobia.system.rendersystem.render2d.texture.Texture2D;
import ruhack.phobia.system.rendersystem.render2d.zippy.Zippy2D;
import ruhack.phobia.system.screensystem.AutoSwapRingScreen;
import ruhack.phobia.system.screensystem.BuffRingScreen;
import ruhack.phobia.system.screensystem.clickgui.ClickGui;

public class Render2D {
   private static final List<Runnable> OVERRIDE_TASKS = new ArrayList<>();
   private static final float Z_OVERRIDE = 0.0F;
   private static final int FIXED_GUI_SCALE = 2;
   private static float contextScale = 1.0F;
   private static Matrix4f cachedProjection;
   private static int cachedProjectionWidth = -1;
   private static int cachedProjectionHeight = -1;
   private static float cachedProjectionScale = -1.0F;

   public static int getFixedScaledWidth() {
      Window window = MinecraftClient.getInstance().getWindow();
      return (int)Math.ceil(window.getWidth() / 2.0);
   }

   public static int getFixedScaledHeight() {
      Window window = MinecraftClient.getInstance().getWindow();
      return (int)Math.ceil(window.getHeight() / 2.0);
   }

   public static boolean hasUsableViewport() {
      Window window = MinecraftClient.getInstance().getWindow();
      return window.getWidth() >= 320 && window.getHeight() >= 240;
   }

   public static float getScaleFactor() {
      Window window = MinecraftClient.getInstance().getWindow();
      int currentScale = window.getScaleFactor();
      return currentScale / 2.0F;
   }

   public static float convertX(float x) {
      return x * getScaleFactor();
   }

   public static float convertY(float y) {
      return y * getScaleFactor();
   }

   public static float convertSize(float size) {
      return size * getScaleFactor();
   }

   public static Matrix4f createProjection() {
      float scale = Math.max(0.01F, contextScale);
      int width = getFixedScaledWidth();
      int height = getFixedScaledHeight();
      if (cachedProjection == null || cachedProjectionWidth != width || cachedProjectionHeight != height || Float.compare(cachedProjectionScale, scale) != 0) {
         cachedProjection = new Matrix4f().ortho(0.0F, width / scale, height / scale, 0.0F, -1000.0F, 1000.0F);
         cachedProjectionWidth = width;
         cachedProjectionHeight = height;
         cachedProjectionScale = scale;
      }

      return cachedProjection;
   }

   public static float getContextScale() {
      return contextScale;
   }

   public static void withContextScale(float scale, Runnable runnable) {
      float previousScale = contextScale;
      contextScale = Math.max(0.01F, scale);

      try {
         runnable.run();
      } finally {
         contextScale = previousScale;
      }
   }

   public static boolean hasScreenOpen() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client.currentScreen != null
         && !(client.currentScreen instanceof ChatScreen)
         && !(client.currentScreen instanceof HandledScreen)
         && !(client.currentScreen instanceof ClickGui)
         && !(client.currentScreen instanceof AutoSwapRingScreen)
         && !(client.currentScreen instanceof BuffRingScreen);
   }

   public static void rect(DrawContext context, float x, float y, float width, float height, float radius, int color, boolean overrideContext) {
      rect(createProjection(), x, y, width, height, radius, color, overrideContext);
   }

   public static void rect(DrawContext context, float x, float y, float width, float height, float radius, boolean overrideContext, int... colors) {
      rect(createProjection(), x, y, width, height, radius, radius, radius, radius, overrideContext, colors);
   }

   public static void rect(
      DrawContext context, float x, float y, float width, float height, float tl, float tr, float br, float bl, boolean overrideContext, int... colors
   ) {
      rect(createProjection(), x, y, width, height, tl, tr, br, bl, overrideContext, colors);
   }

   public static void rect(Matrix4f matrix, float x, float y, float width, float height, float radius, int color, boolean overrideContext) {
      rect(matrix, x, y, width, height, radius, radius, radius, radius, overrideContext, color);
   }

   public static void rect(
      Matrix4f matrix, float x, float y, float width, float height, float tl, float tr, float br, float bl, boolean overrideContext, int... colors
   ) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> Rectangle2D.draw(matrix, x, y, width, height, tl, tr, br, bl, 0.0F, colors));
      } else {
         Rectangle2D.draw(matrix, x, y, width, height, tl, tr, br, bl, 0.0F, colors);
      }
   }

   public static void rect(Matrix4f matrix, float x, float y, float width, float height, float radius, boolean overrideContext, int... colors) {
      rect(matrix, x, y, width, height, radius, radius, radius, radius, overrideContext, colors);
   }

   public static void zippy(DrawContext context, float x, float y, float width, float height, float radius, int color, boolean overrideContext) {
      zippy(createProjection(), x, y, width, height, radius, radius, radius, radius, color, overrideContext);
   }

   public static void zippy(
      DrawContext context, float x, float y, float width, float height, float tl, float tr, float br, float bl, int color, boolean overrideContext
   ) {
      zippy(createProjection(), x, y, width, height, tl, tr, br, bl, color, overrideContext);
   }

   public static void zippy(
      Matrix4f matrix, float x, float y, float width, float height, float tl, float tr, float br, float bl, int color, boolean overrideContext
   ) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> Zippy2D.draw(matrix, x, y, width, height, tl, tr, br, bl, 0.0F, color));
      } else {
         Zippy2D.draw(matrix, x, y, width, height, tl, tr, br, bl, 0.0F, color);
      }
   }

   public static void outline(
      DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color, boolean overrideContext
   ) {
      outline(createProjection(), x, y, width, height, radius, radius, radius, radius, thickness, color, overrideContext);
   }

   public static void outline(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      float tl,
      float tr,
      float br,
      float bl,
      float thickness,
      int color,
      boolean overrideContext
   ) {
      outline(createProjection(), x, y, width, height, tl, tr, br, bl, thickness, color, overrideContext);
   }

   public static void outline(
      DrawContext context, float x, float y, float width, float height, float radius, float thickness, boolean overrideContext, int... colors
   ) {
      outline(createProjection(), x, y, width, height, radius, radius, radius, radius, thickness, overrideContext, colors);
   }

   public static void outline(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      float tl,
      float tr,
      float br,
      float bl,
      float thickness,
      boolean overrideContext,
      int... colors
   ) {
      outline(createProjection(), x, y, width, height, tl, tr, br, bl, thickness, overrideContext, colors);
   }

   public static void outline(Matrix4f matrix, float x, float y, float width, float height, float radius, float thickness, int color, boolean overrideContext) {
      outline(matrix, x, y, width, height, radius, radius, radius, radius, thickness, color, overrideContext);
   }

   public static void outline(
      Matrix4f matrix, float x, float y, float width, float height, float tl, float tr, float br, float bl, float thickness, int color, boolean overrideContext
   ) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> Outline2D.draw(matrix, x, y, width, height, tl, tr, br, bl, thickness, 0.0F, color));
      } else {
         Outline2D.draw(matrix, x, y, width, height, tl, tr, br, bl, thickness, 0.0F, color);
      }
   }

   public static void outline(
      Matrix4f matrix, float x, float y, float width, float height, float radius, float thickness, boolean overrideContext, int... colors
   ) {
      outline(matrix, x, y, width, height, radius, radius, radius, radius, thickness, overrideContext, colors);
   }

   public static void outline(
      Matrix4f matrix,
      float x,
      float y,
      float width,
      float height,
      float tl,
      float tr,
      float br,
      float bl,
      float thickness,
      boolean overrideContext,
      int... colors
   ) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> Outline2D.draw(matrix, x, y, width, height, tl, tr, br, bl, thickness, 0.0F, colors));
      } else {
         Outline2D.draw(matrix, x, y, width, height, tl, tr, br, bl, thickness, 0.0F, colors);
      }
   }

   public static void blur(DrawContext context, float x, float y, float width, float height, float radius, float strength, boolean overrideContext) {
      blur(context, x, y, width, height, radius, radius, radius, radius, strength, overrideContext);
   }

   public static void blur(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      float topLeft,
      float topRight,
      float bottomRight,
      float bottomLeft,
      float strength,
      boolean overrideContext
   ) {
      Matrix4f projection = createProjection();
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> Kawase2D.draw(projection, x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, strength, 0.0F));
      } else {
         Kawase2D.draw(projection, x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, strength, 0.0F);
      }
   }

   public static void beginBlurFrame() {
      Kawase2D.beginFrame();
   }

   public static void endBlurFrame() {
      Kawase2D.endFrame();
   }

   public static void arc(DrawContext context, float x, float y, float size, float thickness, float degree, float rotation, int color, boolean overrideContext) {
      arc(createProjection(), x, y, size, thickness, degree, rotation, color, overrideContext);
   }

   public static void arc(
      DrawContext context, float x, float y, float size, float thickness, float degree, float rotation, boolean overrideContext, int... colors
   ) {
      arc(createProjection(), x, y, size, thickness, degree, rotation, overrideContext, colors);
   }

   public static void arc(Matrix4f matrix, float x, float y, float size, float thickness, float degree, float rotation, int color, boolean overrideContext) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> Arc2D.draw(matrix, x, y, size, thickness, degree, rotation, 0.0F, color));
      } else {
         Arc2D.draw(matrix, x, y, size, thickness, degree, rotation, 0.0F, color);
      }
   }

   public static void arc(Matrix4f matrix, float x, float y, float size, float thickness, float degree, float rotation, boolean overrideContext, int... colors) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> Arc2D.draw(matrix, x, y, size, thickness, degree, rotation, 0.0F, colors));
      } else {
         Arc2D.draw(matrix, x, y, size, thickness, degree, rotation, 0.0F, colors);
      }
   }

   public static void arcOutline(
      DrawContext context,
      float x,
      float y,
      float size,
      float arcThickness,
      float degree,
      float rotation,
      float outlineThickness,
      int fillColor,
      int outlineColor,
      boolean overrideContext
   ) {
      arcOutline(createProjection(), x, y, size, arcThickness, degree, rotation, outlineThickness, fillColor, outlineColor, overrideContext);
   }

   public static void arcOutline(
      Matrix4f matrix,
      float x,
      float y,
      float size,
      float arcThickness,
      float degree,
      float rotation,
      float outlineThickness,
      int fillColor,
      int outlineColor,
      boolean overrideContext
   ) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> ArcOutline2D.draw(matrix, x, y, size, arcThickness, degree, rotation, outlineThickness, fillColor, outlineColor, 0.0F));
      } else {
         ArcOutline2D.draw(matrix, x, y, size, arcThickness, degree, rotation, outlineThickness, fillColor, outlineColor, 0.0F);
      }
   }

   public static void image(DrawContext context, float x, float y, float size, Identifier texture, int color, float radius, boolean overrideContext) {
      image(createProjection(), x, y, size, texture, color, radius, overrideContext);
   }

   public static void imageMask(DrawContext context, float x, float y, float size, Identifier texture, int color, float radius, boolean overrideContext) {
      AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(texture);
      if (tex != null) {
         Matrix4f matrix = createProjection();
         if (overrideContext) {
            OVERRIDE_TASKS.add(() -> Texture2D.drawMask(matrix, x, y, size, tex.getGlTextureView(), color, radius, 0.0F));
         } else {
            Texture2D.drawMask(matrix, x, y, size, tex.getGlTextureView(), color, radius, 0.0F);
         }
      }
   }

   public static void image(Matrix4f matrix, float x, float y, float size, Identifier texture, int color, float radius, boolean overrideContext) {
      MinecraftClient client = MinecraftClient.getInstance();
      AbstractTexture tex = client.getTextureManager().getTexture(texture);
      if (tex != null) {
         if (overrideContext) {
            OVERRIDE_TASKS.add(() -> Texture2D.draw(matrix, x, y, size, tex.getGlTextureView(), color, radius, 0.0F));
            return;
         }

         Texture2D.draw(matrix, x, y, size, tex.getGlTextureView(), color, radius, 0.0F);
      }
   }

   public static void imageRegion(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      Identifier texture,
      int color,
      float radius,
      float u0,
      float v0,
      float u1,
      float v1,
      boolean nearest,
      boolean overrideContext
   ) {
      MinecraftClient client = MinecraftClient.getInstance();
      AbstractTexture tex = client.getTextureManager().getTexture(texture);
      if (tex != null) {
         Matrix4f matrix = createProjection();
         if (overrideContext) {
            OVERRIDE_TASKS.add(() -> Texture2D.drawRegion(matrix, x, y, width, height, tex.getGlTextureView(), color, radius, 0.0F, u0, v0, u1, v1, nearest));
         } else {
            Texture2D.drawRegion(matrix, x, y, width, height, tex.getGlTextureView(), color, radius, 0.0F, u0, v0, u1, v1, nearest);
         }
      }
   }

   public static void gif(DrawContext context, float x, float y, float size, String path, int color, float radius, boolean overrideContext) {
      gif(createProjection(), x, y, size, path, color, radius, overrideContext);
   }

   public static void gif(Matrix4f matrix, float x, float y, float size, String path, int color, float radius, boolean overrideContext) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> Gif2D.draw(matrix, x, y, size, path, color, radius, 0.0F));
      } else {
         Gif2D.draw(matrix, x, y, size, path, color, radius, 0.0F);
      }
   }

   public static void glow(DrawContext context, float x, float y, float size, int color, boolean overrideContext) {
      glow(createProjection(), x, y, size, color, overrideContext);
   }

   public static void glow(Matrix4f matrix, float x, float y, float size, int color, boolean overrideContext) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> Glow2D.draw(matrix, x, y, size, color, 0.0F));
      } else {
         Glow2D.draw(matrix, x, y, size, color, 0.0F);
      }
   }

   public static void glowOutline(float x, float y, float width, float height, float thickness, int color, float radius, float progress, float baseAlpha) {
      GlowOutline2D.draw(createProjection(), x, y, width, height, thickness, color, radius, progress, baseAlpha, 0.0F);
   }

   public static void glowOutline(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      float thickness,
      int color,
      float radius,
      float progress,
      float baseAlpha,
      boolean overrideContext
   ) {
      glowOutline(createProjection(), x, y, width, height, thickness, color, radius, progress, baseAlpha, overrideContext);
   }

   public static void glowOutline(
      Matrix4f matrix,
      float x,
      float y,
      float width,
      float height,
      float thickness,
      int color,
      float radius,
      float progress,
      float baseAlpha,
      boolean overrideContext
   ) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> GlowOutline2D.draw(matrix, x, y, width, height, thickness, color, radius, progress, baseAlpha, 0.0F));
      } else {
         GlowOutline2D.draw(matrix, x, y, width, height, thickness, color, radius, progress, baseAlpha, 0.0F);
      }
   }

   public static void liquidGlass(
      DrawContext context, float x, float y, float width, float height, float squirt, float power, float radius, int color, boolean overrideContext
   ) {
      Matrix4f projection = createProjection();
      float[] radii = new float[]{radius * squirt / 2.0F, radius * squirt / 2.0F, radius * squirt / 2.0F, radius * squirt / 2.0F};
      float globalAlpha = (color >> 24 & 0xFF) / 255.0F;
      float fresnelPower = height == 240.0F ? 100.0F : 50.0F;
      int fresnelColor = color | 0xFF000000;
      float baseAlpha = 1.0F;
      boolean fresnelInvert = true;
      float fresnelMix = 0.0F;
      float distortStrength = power;
      if (overrideContext) {
         OVERRIDE_TASKS.add(
            () -> LiquidGlass2D.draw(
               projection,
               x,
               y,
               width,
               height,
               radii,
               color,
               globalAlpha,
               fresnelPower,
               fresnelColor,
               baseAlpha,
               fresnelInvert,
               fresnelMix,
               distortStrength,
               squirt,
               0.0F
            )
         );
      } else {
         LiquidGlass2D.draw(
            projection,
            x,
            y,
            width,
            height,
            radii,
            color,
            globalAlpha,
            fresnelPower,
            fresnelColor,
            baseAlpha,
            fresnelInvert,
            fresnelMix,
            distortStrength,
            squirt,
            0.0F
         );
      }
   }

   public static void colorPicker(DrawContext context, float x, float y, float width, float height, float hue, float radius, boolean overrideContext) {
      colorPicker(context, x, y, width, height, hue, radius, overrideContext, 1.0F);
   }

   public static void colorPicker(
      DrawContext context, float x, float y, float width, float height, float hue, float radius, boolean overrideContext, float alpha
   ) {
      Matrix4f projection = createProjection();
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> ColorPicker2D.draw(projection, x, y, width, height, hue, radius, 0.0F, alpha));
      } else {
         ColorPicker2D.draw(projection, x, y, width, height, hue, radius, 0.0F, alpha);
      }
   }

   public static void hueBar(DrawContext context, float x, float y, float width, float height, float radius, boolean overrideContext) {
      hueBar(context, x, y, width, height, radius, overrideContext, 1.0F);
   }

   public static void hueBar(DrawContext context, float x, float y, float width, float height, float radius, boolean overrideContext, float alpha) {
      Matrix4f projection = createProjection();
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> Hue2D.draw(projection, x, y, width, height, radius, 0.0F, alpha));
      } else {
         Hue2D.draw(projection, x, y, width, height, radius, 0.0F, alpha);
      }
   }

   public static void taperedLine(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      float leftTaper,
      float rightTaper,
      float leftRadius,
      float rightRadius,
      int color,
      boolean overrideContext
   ) {
      taperedLine(context, x, y, width, height, leftTaper, rightTaper, leftRadius, rightRadius, color, overrideContext, 1.0F);
   }

   public static void taperedLine(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      float leftTaper,
      float rightTaper,
      float leftRadius,
      float rightRadius,
      int color,
      boolean overrideContext,
      float alpha
   ) {
      taperedLine(createProjection(), x, y, width, height, leftTaper, rightTaper, leftRadius, rightRadius, color, overrideContext, alpha);
   }

   public static void taperedLine(
      Matrix4f matrix,
      float x,
      float y,
      float width,
      float height,
      float leftTaper,
      float rightTaper,
      float leftRadius,
      float rightRadius,
      int color,
      boolean overrideContext
   ) {
      taperedLine(matrix, x, y, width, height, leftTaper, rightTaper, leftRadius, rightRadius, color, overrideContext, 1.0F);
   }

   public static void taperedLine(
      Matrix4f matrix,
      float x,
      float y,
      float width,
      float height,
      float leftTaper,
      float rightTaper,
      float leftRadius,
      float rightRadius,
      int color,
      boolean overrideContext,
      float alpha
   ) {
      if (overrideContext) {
         OVERRIDE_TASKS.add(() -> TaperedLine2D.draw(matrix, x, y, width, height, height, 0.0F, leftTaper, rightTaper, leftRadius, rightRadius, color, alpha));
      } else {
         TaperedLine2D.draw(matrix, x, y, width, height, height, 0.0F, leftTaper, rightTaper, leftRadius, rightRadius, color, alpha);
      }
   }

   public static void addOverrideTask(Runnable task) {
      OVERRIDE_TASKS.add(task);
   }

   public static void renderOverrides(DrawContext context) {
      if (!OVERRIDE_TASKS.isEmpty()) {
         OVERRIDE_TASKS.forEach(Runnable::run);
         OVERRIDE_TASKS.clear();
      }
   }

   public static void clearOverrideTasks() {
      OVERRIDE_TASKS.clear();
   }

   public static boolean hasOverrideTasks() {
      return !OVERRIDE_TASKS.isEmpty();
   }

   public static boolean isOverrideActive() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client.currentScreen == null
         || client.currentScreen instanceof ChatScreen
         || client.currentScreen instanceof AutoSwapRingScreen
         || client.currentScreen instanceof BuffRingScreen;
   }
}
