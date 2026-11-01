package ruhack.phobia.system.modulesystem;

import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.display.Notifications;
import ruhack.phobia.system.modulesystem.setting.SettingRepository;

public class ModuleStructure extends SettingRepository implements IMinecraft {
   private final String name;
   private final String description;
   private final ModuleCategory category;
   private int key = -1;
   private int type = 1;
   public boolean state;
   public boolean favorite;

   public ModuleStructure(String name, String description, ModuleCategory category) {
      this.name = name;
      this.description = description;
      this.category = category;
   }

   public void switchState() {
      this.setState(!this.state);
      Notifications.module(this.name, this.state);
   }

   public void setState(boolean state) {
      if (state != this.state) {
         this.state = state;
         this.handleStateChange();
      }
   }

   public void switchFavorite() {
      this.setFavorite(!this.favorite);
   }

   public void setFavorite(boolean favorite) {
      this.favorite = favorite;
   }

   private void handleStateChange() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (this.state) {
         this.activate();
         this.toggleSilent(true);
      } else {
         this.deactivate();
         this.toggleSilent(false);
      }
   }

   private void toggleSilent(boolean activate) {
      EventManager eventManager = Initialization.getInstance().getManager().getEventManager();
      if (activate) {
         EventManager.register(this);
      } else {
         EventManager.unregister(this);
      }
   }

   public void activate() {
   }

   public void deactivate() {
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public String getDescription() {
      return this.description;
   }

   @Generated
   public ModuleCategory getCategory() {
      return this.category;
   }

   @Generated
   public int getKey() {
      return this.key;
   }

   @Generated
   public int getType() {
      return this.type;
   }

   @Generated
   public boolean isState() {
      return this.state;
   }

   @Generated
   public boolean isFavorite() {
      return this.favorite;
   }

   @Generated
   public void setKey(int key) {
      this.key = key;
   }

   @Generated
   public void setType(int type) {
      this.type = type;
   }
}
