package ruhack.phobia.system.modulesystem.impl.render;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.Identifier;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.utils.PhobiaFiguraBridge;
import org.figuramc.figura.utils.PhobiaFiguraBridge.RenderPass;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.eventsystem.impl.WorldLoadEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.display.Notifications;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.render.ClientPipelines;

public final class CustomModels extends ModuleStructure {
   private static CustomModels instance;
   private static final Map<RenderLayer, CustomModels.CachedShaderPasses> SHADER_PASS_CACHE = new IdentityHashMap<>();
   private static final Map<RenderLayer, RenderPass[]> SHADER_ESP_PASS_CACHE = new IdentityHashMap<>();
   private static RenderPass[] updatingPasses;
   private static int updatingPassIndex;
   private static final Map<String, CustomModels.EmbeddedAvatar> AVATARS = Map.of(
      "Repo",
      new CustomModels.EmbeddedAvatar("repo", "/assets/phobia/custom_models/repo_v2.zip"),
      "Peter Griffin",
      new CustomModels.EmbeddedAvatar("peter_griffin", "/assets/phobia/custom_models/peter_griffin.zip"),
      "Aria",
      new CustomModels.EmbeddedAvatar("aria", "/assets/phobia/custom_models/aria.zip")
   );
   private final SelectSetting model = new SelectSetting("Модель", "Модель, применяемая к вашему игроку", "Repo", "Repo", "Peter Griffin", "Aria");
   private final BooleanSetting onFriends = new BooleanSetting("На друзей", "Применять выбранную модель к друзьям");
   private final Set<UUID> appliedFriends = new HashSet<>();
   private String loadedModel;
   private Avatar friendSourceAvatar;
   private int worldReloadDelay;

   public CustomModels() {
      super("Custom Models", "Применяет встроенные анимированные модели к вашему игроку", ModuleCategory.RENDER);
      instance = this;
      this.settings(this.model, this.onFriends);
      PhobiaFiguraBridge.setFirstPersonPassProvider(CustomModels::createShaderHandsPasses);
      PhobiaFiguraBridge.setWorldPassProvider(CustomModels::createShaderEspPasses);
   }

   public static boolean appliesTo(PlayerLikeEntity player) {
      CustomModels customModels = instance;
      MinecraftClient client = MinecraftClient.getInstance();
      return customModels != null && customModels.isState() && PhobiaFiguraBridge.isCustomModelsActive() && client.player != null
         ? player == client.player || customModels.onFriends.isValue() && FriendUtils.isFriend(player)
         : false;
   }

   private static RenderPass[] createShaderEspPasses(Entity entity, RenderLayer original) {
      ShaderESP shaderESP = ShaderESP.getInstance();
      if (entity instanceof PlayerLikeEntity player
         && shaderESP != null
         && shaderESP.isState()
         && shaderESP.shouldOutline(player)
         && original.getDrawMode() == DrawMode.QUADS) {
         Identifier texture = ClientPipelines.layerTextureOf(original);
         if (texture == null) {
            return null;
         }

         RenderPass[] passes = SHADER_ESP_PASS_CACHE.computeIfAbsent(
            original,
            layer -> {
               RenderLayer visible = layer;
               if (layer.isOutline()) {
                  visible = layer.getRenderPipeline().isCull() ? RenderLayers.entityCutout(texture) : RenderLayers.entityCutoutNoCull(texture, false);
               }

               return new RenderPass[]{
                  RenderPass.original(visible), RenderPass.colored(ClientPipelines.CUSTOM_MODEL_OUTLINE.apply(texture), shaderESP.getOutlineColor())
               };
            }
         );
         passes[1].setColor(shaderESP.getOutlineColor());
         return passes;
      } else {
         return null;
      }
   }

   private static RenderPass[] createShaderHandsPasses(RenderLayer original) {
      ShaderHands shaderHands = ShaderHands.getInstance();
      if (shaderHands != null && shaderHands.isState() && original.getDrawMode() == DrawMode.QUADS) {
         Identifier texture = ClientPipelines.layerTextureOf(original);
         if (texture == null) {
            return null;
         }

         int layout = shaderPassLayout(shaderHands);
         CustomModels.CachedShaderPasses cached = SHADER_PASS_CACHE.get(original);
         if (cached == null || cached.layout != layout) {
            cached = new CustomModels.CachedShaderPasses(layout, buildShaderHandsPasses(shaderHands, original, texture));
            SHADER_PASS_CACHE.put(original, cached);
         }

         long frameSerial = shaderHands.getRenderFrameSerial();
         if (cached.updatedFrameSerial != frameSerial) {
            updateShaderHandsPasses(shaderHands, cached.passes);
            cached.updatedFrameSerial = frameSerial;
         }

         return cached.passes;
      } else {
         return null;
      }
   }

