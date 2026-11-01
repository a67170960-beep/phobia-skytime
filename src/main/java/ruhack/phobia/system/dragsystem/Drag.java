package ruhack.phobia.system.dragsystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.configsystem.impl.drag.DragConfig;
import ruhack.phobia.system.modulesystem.impl.display.Hud;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.util.animations.SweepAnim;
import ruhack.phobia.util.colorschem.ColorUtil;

public class Drag {
   private static final float OUTLINE_OFFSET = 3.0F;
   private static final float OUTLINE_THICKNESS = 1.0F;
   private static final int OUTLINE_COLOR = ColorUtil.rgba(255, 255, 255, 255);
   private static final Set<String> EXCLUDED_ELEMENTS = Set.of("Watermark", "Logotype", "Info");
   private static HudElement draggingElement;
   private static int startX;
   private static int startY;
   private static final Map<HudElement, SweepAnim> sweepAnimations = new HashMap<>();
   private static final Map<HudElement, Boolean> wasHovered = new HashMap<>();

   public static void onDraw(DrawContext context, int mouseX, int mouseY, float delta, boolean isChatScreen) {
      HudManager hudManager = getHudManager();
      if (hudManager != null) {
         Hud hud = Hud.getInstance();
         if (hud != null && hud.isState()) {
            if (!isChatScreen) {
               if (draggingElement != null) {
                  DragConfig.getInstance().save();
                  draggingElement = null;
               }

               sweepAnimations.clear();
               wasHovered.clear();
            }

            int hudMouseX = (int)Math.round(hudManager.toHudCoordinate(mouseX));
            int hudMouseY = (int)Math.round(hudManager.toHudCoordinate(mouseY));
            if (isChatScreen && draggingElement != null) {
               draggingElement.setX(hudMouseX - startX);
               draggingElement.setY(hudMouseY - startY);
               hudManager.constrainToViewport(draggingElement);
            }

            hudManager.render(context, delta, mouseX, mouseY);
            if (isChatScreen) {
               Render2D.withContextScale(hudManager.getHudScale(), () -> {
                  for (HudElement element : hudManager.getEnabledElements()) {
                     if (!element.visible()) {
                        sweepAnimations.remove(element);
                        wasHovered.remove(element);
                     } else if (!EXCLUDED_ELEMENTS.contains(element.getName())) {
                        boolean isHovered = isHovered(element, hudMouseX, hudMouseY);
                        boolean previouslyHovered = wasHovered.getOrDefault(element, false);
                        float rounding = element.getRoundingRadius();
                        float offset = 3.0F;
                        float outlineX = element.getX() - offset;
                        float outlineY = element.getY() - offset;
                        float outlineWidth = element.getWidth() + offset * 2.0F;
                        float outlineHeight = element.getHeight() + offset * 2.0F;
                        float outlineRounding = Math.max(0.0F, rounding + offset);
                        SweepAnim anim = sweepAnimations.computeIfAbsent(element, e -> new SweepAnim(0.05F));
                        if (isHovered && !previouslyHovered) {
                           anim.start();
                        } else if (!isHovered && previouslyHovered) {
                           anim.reset();
                        }

                        wasHovered.put(element, isHovered);
                        anim.update();
                        float progress = anim.getProgress();
                        if (isHovered || anim.isActive()) {
                           float baseAlpha = 0.3F;
                           Render2D.glowOutline(outlineX, outlineY, outlineWidth, outlineHeight, 1.0F, OUTLINE_COLOR, outlineRounding, progress, baseAlpha);
                        }

                        if (!isHovered && anim.isCompleted()) {
                           sweepAnimations.remove(element);
                           wasHovered.remove(element);
                        }
                     }
                  }
               });
            }
         }
      }
   }

   public static void onMouseClick(Click click) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.currentScreen instanceof ChatScreen) {
         if (click.button() == 0) {
            HudManager hudManager = getHudManager();
            if (hudManager == null) {
               return;
            }

            double mouseX = hudManager.toHudCoordinate(click.x());
            double mouseY = hudManager.toHudCoordinate(click.y());
            HudElement element = hudManager.getElementAt(click.x(), click.y());
            if (element != null && element instanceof AbstractHudElement abstractElement && abstractElement.isDraggable()) {
               draggingElement = element;
               startX = (int)mouseX - element.getX();
               startY = (int)mouseY - element.getY();
            }
         }
      }
   }

   public static void onMouseRelease(Click click) {
      if (click.button() == 0 && draggingElement != null) {
         DragConfig.getInstance().save();
         draggingElement = null;
      }
   }

   public static void resetDragging() {
      if (draggingElement != null) {
         DragConfig.getInstance().save();
         draggingElement = null;
      }

      sweepAnimations.clear();
      wasHovered.clear();
   }

   public static boolean isDragging() {
      return draggingElement != null;
   }

   private static boolean isHovered(HudElement element, double mouseX, double mouseY) {
      int x = element.getX();
      int y = element.getY();
      int width = element.getWidth();
      int height = element.getHeight();
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private static HudManager getHudManager() {
      if (Initialization.getInstance() == null) {
         return null;
      } else {
         return Initialization.getInstance().getManager() == null ? null : Initialization.getInstance().getManager().getHudManager();
      }
   }

   public static void tick() {
      HudManager hudManager = getHudManager();
      if (hudManager != null) {
         hudManager.tick();
      }
   }
}
