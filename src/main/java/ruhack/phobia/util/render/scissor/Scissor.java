package ruhack.phobia.util.render.scissor;

import java.util.Stack;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import ruhack.phobia.system.rendersystem.render2d.Render2D;

public class Scissor {
   private static final Stack<Scissor.Rectangle> stack = new Stack<>();
   private static final Stack<Scissor.Rectangle> verticalStack = new Stack<>();

   public static void push(double x, double y, double w, double h) {
      push(x, y, w, h, false);
   }

   public static void push(double x, double y, double w, double h, boolean overrideContext) {
      if (overrideContext) {
         Render2D.addOverrideTask(() -> push(x, y, w, h, false));
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         float scale = (float)mc.getWindow().getWidth() / Render2D.getFixedScaledWidth() * Render2D.getContextScale();
         Scissor.Rectangle current = new Scissor.Rectangle(x, y, w, h);
         if (!stack.isEmpty()) {
            Scissor.Rectangle parent = stack.peek();
            double newX = Math.max(current.x, parent.x);
            double newY = Math.max(current.y, parent.y);
            double newW = Math.min(current.x + current.w, parent.x + parent.w) - newX;
            double newH = Math.min(current.y + current.h, parent.y + parent.h) - newY;
            current = new Scissor.Rectangle(newX, newY, Math.max(0.0, newW), Math.max(0.0, newH));
         }

         stack.push(current);
         apply(current, scale, mc);
      }
   }

   public static void pushVertical(double y, double h) {
      pushVertical(y, h, false);
   }

   public static void pushVertical(double y, double h, boolean overrideContext) {
      if (overrideContext) {
         Render2D.addOverrideTask(() -> pushVertical(y, h, false));
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         float scale = (float)mc.getWindow().getWidth() / Render2D.getFixedScaledWidth() * Render2D.getContextScale();
         double screenWidth = Render2D.getFixedScaledWidth();
         Scissor.Rectangle current = new Scissor.Rectangle(0.0, y, screenWidth, h);
         if (!verticalStack.isEmpty()) {
            Scissor.Rectangle parent = verticalStack.peek();
            double newY = Math.max(current.y, parent.y);
            double newH = Math.min(current.y + current.h, parent.y + parent.h) - newY;
            current = new Scissor.Rectangle(0.0, newY, screenWidth, Math.max(0.0, newH));
         }

         verticalStack.push(current);
         apply(current, scale, mc);
      }
   }

   private static void apply(Scissor.Rectangle rect, float scale, MinecraftClient mc) {
      int scissorX = (int)(rect.x * scale);
      int scissorY = (int)(mc.getWindow().getHeight() - (rect.y + rect.h) * scale);
      int scissorW = (int)(rect.w * scale);
      int scissorH = (int)(rect.h * scale);
      GL11.glEnable(3089);
      GL11.glScissor(scissorX, scissorY, Math.max(0, scissorW), Math.max(0, scissorH));
   }

   public static void pop() {
      pop(false);
   }

   public static void pop(boolean overrideContext) {
      if (overrideContext) {
         Render2D.addOverrideTask(() -> pop(false));
      } else {
         if (!stack.isEmpty()) {
            stack.pop();
         }

         if (stack.isEmpty()) {
            GL11.glDisable(3089);
         } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            float scale = (float)mc.getWindow().getWidth() / Render2D.getFixedScaledWidth() * Render2D.getContextScale();
            apply(stack.peek(), scale, mc);
         }
      }
   }

   public static void popVertical() {
      popVertical(false);
   }

   public static void popVertical(boolean overrideContext) {
      if (overrideContext) {
         Render2D.addOverrideTask(() -> popVertical(false));
      } else {
         if (!verticalStack.isEmpty()) {
            verticalStack.pop();
         }

         if (verticalStack.isEmpty()) {
            GL11.glDisable(3089);
         } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            float scale = (float)mc.getWindow().getWidth() / Render2D.getFixedScaledWidth() * Render2D.getContextScale();
            apply(verticalStack.peek(), scale, mc);
         }
      }
   }

   private record Rectangle(double x, double y, double w, double h) {
   }
}
