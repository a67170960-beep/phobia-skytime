package ruhack.phobia.util.math;

import lombok.Generated;

public enum TaskPriority {
   CRITICAL_FOR_USER_PROTECTION(60),
   CRUCIAL_FOR_PLAYER_LIFE(40),
   HIGH_IMPORTANCE_3(35),
   HIGH_IMPORTANCE_2(30),
   HIGH_IMPORTANCE_1(20),
   STANDARD(0),
   LOW_PRIORITY(-20);

   private final int priority;

   @Generated
   public int getPriority() {
      return this.priority;
   }

   @Generated
   TaskPriority(final int priority) {
      this.priority = priority;
   }
}
