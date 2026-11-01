package ruhack.phobia.system.modulesystem.impl.render;

import java.util.Arrays;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;

public final class ItemReplacer extends ModuleStructure {
   private static final String[] ALL_WEAPONS = new String[]{
      "Abominable Blade",
      "Abominable Great Saber",
      "Abominable Scythe",
      "Acidic Cleaver",
      "Amethyst Shuriken",
      "Ancient Royal Great Sword",
      "Aquatic Sacred Blade",
      "Arcanethyst",
      "Ashura's Blade",
      "Awakened Lichblade",
      "Blood Edge",
      "Bloody Death",
      "Bramblethorn",
      "Brimstone Claymore",
      "Carian Knight's Sword",
      "Chrono Blade",
      "Corrupted Mythic Blade",
      "Creation Splitter",
      "Crescent Rose",
      "Cyber Katana",
      "Cyber Mantis Blade",
      "Cyber Sword",
      "Cybernetic Chainsaw Blade",
      "Cybernetic Katana",
      "Cybernetic Knife",
      "Dainsleif",
      "Dark Blade",
      "Dark Cleaver",
      "Death Knight's Dagger",
      "Death Knight's Sword",
      "Demigod's Unholy Blade",
      "Demigod's Unholy Halberd",
      "Demon Lord's Great Axe",
      "Demon Lord's Sword",
      "Demonic Blade",
      "Demonic Cleaver",
      "Divine Axe Rhitta",
      "Divine Justice",
      "Divine Punisher",
      "Divine Reaper",
      "Dragon Slaying blade",
      "Edge Of The Astral Plane",
      "Emberblade",
      "Enigma",
      "Epic Sword",
      "Estoc",
      "Fallen God's Spear",
      "Fallen God's Sword",
      "Floral Longsword",
      "Floral Sabre",
      "Forest Guardian's Glaive",
      "Frost Axe",
      "Frost Blade",
      "Frost Scythe",
      "Hearthflame",
      "Hero Sword",
      "Holy Moonlight Sword",
      "Hornet's Needle",
      "Icewhisper",
      "Jade Halberd",
      "Katana",
      "Legendary Sword",
      "Longsword",
      "Magi Scythe",
      "Masamune",
      "Mjolnir",
      "Molten Blade",
      "Molten Sword",
      "Muramasa",
      "Mystical Spellblade",
      "Mythic Blade",
      "Ocean's Rage",
      "Partisan",
      "Pharaoh's Treasure",
      "Pheonix Grace",
      "Plague Longsword",
      "Power Fuse Hammer",
      "Power Fuse Sword",
      "Requiem of the Ninth Abyss",
      "Ribbon Cleaver",
      "Righteous Relic",
      "Rivers Of Blood",
      "Royal Chakram",
      "Royal Rapier",
      "Sabre",
      "Scissor Blade",
      "Sculk Cleaver",
      "Sculk Scythe",
      "Sculk Sword",
      "Sentinel's Will",
      "Silverine Blade",
      "Soul Claws",
      "Soul Collector",
      "Soul Devourer",
      "Soul Edge",
      "Soul Harvester",
      "Soul Stealer",
      "Soulrender",
      "Star's Edge",
      "Steel Sword",
      "Stop Sign",
      "Storm Bringer",
      "Storm's Edge",
      "Sunbreak",
      "Tengen's Blade",
      "Terra Blade",
      "Thousand Demon Daggers",
      "Thunder Bringer",
      "Thunderbrand",
      "True Excalibur",
      "Vampiric Needle",
      "Wakizashi",
      "Watcher Claymore",
      "Watching Warglaive",
      "Waxweaver",
      "Whisperwind",
      "Wickpiercer",
      "Wraith Scythe",
      "Yoru"
   };
   private static final String[] MACE_WEAPONS = new String[]{
      "Abominable Scythe",
      "Demon Lord's Great Axe",
      "Divine Axe Rhitta",
      "Frost Axe",
      "Frost Scythe",
      "Magi Scythe",
      "Mjolnir",
      "Power Fuse Hammer",
      "Sculk Scythe",
      "Wraith Scythe"
   };
   private static final Set<String> MACE_WEAPON_SET = Set.of(MACE_WEAPONS);
   private static final Set<String> OTHER_WEAPON_SET = Set.of(
      "Acidic Cleaver",
      "Amethyst Shuriken",
      "Cybernetic Chainsaw Blade",
      "Dark Cleaver",
      "Demigod's Unholy Halberd",
      "Demonic Cleaver",
      "Divine Punisher",
      "Divine Reaper",
      "Fallen God's Spear",
      "Forest Guardian's Glaive",
      "Jade Halberd",
      "Ocean's Rage",
      "Partisan",
      "Pharaoh's Treasure",
      "Royal Chakram",
      "Sculk Cleaver",
      "Soul Claws",
      "Soul Collector",
      "Soul Harvester",
      "Stop Sign"
   );
   private static final String[] SWORD_WEAPONS = Arrays.stream(ALL_WEAPONS)
      .filter(name -> !MACE_WEAPON_SET.contains(name) && !OTHER_WEAPON_SET.contains(name))
      .toArray(String[]::new);
   private static ItemReplacer instance;
   public final BooleanSetting selfOnly = new BooleanSetting("Только для себя", "Заменять модель оружия только у своего игрока").setValue(true);
   public final SelectSetting swordModel = new SelectSetting("Меч", "Модель, заменяющая все ванильные мечи", SWORD_WEAPONS[0], SWORD_WEAPONS);
   public final SelectSetting maceModel = new SelectSetting("Булава", "Модель, заменяющая ванильную булаву", MACE_WEAPONS[0], MACE_WEAPONS);

   public ItemReplacer() {
      super("Item Replacer", "Заменяет модель ванильного меча или булавы", ModuleCategory.RENDER);
      instance = this;
      this.settings(this.selfOnly, this.swordModel, this.maceModel);
   }

   public static ItemReplacer getInstance() {
      return instance;
   }

   public String selectedWeapon(ItemStack stack) {
      return isMace(stack) ? this.maceModel.getValue() : this.swordModel.getValue();
   }

   public boolean shouldReplace(ItemStack stack) {
      return isSword(stack) || isMace(stack);
   }

   public static boolean isSword(ItemStack stack) {
      return stack.isOf(Items.WOODEN_SWORD)
         || stack.isOf(Items.STONE_SWORD)
         || stack.isOf(Items.COPPER_SWORD)
         || stack.isOf(Items.IRON_SWORD)
         || stack.isOf(Items.GOLDEN_SWORD)
         || stack.isOf(Items.DIAMOND_SWORD)
         || stack.isOf(Items.NETHERITE_SWORD);
   }

   public static boolean isMace(ItemStack stack) {
      return stack.isOf(Items.MACE);
   }
}
