package ruhack.phobia.system.modulesystem.impl.render;

import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.render.ShaderHandsTrail;
import ruhack.phobia.util.render.ShaderPacking;

public class ShaderHands extends ModuleStructure {
   public static boolean firstPersonArmContext;
   public static boolean firstPersonItemContext;
   private static ShaderHands instance;
   private boolean renderCacheValid;
   private int framePrimaryColor;
   private int frameSecondaryColor;
   private int frameColor1;
   private int frameItemFillColor;
   private int frameBakedItemFill;
   private int frameTrailModelColor;
   private int frameTrailModelOverlay;
   private int frameColor2Speed;
   private float frameSpeed;
   private float frameFill;
   private long renderFrameSerial;
   public final SelectSetting mode = new SelectSetting("Режим", "Режим отображения рук", "Свечение", "Свечение", "Градиент");
   public final SelectSetting colorMode = new SelectSetting("Цвет", "Источник цветов шейдера", "От темы", "От темы", "Кастомный");
   public final ColorSetting color1 = new ColorSetting("Цвет 1", "Основной цвет свечения (у силуэта)")
      .value(ColorUtil.getColor(120, 180, 255, 255))
      .visible(() -> this.colorMode.isSelected("Кастомный"));
   public final ColorSetting color2 = new ColorSetting("Цвет 2", "Цвет внешнего края ореола")
      .value(ColorUtil.getColor(190, 230, 255, 255))
      .visible(() -> this.colorMode.isSelected("Кастомный"));
   public final SliderSettings speed = new SliderSettings("Скорость", "Скорость пульсации свечения", 1.0F).range(0.1F, 3.0F).step(0.1F);
   public final SliderSettings fill = new SliderSettings("Заливка", "0% — светится только контур, 100% — вся поверхность подсвечена", 0.0F)
      .range(0, 100)
      .suffix("%");
   public final BooleanSetting trail = new BooleanSetting("Шлейф", "Оставляет плавный светящийся след за руками и предметом")
      .setValue(false)
      .visible(this::isGlowMode);
   public final SliderSettings trailFade = new SliderSettings("Скорость затухания", "Скорость исчезновения старых кадров шлейфа", 0.009F)
      .range(0.002F, 0.2F)
      .step(0.002F)
      .visible(this::isTrailSelected);
   public final SliderSettings trailRise = new SliderSettings("Подъём", "Скорость подъёма шлейфа вверх", 0.14F)
      .range(0.0F, 1.5F)
      .step(0.05F)
      .visible(this::isTrailSelected);
   public final SliderSettings trailSway = new SliderSettings("Качание", "Амплитуда плавного покачивания шлейфа", 0.025F)
      .range(0.0F, 0.2F)
      .step(0.005F)
      .visible(this::isTrailSelected);
   public final SliderSettings trailTurbulence = new SliderSettings("Турбулентность", "Добавляет неровное волнообразное движение", 0.0F)
      .range(0.0F, 0.6F)
      .step(0.01F)
      .visible(this::isTrailSelected);
   public final SliderSettings trailFlicker = new SliderSettings("Мерцание", "Интенсивность мерцания шлейфа", 0.0F)
      .range(0.0F, 0.2F)
      .step(0.01F)
      .visible(this::isTrailSelected);
   public final BooleanSetting trailBurst = new BooleanSetting("Сдув при ударе", "Быстрее рассеивает шлейф при взмахе руки")
      .setValue(true)
      .visible(this::isTrailSelected);
   public final SliderSettings trailBurstPower = new SliderSettings("Сила сдува", "Сила рассеивания шлейфа при взмахе", 2.5F)
      .range(1.0F, 10.0F)
      .step(0.5F)
      .visible(() -> this.isTrailSelected() && this.trailBurst.isValue());
   public final BooleanSetting trailModel = new BooleanSetting("Шлейф модели", "Добавляет в след силуэт руки и предмета")
      .setValue(true)
      .visible(this::isTrailSelected);
   public final SliderSettings trailModelAlpha = new SliderSettings("Прозрачность модели", "Яркость силуэта руки и предмета в шлейфе", 0.4F)
      .range(0.1F, 1.0F)
      .step(0.05F)
      .visible(() -> this.isTrailSelected() && this.trailModel.isValue());
   private static final float[][] HALO_RINGS = buildHaloRings();
   private static final int[] HALO_PASS_RINGS = buildHaloPassRings();
   private static final int[][] HALO_PASS_OVERLAYS = buildHaloPassOverlays();
   private final int[] cachedRingLights = new int[HALO_RINGS.length];
   private int cachedPrimaryColor = Integer.MIN_VALUE;
   private int cachedSecondaryColor = Integer.MIN_VALUE;

   public static ShaderHands getInstance() {
      return instance;
   }

