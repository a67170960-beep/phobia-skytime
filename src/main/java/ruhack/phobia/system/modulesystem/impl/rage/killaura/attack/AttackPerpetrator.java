package ruhack.phobia.system.modulesystem.impl.rage.killaura.attack;

import java.util.List;
import lombok.Generated;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.UsingItemEvent;
import ruhack.phobia.util.rotation.angle.Angle;

public class AttackPerpetrator implements IMinecraft {
   AttackHandler attackHandler = new AttackHandler();

   public void tick() {
      this.attackHandler.tick();
   }

   public void onPacket(PacketEvent e) {
      this.attackHandler.onPacket(e);
   }

   public void onUsingItem(UsingItemEvent e) {
      this.attackHandler.onUsingItem(e);
   }

   public void performAttack(AttackPerpetrator.AttackPerpetratorConfigurable configurable) {
      this.attackHandler.handleAttack(configurable);
   }

   @Generated
   public AttackHandler getAttackHandler() {
      return this.attackHandler;
   }

   public static class AttackPerpetratorConfigurable {
      private final LivingEntity target;
      private final Angle angle;
      private final float maximumRange;
      private final boolean onlyCritical;
      private final boolean shouldUnPressShield;
      private final boolean eatAndAttack;
      private final boolean shieldBreaker;
      private final Box box;

      public AttackPerpetratorConfigurable(LivingEntity target, Angle angle, float maximumRange, List<String> options, Box box, boolean onlyCritical) {
         this.target = target;
         this.angle = angle;
         this.maximumRange = maximumRange;
         this.onlyCritical = onlyCritical;
         this.shouldUnPressShield = options.contains("Отжимать щит");
         this.eatAndAttack = options.contains("Не бить при использовании");
         this.shieldBreaker = options.contains("Ломать щит");
         this.box = box;
      }

      @Generated
      public LivingEntity getTarget() {
         return this.target;
      }

      @Generated
      public Angle getAngle() {
         return this.angle;
      }

      @Generated
      public float getMaximumRange() {
         return this.maximumRange;
      }

      @Generated
      public boolean isOnlyCritical() {
         return this.onlyCritical;
      }

      @Generated
      public boolean isShouldUnPressShield() {
         return this.shouldUnPressShield;
      }

      @Generated
      public boolean isEatAndAttack() {
         return this.eatAndAttack;
      }

      @Generated
      public boolean isShieldBreaker() {
         return this.shieldBreaker;
      }

      @Generated
      public Box getBox() {
         return this.box;
      }
   }
}
