package com.reborndw.menu;

import com.reborndw.flame.Flame;
import com.reborndw.flame.Rank;
import com.reborndw.infusion.Infusion;
import com.reborndw.infusion.WeaponCategory;
import com.reborndw.registry.GemData;
import com.reborndw.registry.ModRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * The Infusion Anvil: a weapon, bow, crossbow, gun or shield (anything with a weapon category) plus a ranked,
 * attuned gem = the same item permanently infused with the gem's flame at the gem's rank. The gem is used up;
 * infusing again replaces the old infusion. Armor takes only Lightning, the one flame that gives worn armor an effect.
 */
public final class InfusionAnvilLogic {
    private InfusionAnvilLogic() {}

    public static ItemStack result(ItemStack item, ItemStack gem) {
        if (item.isEmpty() || item.getCount() != 1 || !gem.is(ModRegistry.FLAME_GEM.get())) return ItemStack.EMPTY;
        GemData g = gem.getOrDefault(ModRegistry.GEM.get(), GemData.EMPTY);
        Rank rank = g.rank();
        if (g.flame().isEmpty() || rank == null) return ItemStack.EMPTY;
        Flame flame = g.flame().get();
        boolean weapon = WeaponCategory.of(item) != null;
        boolean armor = WeaponCategory.isArmor(item);
        if (!weapon && !(armor && flame == Flame.LIGHTNING)) return ItemStack.EMPTY;
        Infusion old = item.get(ModRegistry.INFUSION.get());
        if (old != null && old.flame() == flame && old.grade() == rank) return ItemStack.EMPTY; // nothing would change
        ItemStack out = item.copy();
        out.set(ModRegistry.INFUSION.get(), new Infusion(flame, rank));
        out.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return out;
    }

    public static boolean canInfuse(ItemStack item) {
        return WeaponCategory.of(item) != null && !item.isEmpty() || WeaponCategory.isArmor(item);
    }
}
