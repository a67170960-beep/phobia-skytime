package ruhack.phobia.system.screensystem;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.impl.display.Notifications;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.colorschem.ColorUtil;

public final class TagPlayerMenuScreen extends Screen {
   private static final float NORMAL_GAP = 2.0F;
   private static final float ITEM_GAP = 12.0F;
   private static final float ROUNDING = 1.25F;
   private static final long OPEN_ANIMATION_NS = 160000000L;
   private final Screen parent;
   private final String playerName;
   private final float tagX;
   private final float tagY;
   private final float tagWidth;
   private final float tagHeight;
   private final boolean hasItems;
   private float menuX;
   private float menuY;
   private float menuWidth;
   private float menuHeight;
   private float drawX;
   private float drawY;
   private float drawWidth;
   private float drawHeight;
   private long openedAt;
   private long lastFrame;
   private float hover;

   public TagPlayerMenuScreen(Screen parent, String playerName, float tagX, float tagY, float tagWidth, float tagHeight, boolean hasItems) {
      super(Text.literal("Player: " + playerName));
      this.parent = parent;
      this.playerName = playerName;
      this.tagX = tagX;
      this.tagY = tagY;
      this.tagWidth = tagWidth;
      this.tagHeight = tagHeight;
      this.hasItems = hasItems;
   }

   protected void init() {
      this.updatePosition();
      this.openedAt = System.nanoTime();
      this.lastFrame = this.openedAt;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      this.updatePosition();
      float mx = Render2D.convertX(mouseX);
      float my = Render2D.convertY(mouseY);
      boolean friend = FriendUtils.isFriend(this.playerName);
      long now = System.nanoTime();
      float appear = this.easeOutCubic(Math.min(1.0F, (float)(now - this.openedAt) / 1.6E8F));
      float frameTime = Math.min(0.05F, (float)(now - this.lastFrame) * 1.0E-9F);
      this.lastFrame = now;
      float hoverTarget = this.containsMenu(mx, my) ? 1.0F : 0.0F;
      this.hover = this.hover + (hoverTarget - this.hover) * Math.min(1.0F, frameTime * 14.0F);
      float scaleX = 0.86F + appear * 0.14F;
      float scaleY = 0.72F + appear * 0.28F;
      this.drawWidth = this.menuWidth * scaleX;
      this.drawHeight = this.menuHeight * scaleY;
      this.drawX = this.menuX + (this.menuWidth - this.drawWidth) / 2.0F;
      this.drawY = this.menuY + (this.menuHeight - this.drawHeight) / 2.0F + (1.0F - appear) * 3.0F;
      MsdfFont font = MsdfManager.BOLD;
      if (font != null) {
         int accent = friend ? ColorUtil.rgba(220, 67, 82, 255) : ColorUtil.getClientColor();
         int background = ColorUtil.multAlpha(ColorUtil.rgba(0, 0, 0, 145), appear);
         Render2D.blur(context, this.drawX, this.drawY, this.drawWidth, this.drawHeight, 1.25F * scaleY, 8.0F * appear, true);
         Render2D.rect(context, this.drawX, this.drawY, this.drawWidth, this.drawHeight, 1.25F * scaleY, background, true);
         if (this.hover > 0.001F) {
            Render2D.rect(
               context,
               this.drawX,
               this.drawY,
               this.drawWidth,
               this.drawHeight,
               1.25F * scaleY,
               ColorUtil.multAlpha(accent, this.hover * appear * (friend ? 0.24F : 0.2F)),
               true
            );
         }

         String action = friend ? "Удалить из друзей" : "Добавить в друзья";
         float textSize = this.fittedTextSize(font, action, 5.5F * scaleY, Math.max(1.0F, this.drawWidth - 4.0F));
         float textY = this.drawY + (this.drawHeight - Msdf.height(font, textSize)) / 2.0F;
         int textColor = ColorUtil.interpolateColor(ColorUtil.rgba(241, 239, 248, 255), accent, this.hover);
         this.centered(context, action, this.drawX + this.drawWidth / 2.0F, textY, textSize, ColorUtil.multAlpha(textColor, appear));
      }
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   protected void applyBlur(DrawContext context) {
   }

   @Override
   public void renderInGameBackground(DrawContext context) {
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      if (click.button() != 0) {
         return true;
      }

      float mx = Render2D.convertX((float)click.x());
      float my = Render2D.convertY((float)click.y());
      if (this.containsMenu(mx, my)) {
         boolean friend = FriendUtils.isFriend(this.playerName);
         if (friend) {
            FriendUtils.removeFriendAndSave(this.playerName);
            Notifications.info("Игрок " + this.playerName + " удалён из друзей");
         } else {
            FriendUtils.addFriendAndSave(this.playerName);
            Notifications.info("Игрок " + this.playerName + " добавлен в друзья");
         }

         this.close();
         return true;
      } else {
         this.close();
         return true;
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      if (this.client != null) {
         this.client.setScreen(this.parent);
      }
   }

   private void updatePosition() {
      this.menuWidth = Math.max(1.0F, this.tagWidth);
      this.menuHeight = Math.max(1.0F, this.tagHeight);
      float maxX = Math.max(1.0F, Render2D.getFixedScaledWidth() - this.menuWidth - 1.0F);
      this.menuX = Math.max(1.0F, Math.min(this.tagX, maxX));
      float gap = this.hasItems ? 12.0F : 2.0F;
      this.menuY = Math.max(1.0F, this.tagY - this.menuHeight - gap);
   }

   private boolean containsMenu(float x, float y) {
      return x >= this.menuX && x <= this.menuX + this.menuWidth && y >= this.menuY && y <= this.menuY + this.menuHeight;
   }

   private float fittedTextSize(MsdfFont font, String text, float preferred, float availableWidth) {
      float width = Msdf.width(font, text, preferred);
      return width <= availableWidth ? preferred : Math.max(3.2F, preferred * availableWidth / width);
   }

   private float easeOutCubic(float value) {
      float inverse = 1.0F - Math.max(0.0F, Math.min(1.0F, value));
      return 1.0F - inverse * inverse * inverse;
   }

   private void centered(DrawContext context, String text, float centerX, float y, float size, int color) {
      Msdf.text(context, MsdfManager.BOLD, text, centerX - Msdf.width(MsdfManager.BOLD, text, size) / 2.0F, y, size, color, true);
   }
}
