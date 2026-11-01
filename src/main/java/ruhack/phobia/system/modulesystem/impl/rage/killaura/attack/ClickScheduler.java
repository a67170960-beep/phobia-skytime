package ruhack.phobia.system.modulesystem.impl.rage.killaura.attack;

import ruhack.phobia.IMinecraft;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.modulesystem.impl.rage.Criticals;
import ruhack.phobia.util.ServerUtil;

public class ClickScheduler implements IMinecraft {
   private final int[] funTimeTicks = new int[]{10, 11, 10, 13};
   private final int[] spookyTicks = new int[]{11, 10, 13, 10, 12, 11, 12};
   private final int[] defaultTicks = new int[]{10, 11};
   private long lastClickTime = System.currentTimeMillis();

   public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
      int boost = this.criticalsBoost();
      boolean dynamic = this.hasTicksElapsedSinceLastClick(this.tickCount() - boost - ticks);
      float threshold = boost > 0 ? 0.65F : 0.8F;
      return dynamic && mc.player.getAttackCooldownProgress(ticks) > threshold;
   }

   private int criticalsBoost() {
      Criticals criticals = Criticals.getInstance();
      return criticals != null ? criticals.getBoostTicks() : 0;
   }

   public boolean hasTicksElapsedSinceLastClick(int ticks) {
      return (float)this.lastClickPassed() >= (float)(ticks * 50L) * (20.0F / ServerUtil.TPS);
   }

   public long lastClickPassed() {
      return System.currentTimeMillis() - this.lastClickTime;
   }

   public void recalculate() {
      this.lastClickTime = System.currentTimeMillis();
   }

   int tickCount() {
      int count = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().getCount();
      String var2 = ServerUtil.server;
      byte var3 = -1;
      var2.hashCode();
      switch (var3) {
         default:
            return this.defaultTicks[count % this.defaultTicks.length];
      }
   }
}
