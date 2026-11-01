package ruhack.phobia.system.modulesystem;

import com.adl.nativeprotect.Native;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ruhack.phobia.system.modulesystem.impl.display.Hud;
import ruhack.phobia.system.modulesystem.impl.legit.LegitAura;
import ruhack.phobia.system.modulesystem.impl.legit.NoFriendDamage;
import ruhack.phobia.system.modulesystem.impl.legit.ShiftTap;
import ruhack.phobia.system.modulesystem.impl.legit.TriggerBot;
import ruhack.phobia.system.modulesystem.impl.misc.AutoDuel;
import ruhack.phobia.system.modulesystem.impl.misc.AutoLeave;
import ruhack.phobia.system.modulesystem.impl.misc.AutoRespawn;
import ruhack.phobia.system.modulesystem.impl.misc.AutoTpAccept;
import ruhack.phobia.system.modulesystem.impl.misc.BowSpammer;
import ruhack.phobia.system.modulesystem.impl.misc.ClickAction;
import ruhack.phobia.system.modulesystem.impl.misc.ElytraHelper;
import ruhack.phobia.system.modulesystem.impl.misc.FuntimeHelper;
import ruhack.phobia.system.modulesystem.impl.misc.HolyworldHelper;
import ruhack.phobia.system.modulesystem.impl.misc.ItemScroller;
import ruhack.phobia.system.modulesystem.impl.misc.NameProtect;
import ruhack.phobia.system.modulesystem.impl.misc.NoSlotChange;
import ruhack.phobia.system.modulesystem.impl.misc.PVPSafe;
import ruhack.phobia.system.modulesystem.impl.misc.PartyHelper;
import ruhack.phobia.system.modulesystem.impl.misc.ReallyWorldHelper;
import ruhack.phobia.system.modulesystem.impl.misc.RegionExploit;
import ruhack.phobia.system.modulesystem.impl.misc.ServerRPSpoofer;
import ruhack.phobia.system.modulesystem.impl.movement.AntiWeb;
import ruhack.phobia.system.modulesystem.impl.movement.AutoSprint;
import ruhack.phobia.system.modulesystem.impl.movement.FireworkBooster;
import ruhack.phobia.system.modulesystem.impl.movement.Fly;
import ruhack.phobia.system.modulesystem.impl.movement.HighJump;
import ruhack.phobia.system.modulesystem.impl.movement.Jesus;
import ruhack.phobia.system.modulesystem.impl.movement.LungeHelper;
import ruhack.phobia.system.modulesystem.impl.movement.NoFallDamage;
import ruhack.phobia.system.modulesystem.impl.movement.NoSlowDown;
import ruhack.phobia.system.modulesystem.impl.movement.Scaffold;
import ruhack.phobia.system.modulesystem.impl.movement.Speed;
import ruhack.phobia.system.modulesystem.impl.movement.Spider;
import ruhack.phobia.system.modulesystem.impl.movement.Strafe;
import ruhack.phobia.system.modulesystem.impl.movement.TargetStrafe;
import ruhack.phobia.system.modulesystem.impl.movement.WaterSpeed;
import ruhack.phobia.system.modulesystem.impl.other.Theme;
import ruhack.phobia.system.modulesystem.impl.player.AutoMine;
import ruhack.phobia.system.modulesystem.impl.player.AutoWarden;
import ruhack.phobia.system.modulesystem.impl.player.FreeCam;
import ruhack.phobia.system.modulesystem.impl.player.NoCommand;
import ruhack.phobia.system.modulesystem.impl.player.NoDelay;
import ruhack.phobia.system.modulesystem.impl.player.NoEntityTrace;
import ruhack.phobia.system.modulesystem.impl.player.NoInteract;
import ruhack.phobia.system.modulesystem.impl.player.NoPush;
import ruhack.phobia.system.modulesystem.impl.player.RwKeyFinder;
import ruhack.phobia.system.modulesystem.impl.pve.TapeMouse;
import ruhack.phobia.system.modulesystem.impl.rage.AimingBalls;
import ruhack.phobia.system.modulesystem.impl.rage.AntiBot;
import ruhack.phobia.system.modulesystem.impl.rage.AutoCart;
import ruhack.phobia.system.modulesystem.impl.rage.AutoExplosion;
import ruhack.phobia.system.modulesystem.impl.rage.AutoGApple;
import ruhack.phobia.system.modulesystem.impl.rage.AutoPotion;
import ruhack.phobia.system.modulesystem.impl.rage.AutoSwap;
import ruhack.phobia.system.modulesystem.impl.rage.AutoTotem;
import ruhack.phobia.system.modulesystem.impl.rage.BuffHelper;
import ruhack.phobia.system.modulesystem.impl.rage.Criticals;
import ruhack.phobia.system.modulesystem.impl.rage.ElytraTarget;
import ruhack.phobia.system.modulesystem.impl.rage.HitBox;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.system.modulesystem.impl.rage.MaceTarget;
import ruhack.phobia.system.modulesystem.impl.rage.TargetPearl;
import ruhack.phobia.system.modulesystem.impl.rage.Velocity;
import ruhack.phobia.system.modulesystem.impl.rage.WebTrap;
import ruhack.phobia.system.modulesystem.impl.render.Ambience;
import ruhack.phobia.system.modulesystem.impl.render.Arrows;
import ruhack.phobia.system.modulesystem.impl.render.AspectRatio;
import ruhack.phobia.system.modulesystem.impl.render.BlockOverlay;
import ruhack.phobia.system.modulesystem.impl.render.Boobs;
import ruhack.phobia.system.modulesystem.impl.render.Box;
import ruhack.phobia.system.modulesystem.impl.render.Chams;
import ruhack.phobia.system.modulesystem.impl.render.ChinaHat;
import ruhack.phobia.system.modulesystem.impl.render.CustomModels;
import ruhack.phobia.system.modulesystem.impl.render.HitPoint;
import ruhack.phobia.system.modulesystem.impl.render.ItemReplacer;
import ruhack.phobia.system.modulesystem.impl.render.NoRender;
import ruhack.phobia.system.modulesystem.impl.render.Particles;
import ruhack.phobia.system.modulesystem.impl.render.Prediction;
import ruhack.phobia.system.modulesystem.impl.render.SeeInvisible;
import ruhack.phobia.system.modulesystem.impl.render.ShaderESP;
import ruhack.phobia.system.modulesystem.impl.render.ShaderHands;
import ruhack.phobia.system.modulesystem.impl.render.ShaderSky;
import ruhack.phobia.system.modulesystem.impl.render.ShulkerPreview;
import ruhack.phobia.system.modulesystem.impl.render.SwingAnimation;
import ruhack.phobia.system.modulesystem.impl.render.Tags;
import ruhack.phobia.system.modulesystem.impl.render.TargetESP;
import ruhack.phobia.system.modulesystem.impl.render.ViewModel;
import ruhack.phobia.system.modulesystem.impl.render.Wings;

