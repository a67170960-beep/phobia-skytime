package ruhack.phobia.util.rpc.callbacks;

import com.sun.jna.Callback;
import ruhack.phobia.util.rpc.utils.DiscordUser;

public interface ReadyCallback extends Callback {
   void apply(DiscordUser var1);
}
