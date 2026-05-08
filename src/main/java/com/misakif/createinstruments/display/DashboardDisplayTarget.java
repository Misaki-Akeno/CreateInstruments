package com.misakif.createinstruments.display;

import java.util.List;

import com.misakif.createinstruments.block.DashboardBlockEntity;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class DashboardDisplayTarget extends DisplayTarget {

    @Override
    public DisplayTargetStats provideStats(DisplayLinkContext context) {
        return new DisplayTargetStats(4, 20, this);
    }

    @Override
    public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {
        if (!(context.level().getBlockEntity(context.getTargetPos()) instanceof DashboardBlockEntity be))
            return;
        String raw = text.isEmpty() ? "" : text.get(0).getString();
        be.acceptValue(line, raw);
    }

    @Override
    public Component getLineOptionText(int line) {
        return Component.translatable("block.createinstruments.dashboard.slot", line + 1);
    }
}
