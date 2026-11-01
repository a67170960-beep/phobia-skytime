package ruhack.phobia.util.math;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3d;
import ruhack.phobia.IMinecraft;

public final class MathUtils implements IMinecraft {
   public static double PI2 = Math.PI * 2;
   private static float contextAlpha = 1.0F;

   public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public static float randomLerp(float min, float max) {
      return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
   }

   public static float clamp(float num, float min, float max) {
      return num < min ? min : Math.min(num, max);
   }

   public static double computeGcd() {
      return Math.pow((Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
   }

   public static int getRandom(int min, int max) {
      return (int)getRandom(min, max + 1.0F);
   }

   public static float getRandom(float min, float max) {
      return (float)getRandom((double)min, (double)max);
   }

   public static double getRandom(double min, double max) {
      if (min == max) {
         return min;
      }

      if (min > max) {
         double d = min;
         min = max;
         max = d;
      }

      return ThreadLocalRandom.current().nextDouble(min, max);
   }

   public static void scale(Matrix3x2fStack stack, float x, float y, float scaleX, float scaleY, Runnable data) {
      float sumScale = scaleX * scaleY;
      if (sumScale != 1.0F && sumScale > 0.0F) {
         float prevAlpha = contextAlpha;
         contextAlpha = sumScale;
         stack.pushMatrix();
         stack.translate(x, y);
         stack.scale(scaleX, scaleY);
         stack.translate(-x, -y);
         data.run();
         stack.popMatrix();
         contextAlpha = prevAlpha;
      } else if (sumScale >= 1.0F) {
         data.run();
      }
   }

   public static float textScrolling(float textWidth) {
      int speed = (int)(textWidth * 75.0F);
      return (float)MathHelper.clamp(System.currentTimeMillis() % speed * Math.PI / speed, 0.0, 1.0) * textWidth;
   }

   public static double round(double num, double increment) {
      double rounded = Math.round(num / increment) * increment;
      return Math.round(rounded * 100.0) / 100.0;
   }

   public static int floorNearestMulN(int x, int n) {
      return n * (int)Math.floor((double)x / n);
   }

   public static int getRed(int hex) {
      return hex >> 16 & 0xFF;
   }

   public static int getGreen(int hex) {
      return hex >> 8 & 0xFF;
   }

   public static int getBlue(int hex) {
      return hex & 0xFF;
   }

   public static int getAlpha(int hex) {
      return hex >> 24 & 0xFF;
   }

   public static int applyOpacity(int color, float opacity) {
      return ColorHelper.getArgb((int)(getAlpha(color) * opacity / 255.0F), getRed(color), getGreen(color), getBlue(color));
   }

   public static int applyContextAlpha(int color) {
      int a = (int)(getAlpha(color) * contextAlpha);
      return ColorHelper.getArgb(a, getRed(color), getGreen(color), getBlue(color));
   }

   public static Vec3d cosSin(int i, int size, double width) {
      int index = Math.min(i, size);
      float cos = (float)(Math.cos(index * PI2 / size) * width);
      float sin = (float)(-Math.sin(index * PI2 / size) * width);
      return new Vec3d(cos, 0.0, sin);
   }

   public static double absSinAnimation(double input) {
      return Math.abs(1.0 + Math.sin(input)) / 2.0;
   }

   public static Vector3d interpolate(Vector3d prevPos, Vector3d pos) {
      return new Vector3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
   }

   public static float interpolate(float prev, float to, float value) {
      return prev + (to - prev) * value;
   }

   public static Vec3d interpolate(Vec3d prevPos, Vec3d pos) {
      return new Vec3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
   }

   public static Vec3d interpolate(Entity entity) {
      return entity == null
         ? Vec3d.ZERO
         : new Vec3d(interpolate(entity.lastX, entity.getX()), interpolate(entity.lastY, entity.getY()), interpolate(entity.lastZ, entity.getZ()));
   }

   public static float interpolate(float prev, float orig) {
      return MathHelper.lerp(tickCounter.getTickProgress(false), prev, orig);
   }

   public static double interpolate(double prev, double orig) {
      return MathHelper.lerp(tickCounter.getTickProgress(false), prev, orig);
   }

   public static float interpolateSmooth(double smooth, float prev, float orig) {
      return (float)MathHelper.lerp(tickCounter.getFixedDeltaTicks() / smooth, prev, orig);
   }

   @Generated
   private MathUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   @Generated
   public static float getContextAlpha() {
      return contextAlpha;
   }
}
