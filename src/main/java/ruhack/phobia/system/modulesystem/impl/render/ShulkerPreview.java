package ruhack.phobia.system.modulesystem.impl.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.DrawEvent;
import ruhack.phobia.system.eventsystem.impl.HandledScreenEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.render.WorldToScreen;

public class ShulkerPreview extends ModuleStructure {
   private static final Identifier SHULKER_TEXTURE = Identifier.of("phobia", "images/hud/container.png");
   private static final int PANEL_WIDTH = 176;
   private static final int PANEL_HEIGHT = 67;
   private static final int SLOT_COLUMNS = 9;
   private static final int SLOT_COUNT = 27;
   private static final int SLOT_SIZE = 18;
   private static final int SLOT_X = 8;
   private static final int SLOT_Y = 7;
   private static final int SCREEN_MARGIN = 4;
   private static final float WORLD_SCALE = 0.5F;
   private static final float WORLD_OFFSET = 18.0F;
   private static final double WORLD_RANGE_SQUARED = 4096.0;
   private final BooleanSetting world = new BooleanSetting("Рендерить в мире", "Показывать содержимое рядом с выброшенным шалкером").setValue(true);
   private final List<ItemEntity> worldShulkers = new ArrayList<>();
   private final List<ShulkerPreview.WorldPreview> worldPreviews = new ArrayList<>();

   public ShulkerPreview() {
      super("ShulkerPreview", "Показывает содержимое шалкера при Ctrl и наведении", ModuleCategory.RENDER);
      this.settings(this.world);
   }

   public static ShulkerPreview getInstance() {
      return Instance.get(ShulkerPreview.class);
   }

   @Override
   public void deactivate() {
      this.worldShulkers.clear();
      this.worldPreviews.clear();
   }

   @EventHandler
   public void onScreen(HandledScreenEvent event) {
      if (this.isControlDown() && event.getSlotHover() != null) {
         ItemStack shulker = event.getSlotHover().getStack();
         List<ItemStack> contents = this.contents(shulker);
         if (contents != null) {
            int x = event.getMouseX() + 12;
            int y = event.getMouseY() + 12;
            if (x + 176 > event.getDrawContext().getScaledWindowWidth() - 4) {
               x = event.getMouseX() - 176 - 12;
            }

            if (y + 67 > event.getDrawContext().getScaledWindowHeight() - 4) {
               y = event.getMouseY() - 67 - 12;
            }

            x = clamp(x, 4, event.getDrawContext().getScaledWindowWidth() - 176 - 4);
            y = clamp(y, 4, event.getDrawContext().getScaledWindowHeight() - 67 - 4);
            this.renderPanel(event.getDrawContext(), contents, x, y, 1.0F);
            event.cancel();
         }
      }
   }

   @EventHandler
   public void onDraw(DrawEvent event) {
      this.worldPreviews.clear();
      if (this.world.isValue() && mc.player != null && mc.world != null && (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen)) {
         this.worldShulkers.clear();

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity item && !(item.squaredDistanceTo(mc.player) > 4096.0)) {
               List<ItemStack> contents = this.contents(item.getStack());
               if (contents != null) {
                  this.worldShulkers.add(item);
               }
            }
         }

         this.worldShulkers.sort(Comparator.comparingDouble(itemx -> -itemx.squaredDistanceTo(mc.player)));
         float tickDelta = WorldToScreen.getTickDelta();
         float fixedToGui = 2.0F / Math.max(1.0F, mc.getWindow().getScaleFactor());
         Vector2f projected = new Vector2f();

