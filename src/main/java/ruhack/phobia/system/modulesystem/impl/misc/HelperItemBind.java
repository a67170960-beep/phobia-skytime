package ruhack.phobia.system.modulesystem.impl.misc;

import net.minecraft.item.Item;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;

public record HelperItemBind(String source, Item item, BindSetting bind) {
}
