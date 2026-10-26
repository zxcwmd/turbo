package dev.turbo.turboboost;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TurboConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue TARGET_FPS;
    public static final ModConfigSpec.IntValue FPS_MARGIN;
    public static final ModConfigSpec.IntValue SAMPLE_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue RECOVERY_SAMPLES;
    public static final ModConfigSpec.IntValue RECOVERY_STEP_TICKS;
    public static final ModConfigSpec.IntValue MIN_RENDER_DISTANCE;
    public static final ModConfigSpec.BooleanValue ADAPTIVE_PARTICLES;
    public static final ModConfigSpec.BooleanValue ADAPTIVE_CLOUDS;
    public static final ModConfigSpec.BooleanValue ADAPTIVE_AMBIENT_OCCLUSION;
    public static final ModConfigSpec.BooleanValue ADAPTIVE_ENTITY_DISTANCE;
    public static final ModConfigSpec.BooleanValue ADAPTIVE_BIOME_BLEND;
    public static final ModConfigSpec.BooleanValue ADAPTIVE_ENTITY_SHADOWS;
    public static final ModConfigSpec.BooleanValue LOWER_SIMULATION_DISTANCE_IN_SINGLEPLAYER;
    public static final ModConfigSpec.IntValue MIN_SINGLEPLAYER_SIMULATION_DISTANCE;
    public static final ModConfigSpec.BooleanValue ENTITY_OCCLUSION_CULLING;
    public static final ModConfigSpec.IntValue OCCLUSION_CACHE_TICKS;
    public static final ModConfigSpec.IntValue MAX_OCCLUSION_TESTS_PER_TICK;
    public static final ModConfigSpec.DoubleValue OCCLUSION_MIN_DISTANCE;
    public static final ModConfigSpec.DoubleValue OCCLUSION_MAX_DISTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("turbo");

        ENABLED = builder
                .comment("Master switch. P toggles the optimizer in-game.")
                .define("enabled", true);
        TARGET_FPS = builder
                .comment("FPS goal used by the adaptive controller. This is not an FPS cap.")
                .defineInRange("targetFps", 90, 20, 360);
        FPS_MARGIN = builder
                .comment("Hysteresis around the target to prevent constant video-setting changes.")
                .defineInRange("fpsMargin", 12, 0, 60);
        SAMPLE_INTERVAL_TICKS = builder
                .comment("Ticks between FPS samples; 40 ticks is about two seconds.")
                .defineInRange("sampleIntervalTicks", 40, 10, 200);
        RECOVERY_SAMPLES = builder
                .comment("Good samples required before restoring visual quality.")
                .defineInRange("recoverySamples", 3, 1, 20);
        RECOVERY_STEP_TICKS = builder
                .comment("Ticks between one-chunk render-distance recovery steps.")
                .defineInRange("recoveryStepTicks", 80, 20, 1200);
        MIN_RENDER_DISTANCE = builder
                .comment("Lowest view distance TurboBoost may use while the FPS is low.")
                .defineInRange("minimumRenderDistance", 4, 2, 32);

        builder.push("adaptive_video");
        ADAPTIVE_PARTICLES = builder.define("reduceParticles", true);
        ADAPTIVE_CLOUDS = builder.define("disableClouds", true);
        ADAPTIVE_AMBIENT_OCCLUSION = builder.define("disableAmbientOcclusion", true);
        ADAPTIVE_ENTITY_DISTANCE = builder.define("reduceEntityDistance", true);
        ADAPTIVE_BIOME_BLEND = builder.define("reduceBiomeBlend", true);
        ADAPTIVE_ENTITY_SHADOWS = builder.define("disableEntityShadows", true);
        builder.pop();

        builder.push("singleplayer");
        LOWER_SIMULATION_DISTANCE_IN_SINGLEPLAYER = builder
                .comment("Optional CPU/tick relief for integrated singleplayer only. This reduces how far away mobs and blocks tick, so it can affect farms and gameplay. Never sent as an automatic change on multiplayer servers.")
                .define("lowerSimulationDistanceWhenFpsIsLow", false);
        MIN_SINGLEPLAYER_SIMULATION_DISTANCE = builder
                .defineInRange("minimumSimulationDistance", 5, 5, 32);
        builder.pop();

        builder.push("occlusion_culling");
        ENTITY_OCCLUSION_CULLING = builder
                .comment("Skip rendering living entities hidden behind fully occluding blocks. Players, glowing entities, named entities, and entities on fire are never culled.")
                .define("enabled", true);
        OCCLUSION_CACHE_TICKS = builder
                .comment("How long a visibility result is reused. Lower values reduce pop-in at additional CPU cost.")
                .defineInRange("cacheTicks", 8, 2, 30);
        MAX_OCCLUSION_TESTS_PER_TICK = builder
                .comment("CPU safety budget: max fresh entity visibility tests per client tick.")
                .defineInRange("maxTestsPerTick", 12, 1, 64);
        OCCLUSION_MIN_DISTANCE = builder
                .defineInRange("minimumDistance", 5.0, 2.0, 32.0);
        OCCLUSION_MAX_DISTANCE = builder
                .defineInRange("maximumDistance", 64.0, 16.0, 128.0);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private TurboConfig() {
    }
}
