package ruhack.phobia.system.modulesystem;

public class DuplicateModuleException extends RuntimeException {
   public DuplicateModuleException(String moduleName) {
      super("Duplicate module registration: " + moduleName);
   }
}
