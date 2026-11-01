package ruhack.phobia.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import ruhack.phobia.system.rendersystem.render2d.Render2D;

public final class WorldToScreen {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static Matrix4f viewProjection;
   private static Vec3d cameraPos = Vec3d.ZERO;
   private static float tickDelta;
   private static final Vector4f CLIP = new Vector4f();

   private WorldToScreen() {
   }

   public static void update(Matrix4f projection, Matrix4f view, float frameTickDelta) {
      viewProjection = new Matrix4f(projection).mul(view);
      cameraPos = MC.gameRenderer.getCamera().getCameraPos();
      tickDelta = frameTickDelta;
   }

   public static float getTickDelta() {
      return tickDelta;
   }

   public static Vector2f project(Vec3d worldPos) {
      Vector2f result = new Vector2f();
      return project(worldPos.x, worldPos.y, worldPos.z, result) ? result : null;
   }

   public static boolean project(double x, double y, double z, Vector2f result) {
      if (viewProjection != null && Render2D.hasUsableViewport()) {
         CLIP.set((float)(x - cameraPos.x), (float)(y - cameraPos.y), (float)(z - cameraPos.z), 1.0F).mul(viewProjection);
         if (CLIP.w <= 0.01F) {
            return false;
         } else {
            float ndcX = CLIP.x / CLIP.w;
            float ndcY = CLIP.y / CLIP.w;
            if (Float.isFinite(ndcX) && Float.isFinite(ndcY)) {
               result.set((ndcX * 0.5F + 0.5F) * Render2D.getFixedScaledWidth(), (0.5F - ndcY * 0.5F) * Render2D.getFixedScaledHeight());
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }
}
