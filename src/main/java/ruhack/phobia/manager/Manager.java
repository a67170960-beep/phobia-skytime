package ruhack.phobia.manager;

import com.adl.nativeprotect.Native;
import lombok.Generated;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.configsystem.ConfigSystem;
import ruhack.phobia.system.configsystem.impl.bind.BindConfig;
import ruhack.phobia.system.configsystem.impl.blockesp.BlockESPConfig;
import ruhack.phobia.system.configsystem.impl.drag.DragConfig;
import ruhack.phobia.system.configsystem.impl.friend.FriendConfig;
import ruhack.phobia.system.configsystem.impl.prefix.PrefixConfig;
import ruhack.phobia.system.configsystem.impl.proxy.ProxyConfig;
import ruhack.phobia.system.configsystem.impl.staff.StaffConfig;
import ruhack.phobia.system.configsystem.impl.userconfig.UserConfigManager;
import ruhack.phobia.system.dragsystem.HudManager;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.macrosystem.MacroRepository;
import ruhack.phobia.system.modulesystem.ModuleProvider;
import ruhack.phobia.system.modulesystem.ModuleRepository;
import ruhack.phobia.system.modulesystem.ModuleSwitcher;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.attack.AttackPerpetrator;
import ruhack.phobia.util.rotation.RotationController;

public class Manager {
   public AttackPerpetrator attackPerpetrator = new AttackPerpetrator();
   private EventManager eventManager;
   private ModuleProvider moduleProvider;
   private ModuleRepository moduleRepository;
   private ModuleSwitcher moduleSwitcher;
   private ConfigSystem configSystem;
   private CommandManager commandManager;
   private HudManager hudManager = new HudManager();

   @Native
   public void init() {
      MacroRepository.getInstance().init();
      BlockESPConfig.getInstance().load();
      FriendConfig.getInstance().load();
      PrefixConfig.getInstance().load();
      StaffConfig.getInstance().load();
      ProxyConfig.getInstance().load();
      DragConfig.getInstance().load();
      BindConfig.getInstance();
      this.eventManager = new EventManager();
      RotationController.INSTANCE.init();
      this.hudManager = new HudManager();
      this.hudManager.initElements();
      this.moduleRepository = new ModuleRepository();
      this.moduleRepository.setup();
      this.moduleProvider = new ModuleProvider(this.moduleRepository.modules());
      this.moduleSwitcher = new ModuleSwitcher(this.moduleRepository.modules(), this.eventManager);
      this.configSystem = new ConfigSystem();
      this.configSystem.init();
      UserConfigManager.getInstance().init();
      this.commandManager = new CommandManager();
      this.commandManager.init();
   }

   @Generated
   public AttackPerpetrator getAttackPerpetrator() {
      return this.attackPerpetrator;
   }

   @Generated
   public EventManager getEventManager() {
      return this.eventManager;
   }

   @Generated
   public ModuleProvider getModuleProvider() {
      return this.moduleProvider;
   }

   @Generated
   public ModuleRepository getModuleRepository() {
      return this.moduleRepository;
   }

   @Generated
   public ModuleSwitcher getModuleSwitcher() {
      return this.moduleSwitcher;
   }

   @Generated
   public ConfigSystem getConfigSystem() {
      return this.configSystem;
   }

   @Generated
   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   @Generated
   public HudManager getHudManager() {
      return this.hudManager;
   }
}
