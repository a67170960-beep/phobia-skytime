package ruhack.phobia.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import ruhack.phobia.system.modulesystem.impl.render.ItemReplacer;

@Mixin(ItemModelManager.class)
public abstract class ItemModelManagerMixin {
   private static final Identifier ITEM_REPLACER_MODEL = Identifier.of("phobia", "item_replacer");

   @WrapMethod(method = "update")
   private void phobia$replaceHeldSword(
      ItemRenderState renderState,
      ItemStack stack,
      ItemDisplayContext displayContext,
      World world,
      HeldItemContext heldItemContext,
      int seed,
      Operation<Void> original
   ) {
      ItemReplacer module = ItemReplacer.getInstance();
      boolean allowedOwner = module != null && (!module.selfOnly.isValue() || heldItemContext == MinecraftClient.getInstance().player);
      original.call(new Object[]{renderState, replacement(stack, module, allowedOwner), displayContext, world, heldItemContext, seed});
   }

   @WrapMethod(method = "updateForLivingEntity")
   private void phobia$replaceLivingSword(
      ItemRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, LivingEntity entity, Operation<Void> original
   ) {
      ItemReplacer module = ItemReplacer.getInstance();
      boolean allowedOwner = module != null && (!module.selfOnly.isValue() || entity == MinecraftClient.getInstance().player);
      original.call(new Object[]{renderState, replacement(stack, module, allowedOwner), displayContext, entity});
   }

   @WrapMethod(method = "updateForNonLivingEntity")
   private void phobia$replaceNonLivingSword(
      ItemRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, Entity entity, Operation<Void> original
   ) {
      ItemReplacer module = ItemReplacer.getInstance();
      boolean allowedOwner = module != null && !module.selfOnly.isValue();
      original.call(new Object[]{renderState, replacement(stack, module, allowedOwner), displayContext, entity});
   }

   private static ItemStack replacement(ItemStack original, ItemReplacer module, boolean allowedOwner) {
      if (module != null && module.isState() && allowedOwner && module.shouldReplace(original)) {
         ItemStack copy = original.copy();
         copy.set(DataComponentTypes.ITEM_MODEL, ITEM_REPLACER_MODEL);
         copy.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(), List.of(module.selectedWeapon(original)), List.of()));
         return copy;
      } else {
         return original;
      }
   }
}
