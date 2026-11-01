package ruhack.phobia.util.animations;

import lombok.Generated;
import ruhack.phobia.util.timer.TimerUtil;

public class Animation implements AnimationCalculation {
   public final TimerUtil counter = new TimerUtil();
   protected int ms;
   protected double value;
   protected Direction direction = Direction.FORWARDS;

   public void reset() {
      this.counter.resetCounter();
   }

   public void update() {
   }

   public boolean isDone() {
      return this.counter.isReached(this.ms);
   }

   public boolean isFinished(Direction direction) {
      return this.direction == direction && this.isDone();
   }

   public Direction getDirection() {
      return this.direction;
   }

   public void setDirection(Direction direction) {
      if (this.direction != direction) {
         this.direction = direction;
         this.adjustTimer();
      }
   }

   public boolean isDirection(Direction direction) {
      return this.direction == direction;
   }

   private void adjustTimer() {
      this.counter.setTime(System.currentTimeMillis() - (this.ms - Math.min(this.ms, this.counter.getTime())));
   }

   public Double getOutput() {
      double time = (1.0 - this.calculation(this.counter.getTime())) * this.value;
      return this.direction == Direction.FORWARDS ? this.endValue() : (this.isDone() ? 0.0 : time);
   }

   protected double endValue() {
      return this.isDone() ? this.value : this.calculation(this.counter.getTime()) * this.value;
   }

   @Generated
   public Animation setMs(int ms) {
      this.ms = ms;
      return this;
   }

   @Generated
   public Animation setValue(double value) {
      this.value = value;
      return this;
   }
}
