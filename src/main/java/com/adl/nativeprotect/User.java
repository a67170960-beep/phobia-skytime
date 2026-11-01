package com.adl.nativeprotect;

import java.util.HashMap;
import java.util.Map;

public class User {
   private static final User instance = new User();
   private final Map<String, String> cache = new HashMap<>();
   private boolean nativeFailed = false;

   public static User getInstance() {
      return instance;
   }

   private User() {
      this.nativeFailed = true;
      this.cache.put("username", "leaked by angelw1ngsov");
      this.cache.put("hwid", "hwid-1231294809786-2348786");
      this.cache.put("role", "Admin");
      this.cache.put("uid", "1337");
      this.cache.put("subTime", "2040-24-05");
   }

   private native String getUsername();

   private native String getHwid();

   private native String getRole();

   private native String getUid();

   private native String getSubsTime();

   public String profile(String profile) {
      if ("username".equalsIgnoreCase(profile)) {
         return "leaked by angelw1ngsov";
      }
      return this.cache.getOrDefault(profile, "");
   }
}
