package ruhack.phobia.util.rpc.utils;

import com.sun.jna.Native;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class DiscordNative {
   public static final DiscordRPC INSTANCE = load();

   private DiscordNative() {
   }

   private static DiscordRPC load() {
      String resourcePath = is64Bit() ? "/win32-x86-64/discord-rpc.dll" : "/win32-x86/discord-rpc.dll";
      File extracted = extract(resourcePath);
      return (DiscordRPC)Native.loadLibrary(extracted.getAbsolutePath(), DiscordRPC.class);
   }

   private static File extract(String resourcePath) {
      try (InputStream input = DiscordNative.class.getResourceAsStream(resourcePath)) {
         if (input == null) {
            throw new IllegalStateException("Discord native resource not found: " + resourcePath);
         }

         File tempDir = new File(System.getProperty("java.io.tmpdir"), "phobia-discord-rpc");
         if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new IllegalStateException("Failed to create temp dir: " + tempDir);
         }

         File target = new File(tempDir, "discord-rpc.dll");

         try (FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];

            int read;
            while ((read = input.read(buffer)) != -1) {
               output.write(buffer, 0, read);
            }
         }

         target.deleteOnExit();
         return target;
      } catch (IOException exception) {
         throw new IllegalStateException("Failed to extract Discord RPC native", exception);
      }
   }

   private static boolean is64Bit() {
      String arch = System.getProperty("os.arch", "").toLowerCase();
      return arch.contains("64");
   }
}
