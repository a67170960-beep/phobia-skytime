package ruhack.phobia.system.friendsystem;

import lombok.Generated;

public class Friend {
   String name;
   long addedAt;

   public Friend(String name) {
      this(name, System.currentTimeMillis());
   }

   public Friend(String name, long addedAt) {
      this.name = name;
      this.addedAt = addedAt;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public long getAddedAt() {
      return this.addedAt;
   }

   @Generated
   public void setName(String name) {
      this.name = name;
   }

   @Generated
   public void setAddedAt(long addedAt) {
      this.addedAt = addedAt;
   }
}