         for (ItemEntity item : this.worldShulkers) {
            List<ItemStack> contents = this.contents(item.getStack());
            if (contents != null) {
               Vec3d position = item.getLerpedPos(tickDelta).add(0.0, 0.15, 0.0);
               if (WorldToScreen.project(position.x, position.y, position.z, projected)) {
                  int anchorX = Math.round(projected.x * fixedToGui);
                  int anchorY = Math.round((projected.y + 18.0F) * fixedToGui);
                  this.worldPreviews.add(new ShulkerPreview.WorldPreview(anchorX, anchorY, copyContents(contents)));
               }
            }
         }
      }
   }

   public boolean queueWorldPreviews(DrawContext context) {
      if (this.isState() && !this.worldPreviews.isEmpty()) {
         int scaledWidth = context.getScaledWindowWidth();
         int scaledHeight = context.getScaledWindowHeight();
         int previewWidth = Math.round(88.0F);
         int previewHeight = Math.round(33.5F);
         boolean rendered = false;

         for (ShulkerPreview.WorldPreview preview : this.worldPreviews) {
            int x = clamp(preview.anchorX - previewWidth / 2, 4, scaledWidth - previewWidth - 4);
            int y = clamp(preview.anchorY, 4, scaledHeight - previewHeight - 4);
            this.renderWorldPanel(context, preview.contents, x, y);
            rendered = true;
         }

         return rendered;
      } else {
         return false;
      }
   }

   private void renderPanel(DrawContext context, List<ItemStack> contents, int x, int y, float scale) {
      Matrix3x2fStack matrices = context.getMatrices();
      matrices.pushMatrix();
      matrices.translate(x, y);
      matrices.scale(scale, scale);
      context.drawTexture(RenderPipelines.GUI_TEXTURED, SHULKER_TEXTURE, 0, 0, 0.0F, 0.0F, 176, 67, 176, 67);

      for (int i = 0; i < Math.min(27, contents.size()); i++) {
         ItemStack stack = contents.get(i);
         if (!stack.isEmpty()) {
            int slotX = 8 + i % 9 * 18;
            int slotY = 7 + i / 9 * 18;
            context.drawItem(stack, slotX, slotY);
            context.drawStackOverlay(mc.textRenderer, stack, slotX, slotY);
         }
      }

      matrices.popMatrix();
   }

   private void renderWorldPanel(DrawContext context, List<ItemStack> contents, int x, int y) {
      Matrix3x2fStack matrices = context.getMatrices();
      matrices.pushMatrix();
      matrices.translate(x, y);
      matrices.scale(0.5F, 0.5F);
      context.drawTexture(RenderPipelines.GUI_TEXTURED, SHULKER_TEXTURE, 0, 0, 0.0F, 0.0F, 176, 67, 176, 67);
      matrices.popMatrix();
      float itemSize = 8.0F;

      for (int i = 0; i < Math.min(27, contents.size()); i++) {
         ItemStack stack = contents.get(i);
         if (!stack.isEmpty()) {
            float itemX = x + (8 + i % 9 * 18) * 0.5F;
            float itemY = y + (7 + i / 9 * 18) * 0.5F;
            this.queueWorldItem(context, stack, itemX, itemY, itemSize);
         }
      }
   }

   private void queueWorldItem(DrawContext context, ItemStack stack, float x, float y, float size) {
      float modelScale = size / 16.0F;
      Matrix3x2fStack matrices = context.getMatrices();
      matrices.pushMatrix();
      matrices.translate(x, y);
      matrices.scale(modelScale, modelScale);
      context.drawItemWithoutEntity(stack, 0, 0);
      context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
      matrices.popMatrix();
   }

   private List<ItemStack> contents(ItemStack stack) {
      if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
         DefaultedList<ItemStack> result = DefaultedList.ofSize(27, ItemStack.EMPTY);
         ContainerComponent container = (ContainerComponent)stack.get(DataComponentTypes.CONTAINER);
         if (container != null) {
            container.copyTo(result);
            if (result.stream().anyMatch(item -> !item.isEmpty())) {
               return result;
            }
         }

         TypedEntityData<?> blockEntityData = (TypedEntityData<?>)stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
         if (blockEntityData != null && mc.world != null) {
            NbtList items = blockEntityData.copyNbtWithoutId().getListOrEmpty("Items");
            RegistryOps<NbtElement> ops = mc.world.getRegistryManager().getOps(NbtOps.INSTANCE);

            for (int i = 0; i < items.size(); i++) {
               NbtCompound entry = items.getCompoundOrEmpty(i);
               int slot = Byte.toUnsignedInt(entry.getByte("Slot", (byte)i));
               if (slot >= 0 && slot < 27) {
                  ItemStack.CODEC.parse(ops, entry).result().ifPresent(item -> result.set(slot, item));
               }
            }

            return result;
         } else {
            return result;
         }
      } else {
         return null;
      }
   }

   private static List<ItemStack> copyContents(List<ItemStack> contents) {
      List<ItemStack> copy = new ArrayList<>(27);

      for (int i = 0; i < 27; i++) {
         ItemStack stack = i < contents.size() ? contents.get(i) : ItemStack.EMPTY;
         copy.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
      }

      return copy;
   }

   private static int clamp(int value, int min, int max) {
      return max < min ? min : Math.max(min, Math.min(max, value));
   }

   private boolean isControlDown() {
      long window = mc.getWindow().getHandle();
      return GLFW.glfwGetKey(window, 341) == 1 || GLFW.glfwGetKey(window, 345) == 1;
   }

   private record WorldPreview(int anchorX, int anchorY, List<ItemStack> contents) {
   }
}
