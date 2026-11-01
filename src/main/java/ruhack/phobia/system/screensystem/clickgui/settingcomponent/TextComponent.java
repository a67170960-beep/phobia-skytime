package ruhack.phobia.system.screensystem.clickgui.settingcomponent;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ruhack.phobia.system.modulesystem.setting.implement.TextSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiLayout;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiTheme;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.interfaces.AbstractSettingComponent;
import ruhack.phobia.util.render.scissor.Scissor;

public class TextComponent extends AbstractSettingComponent {
   public static boolean typing = false;
   private final TextSetting textSetting;
   private boolean focused = false;
   private int cursorPosition = 0;
   private int selectionStart = -1;
   private int selectionEnd = -1;
   private long lastClickTime = 0L;
   private String text = "";
   private float focusAnimation = 0.0F;
   private float hoverAnimation = 0.0F;
   private float textScrollOffset = 0.0F;
   private float targetScrollOffset = 0.0F;
   private float cursorBlinkAnimation = 0.0F;
   private float selectionAnimation = 0.0F;
   private float animatedCursorX = 0.0F;
   private float displayTextWidth = 0.0F;
   private float targetTextWidth = 0.0F;
   private float bgRed = 10.0F;
   private float bgGreen = 10.0F;
   private float bgBlue = 10.0F;
   private float outlineRed = 26.0F;
   private float outlineGreen = 26.0F;
   private float outlineBlue = 26.0F;
   private long lastUpdateTime = System.currentTimeMillis();
   private long lastInputTime = 0L;
   private char lastChar = 0;
   private static final float ANIMATION_SPEED = 10.0F;
   private static final float COLOR_ANIMATION_SPEED = 8.0F;
   private static final float CURSOR_ANIMATION_SPEED = 20.0F;
   private static final float TEXT_ANIMATION_SPEED = 25.0F;
   private static final float SCROLL_ANIMATION_SPEED = 18.0F;
   private static final float INPUT_BOX_HEIGHT_PX = 24.0F;
   private static final float TEXT_PADDING_PX = 8.0F;

   public TextComponent(TextSetting setting) {
      super(setting);
      this.textSetting = setting;
      this.text = this.textSetting.getValue() != null ? this.textSetting.getValue() : "";
      this.cursorPosition = this.text.length();
   }

   private float getDeltaTime() {
      long currentTime = System.currentTimeMillis();
      float deltaTime = Math.min((float)(currentTime - this.lastUpdateTime) / 1000.0F, 0.1F);
      this.lastUpdateTime = currentTime;
      return deltaTime;
   }

