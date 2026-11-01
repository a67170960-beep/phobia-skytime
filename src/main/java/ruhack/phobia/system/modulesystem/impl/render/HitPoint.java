package ruhack.phobia.system.modulesystem.impl.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.hit.EntityHitResult;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.AttackEvent;
import ruhack.phobia.system.eventsystem.impl.WorldRenderEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.rendersystem.render3d.fill.Fill3D;
import ruhack.phobia.system.rendersystem.render3d.glow.Glow3D;
import ruhack.phobia.system.rendersystem.render3d.line.Line3D;
import ruhack.phobia.system.rendersystem.render3d.particle.Particle;

public class HitPoint extends ModuleStructure {
   private final List<Particle> particles = new ArrayList<>();
   private final ArrayDeque<Particle> particlePool = new ArrayDeque<>();
   private final ColorSetting color = new ColorSetting("Цвет", "Цвет точки удара").value(16722474);
   private final SliderSettings size = new SliderSettings("Размер", "Размер квадратика", 0.15F).range(0.05F, 0.5F).step(0.01F);
   private final SliderSettings lifetime = new SliderSettings("Время жизни", "Длительность отображения", 100.0F).range(20, 300);
   private final BooleanSetting glow = new BooleanSetting("Свечение", "Эффект свечения").setValue(true);
   private final BooleanSetting lines = new BooleanSetting("Линии", "Отображать контуры").setValue(true);

   public HitPoint() {
      super("HitPoint", "Отображает место удара по энтити", ModuleCategory.RENDER);
      this.settings(this.color, this.size, this.lifetime, this.glow, this.lines);
   }

   @Override
   public void activate() {
      EventManager.register(this);
      this.recycleParticles();
   }

   @Override
   public void deactivate() {
      EventManager.unregister(this);
      this.recycleParticles();
   }

   @EventHandler
   public void onAttack(AttackEvent event) {
      if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() == event.getTarget()) {
         this.spawn(hit.getPos().x, hit.getPos().y, hit.getPos().z);
      } else {
         this.spawn(event.getTarget().getX(), event.getTarget().getY() + event.getTarget().getHeight() / 2.0, event.getTarget().getZ());
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (!this.particles.isEmpty()) {
         Iterator<Particle> it = this.particles.iterator();

         while (it.hasNext()) {
            Particle p = it.next();
            p.update(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0.0F, 0.0F, 0.0F, 0.0, 0.0, 0.0, 0.0F, 0.0F, 0.0F);
            if (p.isDead()) {
               it.remove();
               this.particlePool.addFirst(p);
            }
         }

         int c = this.color.getColorNoAlpha();
         if (this.glow.isValue()) {
            Glow3D.begin(true);

            for (Particle p : this.particles) {
               float s = p.getCurrentSize(false, 0.0F, 0.0F);
               Glow3D.glow(p.x, p.y, p.z, s * 1.5F, c, p.getAlpha() * 0.4F);
               Glow3D.core(p.x, p.y, p.z, s * 0.4F, c, p.getAlpha() * 0.9F);
            }

            Glow3D.end();
         }

         Fill3D.begin(true);

         for (Particle p : this.particles) {
            Fill3D.box(p.x, p.y, p.z, p.getCurrentSize(false, 0.0F, 0.0F), c, p.getAlpha() * 0.15F);
         }

         Fill3D.end();
         if (this.lines.isValue()) {
            Line3D.begin(true);

            for (Particle p : this.particles) {
               Line3D.box(p.x, p.y, p.z, p.getCurrentSize(false, 0.0F, 0.0F), c, p.getAlpha() * 0.8F);
            }

            Line3D.end();
         }
      }
   }

   private void spawn(double x, double y, double z) {
      Particle particle = this.particlePool.pollFirst();
      if (particle == null) {
         particle = new Particle(x, y, z, this.size.getValue(), this.lifetime.getInt());
      } else {
         particle.reset(x, y, z, this.size.getValue(), this.lifetime.getInt());
      }

      this.particles.add(particle);
   }

   private void recycleParticles() {
      this.particlePool.addAll(this.particles);
      this.particles.clear();
   }
}
