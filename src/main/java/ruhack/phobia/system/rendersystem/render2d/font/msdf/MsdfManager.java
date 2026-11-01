package ruhack.phobia.system.rendersystem.render2d.font.msdf;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Identifier;

public class MsdfManager {
   private static final Map<String, MsdfFont> fonts = new HashMap<>();
   private static MsdfFont defaultFont;
   private static boolean initialized = false;
   public static MsdfFont SANSATION;
   public static MsdfFont HAMBURG_BOLD;
   public static MsdfFont HAMBURG_REGULAR;
   public static MsdfFont BOLD;
   public static MsdfFont ICONS;

   public static void init() {
      if (!initialized) {
         System.out.println("[MSDF] Initializing MsdfManager...");
         Msdf.SANSATION = SANSATION = register("sansation", "sansation.png", "sansation.json");
         Msdf.HAMBURG_BOLD = HAMBURG_BOLD = register("hamburghandbold", "hamburghandbold.png", "hamburghandbold.json");
         Msdf.HAMBURG_REGULAR = HAMBURG_REGULAR = register("hamburghandregular", "hamburghandregular.png", "hamburghandregular.json");
         Msdf.BOLD = BOLD = register("bold", "bold.png", "bold.json");
         Msdf.ICONS = ICONS = register("icons", "icons.png", "icons.json");
         initialized = true;
      }
   }

   public static MsdfFont register(String name, String atlasPath, String jsonPath) {
      MsdfFont font = new MsdfFont(name);
      font.load(Identifier.of("phobia", atlasPath), Identifier.of("phobia", jsonPath));
      fonts.put(name, font);
      if (defaultFont == null) {
         defaultFont = font;
      }

      return font;
   }

   public static MsdfFont get(String name) {
      return fonts.get(name);
   }

   public static MsdfFont getDefault() {
      return defaultFont;
   }

   public static void setDefault(String name) {
      MsdfFont font = fonts.get(name);
      if (font != null) {
         defaultFont = font;
      }
   }

   public static void shutdown() {
      fonts.values().forEach(MsdfFont::shutdown);
      fonts.clear();
      defaultFont = null;
      SANSATION = null;
      Msdf.SANSATION = null;
      HAMBURG_BOLD = null;
      Msdf.HAMBURG_BOLD = null;
      HAMBURG_REGULAR = null;
      Msdf.HAMBURG_REGULAR = null;
      BOLD = null;
      Msdf.BOLD = null;
      ICONS = null;
      Msdf.ICONS = null;
      initialized = false;
   }
}
