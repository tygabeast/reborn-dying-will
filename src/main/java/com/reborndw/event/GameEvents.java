package com.reborndw.event;

import com.reborndw.RebornDW;
import com.reborndw.ability.SkyAbilities;
import com.reborndw.command.RebornCommands;
import com.reborndw.config.ServerConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.network.FlameSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RebornDW.MODID)
public final class GameEvents {
    private GameEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        FlameLogic.tick(player);
        FlameData d = FlameLogic.get(player);
        if (d.active == Flame.SKY) SkyAbilities.tick(player, d);
        else if (d.shieldActive) { d.shieldActive = false; d.markDirty(); }
    }

    /** Near-death awakening: lethal damage to an unawakened player awakens their natural flame instead. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        FlameData d = FlameLogic.get(player);
        if (!d.awakened && ServerConfig.NEAR_DEATH_AWAKENING.get()
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
            player.setHealth(1.0F);
            player.invulnerableTime = 40;
            FlameLogic.awakenNatural(player);
            FlameLogic.enterDyingWill(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeathFinal(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        FlameLogic.onDeath(player);
    }

    /** Milk (or any other cure) can't wash off the weakness a Dying Will form leaves behind. */
    @SubscribeEvent
    public static void onEffectRemove(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove event) {
        if (event.getCure() == null || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getEffect().value() == net.minecraft.world.effect.MobEffects.WEAKNESS.value() && FlameLogic.get(player).ownWeakness)
            event.setCanceled(true);
    }

    /** Sky Shield absorbs damage before it reaches the player. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        FlameData d = FlameLogic.get(player);
        if (!d.shieldActive || d.shieldHp <= 0 || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float amount = event.getAmount();
        double absorbed = Math.min(amount, d.shieldHp);
        d.shieldHp -= absorbed;
        event.setAmount((float) (amount - absorbed));
        if (d.shieldHp <= 0) { d.shieldActive = false; d.shieldHp = 0; }
        d.markDirty();
    }

    /** Dealing or taking damage keeps Hyper mastery ticking. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        int window = ServerConfig.ACTIVITY_WINDOW.get() * 20;
        if (event.getEntity() instanceof ServerPlayer victim) FlameLogic.get(victim).activityTicks = window;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) FlameLogic.get(attacker).activityTicks = window;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) FlameLogic.syncNow(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) FlameLogic.syncNow(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) FlameLogic.syncNow(p);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player target && event.getEntity() instanceof ServerPlayer watcher
                && !watcher.isFakePlayer() && watcher.connection != null && watcher.connection.hasChannel(FlameSyncPayload.TYPE))
            PacketDistributor.sendToPlayer(watcher, FlameSyncPayload.of(target, FlameLogic.get(target)));
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        RebornCommands.register(event.getDispatcher());
    }
}
