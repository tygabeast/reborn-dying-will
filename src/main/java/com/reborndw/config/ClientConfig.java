package com.reborndw.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-player display settings (config/reborn_dying_will-client.toml, also editable from the Mods screen). */
public final class ClientConfig {
    public enum Anchor { HOTBAR_LEFT, HOTBAR_RIGHT, TOP_LEFT, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    public enum Orientation { HORIZONTAL, VERTICAL }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<Anchor> METER_ANCHOR;
    public static final ModConfigSpec.IntValue METER_X, METER_Y, METER_LENGTH, METER_THICKNESS;
    public static final ModConfigSpec.EnumValue<Orientation> METER_ORIENTATION;
    public static final ModConfigSpec.BooleanValue METER_LABELS;

    public static final ModConfigSpec.BooleanValue PANEL_SHOW;
    public static final ModConfigSpec.EnumValue<Anchor> PANEL_ANCHOR;
    public static final ModConfigSpec.IntValue PANEL_X, PANEL_Y;
    public static final ModConfigSpec.DoubleValue PANEL_SCALE;
    public static final ModConfigSpec.BooleanValue PANEL_BACKGROUND;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        String t = "reborn_dying_will.configuration.";

        b.translation(t + "flameMeter").push("flameMeter");
        METER_ANCHOR = b.comment("Where the flame meter sits on screen.").translation(t + "anchor").defineEnum("anchor", Anchor.HOTBAR_LEFT);
        METER_X = b.comment("Horizontal offset from the anchor, in GUI pixels.").translation(t + "offsetX").defineInRange("offsetX", 0, -2000, 2000);
        METER_Y = b.comment("Vertical offset from the anchor, in GUI pixels.").translation(t + "offsetY").defineInRange("offsetY", 0, -2000, 2000);
        METER_ORIENTATION = b.comment("A horizontal bar, or a vertical one that fills upward.").translation(t + "orientation")
                .defineEnum("orientation", Orientation.HORIZONTAL);
        METER_LENGTH = b.translation(t + "length").defineInRange("length", 82, 20, 400);
        METER_THICKNESS = b.translation(t + "thickness").defineInRange("thickness", 5, 2, 20);
        METER_LABELS = b.comment("Show the flame name, pool and form timer next to the bar.").translation(t + "labels").define("labels", true);
        b.pop();

        b.translation(t + "abilityPanel").push("abilityPanel");
        PANEL_SHOW = b.comment("Show the active flame's abilities and their keys. The Toggle ability panel key flips this.")
                .translation(t + "show").define("show", true);
        PANEL_ANCHOR = b.translation(t + "anchor").defineEnum("anchor", Anchor.MIDDLE_LEFT);
        PANEL_X = b.translation(t + "offsetX").defineInRange("offsetX", 0, -2000, 2000);
        PANEL_Y = b.translation(t + "offsetY").defineInRange("offsetY", 0, -2000, 2000);
        PANEL_SCALE = b.translation(t + "scale").defineInRange("scale", 1.0, 0.25, 4.0);
        PANEL_BACKGROUND = b.translation(t + "background").define("background", true);
        b.pop();

        SPEC = b.build();
    }

    private ClientConfig() {}
}
