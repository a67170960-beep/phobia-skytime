package ruhack.phobia.system.modulesystem.impl.legit;

import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.InteractEntityEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;

public class NoFriendDamage extends ModuleStructure {
   public NoFriendDamage() {
      super("NoFriendDamage", "No Friend Damage", ModuleCategory.LEGIT);
   }

   @EventHandler
   public void onAttack(InteractEntityEvent e) {
      e.setCancelled(FriendUtils.isFriend(e.getEntity()));
   }
}
