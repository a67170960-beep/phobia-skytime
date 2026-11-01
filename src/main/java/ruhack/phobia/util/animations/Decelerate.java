package ruhack.phobia.util.animations;

public class Decelerate extends Animation {
   @Override
   public double calculation(double value) {
      double x = value / this.ms;
      return 1.0 - (x - 1.0) * (x - 1.0);
   }
}
