package ruhack.phobia.util.string;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class KeyHelper {
   private static final Map<Integer, String> KEY_NAMES = new HashMap<>();
   private static final Map<String, Integer> NAME_TO_KEY = new HashMap<>();

   public static String getKeyName(int keyCode) {
      return KEY_NAMES.getOrDefault(keyCode, "Unknown(" + keyCode + ")");
   }

   public static int getKeyCode(String name) {
      return NAME_TO_KEY.getOrDefault(name.toLowerCase(), -1);
   }

   public static boolean isValidKey(String name) {
      return NAME_TO_KEY.containsKey(name.toLowerCase());
   }

   public static String[] getAllKeyNames() {
      return KEY_NAMES.values().toArray(new String[0]);
   }

   static {
      KEY_NAMES.put(2, "Mouse3");
      KEY_NAMES.put(3, "Mouse4");
      KEY_NAMES.put(4, "Mouse5");
      KEY_NAMES.put(5, "Mouse6");
      KEY_NAMES.put(6, "Mouse7");
      KEY_NAMES.put(7, "Mouse8");
      KEY_NAMES.put(32, "Space");
      KEY_NAMES.put(39, "'");
      KEY_NAMES.put(44, ",");
      KEY_NAMES.put(45, "-");
      KEY_NAMES.put(46, ".");
      KEY_NAMES.put(47, "/");
      KEY_NAMES.put(48, "0");
      KEY_NAMES.put(49, "1");
      KEY_NAMES.put(50, "2");
      KEY_NAMES.put(51, "3");
      KEY_NAMES.put(52, "4");
      KEY_NAMES.put(53, "5");
      KEY_NAMES.put(54, "6");
      KEY_NAMES.put(55, "7");
      KEY_NAMES.put(56, "8");
      KEY_NAMES.put(57, "9");
      KEY_NAMES.put(59, ";");
      KEY_NAMES.put(61, "=");
      KEY_NAMES.put(65, "A");
      KEY_NAMES.put(66, "B");
      KEY_NAMES.put(67, "C");
      KEY_NAMES.put(68, "D");
      KEY_NAMES.put(69, "E");
      KEY_NAMES.put(70, "F");
      KEY_NAMES.put(71, "G");
      KEY_NAMES.put(72, "H");
      KEY_NAMES.put(73, "I");
      KEY_NAMES.put(74, "J");
      KEY_NAMES.put(75, "K");
      KEY_NAMES.put(76, "L");
      KEY_NAMES.put(77, "M");
      KEY_NAMES.put(78, "N");
      KEY_NAMES.put(79, "O");
      KEY_NAMES.put(80, "P");
      KEY_NAMES.put(81, "Q");
      KEY_NAMES.put(82, "R");
      KEY_NAMES.put(83, "S");
      KEY_NAMES.put(84, "T");
      KEY_NAMES.put(85, "U");
      KEY_NAMES.put(86, "V");
      KEY_NAMES.put(87, "W");
      KEY_NAMES.put(88, "X");
      KEY_NAMES.put(89, "Y");
      KEY_NAMES.put(90, "Z");
      KEY_NAMES.put(91, "[");
      KEY_NAMES.put(92, "\\");
      KEY_NAMES.put(93, "]");
      KEY_NAMES.put(96, "`");
      KEY_NAMES.put(256, "Escape");
      KEY_NAMES.put(257, "Enter");
      KEY_NAMES.put(258, "Tab");
      KEY_NAMES.put(259, "Backspace");
      KEY_NAMES.put(260, "Insert");
      KEY_NAMES.put(261, "Delete");
      KEY_NAMES.put(262, "Right");
      KEY_NAMES.put(263, "Left");
      KEY_NAMES.put(264, "Down");
      KEY_NAMES.put(265, "Up");
      KEY_NAMES.put(266, "PageUp");
      KEY_NAMES.put(267, "PageDown");
      KEY_NAMES.put(268, "Home");
      KEY_NAMES.put(269, "End");
      KEY_NAMES.put(280, "CapsLock");
      KEY_NAMES.put(281, "ScrollLock");
      KEY_NAMES.put(282, "NumLock");
      KEY_NAMES.put(283, "PrintScreen");
      KEY_NAMES.put(284, "Pause");
      KEY_NAMES.put(290, "F1");
      KEY_NAMES.put(291, "F2");
      KEY_NAMES.put(292, "F3");
      KEY_NAMES.put(293, "F4");
      KEY_NAMES.put(294, "F5");
      KEY_NAMES.put(295, "F6");
      KEY_NAMES.put(296, "F7");
      KEY_NAMES.put(297, "F8");
      KEY_NAMES.put(298, "F9");
      KEY_NAMES.put(299, "F10");
      KEY_NAMES.put(300, "F11");
      KEY_NAMES.put(301, "F12");
      KEY_NAMES.put(320, "Numpad0");
      KEY_NAMES.put(321, "Numpad1");
      KEY_NAMES.put(322, "Numpad2");
      KEY_NAMES.put(323, "Numpad3");
      KEY_NAMES.put(324, "Numpad4");
      KEY_NAMES.put(325, "Numpad5");
      KEY_NAMES.put(326, "Numpad6");
      KEY_NAMES.put(327, "Numpad7");
      KEY_NAMES.put(328, "Numpad8");
      KEY_NAMES.put(329, "Numpad9");
      KEY_NAMES.put(330, "NumpadDecimal");
      KEY_NAMES.put(331, "NumpadDivide");
      KEY_NAMES.put(332, "NumpadMultiply");
      KEY_NAMES.put(333, "NumpadSubtract");
      KEY_NAMES.put(334, "NumpadAdd");
      KEY_NAMES.put(335, "NumpadEnter");
      KEY_NAMES.put(340, "LShift");
      KEY_NAMES.put(341, "LCtrl");
      KEY_NAMES.put(342, "LAlt");
      KEY_NAMES.put(344, "RShift");
      KEY_NAMES.put(345, "RCtrl");
      KEY_NAMES.put(346, "RAlt");
      KEY_NAMES.put(348, "Menu");

      for (Entry<Integer, String> entry : KEY_NAMES.entrySet()) {
         NAME_TO_KEY.put(entry.getValue().toLowerCase(), entry.getKey());
      }
   }
}
