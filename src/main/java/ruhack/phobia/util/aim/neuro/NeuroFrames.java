package ruhack.phobia.util.aim.neuro;

import java.util.ArrayList;
import java.util.List;

public final class NeuroFrames {
   private static final float MAX_ERR = 140.0F;
   private static final float MAX_STEP = 60.0F;
   private static final float IDLE_ERR = 8.0F;
   private static final float IDLE_STEP = 0.02F;

   private NeuroFrames() {
   }

   public static float[][] clean(float[][] stream) {
      if (stream == null) {
         return new float[0][];
      }

      List<float[]> clean = new ArrayList<>(stream.length);
      boolean gap = false;
      float lastDy = Float.NaN;
      float lastDp = Float.NaN;
      float lastEy = Float.NaN;
      float lastEp = Float.NaN;

      for (float[] s : stream) {
         if (s != null && s.length >= 4) {
            float dy = s[0];
            float dp = s[1];
            float ey = s[2];
            float ep = s[3];
            if (Math.max(Math.abs(dy), Math.abs(dp)) > 60.0F) {
               gap = true;
            } else if (Math.max(Math.abs(ey), Math.abs(ep)) > 140.0F) {
               gap = true;
            } else {
               float err = (float)Math.hypot(ey, ep);
               if ((!(Math.abs(dy) < 0.02F) || !(Math.abs(dp) < 0.02F) || !(err > 8.0F))
                  && (Float.isNaN(lastDy) || dy != lastDy || dp != lastDp || ey != lastEy || ep != lastEp)) {
                  clean.add(new float[]{dy, dp, ey, ep, gap ? 0.0F : 1.0F});
                  lastDy = dy;
                  lastDp = dp;
                  lastEy = ey;
                  lastEp = ep;
                  gap = false;
               }
            }
         } else {
            gap = true;
         }
      }

      return clean.toArray(new float[0][]);
   }
}
