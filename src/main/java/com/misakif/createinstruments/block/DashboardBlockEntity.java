package com.misakif.createinstruments.block;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.misakif.createinstruments.CreateInstruments;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DashboardBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d*\\.?\\d+");
    private static final float MAX_DIAL_VALUE = 256f;

    public float[] dialTargets   = new float[4];
    public float[] dialStates    = new float[4];
    public float[] prevDialStates = new float[4];
    private String[] displayedValues = new String[]{"", "", "", ""};

    public DashboardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
        for (int i = 0; i < 4; i++) {
            prevDialStates[i] = dialStates[i];
            dialStates[i] += (dialTargets[i] - dialStates[i]) * 0.125f;
        }
    }

    public void acceptValue(int slot, String rawText) {
        if (slot < 0 || slot >= 4) return;
        displayedValues[slot] = rawText == null ? "" : rawText;
        dialTargets[slot] = parseNeedle(rawText);
        setChanged();
        sendData();
    }

    private float parseNeedle(String text) {
        if (text == null || text.isBlank()) return 0f;
        if (text.contains("%")) {
            Matcher m = NUMBER_PATTERN.matcher(text);
            if (m.find()) {
                try {
                    return Math.min(1f, Math.max(0f, Float.parseFloat(m.group()) / 100f));
                } catch (NumberFormatException ignored) {}
            }
        }
        Matcher m = NUMBER_PATTERN.matcher(text);
        if (m.find()) {
            try {
                float val = Math.abs(Float.parseFloat(m.group()));
                return Math.min(1f, val / MAX_DIAL_VALUE);
            } catch (NumberFormatException ignored) {}
        }
        return 0f;
    }

    public String getDisplayedValue(int slot) {
        if (slot < 0 || slot >= 4) return "";
        String v = displayedValues[slot];
        return v == null ? "" : v;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.translate("block.createinstruments.dashboard.goggle_header").forGoggles(tooltip);
        boolean hasAny = false;
        for (int i = 0; i < 4; i++) {
            String val = displayedValues[i];
            if (val != null && !val.isBlank()) {
                CreateLang.translate("block.createinstruments.dashboard.slot", i + 1)
                    .space()
                    .add(Component.literal(val))
                    .forGoggles(tooltip, 1);
                hasAny = true;
            }
        }
        if (!hasAny) {
            CreateLang.translate("block.createinstruments.dashboard.no_data").forGoggles(tooltip, 1);
        }
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        for (int i = 0; i < 4; i++) {
            tag.putFloat("DialTarget" + i, dialTargets[i]);
            tag.putString("DisplayValue" + i, displayedValues[i] == null ? "" : displayedValues[i]);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        for (int i = 0; i < 4; i++) {
            dialTargets[i] = tag.getFloat("DialTarget" + i);
            displayedValues[i] = tag.getString("DisplayValue" + i);
            if (clientPacket) {
                dialStates[i] = dialTargets[i];
                prevDialStates[i] = dialTargets[i];
            }
        }
    }

}
