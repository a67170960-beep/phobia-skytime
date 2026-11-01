package ruhack.phobia.system.configsystem.impl.userconfig;

import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.Generated;

public class UserConfig {
   private String name;
   private String author;
   private long createdAt;
   private long updatedAt;
   private String fileName;
   private boolean cloud;

   public UserConfig(String name, String author, String fileName) {
      this.name = name;
      this.author = author;
      this.fileName = fileName;
      this.createdAt = System.currentTimeMillis();
      this.updatedAt = System.currentTimeMillis();
      this.cloud = false;
   }

   public UserConfig(String name, String author, String fileName, long createdAt, long updatedAt) {
      this.name = name;
      this.author = author;
      this.fileName = fileName;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
      this.cloud = false;
   }

   public UserConfig(String name, String author, String fileName, long createdAt, long updatedAt, boolean cloud) {
      this.name = name;
      this.author = author;
      this.fileName = fileName;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
      this.cloud = cloud;
   }

   public String getFormattedDate() {
      SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
      return sdf.format(new Date(this.updatedAt));
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public String getAuthor() {
      return this.author;
   }

   @Generated
   public long getCreatedAt() {
      return this.createdAt;
   }

   @Generated
   public long getUpdatedAt() {
      return this.updatedAt;
   }

   @Generated
   public String getFileName() {
      return this.fileName;
   }

   @Generated
   public boolean isCloud() {
      return this.cloud;
   }

   @Generated
   public void setName(String name) {
      this.name = name;
   }

   @Generated
   public void setAuthor(String author) {
      this.author = author;
   }

   @Generated
   public void setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
   }

   @Generated
   public void setUpdatedAt(long updatedAt) {
      this.updatedAt = updatedAt;
   }

   @Generated
   public void setFileName(String fileName) {
      this.fileName = fileName;
   }

   @Generated
   public void setCloud(boolean cloud) {
      this.cloud = cloud;
   }
}
