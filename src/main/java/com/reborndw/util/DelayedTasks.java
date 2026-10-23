package com.reborndw.util;

import com.reborndw.RebornDW;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/** Runs small jobs a few ticks later on the server thread (echo hits, projectile splits, effect step-downs). */
@EventBusSubscriber(modid = RebornDW.MODID)
public final class DelayedTasks {
    private record Task(Runnable job, int[] ticks) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private DelayedTasks() {}

    /** Runs the job after the given number of ticks (0 = at the end of this tick). */
    public static void schedule(int ticks, Runnable job) {
        PENDING.add(new Task(job, new int[]{Math.max(0, ticks)}));
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        TASKS.addAll(PENDING);
        PENDING.clear();
        var it = TASKS.iterator();
        List<Runnable> due = new ArrayList<>();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.ticks()[0]-- <= 0) { due.add(t.job()); it.remove(); }
        }
        for (Runnable r : due) {
            try { r.run(); } catch (Exception e) { RebornDW.LOGGER.error("Delayed task failed", e); }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        TASKS.clear();
        PENDING.clear();
    }
}
