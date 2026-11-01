package ruhack.phobia.system.modulesystem.impl.rage;

import com.adl.nativeprotect.Native;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;

public class AimingBalls extends ModuleStructure {
   private final MultiSelectSetting targets = new MultiSelectSetting("Наводиться на", "Типы ценного дропа")
      .value("Элитры", "Шары", "Осколки", "Джек", "О6/О7")
      .selected("Элитры", "Шары", "Осколки", "Джек", "О6/О7");
   private final BooleanSetting clientLook = new BooleanSetting("Client Look", "Поворачивать камеру игрока").setValue(true);
   private final SliderSettings distance = new SliderSettings("Дистанция", "Радиус поиска предметов", 16.0F).range(1.0F, 64.0F).step(1.0F);
   private final SliderSettings speed = new SliderSettings("Скорость", "Скорость поворота", 45.0F).range(1.0F, 180.0F).step(1.0F);

   public AimingBalls() {
      super("AimingBalls", "Наводится на выбранный ценный дроп", ModuleCategory.RAGE);
      this.settings(this.targets, this.clientLook, this.distance, this.speed);
   }

   @EventHandler
   @Native
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         ItemEntity target = null;
         double best = this.distance.getValue() * this.distance.getValue();

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity item && item.isAlive() && this.isTarget(item.getStack())) {
               double squared = mc.player.squaredDistanceTo(item);
               if (squared < best) {
                  best = squared;
                  target = item;
               }
            }
         }

         if (target != null) {
            Angle angle = AngleUtil.calculateAngle(target.getEntityPos().add(0.0, target.getHeight() * 0.5, 0.0));
            float step = this.speed.getValue();
            float yaw = mc.player.getYaw() + MathHelper.clamp(MathHelper.wrapDegrees(angle.getYaw() - mc.player.getYaw()), -step, step);
            float pitch = MathHelper.clamp(mc.player.getPitch() + MathHelper.clamp(angle.getPitch() - mc.player.getPitch(), -step, step), -90.0F, 90.0F);
            if (this.clientLook.isValue()) {
               mc.player.setYaw(yaw);
               mc.player.setPitch(pitch);
            }
         }
      }
   }

   private boolean isTarget(ItemStack stack) {
      return this.targets.isSelected("Элитры") && stack.isOf(Items.ELYTRA)
         || this.targets.isSelected("Шары") && stack.isOf(Items.PLAYER_HEAD)
         || this.targets.isSelected("Осколки") && stack.isOf(Items.GHAST_TEAR)
         || this.targets.isSelected("Джек") && stack.isOf(Items.CARVED_PUMPKIN)
         || this.targets.isSelected("О6/О7") && stack.getName().getString().matches(".*[67].*");
   }
}
