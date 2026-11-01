package ruhack.phobia.util.player.swap;

import net.minecraft.item.ItemStack;

@FunctionalInterface
public interface ItemSearcher {
   boolean matches(ItemStack var1);
}
