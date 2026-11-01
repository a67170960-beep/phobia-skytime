package ruhack.phobia.system.modulesystem.impl.render;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.DrawEvent;
import ruhack.phobia.system.eventsystem.impl.WorldRenderEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.rendersystem.render3d.line.Line3D;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.render.WorldToScreen;

public class Prediction extends ModuleStructure {
   private static final int MAX_STEPS = 300;
   private static final float TICK_SECONDS = 0.05F;
   private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ROOT));
   private static final ItemStack PEARL_ICON = new ItemStack(Items.ENDER_PEARL);
   private static final ItemStack ARROW_ICON = new ItemStack(Items.ARROW);
   private static final ItemStack TRIDENT_ICON = new ItemStack(Items.TRIDENT);
   private final MultiSelectSetting projectiles = new MultiSelectSetting("Снаряды", "Выберите снаряды для расчета траектории")
      .value("Эндер Пёрл", "Стрела", "Трезубец")
      .selected("Эндер Пёрл", "Стрела", "Трезубец");
   private final List<Prediction.Impact> impacts = new ArrayList<>();
   private final Vector2f projectedImpact = new Vector2f();
   private final Mutable predictedBlockPos = new Mutable();
   private final int[] trajectoryColors = new int[301];
   private boolean trajectoryColorsPrepared;
   private boolean showPearls;
   private boolean showArrows;
   private boolean showTridents;

   public Prediction() {
      super("Prediction", "Предугадывает куда и за сколько времени упадет снаряд", ModuleCategory.RENDER);
      this.settings(this.projectiles);
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      this.impacts.clear();
      if (mc.player != null && mc.world != null) {
         this.showPearls = this.projectiles.getSelected().contains(this.projectiles.getList().get(0));
         this.showArrows = this.projectiles.getSelected().contains(this.projectiles.getList().get(1));
         this.showTridents = this.projectiles.getSelected().contains(this.projectiles.getList().get(2));
         if (this.showPearls || this.showArrows || this.showTridents) {
            this.trajectoryColorsPrepared = false;
            Vec3d camera = Line3D.getCameraPos();
            Line3D.begin(true);

            for (Entity entity : mc.world.getEntities()) {
               if (this.validEntity(entity) && this.isMoving(entity)) {
                  this.impacts.add(this.simulate(entity, camera, true));
               }
            }

            Line3D.end();
         }
      }
   }

   @EventHandler
   public void onDraw(DrawEvent event) {
      if (mc.player != null && mc.world != null) {
         for (Prediction.Impact impact : this.impacts) {
            if (WorldToScreen.project(impact.position().x, impact.position().y, impact.position().z, this.projectedImpact)) {
               String time = TIME_FORMAT.format(impact.steps() * 0.05F) + " сек.";
               float textSize = 5.5F;
               float textWidth = Msdf.width(MsdfManager.BOLD, time, textSize);
               float panelWidth = textWidth + 16.5F;
               float x = this.projectedImpact.x - panelWidth / 2.0F;
               float y = this.projectedImpact.y + 3.0F;
               Render2D.rect(event.getDrawContext(), x, y, panelWidth, 11.0F, 2.0F, -1207301618, false);
               this.drawItem(event, impact.stack(), x + 2.0F, y + 1.5F, 8.0F);
               Msdf.text(event.getDrawContext(), MsdfManager.BOLD, time, x + 12.0F, y + 2.2F, textSize, -1, false);
            }
         }
      }
   }

   private Prediction.Impact simulate(Entity entity, Vec3d camera, boolean draw) {
      Vec3d position = entity.getEntityPos();
      Vec3d velocity = entity.getVelocity();
      Vec3d lastPosition = position;
      double vx = velocity.x;
      double vy = velocity.y;
      double vz = velocity.z;
      int steps = 0;
      if (draw && !this.trajectoryColorsPrepared) {
         for (int i = 0; i <= 300; i++) {
            this.trajectoryColors[i] = ColorUtil.getClientColorAt(i / 300.0F);
         }

         this.trajectoryColorsPrepared = true;
      }

      for (int i = 0; i <= 300; i++) {
         steps++;
         lastPosition = position;
         Vec3d nextPosition = new Vec3d(position.x + vx, position.y + vy, position.z + vz);
         this.predictedBlockPos.set(MathHelper.floor(nextPosition.x), MathHelper.floor(nextPosition.y), MathHelper.floor(nextPosition.z));
         boolean inWater = entity.isTouchingWater() || mc.world.getFluidState(this.predictedBlockPos).isIn(FluidTags.WATER);
         double drag = inWater && !(entity instanceof TridentEntity) ? (entity instanceof EnderPearlEntity ? 0.8 : 0.6) : 0.99;
         vx *= drag;
         vy *= drag;
         vz *= drag;
         if (!entity.hasNoGravity()) {
            vy -= entity instanceof EnderPearlEntity ? 0.03 : 0.05;
         }

         if (this.hitsBlock(lastPosition, nextPosition) || nextPosition.y <= 0.0) {
            break;
         }

         if (draw) {
            Line3D.line(
               (float)(lastPosition.x - camera.x),
               (float)(lastPosition.y - camera.y),
               (float)(lastPosition.z - camera.z),
               (float)(nextPosition.x - camera.x),
               (float)(nextPosition.y - camera.y),
               (float)(nextPosition.z - camera.z),
               this.trajectoryColors[i],
               1.0F
            );
         }

         position = nextPosition;
      }

      return new Prediction.Impact(lastPosition, steps, this.projectileStack(entity));
   }

   private boolean hitsBlock(Vec3d start, Vec3d end) {
      return mc.world.raycast(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType() == Type.BLOCK;
   }

   private boolean isMoving(Entity entity) {
      return entity.getVelocity().lengthSquared() > 1.0E-6;
   }

   private boolean validEntity(Entity entity) {
      if (entity instanceof EnderPearlEntity) {
         return this.showPearls;
      } else {
         return entity instanceof ArrowEntity ? this.showArrows : entity instanceof TridentEntity && this.showTridents;
      }
   }

   private ItemStack projectileStack(Entity entity) {
      if (entity instanceof EnderPearlEntity) {
         return PEARL_ICON;
      } else {
         return entity instanceof ArrowEntity ? ARROW_ICON : TRIDENT_ICON;
      }
   }

   private void drawItem(DrawEvent event, ItemStack stack, float x, float y, float size) {
      float fixedToGui = 2.0F / Math.max(1, mc.getWindow().getScaleFactor());
      float modelScale = size * fixedToGui / 16.0F;
      Matrix3x2fStack matrices = event.getDrawContext().getMatrices();
      matrices.pushMatrix();
      matrices.translate(x * fixedToGui, y * fixedToGui);
      matrices.scale(modelScale, modelScale);
      event.getDrawContext().drawItem(stack, 0, 0);
      matrices.popMatrix();
   }

   private record Impact(Vec3d position, int steps, ItemStack stack) {
   }
}
