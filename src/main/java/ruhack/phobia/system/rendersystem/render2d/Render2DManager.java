package ruhack.phobia.system.rendersystem.render2d;

import java.util.ArrayList;
import java.util.List;
import ruhack.phobia.system.rendersystem.render2d.arc.Arc2D;
import ruhack.phobia.system.rendersystem.render2d.arc.ArcOutline2D;
import ruhack.phobia.system.rendersystem.render2d.blur.Kawase2D;
import ruhack.phobia.system.rendersystem.render2d.colored.ColorPicker2D;
import ruhack.phobia.system.rendersystem.render2d.colored.Hue2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfGlow2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.rendersystem.render2d.gif.Gif2D;
import ruhack.phobia.system.rendersystem.render2d.glass.LiquidGlass2D;
import ruhack.phobia.system.rendersystem.render2d.glow.Glow2D;
import ruhack.phobia.system.rendersystem.render2d.line.TaperedLine2D;
import ruhack.phobia.system.rendersystem.render2d.outline.Outline2D;
import ruhack.phobia.system.rendersystem.render2d.rectangle.Rectangle2D;
import ruhack.phobia.system.rendersystem.render2d.ring.SegmentedRing2D;
import ruhack.phobia.system.rendersystem.render2d.saturation.Saturation2D;
import ruhack.phobia.system.rendersystem.render2d.texture.Texture2D;

public class Render2DManager {
   private static final List<Runnable> INITIALIZERS = new ArrayList<>();
   private static boolean initialized = false;

   public static void init() {
      if (!initialized) {
         Texture2D.shutdown();
         Gif2D.shutdown();
         Msdf2D.shutdown();
         Arc2D.shutdown();
         LiquidGlass2D.shutdown();
         SegmentedRing2D.shutdown();
         INITIALIZERS.forEach(Runnable::run);
         initialized = true;
      }
   }

   public static boolean isInitialized() {
      return initialized;
   }

   static {
      INITIALIZERS.add(Rectangle2D::init);
      INITIALIZERS.add(Outline2D::init);
      INITIALIZERS.add(Kawase2D::init);
      INITIALIZERS.add(Arc2D::init);
      INITIALIZERS.add(ArcOutline2D::init);
      INITIALIZERS.add(Texture2D::init);
      INITIALIZERS.add(Gif2D::init);
      INITIALIZERS.add(Glow2D::init);
      INITIALIZERS.add(Msdf2D::init);
      INITIALIZERS.add(MsdfGlow2D::init);
      INITIALIZERS.add(MsdfManager::init);
      INITIALIZERS.add(LiquidGlass2D::init);
      INITIALIZERS.add(ColorPicker2D::init);
      INITIALIZERS.add(Hue2D::init);
      INITIALIZERS.add(TaperedLine2D::init);
      INITIALIZERS.add(Saturation2D::init);
      INITIALIZERS.add(SegmentedRing2D::init);
   }
}
