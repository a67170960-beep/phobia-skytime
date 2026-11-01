package ruhack.phobia.system.rendersystem.render2d.font.msdf;

public class MsdfGlyph {
   public final int codepoint;
   public final float advance;
   public final float x;
   public final float y;
   public final float width;
   public final float height;
   public final float u0;
   public final float v0;
   public final float u1;
   public final float v1;
   public final float bearingX;
   public final float bearingY;

   public MsdfGlyph(
      int codepoint, float advance, float x, float y, float width, float height, float u0, float v0, float u1, float v1, float bearingX, float bearingY
   ) {
      this.codepoint = codepoint;
      this.advance = advance;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.u0 = u0;
      this.v0 = v0;
      this.u1 = u1;
      this.v1 = v1;
      this.bearingX = bearingX;
      this.bearingY = bearingY;
   }
}
