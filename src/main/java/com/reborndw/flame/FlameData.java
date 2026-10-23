package com.reborndw.flame;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.EnumSet;
import java.util.Set;

/** Per-player flame state. Server-authoritative; a copy is synced to clients for the HUD and visuals. */
public class FlameData implements INBTSerializable<CompoundTag> {
    public boolean awakened;
    public final Set<Flame> unlocked = EnumSet.noneOf(Flame.class);
    public Flame active;
    public final double[] capacity = new double[Flame.values().length];
    public final double[] scar = new double[Flame.values().length];
    public double pool;

    public int dwEntries;
    public double dwMaxSeconds;
    public double weaknessSeconds;
    public boolean hyperUnlocked;
    public Form form = Form.NONE;
    public int formTicks;
    public int weaknessTicks;
    public int sharedCooldownTicks;
    public int switchCooldownTicks;
    public double masterySeconds;
    public double ultimateTierSeconds;
    public int activityTicks;

    public boolean shieldActive;
    public double shieldHp;

    // transient, server only
    public transient boolean dirty = true;
    public transient int restingTicks;
    public transient double lastX, lastY, lastZ;
    public transient int syncCooldown;

    public void markDirty() { dirty = true; }

    public double capacity(Flame f) { return capacity[f.ordinal()]; }

    public double usableCapacity(Flame f) { return Math.max(0, capacity[f.ordinal()] - scar[f.ordinal()]); }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("awakened", awakened);
        ListTag list = new ListTag();
        for (Flame f : unlocked) list.add(StringTag.valueOf(f.id()));
        t.put("unlocked", list);
        if (active != null) t.putString("active", active.id());
        CompoundTag caps = new CompoundTag();
        CompoundTag scars = new CompoundTag();
        for (Flame f : Flame.values()) {
            if (capacity[f.ordinal()] > 0) caps.putDouble(f.id(), capacity[f.ordinal()]);
            if (scar[f.ordinal()] > 0) scars.putDouble(f.id(), scar[f.ordinal()]);
        }
        t.put("capacity", caps);
        t.put("scar", scars);
        t.putDouble("pool", pool);
        t.putInt("dwEntries", dwEntries);
        t.putDouble("dwMax", dwMaxSeconds);
        t.putDouble("weaknessLen", weaknessSeconds);
        t.putBoolean("hyperUnlocked", hyperUnlocked);
        t.putString("form", form.name());
        t.putInt("formTicks", formTicks);
        t.putInt("weaknessTicks", weaknessTicks);
        t.putInt("sharedCooldown", sharedCooldownTicks);
        t.putInt("switchCooldown", switchCooldownTicks);
        t.putDouble("mastery", masterySeconds);
        t.putDouble("ultimateTier", ultimateTierSeconds);
        t.putInt("activity", activityTicks);
        t.putBoolean("shield", shieldActive);
        t.putDouble("shieldHp", shieldHp);
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
        awakened = t.getBoolean("awakened");
        unlocked.clear();
        for (Tag tag : t.getList("unlocked", Tag.TAG_STRING)) {
            Flame f = Flame.byId(tag.getAsString());
            if (f != null) unlocked.add(f);
        }
        active = t.contains("active") ? Flame.byId(t.getString("active")) : null;
        CompoundTag caps = t.getCompound("capacity");
        CompoundTag scars = t.getCompound("scar");
        for (Flame f : Flame.values()) {
            capacity[f.ordinal()] = caps.getDouble(f.id());
            scar[f.ordinal()] = scars.getDouble(f.id());
        }
        pool = t.getDouble("pool");
        dwEntries = t.getInt("dwEntries");
        dwMaxSeconds = t.getDouble("dwMax");
        weaknessSeconds = t.getDouble("weaknessLen");
        hyperUnlocked = t.getBoolean("hyperUnlocked");
        try { form = Form.valueOf(t.getString("form")); } catch (IllegalArgumentException e) { form = Form.NONE; }
        formTicks = t.getInt("formTicks");
        weaknessTicks = t.getInt("weaknessTicks");
        sharedCooldownTicks = t.getInt("sharedCooldown");
        switchCooldownTicks = t.getInt("switchCooldown");
        masterySeconds = t.getDouble("mastery");
        ultimateTierSeconds = t.getDouble("ultimateTier");
        activityTicks = t.getInt("activity");
        shieldActive = t.getBoolean("shield");
        shieldHp = t.getDouble("shieldHp");
        dirty = true;
    }

    public void copyFrom(FlameData o) {
        deserializeNBT(null, o.serializeNBT(null));
    }
}
