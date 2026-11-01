package ruhack.phobia.util.rpc.utils;

import java.io.Serializable;

public class RPCButton implements Serializable {
   private static final String FORCED_URL = "https://t.me/pasterend";
   private final String url;
   private final String label;

   public static RPCButton create(String label, String url) {
      label = label.substring(0, Math.min(label.length(), 31));
      if (url == null || url.contains("phobiadlc") || url.contains("discord.gg") || url.contains("discord.com/invite") || url.contains("discordapp.com/invite")
         )
       {
         url = "https://t.me/pasterend";
      }

      return new RPCButton(label, url);
   }

   protected RPCButton(String label, String url) {
      this.label = label;
      this.url = url;
   }

   public String getUrl() {
      return this.url;
   }

   public String getLabel() {
      return this.label;
   }
}