   private static int shaderPassLayout(ShaderHands shaderHands) {
      int layout = shaderHands.isGlowMode() ? 1 : 0;
      if (shaderHands.fillFraction() > 0.01F) {
         layout |= 2;
      }

      if (shaderHands.isTrailEnabled()) {
         layout |= 4;
      }

      if (shaderHands.isTrailModelEnabled()) {
         layout |= 8;
      }

      return layout;
   }

   private static RenderPass[] buildShaderHandsPasses(ShaderHands shaderHands, RenderLayer original, Identifier texture) {
      if (!shaderHands.isGlowMode()) {
         return new RenderPass[]{RenderPass.shader(ClientPipelines.SHADER_HANDS_GRADIENT.apply(texture), 0, 0)};
      }

      List<RenderPass> passes = new ArrayList<>();
      passes.add(RenderPass.original(original));
      RenderLayer halo = ClientPipelines.SHADER_HANDS_HALO.apply(texture);
      shaderHands.forEachHaloPass((light, overlay) -> passes.add(RenderPass.shader(halo, light, overlay)));
      if (shaderHands.isTrailEnabled()) {
         RenderLayer trailHalo = ClientPipelines.SHADER_HANDS_TRAIL_HALO.apply(texture);
         shaderHands.forEachHaloPass((light, overlay) -> passes.add(RenderPass.shader(trailHalo, light, overlay)));
         if (shaderHands.isTrailModelEnabled()) {
            passes.add(RenderPass.shader(trailHalo, shaderHands.packTrailModelColor(), shaderHands.packTrailModelOverlay()));
         }
      }

      return passes.toArray(RenderPass[]::new);
   }

   private static void updateShaderHandsPasses(ShaderHands shaderHands, RenderPass[] passes) {
      if (!shaderHands.isGlowMode()) {
         passes[0].setPackedData(shaderHands.packColor1(), shaderHands.packColor2Speed());
      } else {
         updatingPasses = passes;
         updatingPassIndex = 1;
         shaderHands.forEachHaloPass(CustomModels::updateCurrentPass);
         if (shaderHands.isTrailEnabled()) {
            shaderHands.forEachHaloPass(CustomModels::updateCurrentPass);
            if (shaderHands.isTrailModelEnabled()) {
               passes[updatingPassIndex].setPackedData(shaderHands.packTrailModelColor(), shaderHands.packTrailModelOverlay());
            }
         }

         updatingPasses = null;
      }
   }

   private static void updateCurrentPass(int light, int overlay) {
      updatingPasses[updatingPassIndex++].setPackedData(light, overlay);
   }

   @Override
   public void activate() {
      this.loadedModel = null;
      this.friendSourceAvatar = null;
      this.clearAppliedFriendModels();
      PhobiaFiguraBridge.setCustomModelsActive(false);
      if (isFiguraReady()) {
         this.loadSelected();
      }
   }

