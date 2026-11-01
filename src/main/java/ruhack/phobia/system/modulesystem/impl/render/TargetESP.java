package ruhack.phobia.system.modulesystem.impl.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.WorldRenderEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.rendersystem.render3d.billboard.Billboard3D;
import ruhack.phobia.system.rendersystem.render3d.ring.Ring3D;
import ruhack.phobia.system.rendersystem.render3d.soul.Soul3D;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.istance.Instance;

public class TargetESP extends ModuleStructure {
   private static final Identifier TARGET = Identifier.of("phobia", "textures/targetesp/target.png");
   private static final double[] GHOST_BASE_SPEED = new double[]{-0.25, -0.3, 0.25};
   private static final double[] GHOST_HURT_SPEED = new double[]{-0.3, -0.35, 0.3};
   private static final double[] GHOST_X_SIGN = new double[]{1.0, -1.0, -1.0};
   private static final double[] GHOST_Y_CURVE = new double[]{0.4, 0.3, -0.4};
   private static final double[] GHOST_Y_BASE = new double[]{0.7, 0.08, -0.7};
   private static final double[] GHOST_Z_SIGN = new double[]{-1.0, -1.0, 1.0};
   private static final boolean[] GHOST_USE_X = new boolean[]{false, true, true};
   private final SelectSetting mode = new SelectSetting(
      "Вид", "Визуальный эффект текущей цели", "Призраки", "Кольцо", "Ромб", "Души", "Ghosts", "Не отображать"
   );
   private final BooleanSetting reddenOnHit = new BooleanSetting("Краснеть при ударе", "Окрашивает эффект цели в красный при получении урона").setValue(true);
   private final SliderSettings ghostLength = new SliderSettings("Длина душ", "Продолжительность временного хвоста", 1.0F)
      .range(0.3F, 2.0F)
      .step(0.05F)
      .visible(() -> this.mode.isSelected("Ghosts"));
   private final SliderSettings ghostWidth = new SliderSettings("Ширина", "Расстояние душ от цели", 1.0F)
      .range(0.4F, 2.0F)
      .step(0.05F)
      .visible(() -> this.mode.isSelected("Ghosts"));
   private final SliderSettings soulSize = new SliderSettings("Размер душ", "Масштаб огоньков", 1.0F)
      .range(0.4F, 2.0F)
      .step(0.05F)
      .visible(() -> this.mode.isSelected("Души") || this.mode.isSelected("Призраки"));
   private final SliderSettings soulLength = new SliderSettings("Длина хвоста", "Длина шлейфа за душой", 0.9F)
      .range(0.2F, 1.8F)
      .step(0.05F)
      .visible(() -> this.mode.isSelected("Души") || this.mode.isSelected("Призраки"));
   private final SliderSettings soulSpeed = new SliderSettings("Скорость душ", "Скорость полёта вокруг цели", 1.0F)
      .range(0.2F, 3.0F)
      .step(0.1F)
      .visible(() -> this.mode.isSelected("Души") || this.mode.isSelected("Призраки"));
   private final SliderSettings soulBright = new SliderSettings("Яркость душ", "Яркость свечения", 1.0F)
      .range(0.3F, 1.5F)
      .step(0.05F)
      .visible(() -> this.mode.isSelected("Души") || this.mode.isSelected("Призраки"));
   private final SliderSettings ringSize = new SliderSettings("Размер кольца", "Масштаб радиуса кольца", 1.0F)
      .range(0.5F, 2.0F)
      .step(0.05F)
      .visible(() -> this.mode.isSelected("Кольцо"));
   private final SliderSettings ringThickness = new SliderSettings("Толщина кольца", "Толщина светящейся трубки", 1.0F)
      .range(0.4F, 2.5F)
      .step(0.05F)
      .visible(() -> this.mode.isSelected("Кольцо"));
   private final SliderSettings ringSpeed = new SliderSettings("Скорость кольца", "Скорость движения по цели", 1.0F)
      .range(0.3F, 2.5F)
      .step(0.1F)
      .visible(() -> this.mode.isSelected("Кольцо"));
   private final SliderSettings ringBright = new SliderSettings("Яркость кольца", "Яркость свечения кольца", 1.0F)
      .range(0.3F, 1.5F)
      .step(0.05F)
      .visible(() -> this.mode.isSelected("Кольцо"));
   private final double[] ghostPhase = new double[3];
   private final Vec3d[] floatingGhostPosition = new Vec3d[3];
   private final List<Vec3d>[] floatingGhostTrails = new List[]{new ArrayList(), new ArrayList(), new ArrayList()};
   private double soulPhase;
   private double floatingGhostPhase;
   private int floatingGhostTargetId = Integer.MIN_VALUE;
   private LivingEntity lastTarget;
   private Vec3d lastPosition;
   private long lastFrameTime;
   private float appearance;
   private float hurtAnimation;
   private double diamondRotation;

