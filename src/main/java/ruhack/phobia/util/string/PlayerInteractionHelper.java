package ruhack.phobia.util.string;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Generated;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;

public final class PlayerInteractionHelper implements IMinecraft {
   public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      mc.interactionManager.sendSequencedPacket(mc.world, packetCreator);
   }

   public static void interactItem(Hand hand) {
      interactItem(hand, AngleUtil.cameraAngle());
   }

   public static void interactItem(Hand hand, Angle angle) {
      sendSequencedPacket(i -> new PlayerInteractItemC2SPacket(hand, i, angle.getYaw(), angle.getPitch()));
   }

   public static void interactEntity(Entity entity) {
      mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(entity, false, Hand.MAIN_HAND, entity.getBoundingBox().getCenter()));
      mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(entity, false, Hand.MAIN_HAND));
   }

   public static void startFallFlying() {
      mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
      mc.player.startGliding();
   }

   public static void sendPacketWithOutEvent(Packet<?> packet) {
      mc.getNetworkHandler().getConnection().send(packet, null);
   }

   public static String getHealthString(float hp) {
      return String.format("%.1f", hp).replace(",", ".").replace(".0", "");
   }

   public static void jump() {
      if (mc.player.isSprinting()) {
         float g = mc.player.getYaw() * (float) (Math.PI / 180.0);
         mc.player.addVelocityInternal(new Vec3d(-MathHelper.sin(g) * 0.2F, 0.0, MathHelper.cos(g) * 0.2F));
      }

      mc.player.velocityDirty = true;
   }

   public static List<BlockPos> getCube(BlockPos center, float radius) {
      return getCube(center, radius, radius, true);
   }

   public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
      return getCube(center, radiusXZ, radiusY, true);
   }

   public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY, boolean down) {
      List<BlockPos> positions = new ArrayList<>();
      int centerX = center.getX();
      int centerY = center.getY();
      int centerZ = center.getZ();
      int posY = down ? centerY - (int)radiusY : centerY;

      for (int x = centerX - (int)radiusXZ; x <= centerX + radiusXZ; x++) {
         for (int z = centerZ - (int)radiusXZ; z <= centerZ + radiusXZ; z++) {
            for (int y = posY; y <= centerY + radiusY; y++) {
               positions.add(new BlockPos(x, y, z));
            }
         }
      }

      return positions;
   }

   public static List<BlockPos> getCube(BlockPos start, BlockPos end) {
      List<BlockPos> positions = new ArrayList<>();

      for (int x = start.getX(); x <= end.getX(); x++) {
         for (int z = start.getZ(); z <= end.getZ(); z++) {
            for (int y = start.getY(); y <= end.getY(); y++) {
               positions.add(new BlockPos(x, y, z));
            }
         }
      }

      return positions;
   }

   public static Type getKeyType(int key) {
      return key < 8 ? Type.MOUSE : Type.KEYSYM;
   }

   public static Stream<Entity> streamEntities() {
      return StreamSupport.stream(mc.world.getEntities().spliterator(), false);
   }

   public static boolean canChangeIntoPose(EntityPose pose, Vec3d pos) {
      return mc.player.getEntityWorld().isSpaceEmpty(mc.player, mc.player.getDimensions(pose).getBoxAt(pos).contract(1.0E-7));
   }

   public static boolean isPotionActive(RegistryEntry<StatusEffect> statusEffect) {
      return mc.player.getActiveStatusEffects().containsKey(statusEffect);
   }

   public static boolean isPlayerInBlock(Block block) {
      return isBoxInBlock(mc.player.getBoundingBox().expand(-0.001), block);
   }

   public static boolean isBoxInBlock(Box box, Block block) {
      return isBox(box, pos -> mc.world.getBlockState(pos).getBlock().equals(block));
   }

   public static boolean isBoxInBlocks(Box box, List<Block> blocks) {
      return isBox(box, pos -> blocks.contains(mc.world.getBlockState(pos).getBlock()));
   }

   public static boolean isBox(Box box, Predicate<BlockPos> pos) {
      return BlockPos.stream(box).anyMatch(pos);
   }

   public static boolean isKey(BindSetting setting) {
      int key = setting.getKey();
      return mc.currentScreen == null && setting.isVisible() && isKey(getKeyType(key), key);
   }

   public static boolean isKey(KeyBinding key) {
      return isKey(key.getDefaultKey().getCategory(), key.getDefaultKey().getCode());
   }

   public static boolean isKey(Type type, int keyCode) {
      if (keyCode != -1) {
         switch (type) {
            case KEYSYM:
               return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == 1;
            case MOUSE:
               return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == 1;
         }
      }

      return false;
   }

   public static boolean isAir(BlockPos blockPos) {
      return isAir(mc.world.getBlockState(blockPos));
   }

   public static boolean isAir(BlockState state) {
      return state.isAir() || state.getBlock().equals(Blocks.CAVE_AIR) || state.getBlock().equals(Blocks.VOID_AIR);
   }

   public static boolean isChat(Screen screen) {
      return screen instanceof ChatScreen;
   }

   public static boolean nullCheck() {
      return mc.player == null || mc.world == null;
   }

   @Generated
   private PlayerInteractionHelper() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
