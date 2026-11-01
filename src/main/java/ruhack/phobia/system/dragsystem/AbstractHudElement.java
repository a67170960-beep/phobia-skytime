package ruhack.phobia.system.dragsystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import ruhack.phobia.util.animations.Animation;
import ruhack.phobia.util.animations.Decelerate;
import ruhack.phobia.util.animations.Direction;

public abstract class AbstractHudElement implements HudElement {
   protected int x;
   protected int y;
   protected int width;
   protected int height;
   protected String name;
   protected boolean enabled = true;
   protected boolean draggable = true;
   protected final MinecraftClient mc = MinecraftClient.getInstance();
   protected final Animation scaleAnimation = new Decelerate().setMs(300).setValue(1.0);
   protected float lastTickDelta = 0.0F;

   public AbstractHudElement(String name, int x, int y, int width, int height, boolean draggable) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.draggable = draggable;
   }

   @Override
   public void render(DrawContext context, float tickDelta) {
      if (this.visible()) {
         this.lastTickDelta = tickDelta;
         this.scaleAnimation.update();
         int alpha = (int)(this.scaleAnimation.getOutput().floatValue() * 255.0F);
         if (alpha > 0) {
            this.drawDraggable(context, alpha);
         }
      }
   }

   public abstract void drawDraggable(DrawContext var1, int var2);

   @Override
   public void tick() {
   }

   @Override
   public boolean visible() {
      return true;
   }

   public void startAnimation() {
      this.scaleAnimation.setDirection(Direction.FORWARDS);
   }

   public void stopAnimation() {
      this.scaleAnimation.setDirection(Direction.BACKWARDS);
   }

   protected boolean isChat(Screen screen) {
      return screen instanceof ChatScreen;
   }

   public boolean isDraggable() {
      return this.draggable;
   }

   public float getLastTickDelta() {
      return this.lastTickDelta;
   }

   @Override
   public boolean isEnabled() {
      return this.enabled;
   }

   @Override
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public int getX() {
      return this.x;
   }

   @Override
   public int getY() {
      return this.y;
   }

   @Override
   public void setX(int x) {
      this.x = x;
   }

   @Override
   public void setY(int y) {
      this.y = y;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public void setWidth(int width) {
      this.width = width;
   }

   @Override
   public void setHeight(int height) {
      this.height = height;
   }
}
