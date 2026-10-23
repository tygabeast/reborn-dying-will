package com.reborndw.effect;

import com.reborndw.ability.FlameDamage;
import com.reborndw.config.KitConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Storm's decay from infused melee hits: small armor-ignoring damage every second. */
public class DecayEffect extends MobEffect {
    public DecayEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return true;
        var types = entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        entity.invulnerableTime = 0;
        entity.hurt(new DamageSource(types.getHolderOrThrow(FlameDamage.FLAME_PIERCE)), (float) (KitConfig.STORM_DECAY_DAMAGE.get() * (amplifier + 1)));
        return true;
    }
}
