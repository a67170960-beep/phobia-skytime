package ruhack.phobia;

import com.adl.nativeprotect.Native;
import lombok.Generated;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import ruhack.phobia.manager.Manager;
import ruhack.phobia.system.rendersystem.render2d.menu.MainMenuRenderResources;
import ruhack.phobia.system.screensystem.clickgui.IrcClient;
import ruhack.phobia.util.rpc.DiscordManager;

public class Initialization implements ClientModInitializer {
   private static Initialization instance;
   private Manager manager;
   private DiscordManager discordManager;

   public void onInitializeClient() {
      this.init();
   }

   @Native
   public void init() {
      instance = this;
      ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         public Identifier getFabricId() {
            return Identifier.of("phobia", "main_menu_render_resources");
         }

         public void reload(ResourceManager manager) {
            MainMenuRenderResources.reload();
         }
      });
      this.manager = new Manager();
      this.manager.init();
      IrcClient.INSTANCE.startFromProfile();
      this.discordManager = new DiscordManager().start();
   }

   @Generated
   public static Initialization getInstance() {
      return instance;
   }

   @Generated
   public Manager getManager() {
      return this.manager;
   }

   @Generated
   public DiscordManager getDiscordManager() {
      return this.discordManager;
   }
}
