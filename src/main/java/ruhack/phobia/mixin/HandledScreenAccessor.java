package ruhack.phobia.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
   @Accessor("x")
   int phobia$getX();

   @Accessor("y")
   int phobia$getY();

   @Accessor("backgroundWidth")
   int phobia$getBackgroundWidth();

   @Accessor("backgroundHeight")
   int phobia$getBackgroundHeight();
}
