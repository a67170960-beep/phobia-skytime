package ruhack.phobia.system.eventsystem.impl;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ruhack.phobia.system.eventsystem.api.events.Event;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {
}
