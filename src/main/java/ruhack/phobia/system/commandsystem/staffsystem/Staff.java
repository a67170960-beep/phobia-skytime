package ruhack.phobia.system.commandsystem.staffsystem;

public record Staff(String name, long addedAt) {
   public Staff(String name) {
      this(name, System.currentTimeMillis());
   }

   public String getName() {
      return this.name;
   }
}