   private float lerp(float current, float target, float speed) {
      float diff = target - current;
      return Math.abs(diff) < 0.001F ? target : current + diff * Math.min(speed, 1.0F);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext context, float x, float y, float width, int mouseX, int mouseY, float delta) {
      this.x = x;
      this.y = y;
      this.width = width;
      float inputBoxHeight = ClickGuiLayout.physicalToRender(24.0F);
      this.height = ClickGuiLayout.physicalToRender(43.0F);
      float deltaTime = this.getDeltaTime();
      boolean hovered = this.isInputBoxHover(mouseX, mouseY);
      this.hoverAnimation = this.lerp(this.hoverAnimation, hovered ? 1.0F : 0.0F, deltaTime * 10.0F);
      this.focusAnimation = this.lerp(this.focusAnimation, this.focused ? 1.0F : 0.0F, deltaTime * 10.0F);
      this.selectionAnimation = this.lerp(this.selectionAnimation, this.hasSelection() ? 1.0F : 0.0F, deltaTime * 10.0F);
      if (this.focused) {
         this.cursorBlinkAnimation += deltaTime * 2.0F;
         if (this.cursorBlinkAnimation > 1.0F) {
            this.cursorBlinkAnimation--;
         }
      } else {
         this.cursorBlinkAnimation = 0.0F;
      }

      int textColor = ColorUtil.rgba(255, 255, 255, (int)(255.0F * this.alphaMultiplier));
      if (MsdfManager.BOLD != null) {
         Msdf.text(
            context,
            MsdfManager.BOLD,
            this.textSetting.getName(),
            x,
            y + ClickGuiLayout.physicalToRender(2.0F),
            ClickGuiLayout.physicalToRender(12.0F),
            textColor,
            true
         );
      }

      float boxX = x;
      float boxY = y + ClickGuiLayout.physicalToRender(19.0F);
      float boxWidth = width;
      float targetBgR = this.focused ? 15.0F : 10.0F;
      float targetBgG = this.focused ? 15.0F : 10.0F;
      float targetBgB = this.focused ? 15.0F : 10.0F;
      this.bgRed = this.lerp(this.bgRed, targetBgR, deltaTime * 8.0F);
      this.bgGreen = this.lerp(this.bgGreen, targetBgG, deltaTime * 8.0F);
      this.bgBlue = this.lerp(this.bgBlue, targetBgB, deltaTime * 8.0F);
      int focusAccent = ClickGuiTheme.accentAt(boxX + boxWidth / 2.0F, 255);
      float targetOutR = this.focused ? ColorUtil.getRed(focusAccent) : 26.0F;
      float targetOutG = this.focused ? ColorUtil.getGreen(focusAccent) : 26.0F;
      float targetOutB = this.focused ? ColorUtil.getBlue(focusAccent) : 26.0F;
      this.outlineRed = this.lerp(this.outlineRed, targetOutR, deltaTime * 8.0F);
      this.outlineGreen = this.lerp(this.outlineGreen, targetOutG, deltaTime * 8.0F);
      this.outlineBlue = this.lerp(this.outlineBlue, targetOutB, deltaTime * 8.0F);
      int bgColor = ColorUtil.rgba((int)this.bgRed, (int)this.bgGreen, (int)this.bgBlue, (int)(255.0F * this.alphaMultiplier));
      Render2D.rect(context, boxX, boxY, boxWidth, inputBoxHeight, ClickGuiLayout.physicalToRender(6.0F), bgColor, true);
      int outlineColor = ColorUtil.rgba((int)this.outlineRed, (int)this.outlineGreen, (int)this.outlineBlue, (int)(255.0F * this.alphaMultiplier));
      Render2D.outline(
         context, boxX, boxY, boxWidth, inputBoxHeight, ClickGuiLayout.physicalToRender(6.0F), ClickGuiLayout.physicalToRender(1.0F), outlineColor, true
      );
      this.renderTextContent(context, boxX, boxY, boxWidth, deltaTime);
   }

   public float getHeight() {
      return ClickGuiLayout.physicalToRender(43.0F);
   }

