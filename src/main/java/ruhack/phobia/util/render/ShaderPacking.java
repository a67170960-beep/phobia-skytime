package ruhack.phobia.util.render;

public final class ShaderPacking {
   private ShaderPacking() {
   }

   public static int packColorAlpha(int argb) {
      int a = argb >>> 24 & 0xFF;
      int r = argb >> 16 & 0xFF;
      int g = argb >> 8 & 0xFF;
      int b = argb & 0xFF;
      return (b << 8 | a) << 16 | r << 8 | g;
   }

   public static int packColorSpeedFill(int rgb, float speed, float fill) {
      int speed4 = Math.max(0, Math.min(15, Math.round(speed / 3.0F * 15.0F)));
      int fill4 = Math.max(0, Math.min(15, Math.round(fill * 15.0F)));
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      return (b << 8 | speed4 << 4 | fill4) << 16 | r << 8 | g;
   }

   public static int packBakedItemColors(int primary, int secondary, float speed, float fill) {
      int primaryR = (primary >> 16 & 0xFF) >> 4;
      int primaryG = (primary >> 8 & 0xFF) >> 4;
      int primaryB = (primary & 0xFF) >> 4;
      int secondaryR = (secondary >> 16 & 0xFF) >> 4;
      int secondaryG = (secondary >> 8 & 0xFF) >> 4;
      int secondaryB = (secondary & 0xFF) >> 4;
      int speed4 = Math.max(0, Math.min(15, Math.round(speed / 3.0F * 15.0F)));
      float alpha = (primary >>> 24 & 0xFF) / 255.0F;
      int fill4 = Math.max(0, Math.min(15, Math.round(fill * alpha * 15.0F)));
      return primaryR | primaryG << 4 | primaryB << 8 | secondaryR << 12 | secondaryG << 16 | secondaryB << 20 | speed4 << 24 | fill4 << 28;
   }

   public static int packColorAlpha(int argb, float alphaScale) {
      int a = Math.round((argb >>> 24 & 0xFF) * alphaScale);
      return packColorAlpha(Math.min(255, a) << 24 | argb & 16777215);
   }

   public static int packHaloPass(float angleRadians, float radiusPx, float speed) {
      int angle8 = Math.round((float)(angleRadians / (Math.PI * 2)) * 255.0F) & 0xFF;
      int radiusQ = Math.max(0, Math.min(255, Math.round(radiusPx * 4.0F)));
      int speed4 = Math.max(0, Math.min(15, Math.round(speed / 3.0F * 15.0F)));
      return speed4 << 4 << 16 | angle8 << 8 | radiusQ;
   }
}
