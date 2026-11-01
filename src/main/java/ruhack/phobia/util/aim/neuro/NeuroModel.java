package ruhack.phobia.util.aim.neuro;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class NeuroModel {
   private static final float MAX_ERR = 140.0F;
   private static final int K = 24;
   private static final float CLOSING_GAIN = 5.0F;
   private static final Gson GSON = new Gson();
   private final Random random = new Random();
   private float[][] frames;
   private float[] prevDY;
   private float[] prevDP;
   private float[] invStd;
   private float jerkLimit = 6.0F;
   private int n;
   private float prevStepYaw;
   private float prevStepPitch;

   public boolean isTrained() {
      return this.n > 0;
   }

   public int frameCount() {
      return this.n;
   }

   public int gestureCount() {
      return this.n;
   }

   public int holdCount() {
      return 0;
   }

   public synchronized void fit(float[][] stream) {
      if (stream == null) {
         stream = new float[0][];
      }

      List<float[]> clean = new ArrayList<>(stream.length);
      boolean gap = false;

      for (float[] s : stream) {
         if (s != null && s.length >= 4 && !(Math.max(Math.abs(s[2]), Math.abs(s[3])) > 140.0F)) {
            float contiguous = gap ? 0.0F : (s.length > 4 ? s[4] : 1.0F);
            clean.add(new float[]{s[0], s[1], s[2], s[3], contiguous});
            gap = false;
         } else {
            gap = true;
         }
      }

      this.frames = clean.toArray(new float[0][]);
      this.rebuild();
   }

   private void rebuild() {
      this.n = this.frames == null ? 0 : this.frames.length;
      this.prevDY = new float[this.n];
      this.prevDP = new float[this.n];

      for (int i = 0; i < this.n; i++) {
         boolean contiguous = i > 0 && this.frames[i][4] > 0.5F;
         this.prevDY[i] = contiguous ? this.frames[i - 1][0] : 0.0F;
         this.prevDP[i] = contiguous ? this.frames[i - 1][1] : 0.0F;
      }

      this.invStd = new float[4];

      for (int f = 0; f < 4; f++) {
         double mean = 0.0;

         for (int i = 0; i < this.n; i++) {
            mean += this.feature(i, f);
         }

         mean /= Math.max(1, this.n);
         double var = 0.0;

         for (int i = 0; i < this.n; i++) {
            double d = this.feature(i, f) - mean;
            var += d * d;
         }

         var /= Math.max(1, this.n - 1);
         float std = (float)Math.sqrt(var);
         this.invStd[f] = std < 1.0E-4F ? 0.0F : 1.0F / std;
      }

      List<Float> jerks = new ArrayList<>();

      for (int i = 1; i < this.n; i++) {
         if (this.frames[i][4] > 0.5F) {
            jerks.add((float)Math.hypot(this.frames[i][0] - this.frames[i - 1][0], this.frames[i][1] - this.frames[i - 1][1]));
         }
      }

      if (jerks.size() >= 50) {
         jerks.sort(null);
         this.jerkLimit = Math.max(2.0F, jerks.get((int)(jerks.size() * 0.97F)));
      } else {
         this.jerkLimit = 6.0F;
      }

      this.resetPlayback();
   }

   private float feature(int i, int f) {
      return switch (f) {
         case 0 -> this.frames[i][2];
         case 1 -> this.frames[i][3];
         case 2 -> this.prevDY[i];
         default -> this.prevDP[i];
      };
   }

   public synchronized void resetPlayback() {
      this.prevStepYaw = 0.0F;
      this.prevStepPitch = 0.0F;
   }

   public synchronized float[] next(float errYaw, float errPitch, float hitRadius) {
      if (this.n == 0) {
         return null;
      }

      float errLen = (float)Math.hypot(errYaw, errPitch);
      float[] bestDist = new float[24];
      int[] bestIdx = new int[24];
      Arrays.fill(bestDist, Float.MAX_VALUE);
      Arrays.fill(bestIdx, -1);
      int filled = 0;
      int total = this.n * 2;

      for (int c = 0; c < total; c++) {
         boolean mir = c >= this.n;
         int r = mir ? c - this.n : c;
         float m = mir ? -1.0F : 1.0F;
         float d0 = (errYaw - m * this.frames[r][2]) * this.invStd[0];
         float d1 = (errPitch - this.frames[r][3]) * this.invStd[1];
         float d2 = (this.prevStepYaw - m * this.prevDY[r]) * this.invStd[2];
         float d3 = (this.prevStepPitch - this.prevDP[r]) * this.invStd[3];
         float dist = d0 * d0 + d1 * d1 + d2 * d2 + d3 * d3;
         if (filled < 24) {
            bestDist[filled] = dist;
            bestIdx[filled] = c;
            if (++filled == 24) {
               worstFirst(bestDist, bestIdx);
            }
         } else if (dist < bestDist[0]) {
            bestDist[0] = dist;
            bestIdx[0] = c;
            worstFirst(bestDist, bestIdx);
         }
      }

      if (filled == 0) {
         return null;
      }

      float closing = MathClamp(errLen / Math.max(hitRadius, 1.0F) - 1.0F, 0.0F, 1.0F) * 5.0F;
      float bandwidth = Math.max(0.001F, medianOf(bestDist, filled));
      float errLenSq = errLen * errLen + 0.001F;
      float jerkScale = MathClamp(errLen / Math.max(hitRadius, 1.0F), 1.0F, 3.0F);
      float allowedJerk = this.jerkLimit * jerkScale;
      boolean anyReachable = false;

      for (int i = 0; i < filled; i++) {
         int c = bestIdx[i];
         boolean mir = c >= this.n;
         int r = mir ? c - this.n : c;
         float dy = (mir ? -1.0F : 1.0F) * this.frames[r][0];
         float dp = this.frames[r][1];
         float jerk = (float)Math.hypot(dy - this.prevStepYaw, dp - this.prevStepPitch);
         if (jerk <= allowedJerk) {
            anyReachable = true;
         }
      }

      float totalW = 0.0F;
      float[] weight = new float[filled];

      for (int i = 0; i < filled; i++) {
         int c = bestIdx[i];
         boolean mir = c >= this.n;
         int r = mir ? c - this.n : c;
         float dy = (mir ? -1.0F : 1.0F) * this.frames[r][0];
         float dp = this.frames[r][1];
         float closeFrac = MathClamp((dy * errYaw + dp * errPitch) / errLenSq, -1.0F, 1.0F);
         float w = (float)Math.exp(-bestDist[i] / bandwidth + closing * closeFrac);
         if (anyReachable) {
            float jerk = (float)Math.hypot(dy - this.prevStepYaw, dp - this.prevStepPitch);
            if (jerk > allowedJerk) {
               w = 0.0F;
            }
         }

         weight[i] = w;
         totalW += w;
      }

      int chosen = -1;
      float roll = this.random.nextFloat() * totalW;

      for (int i = 0; i < filled; i++) {
         if (!(weight[i] <= 0.0F)) {
            chosen = i;
            roll -= weight[i];
            if (roll <= 0.0F) {
               break;
            }
         }
      }

      if (chosen < 0) {
         chosen = 0;

         for (int i = 1; i < filled; i++) {
            if (bestDist[i] < bestDist[chosen]) {
               chosen = i;
            }
         }
      }

      int c = bestIdx[chosen];
      boolean mir = c >= this.n;
      int r = mir ? c - this.n : c;
      float dy = (mir ? -1.0F : 1.0F) * this.frames[r][0];
      float dp = this.frames[r][1];
      this.prevStepYaw = dy;
      this.prevStepPitch = dp;
      return new float[]{dy, dp};
   }

   private static float MathClamp(float v, float min, float max) {
      return v < min ? min : Math.min(v, max);
   }

   private static void worstFirst(float[] dist, int[] idx) {
      int max = 0;

      for (int i = 1; i < dist.length; i++) {
         if (dist[i] > dist[max]) {
            max = i;
         }
      }

      if (max != 0) {
         float td = dist[0];
         dist[0] = dist[max];
         dist[max] = td;
         int ti = idx[0];
         idx[0] = idx[max];
         idx[max] = ti;
      }
   }

   private static float medianOf(float[] a, int len) {
      float[] copy = new float[len];
      System.arraycopy(a, 0, copy, 0, len);
      Arrays.sort(copy);
      return copy[len / 2];
   }

   public synchronized void save(Path path) throws Exception {
      NeuroModel.State st = new NeuroModel.State();
      st.frames = this.frames;
      Files.createDirectories(path.getParent());
      Files.writeString(path, GSON.toJson(st));
   }

   public synchronized boolean load(Path path) {
      try {
         if (!Files.exists(path)) {
            return false;
         } else {
            NeuroModel.State st = (NeuroModel.State)GSON.fromJson(Files.readString(path), NeuroModel.State.class);
            if (st != null && st.frames != null) {
               this.fit(st.frames);
               return this.isTrained();
            } else {
               return false;
            }
         }
      } catch (Exception e) {
         return false;
      }
   }

   private static final class State {
      float[][] frames;
   }
}
