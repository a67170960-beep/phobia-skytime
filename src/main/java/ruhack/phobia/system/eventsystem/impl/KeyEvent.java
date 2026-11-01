package ruhack.phobia.system.eventsystem.impl;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil.Type;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.api.events.Event;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;

public record KeyEvent(Screen screen, Type type, int key, int action) implements Event, IMinecraft {
   public boolean isKeyDown(int key) {
      return this.isKeyDown(key, mc.currentScreen == null);
   }

   public boolean isKeyDown(int key, boolean screen) {
      return this.key == key && this.action == 1 && screen;
   }

   public boolean isKeyReleased(int key) {
      return this.isKeyReleased(key, mc.currentScreen == null);
   }

   public boolean isKeyReleased(int key, boolean screen) {
      return this.key == key && this.action == 0 && screen;
   }

   public boolean isBindDown(BindSetting bind) {
      return this.isBindDown(bind, mc.currentScreen == null);
   }

   public boolean isBindDown(BindSetting bind, boolean screen) {
      return bind.isPressed(this.key, this.type) && this.action == 1 && screen;
   }

   public boolean isBindReleased(BindSetting bind) {
      return this.isBindReleased(bind, mc.currentScreen == null);
   }

   public boolean isBindReleased(BindSetting bind, boolean screen) {
      return bind.isPressed(this.key, this.type) && this.action == 0 && screen;
   }

   public boolean isMouse() {
      return this.type == Type.MOUSE;
   }

   public boolean isKeyboard() {
      return this.type == Type.KEYSYM;
   }
}
