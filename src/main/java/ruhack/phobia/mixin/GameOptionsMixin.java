package ruhack.phobia.mixin;

import java.io.File;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
   @Unique
   private static boolean isFirstLaunch = false;

   @Shadow
   public abstract SimpleOption<Double> getSoundVolumeOption(SoundCategory var1);

   @Shadow
   public abstract SimpleOption<Integer> getGuiScale();

   @Shadow
   public abstract SimpleOption<Boolean> getVignette();

   @Inject(method = "<init>", at = @At("HEAD"))
   private static void checkFirstLaunch(MinecraftClient client, File optionsFile, CallbackInfo ci) {
      File options = new File(optionsFile, "options.txt");
      isFirstLaunch = !options.exists();
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void setDefaultSettings(MinecraftClient client, File optionsFile, CallbackInfo ci) {
      if (isFirstLaunch) {
         this.getSoundVolumeOption(SoundCategory.MUSIC).setValue(0.0);
         this.getSoundVolumeOption(SoundCategory.WEATHER).setValue(0.0);
         this.getGuiScale().setValue(2);
         this.getVignette().setValue(false);
         isFirstLaunch = false;
      }
   }
}
