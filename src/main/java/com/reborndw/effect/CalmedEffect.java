package com.reborndw.effect;

import com.reborndw.util.EffectTimers;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Rain's calm, at full Tranquility: positive effects (Dying Will forms included) run out twice as fast.
 * Hyper drain and toggle upkeep are doubled in FlameLogic and the kits.
 */
public class CalmedEffect extends MobEffect {
    public CalmedEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide)
            EffectTimers.shorten(entity, i -> i.getEffect().value().isBeneficial(), 1, entity.tickCount % 20 == 0);
        return true;
    }
}