public class ModuleRepository {
   private final List<ModuleStructure> moduleStructures = new ArrayList<>();
   private final List<ModuleStructure> hiddenModules = new ArrayList<>();
   private final Set<Class<? extends ModuleStructure>> registeredClasses = new HashSet<>();

   @Native
   public void setup() {
      this.builder()
         .add(new Theme())
         .add(new Hud())
         .add(new KillAura())
         .add(new Criticals())
         .add(new TargetPearl())
         .add(new AimingBalls())
         .add(new AutoExplosion())
         .add(new TriggerBot())
         .add(new LegitAura())
         .add(new ClickAction())
         .add(new PartyHelper())
         .add(new FuntimeHelper())
         .add(new HolyworldHelper())
         .add(new ReallyWorldHelper())
         .add(new TapeMouse())
         .add(new NoDelay())
         .add(new NoSlotChange())
         .add(new NameProtect())
         .add(new NoPush())
         .add(new NoRender())
         .add(new SeeInvisible())
         .add(new Chams())
         .add(new ShaderESP())
         .add(new Box())
         .add(new SwingAnimation())
         .add(new Particles())
         .add(new TargetESP())
         .add(new Prediction())
         .add(new AspectRatio())
         .add(new Tags())
         .add(new ShulkerPreview())
         .add(new Arrows())
         .add(new Wings())
         .add(new ShaderSky())
         .add(new AutoSprint())
         .add(new ViewModel())
         .add(new ShaderHands())
         .add(new CustomModels())
         .add(new ItemReplacer())
         .add(new ChinaHat())
         .add(new Boobs())
         .add(new Speed())
         .add(new Spider())
         .add(new Strafe())
         .add(new TargetStrafe())
         .add(new WaterSpeed())
         .add(new Fly())
         .add(new Scaffold())
         .add(new FireworkBooster())
         .add(new LungeHelper())
         .add(new AntiWeb())
         .add(new NoSlowDown())
         .add(new HighJump())
         .add(new Jesus())
         .add(new ElytraTarget())
         .add(new AutoRespawn())
         .add(new ItemScroller())
         .add(new NoEntityTrace())
         .add(new NoFallDamage())
         .add(new RwKeyFinder())
         .add(new AutoMine())
         .add(new AutoPotion())
         .add(new BuffHelper())
         .add(new ServerRPSpoofer())
         .add(new RegionExploit())
         .add(new AutoTpAccept())
         .add(new AutoLeave())
         .add(new PVPSafe())
         .add(new AutoDuel())
         .add(new Velocity())
         .add(new ShiftTap())
         .add(new NoInteract())
         .add(new NoCommand())
         .add(new NoFriendDamage())
         .add(new HitBox())
         .add(new BowSpammer())
         .add(new AutoGApple())
         .add(new AntiBot())
         .add(new MaceTarget())
         .add(new WebTrap())
         .add(new AutoCart())
         .add(new FreeCam())
         .add(new HitPoint())
         .add(new Ambience())
         .add(new BlockOverlay())
         .add(new AutoTotem())
         .add(new ElytraHelper())
         .add(new AutoSwap())
         .add(new AutoWarden());
   }

   public ModuleBuilder builder() {
      return new ModuleBuilder(this);
   }

   void registerModule(ModuleStructure module, boolean hidden) {
      Class<? extends ModuleStructure> clazz = (Class<? extends ModuleStructure>)module.getClass();
      if (this.registeredClasses.contains(clazz)) {
         throw new DuplicateModuleException(clazz.getSimpleName());
      }

      this.registeredClasses.add(clazz);
      if (hidden) {
         this.hiddenModules.add(module);
         module.setState(true);
      } else {
         this.moduleStructures.add(module);
      }
   }

   public List<ModuleStructure> modules() {
      return this.moduleStructures;
   }

   public List<ModuleStructure> hiddenModules() {
      return this.hiddenModules;
   }

   public List<ModuleStructure> allModules() {
      List<ModuleStructure> all = new ArrayList<>(this.moduleStructures);
      all.addAll(this.hiddenModules);
      return all;
   }
}
