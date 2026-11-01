package ruhack.phobia.mixin;

import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.ClientWorld.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientWorld.class)
public interface ClientWorldAccessor {
   @Accessor("pendingUpdateManager")
   PendingUpdateManager phobia$getPendingUpdateManager();

   @Accessor("clientWorldProperties")
   Properties phobia$getClientWorldProperties();
}
