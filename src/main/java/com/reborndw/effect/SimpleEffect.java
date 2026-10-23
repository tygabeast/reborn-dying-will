package com.reborndw.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** A status effect whose behaviour lives elsewhere (event handlers) or only in its attribute modifiers. */
public class SimpleEffect extends MobEffect {
    public SimpleEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
