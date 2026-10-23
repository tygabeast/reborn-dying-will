package com.reborndw.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.reborndw.config.ServerConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Form;
import com.reborndw.flame.Rank;
import com.reborndw.item.FlameGemItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.BiConsumer;

/** Operator-only testing commands: /reborn ... */
public final class RebornCommands {
    private static final SimpleCommandExceptionType BAD_FLAME = new SimpleCommandExceptionType(Component.literal("Unknown flame"));
    private static final SimpleCommandExceptionType BAD_RANK = new SimpleCommandExceptionType(Component.literal("Unknown rank (E, D, C, B, A or S)"));

    private RebornCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        var root = Commands.literal("reborn").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("info").then(player().executes(RebornCommands::info)));
        root.then(Commands.literal("unlock").then(player().then(flame().executes(c -> edit(c, FlameLogic::unlock)))));
        root.then(Commands.literal("lock").then(player().then(flame().executes(c -> edit(c, (p, f) -> {
            FlameData data = FlameLogic.get(p);
            data.unlocked.remove(f);
            if (data.active == f) { data.active = data.unlocked.stream().findFirst().orElse(null); data.pool = 0; data.form = Form.NONE; }
            data.awakened = !data.unlocked.isEmpty();
        })))));
        root.then(Commands.literal("active").then(player().then(flame().executes(c -> edit(c, (p, f) -> {
            FlameLogic.unlock(p, f);
            FlameData data = FlameLogic.get(p);
            data.active = f; data.form = Form.NONE; data.pool = data.usableCapacity(f);
        })))));
        root.then(Commands.literal("capacity")
                .then(Commands.literal("set").then(player().then(flame().then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(c -> edit(c, (p, f) -> {
                            FlameLogic.unlock(p, f);
                            FlameLogic.get(p).capacity[f.ordinal()] = DoubleArgumentType.getDouble(c, "amount");
                        }))))))
                .then(Commands.literal("add").then(player().then(flame().then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                        .executes(c -> edit(c, (p, f) -> {
                            FlameLogic.unlock(p, f);
                            FlameData data = FlameLogic.get(p);
                            data.capacity[f.ordinal()] = Math.max(ServerConfig.rankThreshold(0),
                                    data.capacity[f.ordinal()] + DoubleArgumentType.getDouble(c, "amount"));
                        })))))));
        root.then(Commands.literal("rank").then(player().then(flame().then(rankArg().executes(c -> {
            Rank r = rank(c);
            return edit(c, (p, f) -> { FlameLogic.unlock(p, f); FlameLogic.get(p).capacity[f.ordinal()] = ServerConfig.rankThreshold(r.ordinal()); });
        })))));
        root.then(Commands.literal("pool").then(Commands.literal("fill").then(player().executes(c -> editPlayer(c, p -> {
            FlameData data = FlameLogic.get(p);
            data.pool = FlameLogic.activeCapacity(data);
        })))));
        root.then(Commands.literal("mastery")
                .then(Commands.literal("set").then(player().then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0))
                        .executes(c -> editPlayer(c, p -> {
                            FlameData data = FlameLogic.get(p);
                            data.hyperUnlocked = true;
                            data.masterySeconds = Math.min(FlameLogic.masteryTotal(), DoubleArgumentType.getDouble(c, "seconds"));
                        })))))
                .then(Commands.literal("max").then(player().executes(c -> editPlayer(c, p -> {
                    FlameData data = FlameLogic.get(p);
                    data.hyperUnlocked = true;
                    data.masterySeconds = FlameLogic.masteryTotal();
                }))))
                .then(Commands.literal("ultimate").then(player().executes(c -> editPlayer(c, p -> {
                    FlameData data = FlameLogic.get(p);
                    data.hyperUnlocked = true;
                    data.masterySeconds = FlameLogic.masteryTotal();
                    data.ultimateTierSeconds = FlameLogic.masteryTotal();
                })))));
        root.then(Commands.literal("entries").then(Commands.literal("set").then(player()
                .then(Commands.argument("count", IntegerArgumentType.integer(0, 10000))
                        .executes(c -> editPlayer(c, p -> setEntries(FlameLogic.get(p), IntegerArgumentType.getInteger(c, "count"))))))));
        root.then(Commands.literal("scar").then(Commands.literal("clear").then(player()
                .executes(c -> editPlayer(c, p -> Arrays.fill(FlameLogic.get(p).scar, 0))))));
        root.then(Commands.literal("weakness")
                .then(Commands.literal("clear").then(player().executes(c -> editPlayer(c, p -> FlameLogic.get(p).weaknessTicks = 0))))
                .then(Commands.literal("set").then(player().then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                        .executes(c -> editPlayer(c, p -> FlameLogic.get(p).weaknessTicks = IntegerArgumentType.getInteger(c, "seconds") * 20))))));
        root.then(Commands.literal("cooldowns").then(Commands.literal("clear").then(player().executes(c -> editPlayer(c, p -> {
            FlameData data = FlameLogic.get(p);
            data.sharedCooldownTicks = 0;
            data.switchCooldownTicks = 0;
        })))));
        root.then(Commands.literal("givegem").then(player().then(flame().then(rankArg().executes(c -> {
            Rank r = rank(c);
            return edit(c, (p, f) -> p.getInventory().add(FlameGemItem.create(f, Rank.GEM_FV[r.ordinal()])));
        })))));
        d.register(root);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> rankArg() {
        return Commands.argument("rank", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Rank.values()).map(Rank::name), b));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> player() { return Commands.argument("player", EntityArgument.player()); }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> flame() {
        return Commands.argument("flame", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Flame.values()).map(Flame::id), b));
    }

    private static Flame flame(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Flame f = Flame.byId(StringArgumentType.getString(c, "flame").toLowerCase(Locale.ROOT));
        if (f == null) throw BAD_FLAME.create();
        return f;
    }

    private static Rank rank(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        try { return Rank.valueOf(StringArgumentType.getString(c, "rank").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw BAD_RANK.create(); }
    }

    private static int edit(CommandContext<CommandSourceStack> c, BiConsumer<ServerPlayer, Flame> action) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        Flame f = flame(c);
        action.accept(p, f);
        return done(c, p);
    }

    private static int editPlayer(CommandContext<CommandSourceStack> c, java.util.function.Consumer<ServerPlayer> action) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        action.accept(p);
        return done(c, p);
    }

    private static int done(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        FlameLogic.get(p).markDirty();
        FlameLogic.syncNow(p);
        c.getSource().sendSuccess(() -> Component.literal("Updated " + p.getGameProfile().getName()), true);
        return 1;
    }

    /** Recomputes the Dying Will curves as if this many entries had happened, using average gains. */
    private static void setEntries(FlameData d, int n) {
        d.dwEntries = n;
        if (n == 0) { d.dwMaxSeconds = 0; d.weaknessSeconds = 0; return; }
        double gain = (ServerConfig.DW_GAIN_MIN.get() + ServerConfig.DW_GAIN_MAX.get()) / 2.0;
        double cut = (ServerConfig.WEAKNESS_REDUCE_MIN.get() + ServerConfig.WEAKNESS_REDUCE_MAX.get()) / 2.0;
        d.dwMaxSeconds = Math.min(ServerConfig.DW_CAP.get(), ServerConfig.DW_START.get() + gain * (n - 1));
        d.weaknessSeconds = Math.max(ServerConfig.WEAKNESS_FLOOR.get(), ServerConfig.WEAKNESS_START.get() - cut * (n - 1));
        if (d.dwMaxSeconds >= ServerConfig.DW_CAP.get()) d.hyperUnlocked = true;
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        FlameData d = FlameLogic.get(p);
        StringBuilder sb = new StringBuilder();
        sb.append(p.getGameProfile().getName()).append(": awakened=").append(d.awakened)
                .append(", active=").append(d.active == null ? "none" : d.active.id())
                .append(", natural=").append(FlameLogic.naturalFlame(p).id())
                .append(String.format(", pool=%.0f/%.0f", d.pool, FlameLogic.activeCapacity(d)))
                .append(", form=").append(d.form)
                .append(String.format(", DW %d entries / %.0fs, weakness %.0fs", d.dwEntries, d.dwMaxSeconds, d.weaknessSeconds))
                .append(", hyper=").append(d.hyperUnlocked)
                .append(String.format(", mastery %.0fs (tier %d), ultimate tier %.0fs", d.masterySeconds, FlameLogic.masteryTier(d), d.ultimateTierSeconds));
        for (Flame f : d.unlocked)
            sb.append(String.format("\n  %s: %.0f FV (%s), scar %.0f", f.id(), d.capacity(f), FlameLogic.rank(d, f), d.scar[f.ordinal()]));
        c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}
