package ruhack.phobia.system.configsystem.impl.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Generated;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;
import ruhack.phobia.util.proxy.Proxy;

public class ProxyConfig {
   private static ProxyConfig instance;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Path configPath;
   private boolean proxyEnabled = false;
   private Proxy defaultProxy = new Proxy();
   private Proxy lastUsedProxy = new Proxy();

   private ProxyConfig() {
      Path configDir = Paths.get("Phobia", "configs", "proxy");

      try {
         Files.createDirectories(configDir);
      } catch (IOException var3) {
      }

      this.configPath = configDir.resolve("proxy.file");
   }

   public static ProxyConfig getInstance() {
      if (instance == null) {
         instance = new ProxyConfig();
      }

      return instance;
   }

   public void save() {
      try {
         JsonObject root = new JsonObject();
         root.addProperty("proxyEnabled", this.proxyEnabled);
         JsonObject defaultProxyJson = new JsonObject();
         defaultProxyJson.addProperty("ipPort", this.defaultProxy.ipPort);
         defaultProxyJson.addProperty("type", this.defaultProxy.type.name());
         defaultProxyJson.addProperty("username", this.defaultProxy.username);
         defaultProxyJson.addProperty("password", this.defaultProxy.password);
         root.add("defaultProxy", defaultProxyJson);
         JsonObject lastUsedProxyJson = new JsonObject();
         lastUsedProxyJson.addProperty("ipPort", this.lastUsedProxy.ipPort);
         lastUsedProxyJson.addProperty("type", this.lastUsedProxy.type.name());
         lastUsedProxyJson.addProperty("username", this.lastUsedProxy.username);
         lastUsedProxyJson.addProperty("password", this.lastUsedProxy.password);
         root.add("lastUsedProxy", lastUsedProxyJson);
         Files.writeString(this.configPath, this.gson.toJson(root), StandardCharsets.UTF_8);
      } catch (IOException e) {
         Logger.error("ProxyConfig: Save failed! " + e.getMessage());
      }
   }

   public void load() {
      try {
         if (!Files.exists(this.configPath)) {
            this.save();
            return;
         }

         String json = Files.readString(this.configPath, StandardCharsets.UTF_8);
         if (json.isEmpty()) {
            return;
         }

         JsonObject root = JsonParser.parseString(json).getAsJsonObject();
         if (root.has("proxyEnabled")) {
            this.proxyEnabled = root.get("proxyEnabled").getAsBoolean();
         }

         if (root.has("defaultProxy")) {
            this.defaultProxy = this.parseProxy(root.getAsJsonObject("defaultProxy"));
         }

         if (root.has("lastUsedProxy")) {
            this.lastUsedProxy = this.parseProxy(root.getAsJsonObject("lastUsedProxy"));
         }

         Logger.success("ProxyConfig: proxy.json loaded successfully!");
      } catch (Exception e) {
         Logger.error("ProxyConfig: Load failed! " + e.getMessage());
      }
   }

   private Proxy parseProxy(JsonObject json) {
      Proxy proxy = new Proxy();
      if (json.has("ipPort")) {
         proxy.ipPort = json.get("ipPort").getAsString();
      }

      if (json.has("type")) {
         try {
            proxy.type = Proxy.ProxyType.valueOf(json.get("type").getAsString());
         } catch (IllegalArgumentException var4) {
         }
      }

      if (json.has("username")) {
         proxy.username = json.get("username").getAsString();
      }

      if (json.has("password")) {
         proxy.password = json.get("password").getAsString();
      }

      return proxy;
   }

   public void setDefaultProxyAndSave(Proxy proxy) {
      this.defaultProxy = proxy;
      this.save();
   }

   public void setProxyEnabledAndSave(boolean enabled) {
      this.proxyEnabled = enabled;
      this.save();
   }

   public void setLastUsedProxyAndSave(Proxy proxy) {
      this.lastUsedProxy = proxy;
      this.save();
   }

   @Generated
   public boolean isProxyEnabled() {
      return this.proxyEnabled;
   }

   @Generated
   public void setProxyEnabled(boolean proxyEnabled) {
      this.proxyEnabled = proxyEnabled;
   }

   @Generated
   public Proxy getDefaultProxy() {
      return this.defaultProxy;
   }

   @Generated
   public void setDefaultProxy(Proxy defaultProxy) {
      this.defaultProxy = defaultProxy;
   }

   @Generated
   public Proxy getLastUsedProxy() {
      return this.lastUsedProxy;
   }

   @Generated
   public void setLastUsedProxy(Proxy lastUsedProxy) {
      this.lastUsedProxy = lastUsedProxy;
   }
}
