package ruhack.phobia.system.modulesystem.impl.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.DrawEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.screensystem.clickgui.ClickGui;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiLayout;
import ruhack.phobia.util.colorschem.ColorUtil;

public class Arrows extends ModuleStructure {
   private static final Identifier ARROW_TEXTURE = Identifier.of("phobia", "images/arrows/triangle.png");
   private static final float ARROW_TEXTURE_SIZE = 46.0F;
   private static final float HANDLED_SCREEN_RADIUS_BONUS = 65.0F;
   private final List<Matrix4f> matrixPool = new ArrayList<>();
   private final MultiSelectSetting targets = new MultiSelectSetting("Отображать", "Цели указателей")
      .value("Игроков", "Друзей", "Предметы")
      .selected("Игроков", "Друзей");
   private final ColorSetting playerColor = new ColorSetting("Цвет игроков", "Цвет обычного игрока")
      .value(-1)
      .visible(() -> this.targets.isSelected("Игроков"));
   private final ColorSetting friendColor = new ColorSetting("Цвет друзей", "Цвет друга").value(-11141291).visible(() -> this.targets.isSelected("Друзей"));
   private final ColorSetting itemColor = new ColorSetting("Цвет предметов", "Цвет дропа").value(-43691).visible(() -> this.targets.isSelected("Предметы"));
   private final SliderSettings radius = new SliderSettings("Радиус", "Удаление указателей от центра", 70.0F).range(30.0F, 160.0F).step(5.0F);
   private final BooleanSetting theme = new BooleanSetting("От темы", "Использовать текущую тему клиента").setValue(true);
   private final BooleanSetting onlyHidden = new BooleanSetting("Только невидимые", "Показывать указатели только для целей за препятствием").setValue(false);

   public Arrows() {
      super("Arrows", "Показывает направление на игроков и предметы вне центра экрана", ModuleCategory.RENDER);
      this.settings(this.targets, this.theme, this.playerColor, this.friendColor, this.itemColor, this.onlyHidden, this.radius);
   }