   private void renderTextContent(DrawContext context, float boxX, float boxY, float boxWidth, float deltaTime) {
      float inputBoxHeight = ClickGuiLayout.physicalToRender(24.0F);
      float textPadding = ClickGuiLayout.physicalToRender(8.0F);
      float textSize = ClickGuiLayout.physicalToRender(10.0F);
      float textAreaX = boxX + textPadding;
      float textAreaWidth = boxWidth - textPadding * 2.0F;
      float textY = boxY + ClickGuiLayout.physicalToRender(7.0F);
      String displayText = this.text;
      float fullTextWidth = MsdfManager.BOLD != null ? MsdfManager.BOLD.getStringWidth(displayText, textSize) : 0.0F;
      this.targetTextWidth = fullTextWidth;
      this.displayTextWidth = this.lerp(this.displayTextWidth, this.targetTextWidth, deltaTime * 25.0F);
      if (this.focused) {
         String beforeCursor = this.text.substring(0, this.cursorPosition);
         float targetCursorX = MsdfManager.BOLD != null ? MsdfManager.BOLD.getStringWidth(beforeCursor, textSize) : 0.0F;
         if (this.focusAnimation > 0.95F) {
            this.animatedCursorX = this.lerp(this.animatedCursorX, targetCursorX, deltaTime * 20.0F);
         } else {
            this.animatedCursorX = targetCursorX;
         }

         if (this.animatedCursorX - this.targetScrollOffset > textAreaWidth - 2.0F) {
            this.targetScrollOffset = this.animatedCursorX - textAreaWidth + 2.0F;
         } else if (this.animatedCursorX - this.targetScrollOffset < 0.0F) {
            this.targetScrollOffset = this.animatedCursorX;
         }

         if (fullTextWidth <= textAreaWidth) {
            this.targetScrollOffset = 0.0F;
         }

         this.targetScrollOffset = Math.max(0.0F, Math.min(this.targetScrollOffset, Math.max(0.0F, fullTextWidth - textAreaWidth)));
      } else {
         this.targetScrollOffset = 0.0F;
         this.animatedCursorX = 0.0F;
      }

      this.textScrollOffset = this.lerp(this.textScrollOffset, this.targetScrollOffset, deltaTime * 18.0F);
      Scissor.push(textAreaX, boxY, textAreaWidth, inputBoxHeight, true);
      if (this.text.isEmpty() && !this.focused) {
         if (MsdfManager.BOLD != null) {
            float hintAlpha = 200.0F * (1.0F - this.focusAnimation) * this.alphaMultiplier;
            int hintColor = ColorUtil.rgba(80, 80, 80, (int)hintAlpha);
            Msdf.text(context, MsdfManager.BOLD, "Type here...", textAreaX, textY, textSize, hintColor, true);
         }
      } else {
         if (this.focused && this.hasSelection() && this.selectionAnimation > 0.01F) {
            int start = this.getStartOfSelection();
            int end = this.getEndOfSelection();
            String beforeSelection = this.text.substring(0, start);
            String selection = this.text.substring(start, end);
            float selectionX = textAreaX
               + (MsdfManager.BOLD != null ? MsdfManager.BOLD.getStringWidth(beforeSelection, textSize) : 0.0F)
               - this.textScrollOffset;
            float selectionWidth = MsdfManager.BOLD != null ? MsdfManager.BOLD.getStringWidth(selection, textSize) : 0.0F;
            int selAlpha = (int)(60.0F * this.selectionAnimation * this.alphaMultiplier);
            int selColor = ClickGuiTheme.accent(selAlpha);
            Render2D.rect(
               context,
               selectionX,
               boxY + ClickGuiLayout.physicalToRender(4.0F),
               selectionWidth,
               inputBoxHeight - ClickGuiLayout.physicalToRender(8.0F),
               ClickGuiLayout.physicalToRender(2.0F),
               selColor,
               true
            );
         }

         int textAlpha = (int)((200.0F + this.focusAnimation * 55.0F) * this.alphaMultiplier);
         int textColor = ColorUtil.rgba(255, 255, 255, textAlpha);
         if (MsdfManager.BOLD != null) {
            Msdf.text(context, MsdfManager.BOLD, displayText, textAreaX - this.textScrollOffset, textY, textSize, textColor, true);
         }

         if (this.focused && !this.hasSelection()) {
            float cursorAlpha = (float)(Math.sin(this.cursorBlinkAnimation * Math.PI * 2.0) * 0.5 + 0.5);
            if (cursorAlpha > 0.3F) {
               float cursorXPos = textAreaX + this.animatedCursorX - this.textScrollOffset;
               int cursorAlphaInt = (int)(255.0F * cursorAlpha * this.focusAnimation * this.alphaMultiplier);
               int cursorColor = ClickGuiTheme.accent(cursorAlphaInt);
               Render2D.rect(
                  context,
                  cursorXPos,
                  boxY + ClickGuiLayout.physicalToRender(5.0F),
                  ClickGuiLayout.physicalToRender(1.0F),
                  inputBoxHeight - ClickGuiLayout.physicalToRender(10.0F),
                  0.0F,
                  cursorColor,
                  true
               );
            }
         }
      }

      Scissor.pop(true);
   }

   private boolean isInputBoxHover(double mouseX, double mouseY) {
      float boxWidth = this.width;
      float boxX = this.x;
      float boxY = this.y + ClickGuiLayout.physicalToRender(19.0F);
      return mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + ClickGuiLayout.physicalToRender(24.0F);
   }

