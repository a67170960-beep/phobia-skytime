package ruhack.phobia.util;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.win32.StdCallLibrary;
import org.lwjgl.glfw.GLFWNativeWin32;

public final class DWMApi {
   private DWMApi() {
   }

   public static void setDarkMode(long glfwWindowHandle) {
      if (!setAttribute(glfwWindowHandle, 20, 1)) {
         setAttribute(glfwWindowHandle, 19, 1);
         setAttribute(glfwWindowHandle, 34, 0);
      }
   }

   public static void setLightMode(long glfwWindowHandle) {
      if (!setAttribute(glfwWindowHandle, 20, 0)) {
         setAttribute(glfwWindowHandle, 19, 0);
         setAttribute(glfwWindowHandle, 34, 16777215);
      }
   }

   private static boolean setAttribute(long glfwWindowHandle, int attribute, int value) {
      if (glfwWindowHandle != 0L && System.getProperty("os.name").toLowerCase().contains("win")) {
         try {
            HWND hwnd = new HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(glfwWindowHandle)));
            Pointer ptr = new Pointer(Native.malloc(4L));

            try {
               ptr.setInt(0L, value);
               HRESULT result = DWMApi.Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd, attribute, ptr, 4);
               return result.equals(WinNT.S_OK);
            } finally {
               Native.free(Pointer.nativeValue(ptr));
            }
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   private interface Dwmapi extends StdCallLibrary {
      DWMApi.Dwmapi INSTANCE = (DWMApi.Dwmapi)Native.load("dwmapi", DWMApi.Dwmapi.class);

      HRESULT DwmSetWindowAttribute(HWND var1, int var2, Pointer var3, int var4);
   }
}
