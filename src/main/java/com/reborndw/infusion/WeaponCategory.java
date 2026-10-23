package com.reborndw.infusion;

import com.reborndw.RebornDW;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.Nullable;

/**
 * The nine weapon categories. The category decides the shape of an infused weapon's moves; the flame decides what
 * they do. Categories are item tags (reborn_dying_will:infusion/<category>) so packs can sort other mods' weapons;
 * untagged items fall back on their item class (swords, axes, tridents, bows, crossbows, shields, maces).
 */
public enum WeaponCategory {
    BLADE, HEAVY, POLEARM, SHORT, WHIP, BOW, GUN, SHIELD, FISTS;

    public final TagKey<Item> tag = TagKey.create(Registries.ITEM, RebornDW.id("infusion/" + name().toLowerCase()));

    public String id() { return name().toLowerCase(); }

    public boolean ranged() { return this == BOW || this == GUN; }

    public boolean melee() { return !ranged() && this != SHIELD; }

    /** The category of a held item; an empty hand counts as fists. Null if the item can't carry an infusion. */
    @Nullable
    public static WeaponCategory of(ItemStack stack) {
        if (stack.isEmpty()) return FISTS;
        for (WeaponCategory c : values()) if (c != FISTS && stack.is(c.tag)) return c;
        if (stack.is(FISTS.tag)) return FISTS; // gloves (later) and pack-tagged knuckles
        Item item = stack.getItem();
        if (item instanceof SwordItem) return BLADE;
        if (item instanceof AxeItem || item instanceof MaceItem) return HEAVY;
        if (item instanceof TridentItem) return POLEARM;
        if (item instanceof BowItem || item instanceof CrossbowItem) return BOW;
        if (item instanceof ShieldItem) return SHIELD;
        return null;
    }

    /** Armor can be infused too, though only Lightning gives worn armor an effect so far. */
    public static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }
}
