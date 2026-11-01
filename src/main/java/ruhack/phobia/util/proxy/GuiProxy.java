package ruhack.phobia.util.proxy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.CheckboxWidget.Builder;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import ruhack.phobia.system.configsystem.impl.proxy.ProxyConfig;

public class GuiProxy extends Screen {
   private boolean isSocks4 = false;
   private TextFieldWidget ipPort;
   private TextFieldWidget username;
   private TextFieldWidget password;
   private CheckboxWidget enabledCheck;
   private Screen parentScreen;
   private String msg = "";
   private int[] positionY;
   private int positionX;
   private static String text_proxy = Text.translatable("PROXY").getString();

   public GuiProxy(Screen parentScreen) {
      super(Text.literal(text_proxy));
      this.parentScreen = parentScreen;
   }

   private static boolean isValidIpPort(String ipP) {
      if (ipP != null && !ipP.isEmpty()) {
         String[] split = ipP.split(":");
         if (split.length <= 1) {
            return false;
         }

         if (!StringUtils.isNumeric(split[1])) {
            return false;
         }

         try {
            int port = Integer.parseInt(split[1]);
            return port >= 0 && port <= 65535;
         } catch (NumberFormatException e) {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean checkProxy() {
      if (!isValidIpPort(this.ipPort.getText())) {
         this.ipPort.setFocused(true);
         return false;
      } else {
         return true;
      }
   }

   private void centerButtons(int amount, int buttonLength, int gap) {
      this.positionX = this.width / 2 - buttonLength / 2;
      this.positionY = new int[amount];
      int center = (this.height + amount * gap) / 2;
      int buttonStarts = center - amount * gap;

      for (int i = 0; i != amount; i++) {
         this.positionY[i] = buttonStarts + gap * i;
      }
   }

   public boolean keyPressed(KeyInput input) {
      if (input.isEscape()) {
         MinecraftClient.getInstance().setScreen(this.parentScreen);
         return true;
      } else {
         super.keyPressed(input);
         this.msg = "";
         return true;
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      if (this.enabledCheck.isChecked() && !isValidIpPort(this.ipPort.getText())) {
         this.enabledCheck.onPress(null);
      }

      context.drawTextWithShadow(
         this.textRenderer, Text.translatable("Введите айпи адрес и порт. Пример ниже").getString(), this.width / 2 - 106, this.positionY[3] - 15, 10526880
      );
      context.drawTextWithShadow(this.textRenderer, Text.translatable("Айпи:Порт ▸").getString(), this.width / 2 - 140, this.positionY[3] + 15, 10526880);
      this.ipPort.render(context, mouseX, mouseY, deltaTicks);
      context.drawTextWithShadow(this.textRenderer, Text.translatable("Никнейм ▸").getString(), this.width / 2 - 131, this.positionY[4] + 15, 10526880);
      context.drawTextWithShadow(this.textRenderer, Text.translatable("Пароль ▸").getString(), this.width / 2 - 126, this.positionY[5] + 15, 10526880);
      this.username.render(context, mouseX, mouseY, deltaTicks);
      this.password.render(context, mouseX, mouseY, deltaTicks);
      context.drawCenteredTextWithShadow(this.textRenderer, this.msg, this.width / 2, this.positionY[6] + 5, 10526880);
   }

   public void init() {
      int buttonLength = 160;
      this.centerButtons(10, buttonLength, 26);
      ProxyConfig config = ProxyConfig.getInstance();
      Proxy currentProxy = config.getDefaultProxy();
      this.isSocks4 = currentProxy.type == Proxy.ProxyType.SOCKS4;
      this.ipPort = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[3] + 10, buttonLength, 20, Text.literal(""));
      this.ipPort.setText(currentProxy.ipPort);
      this.ipPort.setMaxLength(1024);
      this.ipPort.setFocused(true);
      this.addSelectableChild(this.ipPort);
      this.username = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[4] + 10, buttonLength, 20, Text.literal(""));
      this.username.setMaxLength(255);
      this.username.setText(currentProxy.username);
      this.addSelectableChild(this.username);
      this.password = new TextFieldWidget(this.textRenderer, this.positionX, this.positionY[5] + 10, buttonLength, 20, Text.literal(""));
      this.password.setMaxLength(255);
      this.password.setText(currentProxy.password);
      this.addSelectableChild(this.password);
      int posXButtons = this.width / 2 - buttonLength / 2 * 3 / 2;
      ButtonWidget apply = ButtonWidget.builder(Text.translatable("Применить"), button -> {
         ProxyConfig cfg = ProxyConfig.getInstance();
         if (this.enabledCheck.isChecked()) {
            if (this.checkProxy()) {
               Proxy newProxy = new Proxy(this.isSocks4, this.ipPort.getText(), this.username.getText(), this.password.getText());
               cfg.setDefaultProxy(newProxy);
               cfg.setProxyEnabled(true);
               cfg.save();
               MinecraftClient.getInstance().setScreen(new MultiplayerScreen(new TitleScreen()));
            }
         } else {
            Proxy newProxy = new Proxy(this.isSocks4, this.ipPort.getText(), this.username.getText(), this.password.getText());
            cfg.setDefaultProxy(newProxy);
            cfg.setProxyEnabled(false);
            cfg.save();
            MinecraftClient.getInstance().setScreen(new MultiplayerScreen(new TitleScreen()));
         }
      }).dimensions(posXButtons + (buttonLength / 2 - 62) * 2, this.positionY[7] - 10, buttonLength / 2 + 3, 20).build();
      this.addDrawableChild(apply);
      Builder checkboxBuilder = CheckboxWidget.builder(Text.translatable("Включить прокси"), this.textRenderer);
      checkboxBuilder.pos(this.width / 2 - 34 - (13 + this.textRenderer.getWidth(Text.translatable("Включить прокси"))) / 2, this.positionY[7] + 15);
      if (config.isProxyEnabled()) {
         checkboxBuilder.checked(true);
      }

      this.enabledCheck = checkboxBuilder.build();
      this.addDrawableChild(this.enabledCheck);
      ButtonWidget cancel = ButtonWidget.builder(Text.translatable("Отменить"), button -> MinecraftClient.getInstance().setScreen(this.parentScreen))
         .dimensions(posXButtons + (buttonLength / 2 - 16) * 2, this.positionY[7] - 10, buttonLength / 2 - 3, 20)
         .build();
      this.addDrawableChild(cancel);
   }

   public void close() {
      this.msg = "";
      MinecraftClient.getInstance().setScreen(this.parentScreen);
   }
}
