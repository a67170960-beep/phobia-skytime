package ruhack.phobia.system.dragsystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.configsystem.impl.drag.DragConfig;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.modulesystem.impl.display.ArrayListElement;
import ruhack.phobia.system.modulesystem.impl.display.AutoWardenHud;
import ruhack.phobia.system.modulesystem.impl.display.BindPreview;
import ruhack.phobia.system.modulesystem.impl.display.Cooldowns;
import ruhack.phobia.system.modulesystem.impl.display.GpsHud;
import ruhack.phobia.system.modulesystem.impl.display.Hotkeys;
import ruhack.phobia.system.modulesystem.impl.display.Hud;
import ruhack.phobia.system.modulesystem.impl.display.Info;
import ruhack.phobia.system.modulesystem.impl.display.Logotype;
import ruhack.phobia.system.modulesystem.impl.display.Notifications;
import ruhack.phobia.system.modulesystem.impl.display.Potions;
import ruhack.phobia.system.modulesystem.impl.display.TargetHud;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf2D;

public class HudManager {
   private final List<HudElement> elements = new ArrayList<>();
   private final GpsHud gpsHud = new GpsHud();
   private final Map<Long, Map<String, HudManager.HudPosition>> viewportLayouts = new HashMap<>();
   private long activeViewportKey = Long.MIN_VALUE;
   private boolean initialized = false;

   public void initElements() {
      if (!this.initialized) {
         Logotype logotype = new Logotype();
         this.register(logotype);
         this.register(new Info(logotype));
         this.register(new Potions());
         this.register(new Cooldowns());
         this.register(new TargetHud());
         this.register(new Notifications());
         this.register(new Hotkeys());
         this.register(new BindPreview());
         this.register(new ArrayListElement());
         this.register(new AutoWardenHud());
         this.initialized = true;
         DragConfig.getInstance().load();
      }
   }

   public void register(HudElement element) {
      this.elements.add(element);
   }

   public void onPacket(PacketEvent e) {
      for (HudElement element : this.elements) {
         element.onPacket(e);
      }
   }

   public void render(DrawContext context, float tickDelta, int mouseX, int mouseY) {
      Hud hud = Hud.getInstance();
      float gpsScale = hud != null && hud.isState() ? hud.getInterfaceScale() : 1.0F;
      Render2D.withContextScale(gpsScale, () -> this.gpsHud.render(context, tickDelta));
      if (hud != null && hud.isState()) {
         float hudScale = hud.getInterfaceScale();
         this.syncViewportLayout(hudScale);
         Render2D.withContextScale(hudScale, () -> {
            Render2D.beginBlurFrame();

            try {
               for (HudElement element : this.elements) {
                  if (this.isElementEnabled(element, hud)) {
                     this.constrainToViewport(element, hudScale);
                     Msdf2D.beginBatch();

                     try {
                        element.render(context, tickDelta);
                     } finally {
                        Msdf2D.endBatch();
                     }

                     this.constrainToViewport(element, hudScale);
                  }
               }
            } finally {
               Render2D.endBlurFrame();
            }
         });
      }
   }

   public void tick() {
      Hud hud = Hud.getInstance();
      if (hud != null && hud.isState()) {
         for (HudElement element : this.elements) {
            if (this.isElementEnabled(element, hud)) {
               element.tick();
            }
         }
      }
   }

   private boolean isElementEnabled(HudElement element) {
      Hud hud = Hud.getInstance();
      return hud != null && hud.isState() && this.isElementEnabled(element, hud);
   }

   private boolean isElementEnabled(HudElement element, Hud hud) {
      return hud.interfaceSettings.isSelected(element.getName());
   }

   public HudElement getElementAt(double mouseX, double mouseY) {
      double logicalMouseX = this.toHudCoordinate(mouseX);
      double logicalMouseY = this.toHudCoordinate(mouseY);

      for (int i = this.elements.size() - 1; i >= 0; i--) {
         HudElement element = this.elements.get(i);
         if (this.isElementEnabled(element)
            && element.visible()
            && logicalMouseX >= element.getX()
            && logicalMouseX <= element.getX() + element.getWidth()
            && logicalMouseY >= element.getY()
            && logicalMouseY <= element.getY() + element.getHeight()) {
            return element;
         }
      }

      return null;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      double logicalMouseX = this.toHudCoordinate(mouseX);
      double logicalMouseY = this.toHudCoordinate(mouseY);

      for (HudElement element : this.elements) {
         if (this.isElementEnabled(element) && element.mouseClicked(logicalMouseX, logicalMouseY, button)) {
            return true;
         }
      }

      return false;
   }

   public void saveConfig() {
      DragConfig.getInstance().save();
   }

   public void loadConfig() {
      DragConfig.getInstance().load();
   }

   public List<HudElement> getElements() {
      return this.elements;
   }

   public List<HudElement> getEnabledElements() {
      List<HudElement> enabled = new ArrayList<>();

      for (HudElement element : this.elements) {
         if (this.isElementEnabled(element)) {
            enabled.add(element);
         }
      }

      return enabled;
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   public float getHudScale() {
      Hud hud = Hud.getInstance();
      return hud != null ? hud.getInterfaceScale() : 1.0F;
   }

   public double toHudCoordinate(double coordinate) {
      return coordinate / Math.max(0.01F, this.getHudScale());
   }

   public void constrainToViewport(HudElement element) {
      this.constrainToViewport(element, this.getHudScale());
   }

   private void constrainToViewport(HudElement element, float hudScale) {
      if (Render2D.hasUsableViewport()) {
         int margin = 4;
         int viewportWidth = (int)Math.floor(Render2D.getFixedScaledWidth() / Math.max(0.01F, hudScale));
         int viewportHeight = (int)Math.floor(Render2D.getFixedScaledHeight() / Math.max(0.01F, hudScale));
         int maxX = Math.max(margin, viewportWidth - element.getWidth() - margin);
         int maxY = Math.max(margin, viewportHeight - element.getHeight() - margin);
         element.setX(Math.max(margin, Math.min(element.getX(), maxX)));
         element.setY(Math.max(margin, Math.min(element.getY(), maxY)));
      }
   }

   private void syncViewportLayout(float hudScale) {
      if (Render2D.hasUsableViewport()) {
         int viewportWidth = (int)Math.floor(Render2D.getFixedScaledWidth() / Math.max(0.01F, hudScale));
         int viewportHeight = (int)Math.floor(Render2D.getFixedScaledHeight() / Math.max(0.01F, hudScale));
         long viewportKey = (long)viewportWidth << 32 | viewportHeight & 4294967295L;
         if (this.activeViewportKey == Long.MIN_VALUE) {
            this.activeViewportKey = viewportKey;
            this.saveViewportLayout(viewportKey);
         } else if (this.activeViewportKey != viewportKey) {
            this.saveViewportLayout(this.activeViewportKey);
            this.activeViewportKey = viewportKey;
            Map<String, HudManager.HudPosition> saved = this.viewportLayouts.get(viewportKey);
            if (saved != null) {
               for (HudElement element : this.elements) {
                  HudManager.HudPosition position = saved.get(element.getName());
                  if (position != null) {
                     element.setX(position.x());
                     element.setY(position.y());
                  }
               }
            }
         }
      }
   }

   private void saveViewportLayout(long viewportKey) {
      Map<String, HudManager.HudPosition> positions = new HashMap<>();

      for (HudElement element : this.elements) {
         positions.put(element.getName(), new HudManager.HudPosition(element.getX(), element.getY()));
      }

      this.viewportLayouts.put(viewportKey, positions);
   }

   private record HudPosition(int x, int y) {
   }
}
