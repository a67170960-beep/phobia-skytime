package ruhack.phobia.system.modulesystem.impl.render;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.PlayerLikeEntity;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.screensystem.clickgui.ClickGui;
import ruhack.phobia.util.colorschem.ColorUtil;

public class ShaderESP extends ModuleStructure {
   static final int FULL_ALPHA = -16777216;
   static ShaderESP instance;
   public final SelectSetting colorMode = new SelectSetting("Color", "Outline color source", "Theme", "Theme", "Custom");
   public final ColorSetting color = new ColorSetting("Custom Color", "Shader outline color")
      .value(ColorUtil.getColor(150, 60, 255, 255))
      .visible(() -> this.colorMode.isSelected("Custom"));
   public final BooleanSetting onSelf = new BooleanSetting("On Self", "Outline your own player in third person");

   public ShaderESP() {
      super("ShaderESP", "Shader outline around players", ModuleCategory.RENDER);
      instance = this;
      this.settings(this.colorMode, this.color, this.onSelf);
   }

   public static ShaderESP getInstance() {
      return instance;
   }

   public boolean shouldOutline(PlayerLikeEntity player) {
      if (mc.currentScreen != null
         && !(mc.currentScreen instanceof ChatScreen)
         && !(mc.currentScreen instanceof GameMenuScreen)
         && !(mc.currentScreen instanceof HandledScreen)
         && !(mc.currentScreen instanceof ClickGui)) {
         return false;
      } else {
         return mc.player != null && player == mc.player ? this.onSelf.isValue() && !mc.options.getPerspective().isFirstPerson() : true;
      }
   }

   public boolean usesCustomModelOutline(PlayerLikeEntity player) {
      return CustomModels.appliesTo(player);
   }

   public int getOutlineColor() {
      int value = this.colorMode.isSelected("Theme") ? ColorUtil.getClientColorAt(0.0F) : this.color.getColor();
      return value | 0xFF000000;
   }
}
