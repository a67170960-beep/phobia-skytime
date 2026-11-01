package ruhack.phobia.system.modulesystem.impl.display;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.dragsystem.PillHudElement;
import ruhack.phobia.system.modulesystem.ModuleRepository;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.setting.Setting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.colorschem.ColorUtil;

public class ArrayListElement extends PillHudElement {
   private static final float BAR_WIDTH = 2.0F;
   private static final float ROW_RADIUS = 4.0F;
   private static final float LEFT_PADDING = 4.0F;
   private static final float RIGHT_PADDING = 4.0F;
   private static final float MODE_GAP = 3.5F;
   private static final int SOFT_WHITE = ColorUtil.rgba(243, 243, 245, 255);
   private final Map<ModuleStructure, ArrayListElement.Row> rows = new LinkedHashMap<>();
   private final Map<ModuleStructure, List<SelectSetting>> modeSettings = new HashMap<>();
   private final List<ArrayListElement.Row> orderedRows = new ArrayList<>();
   private long lastFrameTime;
   private boolean orderDirty = true;
   private float cachedFontSize = Float.NaN;
   private boolean cachedSideBar;

   public ArrayListElement() {
      super("ArrayList", 6, 56, true);
      this.width = 90;
      this.height = 11;
   }

   @Override
   public float getRoundingRadius() {
      return 4.0F;
   }

   @Override
   public void setX(int x) {
      super.setX(x);
   }

   @Override
   public void setY(int y) {
      super.setY(y);
   }

   @Override
   public boolean visible() {
      return this.mc.player != null;
   }

   @Override
   public void drawDraggable(DrawContext context, int parentAlpha) {
      Hud hud = Hud.getInstance();
      ModuleRepository repository = this.repository();
      if (hud != null && repository != null) {
         long now = System.currentTimeMillis();
         float delta = this.lastFrameTime == 0L ? 0.016666668F : Math.min(0.05F, (float)(now - this.lastFrameTime) / 1000.0F);
         this.lastFrameTime = now;
         float duration = Math.max(0.08F, hud.arrayListAnimation.getValue() / 1000.0F);
         float animation = 1.0F - (float)Math.exp(-delta * 4.6F / duration);
         float fontSize = hud.arrayListFontSize.getValue();
         float rowHeight = Math.max(9.0F, Msdf.height(MsdfManager.BOLD, fontSize) + 3.0F);
         float parentOpacity = parentAlpha / 255.0F;
         this.syncRows(repository, hud, fontSize);
         if (this.orderDirty) {
            this.orderedRows.clear();
            this.orderedRows.addAll(this.rows.values());
            this.orderedRows
               .sort(
                  Comparator.<ArrayListElement.Row>comparingDouble(row -> row.targetWidth)
                     .reversed()
                     .thenComparing(row -> row.module.getName(), String.CASE_INSENSITIVE_ORDER)
               );
            this.orderDirty = false;
         }

         float targetY = 0.0F;

         for (ArrayListElement.Row row : this.orderedRows) {
            row.alpha = approach(row.alpha, row.enabled ? 1.0F : 0.0F, animation);
            row.width = approach(row.width, row.targetWidth, animation);
            row.yOffset = approach(row.yOffset, targetY, animation);
            if (row.enabled || row.alpha > 0.02F) {
               targetY += rowHeight;
            }
         }

         float layoutWidth = 1.0F;

         for (ArrayListElement.Row row : this.orderedRows) {
            if (row.alpha > 0.01F && row.width > 1.0F) {
               layoutWidth = Math.max(layoutWidth, row.width);
            }
         }

         float viewportWidth = Render2D.getFixedScaledWidth() / Math.max(0.01F, Render2D.getContextScale());
         boolean alignRight = this.x + layoutWidth / 2.0F >= viewportWidth / 2.0F;
         float maxBottom = rowHeight;
         int visibleIndex = 0;
         float barTop = Float.POSITIVE_INFINITY;
         float barBottom = Float.NEGATIVE_INFINITY;

         for (ArrayListElement.Row row : this.orderedRows) {
            if (!(row.alpha <= 0.01F) && !(row.width <= 1.0F)) {
               float opacity = parentOpacity * MathHelper.clamp(row.alpha, 0.0F, 1.0F);
               float rowY = this.y + row.yOffset;
               float slide = (1.0F - row.alpha) * 5.0F;
               float rowWidth = Math.max(3.0F, row.width);
               float rowX = alignRight ? this.x + layoutWidth - rowWidth + slide : this.x - slide;
               int background = ColorUtil.rgba(11, 12, 18, Math.round(hud.arrayListBackgroundAlpha.getValue() * opacity));
               int modeColor = ColorUtil.multAlpha(SOFT_WHITE, opacity);
               Render2D.blur(context, rowX, rowY - 0.12F, rowWidth, rowHeight + 0.24F, 4.0F, 8.0F, false);
               Render2D.rect(context, rowX, rowY - 0.12F, rowWidth, rowHeight + 0.24F, 4.0F, background, false);
               barTop = Math.min(barTop, rowY);
               barBottom = Math.max(barBottom, rowY + rowHeight);
               float textX = rowX + 4.0F;
               if (hud.arrayListSideBar.isValue() && !alignRight) {
                  textX += 2.0F;
               }

               float textY = this.textY(MsdfManager.BOLD, fontSize, rowY + rowHeight * 0.5F);
               float colorPosition = (float)visibleIndex / Math.max(1, this.orderedRows.size());
               int nameStart = ColorUtil.multAlpha(ColorUtil.getClientColorAt(colorPosition), opacity);
               int nameEnd = ColorUtil.multAlpha(ColorUtil.getClientColorAt(colorPosition + 0.18F), opacity);
               Msdf.textGradient(context, MsdfManager.BOLD, row.name, textX, textY, fontSize, nameStart, nameEnd, false);
               if (!row.mode.isEmpty()) {
                  float modeX = textX + Msdf.width(MsdfManager.BOLD, row.name, fontSize) + 3.5F;
                  Msdf.text(context, MsdfManager.BOLD, row.mode, modeX, textY, fontSize, modeColor, false);
               }

               maxBottom = Math.max(maxBottom, row.yOffset + rowHeight);
               visibleIndex++;
            }
         }

         if (hud.arrayListSideBar.isValue() && barTop < barBottom) {
            int top = ColorUtil.multAlpha(ColorUtil.getClientColorAt(0.0F), parentOpacity);
            int middle = ColorUtil.multAlpha(ColorUtil.getClientColorAt(0.5F), parentOpacity);
            int bottom = ColorUtil.multAlpha(ColorUtil.getClientColorAt(0.999F), parentOpacity);
            float barX = alignRight ? this.x + layoutWidth - 2.0F : this.x;
            Render2D.rect(context, barX, barTop, 2.0F, barBottom - barTop, 0.0F, false, top, top, middle, top, middle, bottom, middle, bottom, bottom);
         }

         this.width = Math.max(1, (int)Math.ceil(layoutWidth));
         this.height = Math.max(1, (int)Math.ceil(maxBottom));
         this.removeFinishedRows();
      }
   }

