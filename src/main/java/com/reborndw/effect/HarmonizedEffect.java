package com.reborndw.effect;

import com.reborndw.RebornDW;
import com.reborndw.flame.Flame;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Sky's Harmony: a share of a Sky user's boost. Each amplifier level is 1% attack damage and 0.5% speed. */
public class HarmonizedEffect extends MobEffect {
    protected HarmonizedEffect() {
        super(MobEffectCategory.BENEFICIAL, Flame.SKY.color);
    }

    public static MobEffect create() {
        return new HarmonizedEffect()
                .addAttributeModifier(Attributes.ATTACK_DAMAGE, RebornDW.id("harmonized_attack"), 0.01, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .addAttributeModifier(Attributes.MOVEMENT_SPEED, RebornDW.id("harmonized_speed"), 0.005, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
