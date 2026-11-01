package ruhack.phobia.system.dragsystem;

import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;

public interface HudElement {
   void render(DrawContext var1, float var2);

   void tick();

   default void onPacket(PacketEvent e) {
   }

   boolean isEnabled();

   void setEnabled(boolean var1);

   String getName();

   int getX();

   int getY();

   void setX(int var1);

   void setY(int var1);

   int getWidth();

   int getHeight();

   void setWidth(int var1);

   void setHeight(int var1);

   default float getRoundingRadius() {
      return 4.0F;
   }

   default boolean visible() {
      return true;
   }

   default boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
   }

   default boolean mouseReleased(double mouseX, double mouseY, int button) {
      return false;
   }
}
