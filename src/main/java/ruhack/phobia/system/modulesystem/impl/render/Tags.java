package ruhack.phobia.system.modulesystem.impl.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource.Sprite;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import ruhack.phobia.mixin.HandledScreenAccessor;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.DrawEvent;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.display.Hud;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfGlyph;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.TagPlayerMenuScreen;
import ruhack.phobia.system.screensystem.clickgui.ClickGui;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.render.WorldToScreen;

public class Tags extends ModuleStructure {
   private static final String ICON_PING = "3";
   private static final float NAME_SIZE = 6.8F;
   private static final float SMALL_SIZE = 5.4F;
   private static final float UNIT_SIZE = 5.0F;
   private static final float TAG_MAIN_H = 14.0F;
   private static final float EQUIP_PILL_H = 10.0F;
   private static final float EQUIP_GAP = 2.0F;
   private static final float TAG_RADIUS = 4.5F;
   private static final float SCALE_NEAR = 5.0F;
   private static final float SCALE_FAR = 44.0F;
   private static final float SCALE_MIN = 0.5F;
   private static final float TAG_PAD = 6.0F;
   private static final float HEAD_SIZE = 9.0F;
   private static final float HEAD_RADIUS = 3.0F;
   private static final float BLUR_STRENGTH = 8.0F;
   private static final float ICON_VISUAL = 6.0F;
   private static final float STAT_GAP = 3.5F;
   private static final float SLOT_SIZE = 7.5F;
   private static final float SLOT_GAP = 2.5F;
   private static final float DOT_GAP = 5.0F;
   private static final float DOT_SIZE = 2.0F;
   private static final float FRIEND_DOT = 3.0F;
   private static final int TARGET_PLAYERS = 1;
   private static final int TARGET_MONSTERS = 2;
   private static final int TARGET_FRIENDS = 4;
   private static final int TARGET_ANIMALS = 8;
   private static final int TARGET_SELF = 16;
   private static final int TARGET_VILLAGERS = 32;
   private static final int TARGET_ITEMS = 64;
   private static final int TARGET_NAKED = 128;
   private static final Comparator<StatusEffectInstance> EFFECT_COMPARATOR = Comparator.comparing(
      effect -> ((StatusEffect)effect.getEffectType().value()).getName().getString()
   );
   private static ResourceManager cachedFontResources;
   private static Map<Integer, String> cachedServerGlyphs = Map.of();
   private final List<Tags.VisibleTag> visibleTags = new ArrayList<>();
   private final List<StatusEffectInstance> effectBuffer = new ArrayList<>();
   private final Map<Integer, Tags.CachedName> nameCache = new LinkedHashMap<Integer, Tags.CachedName>(64, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<Integer, Tags.CachedName> eldest) {
         return this.size() > 256;
      }
   };
   private final Vector2f projectedPoint = new Vector2f();
   private int visibleTagCount;
   private boolean boundsPreparedForDraw;
   private String pendingMenuPlayer;
   private float pendingMenuX;
   private float pendingMenuY;
   private float pendingMenuWidth;
   private float pendingMenuHeight;
   private boolean pendingMenuHasItems;
   private ChatScreen pendingMenuParent;
   private static final EquipmentSlot[] EQUIPMENT = new EquipmentSlot[]{
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
   };
   private final MultiSelectSetting targets = new MultiSelectSetting("Отображать", "Сущности с улучшенными тегами")
      .value("Игроков", "Голых", "Монстров", "Друзей", "Животных", "Себя", "Жителей", "Предметы")
      .selected("Игроков", "Друзей", "Предметы");
   private final BooleanSetting effects = new BooleanSetting("Отображать зелья", "Показывать активные эффекты").setValue(false);
   private final BooleanSetting armor = new BooleanSetting("Отображать броню", "Показывать экипировку в теге").setValue(true);
   private final BooleanSetting enchants = new BooleanSetting("Отображать зачарования", "Показывать зачарования экипировки")
      .setValue(false)
      .visible(this.armor::isValue);
   private final BooleanSetting heldItems = new BooleanSetting("Предметы в руках", "Показывать названия предметов в руках").setValue(false);
   private final BooleanSetting displayName = new BooleanSetting("Визуальное имя предмета", "Использовать полное имя предмета")
      .setValue(false)
      .visible(this.heldItems::isValue);

   public Tags() {
      super("Tags", "Отображает имя и информацию о существах", ModuleCategory.RENDER);
      this.settings(this.targets, this.effects, this.armor, this.enchants, this.heldItems, this.displayName);
   }

   public static Tags getInstance() {
      return Instance.get(Tags.class);
   }

   @EventHandler
   public void onDraw(DrawEvent event) {
      if (mc.world != null
         && mc.player != null
         && this.canRenderOverCurrentScreen()
         && !mc.options.hudHidden
         && MsdfManager.BOLD != null
         && MsdfManager.ICONS != null) {
         if (!this.boundsPreparedForDraw) {
            this.prepareVisibleTags();
         }

         this.boundsPreparedForDraw = true;
         Render2D.beginBlurFrame();

         try {
            for (int i = 0; i < this.visibleTagCount; i++) {
               Tags.VisibleTag visible = this.visibleTags.get(i);
               Entity entity = visible.entity;
               Tags.ScreenBounds bounds = visible.bounds;
               if (entity instanceof ItemEntity item) {
                  this.renderItemTag(event, item, bounds, visible.scale);
               } else if (entity instanceof LivingEntity living) {
                  this.renderLivingTag(event, living, bounds, visible.scale, visible.layout, visible.equipment);
               }
            }
         } finally {
            Render2D.endBlurFrame();
         }
      }
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (event.isMouse() && event.key() == 1 && event.action() == 1 && mc.player != null && mc.world != null) {
         if (mc.currentScreen instanceof ChatScreen chat) {
            float var9 = (float)(mc.mouse.getX() * Render2D.getFixedScaledWidth() / Math.max(1.0, mc.getWindow().getWidth()));
            float mouseY = (float)(mc.mouse.getY() * Render2D.getFixedScaledHeight() / Math.max(1.0, mc.getWindow().getHeight()));

            for (int i = this.visibleTagCount - 1; i >= 0; i--) {
               Tags.VisibleTag visible = this.visibleTags.get(i);
               if (visible.entity instanceof PlayerEntity player && player != mc.player && visible.bounds.containsTag(var9, mouseY)) {
                  this.pendingMenuPlayer = player.getGameProfile().name();
                  this.pendingMenuX = visible.bounds.tagMinX;
                  this.pendingMenuY = visible.bounds.tagMinY;
                  this.pendingMenuWidth = visible.bounds.tagMaxX - visible.bounds.tagMinX;
                  this.pendingMenuHeight = visible.bounds.tagMaxY - visible.bounds.tagMinY;
                  this.pendingMenuHasItems = this.hasVisibleEquipment(player);
                  this.pendingMenuParent = chat;
                  return;
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.pendingMenuPlayer != null) {
         String playerName = this.pendingMenuPlayer;
         ChatScreen parent = this.pendingMenuParent;
         this.pendingMenuPlayer = null;
         this.pendingMenuParent = null;
         if (mc.currentScreen == parent && parent != null && mc.world != null && mc.player != null) {
            mc.setScreen(
               new TagPlayerMenuScreen(
                  parent, playerName, this.pendingMenuX, this.pendingMenuY, this.pendingMenuWidth, this.pendingMenuHeight, this.pendingMenuHasItems
               )
            );
         }
      }
   }

   private boolean hasVisibleEquipment(LivingEntity entity) {
      if (!this.armor.isValue()) {
         return false;
      }

      for (EquipmentSlot slot : EQUIPMENT) {
         if (!entity.getEquippedStack(slot).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldTag(Entity entity) {
      return this.shouldTag(entity, this.targetMask());
   }

   private boolean shouldTag(Entity entity, int targetMask) {
      if (entity == mc.player) {
         return (targetMask & 16) != 0;
      } else if (entity instanceof PlayerEntity player) {
         boolean selected = FriendUtils.isFriend(player) ? (targetMask & 4) != 0 : (targetMask & 1) != 0;
         return selected && ((targetMask & 128) != 0 || hasArmor(player));
      } else if (entity instanceof ItemEntity) {
         return (targetMask & 64) != 0;
      } else if (entity instanceof VillagerEntity) {
         return (targetMask & 32) != 0;
      } else if (entity instanceof Monster) {
         return (targetMask & 2) != 0;
      } else {
         return entity instanceof AnimalEntity ? (targetMask & 8) != 0 : false;
      }
   }

   private int targetMask() {
      int mask = 0;
      List<String> selected = this.targets.getSelected();
      List<String> values = this.targets.getList();
      if (selected.contains(values.get(0))) {
         mask |= 1;
      }

      if (selected.contains(values.get(1))) {
         mask |= 128;
      }

      if (selected.contains(values.get(2))) {
         mask |= 2;
      }

      if (selected.contains(values.get(3))) {
         mask |= 4;
      }

      if (selected.contains(values.get(4))) {
         mask |= 8;
      }

      if (selected.contains(values.get(5))) {
         mask |= 16;
      }

      if (selected.contains(values.get(6))) {
         mask |= 32;
      }

      if (selected.contains(values.get(7))) {
         mask |= 64;
      }

      return mask;
   }

   private static boolean hasArmor(PlayerEntity player) {
      return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
         || !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
         || !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
         || !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
   }

   public boolean queueEquipmentModels(DrawContext context) {
      if (this.isState() && mc.world != null && mc.player != null && this.canRenderOverCurrentScreen() && !mc.options.hudHidden) {
         if (!this.boundsPreparedForDraw) {
            this.prepareVisibleTags();
         }

         if (!this.armor.isValue()) {
            this.boundsPreparedForDraw = false;
            return false;
         }

         boolean queued = false;

         for (int i = 0; i < this.visibleTagCount; i++) {
            Tags.VisibleTag visible = this.visibleTags.get(i);
            if (visible.entity instanceof LivingEntity living) {
               Tags.LivingTagLayout layout = visible.layout;
               if (layout.equipCount > 0) {
                  this.queueEquipmentModels(context, visible.bounds, layout, visible.scale, visible.equipment);
                  queued = true;
               }
            }
         }

         this.boundsPreparedForDraw = false;
         return queued;
      } else {
         this.boundsPreparedForDraw = false;
         return false;
      }
   }

   private void prepareVisibleTags() {
      this.visibleTagCount = 0;
      float tickDelta = WorldToScreen.getTickDelta();
      int screenWidth = Render2D.getFixedScaledWidth();
      int screenHeight = Render2D.getFixedScaledHeight();
      int targetMask = this.targetMask();
      boolean needsAllEntities = (targetMask & 106) != 0;

      for (Entity entity : needsAllEntities ? mc.world.getEntities() : mc.world.getPlayers()) {
         if (entity.isAlive() && this.shouldTag(entity, targetMask)) {
            if (this.visibleTagCount == this.visibleTags.size()) {
               this.visibleTags.add(new Tags.VisibleTag());
            }

            Tags.VisibleTag visible = this.visibleTags.get(this.visibleTagCount);
            visible.bounds.clearTag();
            Tags.ScreenBounds bounds = this.bounds(entity, tickDelta, visible.bounds);
            if (bounds != null
               && !(bounds.maxX < 0.0F)
               && !(bounds.minX > screenWidth)
               && !(bounds.maxY < 0.0F)
               && !(bounds.minY > screenHeight)
               && !this.currentScreenOccludes(bounds)) {
               this.visibleTagCount++;
               visible.entity = entity;
               visible.scale = this.tagScale(bounds.worldX, bounds.worldY, bounds.worldZ);
               if (entity instanceof LivingEntity living) {
                  this.prepareLivingLayout(living, visible);
               }
            }
         }
      }
   }

   private float tagScale(double x, double y, double z) {
      if (mc.gameRenderer == null) {
         return 1.0F;
      }

      Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
      double dx = camera.x - x;
      double dy = camera.y - y;
      double dz = camera.z - z;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (dist <= 5.0) {
         return 1.0F;
      }

      if (dist >= 44.0) {
         return 0.5F;
      }

      float t = (float)((dist - 5.0) / 39.0);
      return 1.0F - t * 0.5F;
   }

   private boolean currentScreenOccludes(Tags.ScreenBounds bounds) {
      float padding = 4.0F;
      float x = bounds.minX - padding;
      float y = bounds.topY() - 14.0F - 18.0F;
      float width = bounds.maxX - bounds.minX + padding * 2.0F;
      float height = bounds.maxY - y + padding;
      if (mc.currentScreen instanceof ClickGui clickGui) {
         return clickGui.overlapsPanel(x, y, width, height);
      } else if (!(mc.currentScreen instanceof HandledScreen<?> handled)) {
         return false;
      } else {
         HandledScreenAccessor accessor = (HandledScreenAccessor)handled;
         float guiToFixed = Math.max(1.0F, mc.getWindow().getScaleFactor()) / 2.0F;
         float panelX = accessor.phobia$getX() * guiToFixed;
         float panelY = accessor.phobia$getY() * guiToFixed;
         float panelWidth = accessor.phobia$getBackgroundWidth() * guiToFixed;
         float panelHeight = accessor.phobia$getBackgroundHeight() * guiToFixed;
         return x < panelX + panelWidth && x + width > panelX && y < panelY + panelHeight && y + height > panelY;
      }
   }

   private void renderItemTag(DrawEvent event, ItemEntity entity, Tags.ScreenBounds bounds, float scale) {
      ItemStack stack = entity.getStack();
      String name = (this.displayName.isValue() ? stack.getName() : stack.getItem().getName(stack)).getString();
      if (stack.getCount() > 1) {
         name = name + " x" + stack.getCount();
      }

      MsdfFont font = MsdfManager.BOLD;
      float centerX = bounds.centerX();
      Tags.TagTransform tr = new Tags.TagTransform(centerX, bounds.topY(), scale);
      float nameSize = tr.sz(6.8F);
      float baseTextWidth = Msdf.width(font, name, 6.8F);
      float textWidth = tr.sz(baseTextWidth);
      float pillW = tr.sz(baseTextWidth + 12.0F);
      float pillH = tr.sz(14.0F);
      float yBase = bounds.topY() - 14.0F - 3.0F;
      float x = tr.x(centerX - (baseTextWidth + 12.0F) / 2.0F);
      float y = tr.y(yBase);
      Tags.TagPalette palette = this.tagPalette(centerX, false);
      this.drawPill(event, x, y, pillW, pillH, palette, tr.sz(4.5F));
      Msdf.text(
         event.getDrawContext(), font, name, x + (pillW - textWidth) / 2.0F, centeredTextY(font, nameSize, y + pillH / 2.0F), nameSize, palette.text(), false
      );
   }

   private void renderLivingTag(DrawEvent event, LivingEntity entity, Tags.ScreenBounds bounds, float scale, Tags.LivingTagLayout layout, ItemStack[] equipment) {
      MsdfFont font = MsdfManager.BOLD;
      MsdfFont icons = MsdfManager.ICONS;
      float centerX = bounds.centerX();
      Tags.TagTransform tr = new Tags.TagTransform(centerX, bounds.topY(), scale);
      float mainYBase = bounds.topY() - layout.totalHeight - 4.0F;
      float mainX = tr.x(centerX - layout.mainWidth / 2.0F);
      float mainY = tr.y(mainYBase);
      float mainW = tr.sz(layout.mainWidth);
      float mainH = tr.sz(14.0F);
      if (entity instanceof PlayerEntity && entity != mc.player) {
         bounds.setTag(mainX, mainY, mainW, tr.sz(layout.totalHeight));
      }

      boolean friend = layout.friend;
      Tags.TagPalette palette = this.tagPalette(centerX, friend);
      this.drawPill(event, mainX, mainY, mainW, mainH, palette, tr.sz(4.5F));
      float nameSize = tr.sz(6.8F);
      float unitSize = tr.sz(5.0F);
      float statGap = tr.sz(3.5F);
      float tagPad = tr.sz(6.0F);
      float centerY = mainY + mainH / 2.0F;
      float cursor = mainX + tagPad;
      float textY = tagTextY(font, nameSize, centerY);
      if (entity instanceof AbstractClientPlayerEntity player) {
         Identifier skin = player.getSkin().body().texturePath();
         float headSize = tr.sz(9.0F);
         float headY = mainY + (mainH - headSize) / 2.0F;
         float headRadius = tr.sz(3.0F);
         Render2D.imageRegion(event.getDrawContext(), cursor, headY, headSize, headSize, skin, -1, headRadius, 0.125F, 0.125F, 0.25F, 0.25F, true, false);
         Render2D.imageRegion(event.getDrawContext(), cursor, headY, headSize, headSize, skin, -1, headRadius, 0.625F, 0.125F, 0.75F, 0.25F, true, false);
         cursor += headSize + statGap;
      }

      if (friend) {
         float dot = tr.sz(3.0F);
         Render2D.rect(event.getDrawContext(), cursor, centerY - dot / 2.0F, dot, dot, dot / 2.0F, palette.accent(), false);
         cursor += dot + statGap;
      }

      Tags.CachedName resolvedName = layout.resolvedName;
      cursor = drawStyledName(event, font, resolvedName.segments, cursor, textY, nameSize);
      cursor = this.drawDot(event, cursor, centerY, palette.muted(), tr);
      String hp = layout.hp;
      Msdf.text(event.getDrawContext(), font, hp, cursor, textY, nameSize, palette.text(), false);
      cursor += Msdf.width(font, hp, nameSize) + tr.sz(1.0F);
      Msdf.text(event.getDrawContext(), font, "hp", cursor, tagUnitY(font, textY, nameSize, unitSize), unitSize, palette.accent(), false);
      cursor += Msdf.width(font, "hp", unitSize);
      if (entity instanceof PlayerEntity player) {
         cursor = this.drawDot(event, cursor, centerY, palette.muted(), tr);
         String ping = layout.ping;
         float pingIcon = this.iconSize(icons, "3", tr.sz(6.0F));
         Msdf.text(event.getDrawContext(), icons, "3", cursor, this.glyphY(icons, "3", pingIcon, centerY), pingIcon, palette.accent(), false);
         cursor += Msdf.width(icons, "3", pingIcon) + statGap;
         Msdf.text(event.getDrawContext(), font, ping, cursor, textY, nameSize, palette.text(), false);
         cursor += Msdf.width(font, ping, nameSize) + tr.sz(1.0F);
         Msdf.text(event.getDrawContext(), font, "ms", cursor, tagUnitY(font, textY, nameSize, unitSize), unitSize, palette.accent(), false);
      }

      if (layout.equipCount > 0) {
         float equipW = tr.sz(layout.equipWidth);
         float equipH = tr.sz(10.0F);
         float equipX = centerX - equipW / 2.0F;
         float equipY = tr.y(mainYBase + 14.0F + 2.0F);
         this.drawPill(event, equipX, equipY, equipW, equipH, palette, tr.sz(4.5F));
         if (this.enchants.isValue()) {
            float slotSize = tr.sz(7.5F);
            float slotStep = tr.sz(10.0F);
            float slotX = equipX + tagPad;
            float slotY = equipY + (equipH - slotSize) / 2.0F;

            for (ItemStack stack : equipment) {
               if (!stack.isEmpty()) {
                  this.renderEnchantments(event, stack, slotX + slotSize / 2.0F, slotY - tr.sz(2.0F), palette, tr.sz(5.4F));
                  slotX += slotStep;
               }
            }
         }
      }

      float belowY = bounds.maxY + tr.sz(4.0F);
      if (this.heldItems.isValue()) {
         belowY = this.renderHeldItems(event, entity, centerX, belowY, palette, tr);
      }

      if (this.effects.isValue()) {
         this.renderEffects(event, entity, centerX, belowY, palette, tr);
      }
   }

   private void prepareLivingLayout(LivingEntity entity, Tags.VisibleTag visible) {
      Tags.LivingTagLayout layout = visible.layout;
      MsdfFont font = MsdfManager.BOLD;
      MsdfFont icons = MsdfManager.ICONS;
      String profileName = entity instanceof PlayerEntity player ? player.getGameProfile().name() : null;
      Tags.CachedName resolvedName = this.resolveNameSegments(entity, profileName, font);
      boolean friend = entity instanceof PlayerEntity && FriendUtils.isFriend(entity);
      int hpValue = Math.round(Math.max(0.0F, entity.getHealth() + entity.getAbsorptionAmount()));
      int pingValue = entity instanceof PlayerEntity player ? this.getPlayerPing(player) : -1;
      int equipCount = 0;
      ItemStack[] equipment = visible.equipment;
      if (this.armor.isValue()) {
         for (int i = 0; i < EQUIPMENT.length; i++) {
            ItemStack stack = entity.getEquippedStack(EQUIPMENT[i]);
            equipment[i] = stack;
            if (!stack.isEmpty()) {
               equipCount++;
            }
         }
      } else {
         for (int i = 0; i < equipment.length; i++) {
            equipment[i] = ItemStack.EMPTY;
         }
      }

      boolean layoutChanged = layout.resolvedName != resolvedName
         || layout.friend != friend
         || layout.hpValue != hpValue
         || layout.pingValue != pingValue
         || layout.equipCount != equipCount;
      if (layoutChanged) {
         if (layout.hpValue != hpValue || layout.hp == null) {
            layout.hp = Integer.toString(hpValue);
         }

         if (pingValue < 0 || layout.pingValue == pingValue && layout.ping != null) {
            if (pingValue < 0) {
               layout.ping = null;
            }
         } else {
            layout.ping = Integer.toString(pingValue);
         }

         float width = 12.0F;
         if (entity instanceof AbstractClientPlayerEntity) {
            width += 12.5F;
         }

         if (friend) {
            width += 6.5F;
         }

         width += resolvedName.width + this.dotBlockWidth();
         width += Msdf.width(font, layout.hp, 6.8F) + 1.0F + Msdf.width(font, "hp", 5.0F);
         if (pingValue >= 0) {
            width += this.dotBlockWidth();
            float pingIcon = this.iconSize(icons, "3", 6.0F);
            width += Msdf.width(icons, "3", pingIcon) + 3.5F;
            width += Msdf.width(font, layout.ping, 6.8F) + 1.0F + Msdf.width(font, "ms", 5.0F);
         }

         float equipWidth = 0.0F;
         if (equipCount > 0) {
            equipWidth = 12.0F + equipCount * 7.5F + (equipCount - 1) * 2.5F;
         }

         float totalHeight = 14.0F + (equipCount > 0 ? 12.0F : 0.0F);
         layout.mainWidth = width;
         layout.equipWidth = equipWidth;
         layout.totalHeight = totalHeight;
         layout.equipCount = equipCount;
         layout.friend = friend;
         layout.resolvedName = resolvedName;
         layout.hpValue = hpValue;
         layout.pingValue = pingValue;
      }
   }

   private int getPlayerPing(PlayerEntity player) {
      if (mc.getNetworkHandler() == null) {
         return 0;
      }

      PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
      return entry != null ? Math.max(entry.getLatency(), 0) : 0;
   }

   private float dotBlockWidth() {
      return 12.0F;
   }

   private float drawDot(DrawEvent event, float cursor, float centerY, int color, Tags.TagTransform tr) {
      float dotGap = tr.sz(5.0F);
      float dotSize = tr.sz(2.0F);
      float dotX = cursor + dotGap;
      float dotY = centerY - dotSize / 2.0F;
      Render2D.rect(event.getDrawContext(), dotX, dotY, dotSize, dotSize, dotSize / 2.0F, color, false);
      return dotX + dotSize + dotGap;
   }

   private Tags.TagPalette tagPalette(float centerX, boolean friend) {
      int bgAlpha = 195;
      Hud hud = Hud.getInstance();
      if (hud != null && hud.isState()) {
         bgAlpha = Math.round(hud.backgroundAlpha.getValue());
      }

      float screenW = Math.max(1.0F, Render2D.getFixedScaledWidth());
      int accent = friend ? ColorUtil.rgba(88, 214, 120, 255) : ColorUtil.getClientColorAt(centerX / screenW);
      return new Tags.TagPalette(ColorUtil.rgba(11, 12, 18, bgAlpha), accent, ColorUtil.rgba(150, 152, 165, 255), ColorUtil.rgba(236, 238, 245, 255));
   }

   private void drawPill(DrawEvent event, float x, float y, float width, float height, Tags.TagPalette palette, float radius) {
      Render2D.blur(event.getDrawContext(), x, y, width, height, radius, 8.0F, false);
      Render2D.rect(event.getDrawContext(), x, y, width, height, radius, palette.bg(), false);
   }

   private float iconSize(MsdfFont icons, String icon, float visual) {
      if (icons != null && icon != null && !icon.isEmpty()) {
         MsdfGlyph glyph = icons.getGlyph(icon.charAt(0));
         return glyph != null && !(glyph.height <= 0.0F) ? visual * icons.getEmSize() / glyph.height : visual;
      } else {
         return visual;
      }
   }

   private float glyphY(MsdfFont icons, String icon, float size, float centerY) {
      if (icons != null && icon != null && !icon.isEmpty()) {
         MsdfGlyph glyph = icons.getGlyph(icon.charAt(0));
         if (glyph != null && !(glyph.height <= 0.0F)) {
            float scale = size / icons.getEmSize();
            return centerY - (icons.getAscender() - glyph.bearingY + glyph.height / 2.0F) * scale;
         } else {
            return centerY - size / 2.0F;
         }
      } else {
         return centerY - size / 2.0F;
      }
   }

   private static float tagTextY(MsdfFont font, float size, float centerY) {
      if (font == null) {
         return centerY - size / 2.0F;
      } else {
         MsdfGlyph ref = font.getGlyph(48);
         if (ref != null && !(ref.height <= 0.0F)) {
            float scale = size / font.getEmSize();
            return centerY - (font.getAscender() - ref.bearingY + ref.height / 2.0F) * scale;
         } else {
            return centerY - font.getLineHeight() * size / 2.0F;
         }
      }
   }

   private static float tagUnitY(MsdfFont font, float textDrawY, float nameSize, float unitSize) {
      if (font == null) {
         return textDrawY + nameSize - unitSize;
      }

      float ascender = font.getAscender() / font.getEmSize();
      return textDrawY + ascender * (nameSize - unitSize);
   }

   private void queueEquipmentModels(DrawContext context, Tags.ScreenBounds bounds, Tags.LivingTagLayout layout, float scale, ItemStack[] equipment) {
      if (layout.equipCount != 0) {
         Tags.TagTransform tr = new Tags.TagTransform(bounds.centerX(), bounds.topY(), scale);
         float mainYBase = bounds.topY() - layout.totalHeight - 4.0F;
         float equipW = tr.sz(layout.equipWidth);
         float equipH = tr.sz(10.0F);
         float equipX = bounds.centerX() - equipW / 2.0F;
         float equipY = tr.y(mainYBase + 14.0F + 2.0F);
         float slotSize = tr.sz(7.5F);
         float slotStep = tr.sz(10.0F);
         float slotX = equipX + tr.sz(6.0F);
         float slotY = equipY + (equipH - slotSize) / 2.0F;

         for (ItemStack stack : equipment) {
            if (!stack.isEmpty()) {
               this.queueItemModel(context, stack, slotX, slotY, slotSize);
               slotX += slotStep;
            }
         }
      }
   }

   private void queueItemModel(DrawContext context, ItemStack stack, float x, float y, float size) {
      float fixedToGui = 2.0F / Math.max(1, mc.getWindow().getScaleFactor());
      float modelScale = size * fixedToGui / 16.0F;
      Matrix3x2fStack matrices = context.getMatrices();
      matrices.pushMatrix();
      matrices.translate(x * fixedToGui, y * fixedToGui);
      matrices.scale(modelScale, modelScale);
      context.drawItem(stack, 0, 0);
      matrices.popMatrix();
   }

   private void renderEnchantments(DrawEvent event, ItemStack stack, float centerX, float bottomY, Tags.TagPalette palette, float textSize) {
      ItemEnchantmentsComponent component = EnchantmentHelper.getEnchantments(stack);
      if (!component.isEmpty()) {
         int lineCount = 0;

         for (RegistryEntry<Enchantment> ignored : component.getEnchantments()) {
            lineCount++;
         }

         int lineIndex = 0;

         for (RegistryEntry<Enchantment> enchantment : component.getEnchantments()) {
            String name = ((Enchantment)enchantment.value()).description().getString();
            String shortName = name.length() > 2 ? name.substring(0, 2) : name;
            String text = shortName + component.getLevel(enchantment);
            float width = Msdf.width(MsdfManager.BOLD, text, textSize);
            Msdf.text(
               event.getDrawContext(),
               MsdfManager.BOLD,
               text,
               centerX - width / 2.0F,
               bottomY - (lineCount - lineIndex++) * textSize,
               textSize,
               palette.accent(),
               false
            );
         }
      }
   }

   private float renderHeldItems(DrawEvent event, LivingEntity entity, float centerX, float y, Tags.TagPalette palette, Tags.TagTransform tr) {
      y = this.renderHeldItem(event, entity.getMainHandStack(), centerX, y, palette, tr);
      return this.renderHeldItem(event, entity.getOffHandStack(), centerX, y, palette, tr);
   }

   private float renderHeldItem(DrawEvent event, ItemStack stack, float centerX, float y, Tags.TagPalette palette, Tags.TagTransform tr) {
      if (stack.isEmpty()) {
         return y;
      }

      String text = (this.displayName.isValue() ? stack.getName() : stack.getItem().getName(stack)).getString();
      MsdfFont font = MsdfManager.BOLD;
      float textSize = tr.sz(5.4F);
      float width = Msdf.width(font, text, textSize);
      float pillW = width + tr.sz(12.0F);
      float pillH = tr.sz(9.0F);
      float x = centerX - pillW / 2.0F;
      this.drawPill(event, x, y, pillW, pillH, palette, tr.sz(4.5F));
      Msdf.text(event.getDrawContext(), font, text, centerX - width / 2.0F, centeredTextY(font, textSize, y + pillH / 2.0F), textSize, palette.text(), false);
      return y + pillH + tr.sz(3.0F);
   }

   private void renderEffects(DrawEvent event, LivingEntity entity, float centerX, float y, Tags.TagPalette palette, Tags.TagTransform tr) {
      this.effectBuffer.clear();

      for (StatusEffectInstance effect : entity.getStatusEffects()) {
         if (effect.getDuration() > 20) {
            this.effectBuffer.add(effect);
         }
      }

      this.effectBuffer.sort(EFFECT_COMPARATOR);
      MsdfFont font = MsdfManager.BOLD;
      float textSize = tr.sz(5.4F);

      for (StatusEffectInstance effect : this.effectBuffer) {
         String text = ((StatusEffect)effect.getEffectType().value()).getName().getString();
         if (effect.getAmplifier() > 0) {
            text = text + " " + (effect.getAmplifier() + 1);
         }

         text = text + " " + formatDuration(effect.getDuration());
         float width = Msdf.width(font, text, textSize);
         float pillW = width + tr.sz(12.0F);
         float pillH = tr.sz(9.0F);
         float x = centerX - pillW / 2.0F;
         this.drawPill(event, x, y, pillW, pillH, palette, tr.sz(4.5F));
         Msdf.text(
            event.getDrawContext(), font, text, centerX - width / 2.0F, centeredTextY(font, textSize, y + pillH / 2.0F), textSize, palette.muted(), false
         );
         y += pillH + tr.sz(2.0F);
      }
   }

   private Tags.ScreenBounds bounds(Entity entity, float tickDelta, Tags.ScreenBounds result) {
      Vec3d lerped = entity.getLerpedPos(tickDelta);
      Vec3d current = entity.getEntityPos();
      net.minecraft.util.math.Box box = entity.getBoundingBox()
         .offset(lerped.x - current.x, lerped.y - current.y, lerped.z - current.z)
         .expand(0.05, 0.1, 0.05);
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;

      for (int xi = 0; xi < 2; xi++) {
         for (int yi = 0; yi < 2; yi++) {
            for (int zi = 0; zi < 2; zi++) {
               if (!WorldToScreen.project(xi == 0 ? box.minX : box.maxX, yi == 0 ? box.minY : box.maxY, zi == 0 ? box.minZ : box.maxZ, this.projectedPoint)) {
                  return null;
               }

               minX = Math.min(minX, this.projectedPoint.x);
               minY = Math.min(minY, this.projectedPoint.y);
               maxX = Math.max(maxX, this.projectedPoint.x);
               maxY = Math.max(maxY, this.projectedPoint.y);
            }
         }
      }

      result.minX = minX;
      result.minY = minY;
      result.maxX = maxX;
      result.maxY = maxY;
      if (!WorldToScreen.project(lerped.x, box.maxY, lerped.z, this.projectedPoint)) {
         return null;
      }

      result.anchorX = this.projectedPoint.x;
      result.anchorY = this.projectedPoint.y;
      result.worldX = lerped.x;
      result.worldY = lerped.y;
      result.worldZ = lerped.z;
      return result;
   }

   private static String formatDuration(int ticks) {
      int seconds = ticks / 20;
      int remaining = seconds % 60;
      return seconds / 60 + (remaining < 10 ? ":0" : ":") + remaining;
   }

   private static float centeredTextY(MsdfFont font, float size, float centerY) {
      return centerY - Msdf.height(font, size) / 2.0F;
   }

   private static List<Tags.NameSegment> styledName(Text text, String profileName) {
      List<Tags.NameSegment> result = new ArrayList<>();
      text.visit((style, value) -> {
         String normalized = normalizeStyledText(value, style);
         if (!normalized.isEmpty()) {
            appendLegacySegments(result, normalized, styleColor(style));
         }

         return Optional.empty();
      }, Style.EMPTY);
      stripLeadingDecoration(result);
      insertSpaceBeforeProfileName(result, profileName);
      return result;
   }

   private Tags.CachedName resolveNameSegments(LivingEntity entity, String profileName, MsdfFont font) {
      Text entityText = entity.getDisplayName();
      Text tabText = null;
      if (entity instanceof PlayerEntity player && mc.getNetworkHandler() != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         if (entry != null) {
            tabText = entry.getDisplayName();
         }
      }

      ResourceManager resources = mc.getResourceManager();
      Tags.CachedName cached = this.nameCache.get(entity.getId());
      if (cached != null && cached.matches(entityText, tabText, profileName, resources, font)) {
         return cached;
      }

      List<Tags.NameSegment> entityName = styledName(entityText, profileName);
      List<Tags.NameSegment> selected = entityName;
      if (tabText != null) {
         List<Tags.NameSegment> tabName = styledName(tabText, profileName);
         if (readablePrefixLength(tabName, profileName) > readablePrefixLength(entityName, profileName)) {
            selected = tabName;
         }
      }

      Tags.CachedName resolved = new Tags.CachedName(
         entityText.copy(), tabText == null ? null : tabText.copy(), profileName, resources, font, selected, styledWidth(font, selected, 6.8F)
      );
      this.nameCache.put(entity.getId(), resolved);
      return resolved;
   }

   private static int readablePrefixLength(List<Tags.NameSegment> segments, String profileName) {
      StringBuilder text = new StringBuilder();

      for (Tags.NameSegment segment : segments) {
         text.append(segment.text());
      }

      String value = text.toString();
      if (profileName != null && !profileName.isBlank()) {
         int profileIndex = value.toLowerCase().indexOf(profileName.toLowerCase());
         if (profileIndex >= 0) {
            value = value.substring(0, profileIndex);
         }
      }

      int readable = 0;
      int offset = 0;

      while (offset < value.length()) {
         int codePoint = value.codePointAt(offset);
         offset += Character.charCount(codePoint);
         if (Character.isLetterOrDigit(codePoint)) {
            readable++;
         }
      }

      return readable;
   }

   private static String normalizeStyledText(String value, Style style) {
      if (value != null && !value.isEmpty()) {
         if (style != null && style.getFont() instanceof Sprite) {
            return "";
         }

         String normalized = Normalizer.normalize(value, Form.NFKC);
         StringBuilder plain = new StringBuilder(normalized.length());
         Map<Integer, String> glyphs = serverGlyphs();
         int offset = 0;

         while (offset < normalized.length()) {
            int codePoint = normalized.codePointAt(offset);
            offset += Character.charCount(codePoint);
            String mapped = glyphs.get(codePoint);
            if (mapped != null) {
               plain.append(mapped);
            } else {
               String decorative = normalizeDecorativeCodePoint(codePoint);
               if (decorative != null) {
                  plain.append(decorative);
               } else {
                  int type = Character.getType(codePoint);
                  if (type != 18 && type != 19 && type != 0 && type != 16 && (!Character.isISOControl(codePoint) || Character.isWhitespace(codePoint))) {
                     plain.appendCodePoint(codePoint);
                  }
               }
            }
         }

         return plain.toString();
      } else {
         return "";
      }
   }

   private boolean canRenderOverCurrentScreen() {
      return mc.currentScreen == null
         || mc.currentScreen instanceof ChatScreen
         || mc.currentScreen instanceof GameMenuScreen
         || mc.currentScreen instanceof HandledScreen
         || mc.currentScreen instanceof ClickGui
         || mc.currentScreen instanceof TagPlayerMenuScreen;
   }

   private static String normalizeDecorativeCodePoint(int codePoint) {
      return switch (codePoint) {
         case 491, 42927 -> "Q";
         case 610 -> "G";
         case 618 -> "I";
         case 628 -> "N";
         case 640 -> "R";
         case 655 -> "Y";
         case 665 -> "B";
         case 668 -> "H";
         case 671 -> "L";
         case 739 -> "X";
         case 954, 7435 -> "K";
         case 1109 -> "S";
         case 7424 -> "A";
         case 7428 -> "C";
         case 7429 -> "D";
         case 7431 -> "E";
         case 7434 -> "J";
         case 7437 -> "M";
         case 7439 -> "O";
         case 7448 -> "P";
         case 7451 -> "T";
         case 7452 -> "U";
         case 7456 -> "V";
         case 7457 -> "W";
         case 7458 -> "Z";
         case 42800 -> "F";
         default -> null;
      };
   }

   private static Map<Integer, String> serverGlyphs() {
      ResourceManager resources = MinecraftClient.getInstance().getResourceManager();
      if (resources == cachedFontResources) {
         return cachedServerGlyphs;
      }

      synchronized (Tags.class) {
         if (resources == cachedFontResources) {
            return cachedServerGlyphs;
         }

         rebuildServerGlyphs(resources);
         return cachedServerGlyphs;
      }
   }

   private static void rebuildServerGlyphs(ResourceManager resources) {
      Map<Tags.GlyphCell, Integer> plainCells = new HashMap<>();
      Map<Tags.GlyphCell, Integer> customCells = new HashMap<>();
      Map<Identifier, List<Resource>> fonts = resources.findAllResources("font", id -> id.getPath().endsWith(".json"));

      for (List<Resource> layers : fonts.values()) {
         for (Resource resource : layers) {
            try (BufferedReader reader = resource.getReader()) {
               JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
               JsonArray providers = root.getAsJsonArray("providers");
               if (providers != null) {
                  for (JsonElement providerElement : providers) {
                     if (providerElement.isJsonObject()) {
                        JsonObject provider = providerElement.getAsJsonObject();
                        if ("bitmap".equals(jsonString(provider, "type"))) {
                           String file = jsonString(provider, "file");
                           JsonArray rows = provider.getAsJsonArray("chars");
                           if (file != null && rows != null) {
                              for (int row = 0; row < rows.size(); row++) {
                                 String chars = rows.get(row).getAsString();
                                 int column = 0;

                                 for (int offset = 0; offset < chars.length(); column++) {
                                    int codePoint = chars.codePointAt(offset);
                                    offset += Character.charCount(codePoint);
                                    if (codePoint != 0) {
                                       Tags.GlyphCell cell = new Tags.GlyphCell(file, row, column);
                                       if (codePoint >= 32 && codePoint <= 126) {
                                          plainCells.putIfAbsent(cell, codePoint);
                                       } else {
                                          customCells.putIfAbsent(cell, codePoint);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            } catch (Exception var24) {
            }
         }
      }

      Map<Integer, String> resolved = new HashMap<>();

      for (Entry<Tags.GlyphCell, Integer> entry : customCells.entrySet()) {
         Integer plain = plainCells.get(entry.getKey());
         if (plain != null) {
            resolved.put(entry.getValue(), new String(Character.toChars(plain)));
         }
      }

      cachedFontResources = resources;
      cachedServerGlyphs = Map.copyOf(resolved);
   }

   private static String jsonString(JsonObject object, String key) {
      JsonElement value = object.get(key);
      return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
   }

   private static void stripLeadingDecoration(List<Tags.NameSegment> segments) {
      while (!segments.isEmpty()) {
         Tags.NameSegment first = segments.getFirst();
         int keepFrom = 0;

         while (keepFrom < first.text.length()) {
            int codePoint = first.text.codePointAt(keepFrom);
            if (Character.isLetterOrDigit(codePoint) || codePoint == 91 || codePoint == 40) {
               break;
            }

            keepFrom += Character.charCount(codePoint);
         }

         if (keepFrom < first.text.length()) {
            if (keepFrom > 0) {
               segments.set(0, new Tags.NameSegment(first.text.substring(keepFrom), first.color));
            }

            return;
         }

         segments.removeFirst();
      }
   }

   private static void insertSpaceBeforeProfileName(List<Tags.NameSegment> segments, String profileName) {
      if (profileName != null && !profileName.isBlank() && !segments.isEmpty()) {
         StringBuilder fullText = new StringBuilder();

         for (Tags.NameSegment segment : segments) {
            fullText.append(segment.text);
         }

         int nameIndex = fullText.indexOf(profileName);
         if (nameIndex > 0 && !Character.isWhitespace(fullText.charAt(nameIndex - 1))) {
            int offset = 0;

            for (int i = 0; i < segments.size(); i++) {
               Tags.NameSegment segment = segments.get(i);
               int segmentEnd = offset + segment.text.length();
               if (nameIndex <= segmentEnd) {
                  int localIndex = nameIndex - offset;
                  if (localIndex == 0) {
                     segments.add(i, new Tags.NameSegment(" ", segment.color));
                  } else if (localIndex == segment.text.length()) {
                     segments.add(i + 1, new Tags.NameSegment(" ", segment.color));
                  } else {
                     String before = segment.text.substring(0, localIndex);
                     String after = segment.text.substring(localIndex);
                     segments.set(i, new Tags.NameSegment(before, segment.color));
                     segments.add(i + 1, new Tags.NameSegment(" ", segment.color));
                     segments.add(i + 2, new Tags.NameSegment(after, segment.color));
                  }

                  return;
               }

               offset = segmentEnd;
            }
         }
      }
   }

   private static void appendLegacySegments(List<Tags.NameSegment> result, String value, int baseColor) {
      value = value.replace("�", "").replaceAll("(^|\\s)\\?\\s+(?=[\\p{L}\\p{N}_])", "$1");
      int color = baseColor;
      StringBuilder part = new StringBuilder();

      for (int i = 0; i < value.length(); i++) {
         char current = value.charAt(i);
         if (current != 194 && current != 1042 || i + 1 >= value.length() || value.charAt(i + 1) != 167) {
            if (current == 167 && i + 1 < value.length()) {
               flushSegment(result, part, color);
               char code = Character.toLowerCase(value.charAt(++i));
               if (code == '#' && i + 6 < value.length()) {
                  String hex = value.substring(i + 1, i + 7);

                  try {
                     color = 0xFF000000 | Integer.parseInt(hex, 16);
                     i += 6;
                     continue;
                  } catch (NumberFormatException var12) {
                  }
               }

               if (code == 'x') {
                  StringBuilder hex = new StringBuilder(6);

                  int cursor;
                  for (cursor = i + 1; hex.length() < 6 && cursor + 1 < value.length() && value.charAt(cursor) == 167; cursor += 2) {
                     hex.append(value.charAt(cursor + 1));
                  }

                  if (hex.length() == 6) {
                     try {
                        color = 0xFF000000 | Integer.parseInt(hex.toString(), 16);
                        i = cursor - 1;
                        continue;
                     } catch (NumberFormatException var11) {
                     }
                  }
               }

               Formatting formatting = Formatting.byCode(code);
               if (formatting == Formatting.RESET) {
                  color = baseColor;
               } else if (formatting != null && formatting.isColor() && formatting.getColorValue() != null) {
                  color = 0xFF000000 | formatting.getColorValue();
               }
            } else {
               part.append(current);
            }
         }
      }

      flushSegment(result, part, color);
   }

   private static int styleColor(Style style) {
      return style != null && style.getColor() != null ? 0xFF000000 | style.getColor().getRgb() : -921608;
   }

   private static void flushSegment(List<Tags.NameSegment> result, StringBuilder part, int color) {
      if (!part.isEmpty()) {
         String text = part.toString();
         part.setLength(0);
         if (!result.isEmpty() && result.getLast().color == color) {
            Tags.NameSegment previous = result.removeLast();
            result.add(new Tags.NameSegment(previous.text + text, color));
         } else {
            result.add(new Tags.NameSegment(text, color));
         }
      }
   }

   private static float styledWidth(MsdfFont font, List<Tags.NameSegment> segments, float size) {
      float width = 0.0F;

      for (Tags.NameSegment segment : segments) {
         width += Msdf.width(font, segment.text, size);
      }

      return width;
   }

   private static float drawStyledName(DrawEvent event, MsdfFont font, List<Tags.NameSegment> segments, float x, float y, float size) {
      float cursor = x;

      for (Tags.NameSegment segment : segments) {
         Msdf.text(event.getDrawContext(), font, segment.text, cursor, y, size, segment.color, false);
         cursor += Msdf.width(font, segment.text, size);
      }

      return cursor;
   }

   private record CachedName(
      Text entityText, Text tabText, String profileName, ResourceManager resources, MsdfFont font, List<Tags.NameSegment> segments, float width
   ) {
      private boolean matches(Text currentEntityText, Text currentTabText, String currentProfileName, ResourceManager currentResources, MsdfFont currentFont) {
         return this.resources == currentResources
            && this.font == currentFont
            && Objects.equals(this.profileName, currentProfileName)
            && Objects.equals(this.entityText, currentEntityText)
            && Objects.equals(this.tabText, currentTabText);
      }
   }

   private record GlyphCell(String file, int row, int column) {
   }

   private static final class LivingTagLayout {
      private float mainWidth;
      private float equipWidth;
      private float totalHeight;
      private int equipCount;
      private boolean friend;
      private Tags.CachedName resolvedName;
      private String hp;
      private String ping;
      private int hpValue = Integer.MIN_VALUE;
      private int pingValue = Integer.MIN_VALUE;
   }

   private record NameSegment(String text, int color) {
   }

   private static final class ScreenBounds {
      private float minX;
      private float minY;
      private float maxX;
      private float maxY;
      private float anchorX;
      private float anchorY;
      private double worldX;
      private double worldY;
      private double worldZ;
      private float tagMinX;
      private float tagMinY;
      private float tagMaxX;
      private float tagMaxY;
      private boolean tagValid;

      float centerX() {
         return this.anchorX;
      }

      float topY() {
         return this.anchorY;
      }

      void setTag(float x, float y, float width, float height) {
         this.tagMinX = x;
         this.tagMinY = y;
         this.tagMaxX = x + width;
         this.tagMaxY = y + height;
         this.tagValid = true;
      }

      void clearTag() {
         this.tagValid = false;
      }

      boolean containsTag(float x, float y) {
         return this.tagValid && x >= this.tagMinX && x <= this.tagMaxX && y >= this.tagMinY && y <= this.tagMaxY;
      }
   }

   private record TagPalette(int bg, int accent, int muted, int text) {
   }

   private record TagTransform(float anchorX, float anchorY, float scale) {
      float x(float base) {
         return this.anchorX + (base - this.anchorX) * this.scale;
      }

      float y(float base) {
         return this.anchorY + (base - this.anchorY) * this.scale;
      }

      float sz(float base) {
         return base * this.scale;
      }
   }

   private static final class VisibleTag {
      private Entity entity;
      private float scale = 1.0F;
      private final Tags.ScreenBounds bounds = new Tags.ScreenBounds();
      private final Tags.LivingTagLayout layout = new Tags.LivingTagLayout();
      private final ItemStack[] equipment = new ItemStack[Tags.EQUIPMENT.length];
   }
}