   private List<String> wrapText(String text, float maxWidth, float size, MsdfFont font) {
      List<String> lines = new ArrayList<>();
      if (text != null && !text.isEmpty()) {
         String[] words = text.split(" ");
         String currentLine = "";

         for (String word : words) {
            String test = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.getStringWidth(test, size) <= maxWidth) {
               currentLine = test;
            } else {
               if (!currentLine.isEmpty()) {
                  lines.add(currentLine);
               }

               currentLine = word;
            }
         }

         if (!currentLine.isEmpty()) {
            lines.add(currentLine);
         }

         return lines;
      } else {
         return lines;
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      boolean wasInside = this.isInputBoxHover(mouseX, mouseY);
      if (wasInside && button == 0) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastClickTime < 250L && this.focused) {
            this.selectAllText();
         } else {
            if (!this.focused && MsdfManager.BOLD != null) {
               this.cursorPosition = this.getCursorIndexAt(mouseX);
               String beforeCursor = this.text.substring(0, this.cursorPosition);
               this.animatedCursorX = MsdfManager.BOLD.getStringWidth(beforeCursor, 7.0F);
            } else {
               this.cursorPosition = this.getCursorIndexAt(mouseX);
            }

            this.focused = true;
            typing = true;
            this.selectionStart = this.cursorPosition;
            this.selectionEnd = this.cursorPosition;
         }

