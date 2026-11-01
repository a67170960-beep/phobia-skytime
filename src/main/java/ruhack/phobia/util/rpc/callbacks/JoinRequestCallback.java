package ruhack.phobia.util.rpc.callbacks;

import com.sun.jna.Callback;
import ruhack.phobia.util.rpc.utils.DiscordUser;

public interface JoinRequestCallback extends Callback {
   void apply(DiscordUser var1);
}
