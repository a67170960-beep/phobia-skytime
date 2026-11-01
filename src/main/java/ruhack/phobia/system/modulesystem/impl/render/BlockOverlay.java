package ruhack.phobia.system.modulesystem.impl.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.WorldRenderEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.rendersystem.render3d.watercaustic.WaterCaustic3D;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.istance.Instance;

public class BlockOverlay extends ModuleStructure {
   private static final double SMOOTHING_SPEED = 14.0;
   private final SelectSetting colorMode = new SelectSetting("Цвет", "Источник цветов подсветки", "От темы", "От темы", "Кастомный");
   private final ColorSetting color = new ColorSetting("Цвет фона", "Цвет заполнения фона").value(22912).visible(() -> this.colorMode.isSelected("Кастомный"));
   private final ColorSetting patternColor = new ColorSetting("Цвет узора", "Цвет самого эффекта воды")
      .value(16777215)
      .visible(() -> this.colorMode.isSelected("Кастомный"));
   private final SliderSettings alpha = new SliderSettings("Прозрачность", "Прозрачность эффекта", 0.4F).range(0.1F, 1.0F).step(0.01F);
   private final SliderSettings scale = new SliderSettings("Масштаб", "Масштаб узора", 0.5F).range(0.1F, 2.0F).step(0.1F);
   private final SliderSettings speed = new SliderSettings("Скорость", "Скорость анимации", 1.0F).range(0.1F, 5.0F).step(0.1F);
   private final BooleanSetting smoothness = new BooleanSetting("Плавность", "Плавно перемещает подсветку между блоками");
   private final List<net.minecraft.util.math.Box> animatedBoxes = new ArrayList<>();
   private long lastRenderNanos;

   public static BlockOverlay getInstance() {
      return Instance.get(BlockOverlay.class);
   }

   public BlockOverlay() {
      super("BlockOverlay", "Красивая подсветка блока под прицелом", ModuleCategory.RENDER);
      this.settings(this.colorMode, this.color, this.patternColor, this.alpha, this.scale, this.speed, this.smoothness);
   }

   @Override
   public void activate() {
      this.resetAnimation();
      EventManager.register(this);
   }

   @Override
   public void deactivate() {
      EventManager.unregister(this);
      this.resetAnimation();
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      List<net.minecraft.util.math.Box> targetBoxes = this.getTargetBoxes();
      if (targetBoxes.isEmpty()) {
         this.resetAnimation();
      } else {
         List<net.minecraft.util.math.Box> renderBoxes = this.smoothness.isValue() ? this.updateAnimatedBoxes(targetBoxes) : copyBoxes(targetBoxes);
         if (!this.smoothness.isValue()) {
            this.animatedBoxes.clear();
            this.animatedBoxes.addAll(renderBoxes);
         }

         float time = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F * this.speed.getValue();
         WaterCaustic3D.begin();

         for (net.minecraft.util.math.Box box : renderBoxes) {
            WaterCaustic3D.box(box);
         }

         int background = this.colorMode.isSelected("От темы") ? ColorUtil.getClientColorAt(0.0F) : this.color.getColorNoAlpha();
         int pattern = this.colorMode.isSelected("От темы") ? ColorUtil.getClientColorAt(0.5F) : this.patternColor.getColorNoAlpha();
         WaterCaustic3D.end(background, pattern, this.alpha.getValue(), time, this.scale.getValue());
      }
   }

   private List<net.minecraft.util.math.Box> getTargetBoxes() {
      if (mc.player != null
         && mc.world != null
         && mc.crosshairTarget instanceof BlockHitResult blockHit
         && blockHit.getType() == Type.BLOCK
         && !(mc.player.getEyePos().distanceTo(blockHit.getPos()) > mc.player.getBlockInteractionRange())) {
         BlockPos pos = blockHit.getBlockPos();
         BlockState state = mc.world.getBlockState(pos);
         if (!state.isAir()
            && !state.isOf(Blocks.SHORT_GRASS)
            && !state.isOf(Blocks.TALL_GRASS)
            && !state.isOf(Blocks.FERN)
            && !state.isOf(Blocks.LARGE_FERN)
            && !state.isOf(Blocks.SEAGRASS)
            && !state.isOf(Blocks.TALL_SEAGRASS)) {
            VoxelShape shape = state.getOutlineShape(mc.world, pos);
            if (shape.isEmpty()) {
               return List.of();
            }

            List<net.minecraft.util.math.Box> boxes = new ArrayList<>(shape.getBoundingBoxes().size());

            for (net.minecraft.util.math.Box box : shape.getBoundingBoxes()) {
               boxes.add(box.offset(pos).expand(0.002));
            }

            return boxes;
         } else {
            return List.of();
         }
      } else {
         return List.of();
      }
   }

   private List<net.minecraft.util.math.Box> updateAnimatedBoxes(List<net.minecraft.util.math.Box> targets) {
      long now = System.nanoTime();
      double deltaSeconds = this.lastRenderNanos == 0L ? 0.016666666666666666 : Math.min(0.1, (now - this.lastRenderNanos) / 1.0E9);
      this.lastRenderNanos = now;
      if (this.animatedBoxes.isEmpty()) {
         this.animatedBoxes.addAll(copyBoxes(targets));
         return this.animatedBoxes;
      }

      while (this.animatedBoxes.size() < targets.size()) {
         this.animatedBoxes.add(this.animatedBoxes.get(this.animatedBoxes.size() - 1));
      }

      while (this.animatedBoxes.size() > targets.size()) {
         this.animatedBoxes.remove(this.animatedBoxes.size() - 1);
      }

      double factor = 1.0 - Math.exp(-14.0 * deltaSeconds);

      for (int i = 0; i < targets.size(); i++) {
         this.animatedBoxes.set(i, interpolate(this.animatedBoxes.get(i), targets.get(i), factor));
      }

      return this.animatedBoxes;
   }

   private static List<net.minecraft.util.math.Box> copyBoxes(List<net.minecraft.util.math.Box> boxes) {
      return new ArrayList<>(boxes);
   }

   private static net.minecraft.util.math.Box interpolate(net.minecraft.util.math.Box from, net.minecraft.util.math.Box to, double factor) {
      return new net.minecraft.util.math.Box(
         lerp(from.minX, to.minX, factor),
         lerp(from.minY, to.minY, factor),
         lerp(from.minZ, to.minZ, factor),
         lerp(from.maxX, to.maxX, factor),
         lerp(from.maxY, to.maxY, factor),
         lerp(from.maxZ, to.maxZ, factor)
      );
   }

   private static double lerp(double from, double to, double factor) {
      return from + (to - from) * factor;
   }

   private void resetAnimation() {
      this.animatedBoxes.clear();
      this.lastRenderNanos = 0L;
   }
}
