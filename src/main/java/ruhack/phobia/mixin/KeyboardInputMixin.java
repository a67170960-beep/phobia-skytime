package ruhack.phobia.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.InputEvent;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.RotationPlan;
import ruhack.phobia.util.rotation.angle.Angle;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
   @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
   private PlayerInput tickHook(PlayerInput original) {
      InputEvent event = new InputEvent(original);
      EventManager.callEvent(event);
      return this.transformInput(event.getInput());
   }

   @Unique
   private PlayerInput transformInput(PlayerInput input) {
      RotationController rotationController = RotationController.INSTANCE;
      Angle angle = rotationController.getCurrentAngle();
      RotationPlan configurable = rotationController.getCurrentRotationPlan();
      if (IMinecraft.mc.player != null && angle != null && configurable != null && configurable.isMoveCorrection() && configurable.isFreeCorrection()) {
         float deltaYaw = IMinecraft.mc.player.getYaw() - angle.getYaw();
         float z = KeyboardInput.getMovementMultiplier(input.forward(), input.backward());
         float x = KeyboardInput.getMovementMultiplier(input.left(), input.right());
         float newX = x * MathHelper.cos(deltaYaw * (float) (Math.PI / 180.0)) - z * MathHelper.sin(deltaYaw * (float) (Math.PI / 180.0));
         float newZ = z * MathHelper.cos(deltaYaw * (float) (Math.PI / 180.0)) + x * MathHelper.sin(deltaYaw * (float) (Math.PI / 180.0));
         int movementSideways = Math.round(newX);
         int movementForward = Math.round(newZ);
         return new PlayerInput(
            movementForward > 0.0F, movementForward < 0.0F, movementSideways > 0.0F, movementSideways < 0.0F, input.jump(), input.sneak(), input.sprint()
         );
      } else {
         return input;
      }
   }
}
