package ruhack.phobia.util.string;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Generated;

public final class StringHelper {
   public static String randomString(int length) {
      return IntStream.range(0, length).mapToObj(operand -> String.valueOf((char)new Random().nextInt(97, 123))).collect(Collectors.joining());
   }

   public static String getBindName(int key) {
      return key < 0 ? "N/A" : KeyHelper.getKeyName(key).toUpperCase();
   }

   public static String getUserRole() {
      return switch ("DEVELOPER") {
         case "Разработчик" -> "Developer";
         case "Администратор" -> "Admin";
         default -> "User";
      };
   }

   public static String getDuration(int time) {
      int mins = time / 60;
      String sec = String.format("%02d", time % 60);
      return mins + ":" + sec;
   }

   @Generated
   private StringHelper() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