         this.lastClickTime = currentTime;
         return true;
      } else {
         if (!wasInside && this.focused) {
            this.applyText();
            this.focused = false;
            typing = false;
            this.clearSelection();
         }

         return false;
      }
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.focused && button == 0) {
         this.cursorPosition = this.getCursorIndexAt(mouseX);
         this.selectionEnd = this.cursorPosition;
         return true;
      } else {
         return false;
      }
   }

   private boolean isControlDown() {
      long window = mc.getWindow().getHandle();
      return GLFW.glfwGetKey(window, 341) == 1 || GLFW.glfwGetKey(window, 345) == 1;
   }

   private boolean isShiftDown() {
      long window = mc.getWindow().getHandle();
      return GLFW.glfwGetKey(window, 340) == 1 || GLFW.glfwGetKey(window, 344) == 1;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.focused) {
         return false;
      }

      if (this.isControlDown()) {
         switch (keyCode) {
            case 65:
               this.selectAllText();
               return true;
            case 67:
               this.copyToClipboard();
               return true;
            case 86:
               this.pasteFromClipboard();
               return true;
            case 88:
               if (this.hasSelection()) {
                  this.copyToClipboard();
                  this.deleteSelectedText();
               }

               return true;
         }
      } else {
         switch (keyCode) {
            case 256:
               this.text = this.textSetting.getValue() != null ? this.textSetting.getValue() : "";
               this.cursorPosition = this.text.length();
               this.focused = false;
               typing = false;
               return true;
            case 257:
               this.applyText();
               this.focused = false;
               typing = false;
               return true;
            case 258:
            case 260:
            case 264:
            case 265:
            case 266:
            case 267:
            default:
               break;
            case 259:
               this.handleBackspace();
               return true;
            case 261:
               this.handleDelete();
               return true;
            case 262:
               this.moveCursor(1);
               return true;
            case 263:
               this.moveCursor(-1);
               return true;
            case 268:
               this.cursorPosition = 0;
               this.updateSelectionAfterCursorMove();
               return true;
            case 269:
               this.cursorPosition = this.text.length();
               this.updateSelectionAfterCursorMove();
               return true;
         }
      }

      return false;
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (!this.focused) {
         return false;
      }

      if (Character.isISOControl(chr)) {
         return false;
      }

      long currentTime = System.currentTimeMillis();
      if (chr == this.lastChar && currentTime - this.lastInputTime < 10L) {
         return false;
      }

      this.lastChar = chr;
      this.lastInputTime = currentTime;
      int maxLength = this.textSetting.getMax() > 0 ? this.textSetting.getMax() : Integer.MAX_VALUE;
      if (this.text.length() >= maxLength && !this.hasSelection()) {
         return false;
      }

      this.deleteSelectedText();
      this.text = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
      this.cursorPosition++;
      this.clearSelection();
      return true;
   }

   @Override
   public void tick() {
   }

   private void applyText() {
      int minLength = this.textSetting.getMin() > 0 ? this.textSetting.getMin() : 0;
      int maxLength = this.textSetting.getMax() > 0 ? this.textSetting.getMax() : Integer.MAX_VALUE;
      if (this.text.length() >= minLength && this.text.length() <= maxLength) {
         this.textSetting.setValue(this.text);
      } else {
         this.text = this.textSetting.getValue() != null ? this.textSetting.getValue() : "";
         this.cursorPosition = this.text.length();
      }
   }

   private void handleBackspace() {
      if (this.hasSelection()) {
         this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
      } else if (this.cursorPosition > 0) {
         this.replaceText(this.cursorPosition - 1, this.cursorPosition, "");
      }
   }

   private void handleDelete() {
      if (this.hasSelection()) {
         this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
      } else if (this.cursorPosition < this.text.length()) {
         this.text = this.text.substring(0, this.cursorPosition) + this.text.substring(this.cursorPosition + 1);
      }
   }

   private void moveCursor(int direction) {
      if (this.hasSelection() && !this.isShiftDown()) {
         this.cursorPosition = direction < 0 ? this.getStartOfSelection() : this.getEndOfSelection();
         this.clearSelection();
      } else {
         if (direction < 0 && this.cursorPosition > 0) {
            this.cursorPosition--;
         } else if (direction > 0 && this.cursorPosition < this.text.length()) {
            this.cursorPosition++;
         }

         this.updateSelectionAfterCursorMove();
      }
   }

   private void updateSelectionAfterCursorMove() {
      if (this.isShiftDown()) {
         if (this.selectionStart == -1) {
            this.selectionStart = this.selectionEnd != -1 ? this.selectionEnd : this.cursorPosition;
         }

         this.selectionEnd = this.cursorPosition;
      } else {
         this.clearSelection();
      }
   }

   private void pasteFromClipboard() {
      String clipboardText = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
      if (clipboardText != null && !clipboardText.isEmpty()) {
         clipboardText = clipboardText.replaceAll("[\n\r\t]", "");
         if (this.hasSelection()) {
            this.deleteSelectedText();
         }

         int maxLength = this.textSetting.getMax() > 0 ? this.textSetting.getMax() : Integer.MAX_VALUE;
         int remainingSpace = maxLength - this.text.length();
         if (clipboardText.length() > remainingSpace) {
            clipboardText = clipboardText.substring(0, remainingSpace);
         }

         if (!clipboardText.isEmpty()) {
            this.text = this.text.substring(0, this.cursorPosition) + clipboardText + this.text.substring(this.cursorPosition);
            this.cursorPosition = this.cursorPosition + clipboardText.length();
         }
      }
   }

   private void copyToClipboard() {
      if (this.hasSelection()) {
         GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), this.getSelectedText());
      }
   }

   private void selectAllText() {
      this.selectionStart = 0;
      this.selectionEnd = this.text.length();
      this.cursorPosition = this.text.length();
   }

   private void replaceText(int start, int end, String replacement) {
      if (start < 0) {
         start = 0;
      }

      if (end > this.text.length()) {
         end = this.text.length();
      }

      if (start > end) {
         int temp = start;
         start = end;
         end = temp;
      }

      this.text = this.text.substring(0, start) + replacement + this.text.substring(end);
      this.cursorPosition = start + replacement.length();
      this.clearSelection();
   }

   private void deleteSelectedText() {
      if (this.hasSelection()) {
         this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
      }
   }

   private boolean hasSelection() {
      return this.selectionStart != -1 && this.selectionEnd != -1 && this.selectionStart != this.selectionEnd;
   }

   private String getSelectedText() {
      return !this.hasSelection() ? "" : this.text.substring(this.getStartOfSelection(), this.getEndOfSelection());
   }

   private int getStartOfSelection() {
      return Math.min(this.selectionStart, this.selectionEnd);
   }

   private int getEndOfSelection() {
      return Math.max(this.selectionStart, this.selectionEnd);
   }

   private void clearSelection() {
      this.selectionStart = -1;
      this.selectionEnd = -1;
   }

   private int getCursorIndexAt(double mouseX) {
      float boxX = this.x;
      float textAreaX = boxX + ClickGuiLayout.physicalToRender(8.0F);
      float relativeX = (float)(mouseX - textAreaX + this.textScrollOffset);
      if (relativeX <= 0.0F) {
         return 0;
      }

      int position = 0;
      float lastWidth = 0.0F;

      while (position < this.text.length() && MsdfManager.BOLD != null) {
         float currentWidth = MsdfManager.BOLD.getStringWidth(this.text.substring(0, position + 1), 7.0F);
         float midPoint = (lastWidth + currentWidth) / 2.0F;
         if (relativeX < midPoint) {
            return position;
         }

         lastWidth = currentWidth;
         position++;
      }

      return this.text.length();
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public boolean isFocused() {
      return this.focused;
   }
}