   @EventHandler
   public void onDraw(DrawEvent event) {
      if (mc.player != null
         && mc.world != null
         && (
            mc.currentScreen == null
               || mc.currentScreen instanceof ChatScreen
               || mc.currentScreen instanceof GameMenuScreen
               || mc.currentScreen instanceof HandledScreen
               || mc.currentScreen instanceof ClickGui
         )
         && !mc.options.hudHidden) {
         float cx = Render2D.getFixedScaledWidth() / 2.0F;
         float cy = Render2D.getFixedScaledHeight() / 2.0F;
         double cameraYaw = Math.toRadians(mc.gameRenderer.getCamera().getYaw());
         double cos = Math.cos(cameraYaw);
         double sin = Math.sin(cameraYaw);
         Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
         Vec3d eyePos = mc.player.getEyePos();
         float tickDelta = event.getPartialTicks();
         int matrixIndex = 0;
         boolean showPlayers = this.targets.getSelected().contains(this.targets.getList().get(0));
         boolean showFriends = this.targets.getSelected().contains(this.targets.getList().get(1));
         boolean showItems = this.targets.getSelected().contains(this.targets.getList().get(2));
         if (showPlayers || showFriends || showItems) {
            boolean themed = this.theme.isValue();
            int cachedPlayerColor = showPlayers ? this.selected(this.playerColor, 0.0F, themed) : 0;
            int cachedFriendColor = showFriends ? this.selected(this.friendColor, 0.35F, themed) : 0;
            int cachedItemColor = showItems ? this.selected(this.itemColor, 0.7F, themed) : 0;
            float configuredRadius = this.radius.getValue();
            float screenRadiusBonus = mc.currentScreen instanceof HandledScreen ? 65.0F : 0.0F;

            for (Entity entity : showItems ? mc.world.getEntities() : mc.world.getPlayers()) {
               int color = this.color(entity, cachedPlayerColor, cachedFriendColor, cachedItemColor);
               if (color != 0 && entity != mc.player && entity.isAlive()) {
                  Vec3d position = entity.getLerpedPos(tickDelta);
                  if (!this.onlyHidden.isValue() || !this.isVisible(eyePos, new Vec3d(position.x, position.y + entity.getHeight() * 0.5, position.z))) {
                     double dx = position.x - cameraPos.x;
                     double dz = position.z - cameraPos.z;
                     if (!(dx * dx + dz * dz < 4.0)) {
                        double rotationY = -(dz * cos - dx * sin);
                        double rotationX = -(dx * cos + dz * sin);
                        float angle = (float)Math.toDegrees(Math.atan2(rotationY, rotationX));
                        double radians = Math.toRadians(angle);
                        float entityRadius = entity instanceof ItemEntity ? Math.max(20.0F, configuredRadius - 20.0F) : configuredRadius;
                        entityRadius += screenRadiusBonus;
                        if (mc.currentScreen instanceof ClickGui) {
                           entityRadius = Math.max(entityRadius, this.clickGuiOuterRadius(radians));
                        }

                        float x = cx + (float)Math.cos(radians) * entityRadius;
                        float y = cy + (float)Math.sin(radians) * entityRadius;
                        Matrix4f rotation;
                        if (matrixIndex < this.matrixPool.size()) {
                           rotation = this.matrixPool.get(matrixIndex);
                        } else {
                           rotation = new Matrix4f();
                           this.matrixPool.add(rotation);
                        }

                        matrixIndex++;
                        rotation.set(Render2D.createProjection()).translate(x, y, 0.0F).rotateZ((float)Math.toRadians(angle)).translate(-x, -y, 0.0F);
                        Render2D.image(rotation, x - 23.0F, y - 23.0F, 46.0F, ARROW_TEXTURE, color, 0.0F, true);
                     }
                  }
               }
            }
         }
      }
   }

   private float clickGuiOuterRadius(double radians) {
      ClickGuiLayout layout = ClickGuiLayout.current();
      float arrowMargin = 23.0F + ClickGuiLayout.physicalToRender(8.0F);
      double absCos = Math.max(0.001, Math.abs(Math.cos(radians)));
      double absSin = Math.max(0.001, Math.abs(Math.sin(radians)));
      float horizontalExit = (float)(layout.windowWidth * 0.5F / absCos);
      float verticalExit = (float)(layout.windowHeight * 0.5F / absSin);
      return Math.min(horizontalExit, verticalExit) + arrowMargin;
   }

   private boolean isVisible(Vec3d eyePos, Vec3d target) {
      HitResult hit = mc.world.raycast(new RaycastContext(eyePos, target, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
      return hit.getType() == Type.MISS;
   }

   private int color(Entity entity, int playerColor, int friendColor, int itemColor) {
      if (entity instanceof PlayerEntity player) {
         return FriendUtils.isFriend(player) ? friendColor : playerColor;
      } else {
         return entity instanceof ItemEntity ? itemColor : 0;
      }
   }

   private int color(Entity entity) {
      if (entity instanceof PlayerEntity player) {
         if (FriendUtils.isFriend(player)) {
            return this.targets.isSelected("Друзей") ? this.selected(this.friendColor, 0.35F) : 0;
         } else {
            return this.targets.isSelected("Игроков") ? this.selected(this.playerColor, 0.0F) : 0;
         }
      } else if (entity instanceof ItemEntity) {
         return this.targets.isSelected("Предметы") ? this.selected(this.itemColor, 0.7F) : 0;
      } else {
         return 0;
      }
   }

   private int selected(ColorSetting custom, float position, boolean themed) {
      return themed ? ColorUtil.getClientColorAt(position) : custom.getColor();
   }

   private int selected(ColorSetting custom, float position) {
      return this.theme.isValue() ? ColorUtil.getClientColorAt(position) : custom.getColor();
   }
}