   public TargetESP() {
      super("TargetESP", "Отображает текущую цель KillAura", ModuleCategory.RENDER);
      this.settings(
         this.mode,
         this.reddenOnHit,
         this.ghostLength,
         this.ghostWidth,
         this.soulSize,
         this.soulLength,
         this.soulSpeed,
         this.soulBright,
         this.ringSize,
         this.ringThickness,
         this.ringSpeed,
         this.ringBright
      );
   }

   public static TargetESP getInstance() {
      return Instance.get(TargetESP.class);
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      long now = System.currentTimeMillis();
      float delta = this.lastFrameTime == 0L ? 0.016F : Math.min(0.05F, (float)(now - this.lastFrameTime) / 1000.0F);
      this.lastFrameTime = now;
      KillAura aura = KillAura.getInstance();
      LivingEntity target = aura != null && aura.isState() ? aura.getTarget() : null;
      boolean visible = target != null && target.isAlive() && !this.mode.isSelected("Не отображать");
      this.appearance = approach(this.appearance, visible ? 1.0F : 0.0F, delta * 6.0F);
      if (visible) {
         this.lastTarget = target;
         this.lastPosition = interpolate(target, event.getPartialTicks());
      }

      if (!(this.appearance <= 0.01F) && this.lastTarget != null && this.lastPosition != null) {
         float hurt = visible && this.lastTarget.maxHurtTime > 0
            ? MathHelper.clamp((float)this.lastTarget.hurtTime / this.lastTarget.maxHurtTime, 0.0F, 1.0F)
            : 0.0F;
         this.hurtAnimation = approach(this.hurtAnimation, hurt, delta * 10.0F);
         int color = this.reddenOnHit.isValue() ? hurtColor(ColorUtil.getClientColor(), this.hurtAnimation) : ColorUtil.getClientColor();
         if (!this.mode.isSelected("Ghosts") && this.floatingGhostTargetId != Integer.MIN_VALUE) {
            this.resetFloatingGhosts();
            this.floatingGhostTargetId = Integer.MIN_VALUE;
         }

         switch (this.mode.getValue()) {
            case "Кольцо":
               this.renderRing(this.lastTarget, this.lastPosition, color);
               break;
            case "Ромб":
               this.renderDiamond(this.lastTarget, this.lastPosition, color, delta);
               break;
            case "Призраки":
               this.renderGhosts(this.lastTarget, this.lastPosition, color, delta);
               break;
            case "Души":
               this.renderSouls(this.lastTarget, this.lastPosition, color, delta);
               break;
            case "Ghosts":
               this.renderFloatingGhosts(this.lastTarget, this.lastPosition, color, delta);
         }
      } else {
         if (!visible) {
            this.lastTarget = null;
            this.lastPosition = null;
            this.resetFloatingGhosts();
            this.floatingGhostTargetId = Integer.MIN_VALUE;
         }
      }
   }

   private void renderRing(LivingEntity target, Vec3d position, int color) {
      double duration = 3000.0 / this.ringSpeed.getValue();
      double elapsed = System.currentTimeMillis() % (long)duration;
      boolean reverse = elapsed > duration / 2.0;
      double progress = elapsed / (duration / 2.0);
      progress = reverse ? progress - 1.0 : 1.0 - progress;
      progress = progress < 0.5 ? 2.0 * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;
      double fadeHeight = target.getHeight() * 0.5 * (progress > 0.5 ? 1.0 - progress : progress) * (reverse ? -1.0 : 1.0);
      double y = position.y + target.getHeight() * progress;
      float radius = target.getWidth() * 0.8F * this.ringSize.getValue();
      float tube = Math.max(0.045F, radius * 0.1F) * this.ringThickness.getValue();
      float bright = this.ringBright.getValue();
      Ring3D.begin(true);
      Ring3D.ring(position.x, y, position.z, radius, tube, color, this.appearance * bright);
      int trail = 3;

      for (int i = 1; i <= trail; i++) {
         float k = (float)i / trail;
         Ring3D.ring(position.x, y + fadeHeight * k, position.z, radius, tube, color, this.appearance * bright * 0.22F * (1.0F - k));
      }

      Ring3D.end();
   }