   private void syncRows(ModuleRepository repository, Hud hud, float fontSize) {
      boolean metricsChanged = Float.compare(this.cachedFontSize, fontSize) != 0 || this.cachedSideBar != hud.arrayListSideBar.isValue();
      this.cachedFontSize = fontSize;
      this.cachedSideBar = hud.arrayListSideBar.isValue();

      for (ArrayListElement.Row row : this.rows.values()) {
         row.enabled = false;
      }

      for (ModuleStructure module : repository.modules()) {
         ArrayListElement.Row row = this.rows.get(module);
         if (module.isState()) {
            if (row == null) {
               row = new ArrayListElement.Row(module);
               this.rows.put(module, row);
               this.orderDirty = true;
            }

            row.enabled = true;
            String name = module.getName();
            String mode = hud.arrayListShowModes.isValue() ? this.selectedMode(module) : "";
            if (metricsChanged || row.targetWidth <= 0.0F || !name.equals(row.name) || !mode.equals(row.mode)) {
               row.name = name;
               row.mode = mode;
               float newWidth = this.calculateWidth(row, fontSize, this.cachedSideBar);
               if (Float.compare(row.targetWidth, newWidth) != 0) {
                  row.targetWidth = newWidth;
                  this.orderDirty = true;
               }
            }
         }
      }
   }

   private String selectedMode(ModuleStructure module) {
      for (SelectSetting setting : this.modeSettings.computeIfAbsent(module, ignored -> {
         List<SelectSetting> result = new ArrayList<>();

         for (Setting settingx : module.settings()) {
            if (settingx instanceof SelectSetting select) {
               result.add(select);
            }
         }

         return result;
      })) {
         if (setting.isVisible() && setting.getValue() != null && !setting.getValue().isBlank()) {
            return setting.getValue();
         }
      }

      return "";
   }

   private float calculateWidth(ArrayListElement.Row row, float fontSize, boolean sideBar) {
      float textWidth = Msdf.width(MsdfManager.BOLD, row.name, fontSize);
      if (!row.mode.isEmpty()) {
         textWidth += 3.5F + Msdf.width(MsdfManager.BOLD, row.mode, fontSize);
      }

      return (sideBar ? 2.0F : 0.0F) + 4.0F + textWidth + 4.0F;
   }

   private int themeColor(Hud hud, int index, int count) {
      return hud.arrayListColorMode.isSelected("Градиент") && count > 1
         ? ColorUtil.getClientColorAt((float)index / Math.max(1, count - 1))
         : ColorUtil.getClientColorAt((float)index / Math.max(1, count));
   }

   private void removeFinishedRows() {
      Iterator<Entry<ModuleStructure, ArrayListElement.Row>> iterator = this.rows.entrySet().iterator();

      while (iterator.hasNext()) {
         ArrayListElement.Row row = iterator.next().getValue();
         if (!row.enabled && row.alpha <= 0.01F) {
            iterator.remove();
            this.orderDirty = true;
         }
      }
   }

   private ModuleRepository repository() {
      Initialization initialization = Initialization.getInstance();
      return initialization != null && initialization.getManager() != null ? initialization.getManager().getModuleRepository() : null;
   }

   private static float approach(float current, float target, float amount) {
      return current + (target - current) * MathHelper.clamp(amount, 0.0F, 1.0F);
   }

   private static final class Row {
      final ModuleStructure module;
      String name;
      String mode = "";
      float targetWidth;
      float width;
      float yOffset;
      float alpha;
      boolean enabled;

      private Row(ModuleStructure module) {
         this.module = module;
         this.name = module.getName();
      }
   }
}
