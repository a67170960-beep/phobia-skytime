package ruhack.phobia.system.modulesystem.impl.misc;

import java.util.Locale;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.api.events.render.TextFactoryEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.TextSetting;

public class NameProtect extends ModuleStructure {
   private final TextSetting replacement = new TextSetting("Замена", "Текст вместо настоящего ника", "leaked by angelw1ngsov");
   private final BooleanSetting friends = new BooleanSetting("Скрывать друзей", "Также заменять ники друзей");

   public NameProtect() {
      super("NameProtect", "Скрывает ваш ник в отображаемом тексте", ModuleCategory.MISC);
      this.settings(this.replacement, this.friends);
      this.state = true;
   }

   @EventHandler
   public void onText(TextFactoryEvent event) {
      String value = event.getText();
      if (value != null && mc.getSession() != null) {
         value = replaceIgnoreCase(value, mc.getSession().getUsername(), this.replacement.getValue());
         if (this.friends.isValue()) {
            for (String name : FriendUtils.getFriendNames()) {
               value = replaceIgnoreCase(value, name, this.replacement.getValue());
            }
         }

         event.setText(value);
      }
   }

   private static String replaceIgnoreCase(String text, String search, String replacement) {
      if (search != null && !search.isBlank()) {
         String lower = text.toLowerCase(Locale.ROOT);
         String needle = search.toLowerCase(Locale.ROOT);
         int index = lower.indexOf(needle);
         if (index < 0) {
            return text;
         }

         StringBuilder result = new StringBuilder(text.length());

         int start;
         for (start = 0; index >= 0; index = lower.indexOf(needle, start)) {
            result.append(text, start, index).append(replacement);
            start = index + search.length();
         }

         return result.append(text, start, text.length()).toString();
      } else {
         return text;
      }
   }
}