   private void renderDiamond(LivingEntity target, Vec3d position, int color, float delta) {
      this.diamondRotation = this.diamondRotation + 95.5 * delta * (this.lastTarget == target ? 1.0 : 1.5);
      float size = (1.0F + this.hurtAnimation * -0.5F) * this.appearance;
      Billboard3D.draw(
         TARGET,
         position.x,
         position.y + target.getHeight() * 0.5,
         position.z,
         size,
         (float)(Math.sin(Math.toRadians(this.diamondRotation)) * 180.0),
         color,
         this.appearance,
         true
      );
   }

   private void renderGhosts(LivingEntity target, Vec3d position, int color, float delta) {
      double centerX = position.x;
      double centerY = position.y + target.getHeight() / 1.75;
      double centerZ = position.z;
      float scale = this.soulSize.getValue();
      float bright = this.soulBright.getValue();
      float spd = this.soulSpeed.getValue();
      Soul3D.begin(true);

      for (int trail = 0; trail < 3; trail++) {
         double speed = MathHelper.lerp(this.hurtAnimation, GHOST_BASE_SPEED[trail], GHOST_HURT_SPEED[trail]);
         this.ghostPhase[trail] = this.ghostPhase[trail] + (trail == 1 ? -speed : speed) * delta * 23.53 * spd;

         for (int i = 0; i < 25; i++) {
            float progress = 1.0F - i / 25.0F;
            float size = (0.1F + 0.25F * progress) * scale * this.appearance;
            float alpha = progress * bright * this.appearance;
            double angle = this.ghostPhase[trail] - i * 10.0 * (trail == 1 ? -speed : speed) / 42.5;
            double ox = Math.sin(angle) * 0.7;
            double oy = Math.cos(angle) * 0.7;
            Soul3D.soul(
               centerX + GHOST_X_SIGN[trail] * ox,
               centerY + (GHOST_USE_X[trail] ? ox * GHOST_Y_CURVE[trail] : oy * GHOST_Y_CURVE[trail]) + GHOST_Y_BASE[trail],
               centerZ + GHOST_Z_SIGN[trail] * oy,
               size,
               color,
               alpha,
               trail * 64 + i
            );
         }
      }

      Soul3D.end();
   }

   private void renderFloatingGhosts(LivingEntity target, Vec3d position, int color, float delta) {
      if (this.floatingGhostTargetId != target.getId()) {
         this.resetFloatingGhosts();
         this.floatingGhostTargetId = target.getId();
      }

      int trailLength = Math.max(8, Math.round(25.0F * this.ghostLength.getValue()));
      double centerX = position.x;
      double centerY = position.y + target.getHeight() / 1.75;
      double centerZ = position.z;
      double radius = Math.max(0.8, Math.max(target.getWidth(), target.getHeight() * 0.35) * 0.95) * this.ghostWidth.getValue();
      this.floatingGhostPhase = this.floatingGhostPhase + delta * 3.333 * this.soulSpeed.getValue();
      double sin = Math.sin(this.floatingGhostPhase) * radius;
      double cos = Math.cos(this.floatingGhostPhase) * radius;
      Soul3D.begin(true);

      for (int ghost = 0; ghost < 3; ghost++) {
         Vec3d current = switch (ghost) {
            case 0 -> new Vec3d(centerX + sin, centerY + cos, centerZ - cos);
            case 1 -> new Vec3d(centerX - sin, centerY + sin, centerZ - cos);
            default -> new Vec3d(centerX - sin, centerY - sin, centerZ + cos);
         };
         this.floatingGhostPosition[ghost] = current;
         List<Vec3d> trail = this.floatingGhostTrails[ghost];
         if (trail.isEmpty() || trail.getFirst().distanceTo(current) > 0.015) {
            trail.addFirst(current);

            while (trail.size() > trailLength) {
               trail.removeLast();
            }
         }

         int ghostColor = ghost == 0 ? color : ColorUtil.getClientColorAt(ghost / 2.0F);
         if (ghost > 0 && this.reddenOnHit.isValue()) {
            ghostColor = hurtColor(ghostColor, this.hurtAnimation);
         }

         int brightColor = ColorUtil.interpolateColor(ghostColor, ColorUtil.rgba(255, 255, 255, 255), 0.4F);

         for (int index = 0; index < trail.size(); index++) {
            float progress = 1.0F - (float)index / trailLength;
            boolean head = index == 0;
            float scale = (head ? 0.28F : 0.04F + 0.21F * (float)Math.pow(progress, 1.3F)) * this.appearance;
            float opacity = (head ? 0.8F : 0.35F * (float)Math.pow(progress, 1.5)) * this.appearance;
            Vec3d trailPosition = trail.get(index);
            Soul3D.soul(trailPosition.x, trailPosition.y, trailPosition.z, scale, brightColor, opacity, ghost * 64 + index);
            if (head) {
               Soul3D.soul(trailPosition.x, trailPosition.y, trailPosition.z, 0.55F * this.appearance, brightColor, 0.14F * this.appearance, 192 + ghost);
            }
         }
      }

      Soul3D.end();
   }

