package com.reborndw.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

/**
 * The Dying Will forms as status effects. The effect's timer is the form's timer: when it runs out (or is removed
 * with /effect clear) the form ends. The boost itself comes from FlameLogic, since it scales with rank and mastery.
 */
public class FormEffect extends MobEffect {
    public FormEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    /** Milk and similar cures don't end a form. */
    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {}
}