   @Override
   public void deactivate() {
      this.loadedModel = null;
      this.friendSourceAvatar = null;
      this.clearAppliedFriendModels();
      PhobiaFiguraBridge.setCustomModelsActive(false);
      AvatarManager.loadLocalAvatar(null);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.worldReloadDelay > 0) {
         this.worldReloadDelay--;
      } else {
         if (isFiguraReady() && !this.model.getValue().equals(this.loadedModel)) {
            this.loadSelected();
         }

         this.syncFriendModels();
      }
   }

   @EventHandler
   public void onWorldLoad(WorldLoadEvent event) {
      this.loadedModel = null;
      this.friendSourceAvatar = null;
      this.clearAppliedFriendModels();
      PhobiaFiguraBridge.setCustomModelsActive(false);
      this.worldReloadDelay = 5;
   }

   private static boolean isFiguraReady() {
      return !PermissionManager.CATEGORIES.isEmpty();
   }

   private void loadSelected() {
      String selected = this.model.getValue();
      CustomModels.EmbeddedAvatar avatar = AVATARS.get(selected);
      if (avatar != null) {
         try {
            this.friendSourceAvatar = null;
            this.clearAppliedFriendModels();
            Path avatarRoot = this.prepareAvatar(avatar);
            AvatarManager.loadLocalAvatar(avatarRoot);
            this.loadedModel = selected;
            PhobiaFiguraBridge.setCustomModelsActive(true);
         } catch (Exception exception) {
            this.loadedModel = selected;
            PhobiaFiguraBridge.setCustomModelsActive(false);
            Notifications.info("Не удалось загрузить модель " + selected);
         }
      }
   }

   private void syncFriendModels() {
      if (this.onFriends.isValue() && mc.player != null && mc.world != null) {
         Avatar source = AvatarManager.getLoadedAvatar(mc.player.getUuid());
         if (source != null && source.loaded && source.renderer != null && source.nbt != null) {
            if (source != this.friendSourceAvatar) {
               this.clearAppliedFriendModels();
               this.friendSourceAvatar = source;
            }

            Set<UUID> desiredFriends = new HashSet<>();
            mc.world.getPlayers().forEach(player -> {
               if (player != mc.player && FriendUtils.isFriend(player)) {
                  desiredFriends.add(player.getUuid());
               }
            });

            for (UUID uuid : new HashSet<>(this.appliedFriends)) {
               if (!desiredFriends.contains(uuid)) {
                  AvatarManager.clearAvatars(uuid);
                  this.appliedFriends.remove(uuid);
               }
            }

            for (UUID uuid : desiredFriends) {
               if (this.appliedFriends.add(uuid)) {
                  AvatarManager.setAvatar(uuid, source.nbt);
               }
            }
         }
      } else {
         this.friendSourceAvatar = null;
         this.clearAppliedFriendModels();
      }
   }

   private void clearAppliedFriendModels() {
      for (UUID uuid : this.appliedFriends) {
         AvatarManager.clearAvatars(uuid);
      }

      this.appliedFriends.clear();
   }

   private Path prepareAvatar(CustomModels.EmbeddedAvatar avatar) throws IOException, NoSuchAlgorithmException {
      byte[] archive;
      try (InputStream input = CustomModels.class.getResourceAsStream(avatar.resource())) {
         if (input == null) {
            throw new IOException("Missing embedded avatar " + avatar.resource());
         }

         archive = input.readAllBytes();
      }

      Path cache = Path.of("").toAbsolutePath().resolve("phobia").resolve("custom_models").resolve(avatar.cacheName());
      String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(archive));
      Path marker = cache.resolve(".archive.sha256");
      if (!Files.isRegularFile(marker) || !hash.equals(Files.readString(marker))) {
         deleteTree(cache);
         Files.createDirectories(cache);
         extract(archive, cache);
         Files.writeString(marker, hash, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      }

      try (Stream<Path> paths = Files.walk(cache)) {
         return paths.filter(path -> path.getFileName().toString().equals("avatar.json"))
            .filter(path -> !path.toString().contains("__MACOSX"))
            .map(Path::getParent)
            .findFirst()
            .orElseThrow(() -> new IOException("avatar.json not found"));
      }
   }

   private static void extract(byte[] archive, Path destination) throws IOException {
      ZipEntry entry;
      try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
         for (; (entry = zip.getNextEntry()) != null; zip.closeEntry()) {
            Path target = destination.resolve(entry.getName()).normalize();
            if (!target.startsWith(destination)) {
               throw new IOException("Invalid ZIP path");
            }

            if (entry.isDirectory()) {
               Files.createDirectories(target);
            } else {
               Files.createDirectories(target.getParent());
               Files.copy(zip, target);
            }
         }
      }
   }

   private static void deleteTree(Path root) throws IOException {
      if (Files.exists(root)) {
         try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
               Files.deleteIfExists(path);
            }
         }
      }
   }

   private static final class CachedShaderPasses {
      private final int layout;
      private final RenderPass[] passes;
      private long updatedFrameSerial = Long.MIN_VALUE;

      private CachedShaderPasses(int layout, RenderPass[] passes) {
         this.layout = layout;
         this.passes = passes;
      }
   }

   private record EmbeddedAvatar(String cacheName, String resource) {
   }
}