   private void resetFloatingGhosts() {
      for (int ghost = 0; ghost < this.floatingGhostPosition.length; ghost++) {
         this.floatingGhostPosition[ghost] = null;
         this.floatingGhostTrails[ghost].clear();
      }
   }

   private void renderSouls(LivingEntity target, Vec3d position, int color, float delta) {
      double centerX = position.x;
      double centerY = position.y + target.getHeight() / 1.75;
      double centerZ = position.z;
      double radius = Math.max(0.8, Math.max(target.getWidth(), target.getHeight() * 0.35) * 0.95);
      this.soulPhase = this.soulPhase + delta * 3.333 * this.soulSpeed.getValue();
      float scale = this.soulSize.getValue();
      float bright = this.soulBright.getValue();
      int trailLength = 48;
      double arcStep = this.soulLength.getValue() / trailLength;
      float hurtBoost = 1.0F + this.hurtAnimation * 0.35F;
      Soul3D.begin(true);

      for (int i = 0; i < trailLength; i++) {
         double angle = this.soulPhase - i * arcStep;
         double sin = Math.sin(angle) * radius;
         double cos = Math.cos(angle) * radius;
         float progress = 1.0F - (float)i / trailLength;
         boolean head = i == 0;
         float size = (head ? 0.28F : 0.04F + 0.21F * (float)Math.pow(progress, 1.3F)) * scale * hurtBoost * this.appearance;
         float alpha = (head ? 0.8F : 0.35F * (float)Math.pow(progress, 1.5)) * bright * this.appearance;
         Soul3D.soul(centerX + sin, centerY + cos, centerZ - cos, size, color, alpha, i);
         Soul3D.soul(centerX - sin, centerY + sin, centerZ - cos, size, color, alpha, 64 + i);
         Soul3D.soul(centerX - sin, centerY - sin, centerZ + cos, size, color, alpha, 128 + i);
         if (head) {
            float auraSize = 0.55F * scale * hurtBoost * this.appearance;
            float auraAlpha = 0.14F * bright * this.appearance;
            Soul3D.soul(centerX + sin, centerY + cos, centerZ - cos, auraSize, color, auraAlpha, 60);
            Soul3D.soul(centerX - sin, centerY + sin, centerZ - cos, auraSize, color, auraAlpha, 124);
            Soul3D.soul(centerX - sin, centerY - sin, centerZ + cos, auraSize, color, auraAlpha, 188);
         }
      }

      Soul3D.end();
   }

   private static Vec3d interpolate(LivingEntity entity, float tickDelta) {
      return new Vec3d(
         MathHelper.lerp(tickDelta, entity.lastX, entity.getX()),
         MathHelper.lerp(tickDelta, entity.lastY, entity.getY()),
         MathHelper.lerp(tickDelta, entity.lastZ, entity.getZ())
      );
   }

   private static float approach(float current, float target, float amount) {
      return current + (target - current) * MathHelper.clamp(amount, 0.0F, 1.0F);
   }

   private static int hurtColor(int base, float amount) {
      return ColorUtil.getColor(
         MathHelper.lerp(amount, ColorUtil.red(base), 255),
         MathHelper.lerp(amount, ColorUtil.green(base), 0),
         MathHelper.lerp(amount, ColorUtil.blue(base), 0),
         255
      );
   }
}
