package com.reborndw.client;

import com.reborndw.flame.FlameData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/** Client copies of flame state, keyed by entity id. */
public final class ClientFlameCache {
    private static final Int2ObjectOpenHashMap<FlameData> DATA = new Int2ObjectOpenHashMap<>();

    private ClientFlameCache() {}

    public static FlameData get(int entityId) { return DATA.get(entityId); }

    public static void put(int entityId, FlameData d) { DATA.put(entityId, d); }

    public static void clear() { DATA.clear(); }
}
