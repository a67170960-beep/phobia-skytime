package ruhack.phobia.system.modulesystem.impl.rage.killaura.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.IMinecraft;

public class FinalVector implements IMinecraft {
   public static Vec3d expensiveUpgradePoint(Entity entity) {
      return FromVector.hitbox(entity, 1.0F, 1.0F, 1.0F, 4.0F);
   }

   public static Vec3d mincedPoint(Entity entity) {
      return FromVector.getBestPoint(mc.player.getEyePos(), entity);
   }

   public static Vec3d celestialPoint(Entity entity) {
      return FromVector.closest(entity);
   }

   public static Vec3d randomPoint(Entity entity) {
      return FromVector.custom(entity, 75, 1000.0F);
   }

   public static Vec3d expensivePoint(Entity entity) {
      return FromVector.brain(entity, 2.0F, 3.0F);
   }
}
