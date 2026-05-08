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

        // Progress bar: "████░░░" — count filled(█) vs empty(░) blocks
        long filled = text.chars().filter(c -> c == '█').count();
        long empty  = text.chars().filter(c -> c == '▒').count();
        if (filled + empty > 0) return (float) filled / (filled + empty);

        // Percentage: take LAST number before '%' to correctly handle labels like "Room 1 75%"
        if (text.contains("%")) {
            int pctIdx = text.indexOf('%');
            Matcher m = NUMBER_PATTERN.matcher(text.substring(0, pctIdx));
            float last = -1f;
            while (m.find()) {
                try { last = Float.parseFloat(m.group()); } catch (NumberFormatException ignored) {}
            }
            if (last >= 0f) return Math.max(0f, last / 100f);
        }

        // RPM (KineticSpeed): "750.5 RPM" → scale by 256
        if (text.toLowerCase().contains("rpm")) {
            Matcher m = NUMBER_PATTERN.matcher(text);
            if (m.find()) {
                try { return Math.max(0f, Math.abs(Float.parseFloat(m.group())) / 256f); }
                catch (NumberFormatException ignored) {}
            }
        }

        // Generic number: extract first numeric value, then inspect suffix to pick denominator
        Matcher m = NUMBER_PATTERN.matcher(text);
        if (m.find()) {
            try {
                float val = Math.abs(Float.parseFloat(m.group()));
                // Has a non-numeric unit suffix (e.g. "0.75 SU") → treat as /64 unknown unit
                String suffix = text.substring(m.end()).trim();
                if (!suffix.isEmpty()) return Math.max(0f, val / 64f);
                // Plain integer-like and in [0,15]: likely RedstonePower (0-15) → /16
                if (val <= 15f) return Math.max(0f, val / 16f);
                // Larger plain count (AccumulatedItemCount, etc.) → /64; allows multi-rotation
                return Math.max(0f, val / 64f);
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
        CreateLang.builder().add(Component.translatable("block.createinstruments.dashboard.goggle_header")).forGoggles(tooltip);
        boolean hasAny = false;
        for (int i = 0; i < 4; i++) {
            String val = displayedValues[i];
            if (val != null && !val.isBlank()) {
                CreateLang.builder()
                    .add(Component.translatable("block.createinstruments.dashboard.slot", i + 1))
                    .space()
                    .add(Component.literal(val))
                    .forGoggles(tooltip, 1);
                hasAny = true;
            }
        }
        if (!hasAny) {
            CreateLang.builder().add(Component.translatable("block.createinstruments.dashboard.no_data")).forGoggles(tooltip, 1);
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