   public ShaderHands() {
      super("ShaderHands", "Накладывает шейдер на руки от первого лица", ModuleCategory.RENDER);
      instance = this;
      this.settings(
         this.mode,
         this.colorMode,
         this.color1,
         this.color2,
         this.speed,
         this.fill,
         this.trail,
         this.trailFade,
         this.trailRise,
         this.trailSway,
         this.trailTurbulence,
         this.trailFlicker,
         this.trailBurst,
         this.trailBurstPower,
         this.trailModel,
         this.trailModelAlpha
      );
   }

   public float fillFraction() {
      return this.renderCacheValid ? this.frameFill : this.fill.getValue() / 100.0F;
   }

   public boolean isGlowMode() {
      return this.mode.isSelected("Свечение");
   }

   public boolean isTrailEnabled() {
      return this.isState() && this.isTrailSelected();
   }

   private boolean isTrailSelected() {
      return this.isGlowMode() && this.trail.isValue();
   }

   @Override
   public void deactivate() {
      ShaderHandsTrail.reset();
      this.renderCacheValid = false;
   }

   public void refreshRenderCache() {
      this.framePrimaryColor = this.getPrimaryColor();
      this.frameSecondaryColor = this.getSecondaryColor();
      this.frameSpeed = this.speed.getValue();
      this.frameFill = this.fill.getValue() / 100.0F;
      this.frameColor1 = ShaderPacking.packColorAlpha(this.framePrimaryColor);
      this.frameItemFillColor = ShaderPacking.packColorAlpha(this.framePrimaryColor, this.frameFill);
      this.frameBakedItemFill = ShaderPacking.packBakedItemColors(this.framePrimaryColor, this.frameSecondaryColor, this.frameSpeed, this.frameFill);
      this.frameTrailModelColor = ShaderPacking.packColorAlpha(this.framePrimaryColor, this.trailModelAlpha.getValue());
      this.frameTrailModelOverlay = ShaderPacking.packHaloPass(0.0F, 0.0F, this.frameSpeed);
      this.frameColor2Speed = ShaderPacking.packColorSpeedFill(this.frameSecondaryColor, this.frameSpeed, this.frameFill);
      this.renderFrameSerial++;
      this.renderCacheValid = true;
   }

   public long getRenderFrameSerial() {
      this.ensureRenderCache();
      return this.renderFrameSerial;
   }

   private void ensureRenderCache() {
      if (!this.renderCacheValid) {
         this.refreshRenderCache();
      }
   }

   public int packColor1() {
      this.ensureRenderCache();
      return this.frameColor1;
   }

   public int packItemFillColor() {
      this.ensureRenderCache();
      return this.frameItemFillColor;
   }

   public int packBakedItemFill() {
      this.ensureRenderCache();
      return this.frameBakedItemFill;
   }

   public int packTrailModelColor() {
      this.ensureRenderCache();
      return this.frameTrailModelColor;
   }

   public boolean isTrailModelEnabled() {
      return this.isTrailEnabled() && this.trailModel.isValue();
   }

   public int packTrailModelOverlay() {
      this.ensureRenderCache();
      return this.frameTrailModelOverlay;
   }

   public int packColor2Speed() {
      this.ensureRenderCache();
      return this.frameColor2Speed;
   }

   private static float[][] buildHaloRings() {
      float[] radii = new float[]{1.5F, 3.0F, 4.5F, 6.0F, 8.0F, 10.0F};
      float sigma = 5.0F;
      float peak = 0.32F;
      float[][] rings = new float[radii.length][3];

      for (int i = 0; i < radii.length; i++) {
         float cur = peak * (float)Math.exp(-Math.pow(radii[i] / sigma, 2.0));
         float next = i + 1 < radii.length ? peak * (float)Math.exp(-Math.pow(radii[i + 1] / sigma, 2.0)) : 0.0F;
         int dirs = Math.max(8, Math.min(16, Math.round((float)((Math.PI * 2) * radii[i]) / 3.5F)));
         rings[i][0] = radii[i];
         rings[i][1] = Math.max(0.008F, cur - next);
         rings[i][2] = dirs;
      }

      return rings;
   }

   private static int[] buildHaloPassRings() {
      int count = 0;

      for (float[] ring : HALO_RINGS) {
         count += (int)ring[2];
      }

      int[] result = new int[count];
      int pass = 0;

      for (int ring = 0; ring < HALO_RINGS.length; ring++) {
         int directions = (int)HALO_RINGS[ring][2];

         for (int direction = 0; direction < directions; direction++) {
            result[pass++] = ring;
         }
      }

      return result;
   }

   private static int[][] buildHaloPassOverlays() {
      int[][] overlays = new int[16][HALO_PASS_RINGS.length];

      for (int speedStep = 0; speedStep < overlays.length; speedStep++) {
         float speed = speedStep / 15.0F * 3.0F;
         int pass = 0;

         for (int ring = 0; ring < HALO_RINGS.length; ring++) {
            float radius = HALO_RINGS[ring][0];
            int directions = (int)HALO_RINGS[ring][2];
            float stagger = ring * (float) Math.PI / directions;

            for (int direction = 0; direction < directions; direction++) {
               float angle = stagger + (float) (Math.PI * 2) * direction / directions;
               float passRadius = radius + (direction % 2 == 0 ? 0.0F : 1.2F);
               overlays[speedStep][pass++] = ShaderPacking.packHaloPass(angle, passRadius, speed);
            }
         }
      }

      return overlays;
   }

   public void forEachHaloPass(ShaderHands.HaloPassConsumer pass) {
      this.forEachHaloPass(pass, true);
   }

   public void forEachHaloOutlinePass(ShaderHands.HaloPassConsumer pass) {
      this.forEachHaloPass(pass, false);
   }

   public void forEachItemHaloOutlinePass(int quadCount, boolean duplicatedForTrail, ShaderHands.HaloPassConsumer pass) {
      if (quadCount <= 96) {
         this.forEachHaloPass(pass, false);
      } else {
         int passLimit;
         if (quadCount >= 600) {
            passLimit = duplicatedForTrail ? 8 : 12;
         } else if (quadCount >= 300) {
            passLimit = duplicatedForTrail ? 10 : 16;
         } else {
            passLimit = duplicatedForTrail ? 14 : 20;
         }

         this.forEachReducedHaloPass(pass, passLimit);
      }
   }

   private void forEachReducedHaloPass(ShaderHands.HaloPassConsumer pass, int passLimit) {
      this.ensureRenderCache();
      int c1 = this.framePrimaryColor;
      int c2 = this.frameSecondaryColor;
      this.updateRingLights(c1, c2);
      int speedStep = Math.max(0, Math.min(15, Math.round(this.frameSpeed / 3.0F * 15.0F)));
      int[] overlays = HALO_PASS_OVERLAYS[speedStep];
      int totalPasses = HALO_PASS_RINGS.length;
      float alphaScale = (float)totalPasses / passLimit;

      for (int sample = 0; sample < passLimit; sample++) {
         int index = Math.min(totalPasses - 1, Math.round((sample + 0.5F) * totalPasses / passLimit - 0.5F));
         int packedLight = scalePackedAlpha(this.cachedRingLights[HALO_PASS_RINGS[index]], alphaScale);
         pass.accept(packedLight, overlays[index]);
      }
   }

   private static int scalePackedAlpha(int packedColor, float scale) {
      int alpha = packedColor >>> 16 & 0xFF;
      int scaledAlpha = Math.min(255, Math.round(alpha * scale));
      return packedColor & -16711681 | scaledAlpha << 16;
   }

   private void forEachHaloPass(ShaderHands.HaloPassConsumer pass, boolean includeFill) {
      this.ensureRenderCache();
      int c1 = this.framePrimaryColor;
      int c2 = this.frameSecondaryColor;
      this.updateRingLights(c1, c2);
      int speedStep = Math.max(0, Math.min(15, Math.round(this.frameSpeed / 3.0F * 15.0F)));
      int[] overlays = HALO_PASS_OVERLAYS[speedStep];

      for (int index = 0; index < HALO_PASS_RINGS.length; index++) {
         pass.accept(this.cachedRingLights[HALO_PASS_RINGS[index]], overlays[index]);
      }

      if (includeFill && this.fillFraction() > 0.01F) {
         pass.accept(ShaderPacking.packColorAlpha(c1, this.fillFraction() * 0.5F), this.frameTrailModelOverlay);
      }
   }

   private void updateRingLights(int primary, int secondary) {
      if (primary != this.cachedPrimaryColor || secondary != this.cachedSecondaryColor) {
         this.cachedPrimaryColor = primary;
         this.cachedSecondaryColor = secondary;

         for (int ring = 0; ring < HALO_RINGS.length; ring++) {
            int color = lerpColor(primary, secondary, (float)ring / (HALO_RINGS.length - 1));
            this.cachedRingLights[ring] = ShaderPacking.packColorAlpha(color, HALO_RINGS[ring][1]);
         }
      }
   }

   public int getPrimaryColor() {
      return this.colorMode.isSelected("От темы") ? ColorUtil.getClientColorAt(0.0F) : this.color1.getColor();
   }

   public int getSecondaryColor() {
      return this.colorMode.isSelected("От темы") ? ColorUtil.getClientColorAt(0.5F) : this.color2.getColorNoAlpha();
   }

   private static int lerpColor(int a, int b, float t) {
      int aa = a >>> 24 & 0xFF;
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int ba = b >>> 24 & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int ra = Math.round(aa + (ba - aa) * t);
      int rr = Math.round(ar + (br - ar) * t);
      int rg = Math.round(ag + (bg - ag) * t);
      int rb = Math.round(ab + (bb - ab) * t);
      return ra << 24 | rr << 16 | rg << 8 | rb;
   }

   @FunctionalInterface
   public interface HaloPassConsumer {
      void accept(int var1, int var2);
   }
}
