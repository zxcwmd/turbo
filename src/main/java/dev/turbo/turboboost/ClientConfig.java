package dev.turbo.turboboost;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue TARGET_FPS;
    public static final ForgeConfigSpec.IntValue FPS_MARGIN;
    public static final ForgeConfigSpec.IntValue MIN_RENDER_DISTANCE;
    public static final ForgeConfigSpec.IntValue SAMPLE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue RECOVERY_SAMPLES;
    public static final ForgeConfigSpec.IntValue RECOVERY_STEP_TICKS;
    public static final ForgeConfigSpec.BooleanValue REDUCE_PARTICLES;
    public static final ForgeConfigSpec.BooleanValue DISABLE_ENTITY_SHADOWS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_CLOUDS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_AMBIENT_OCCLUSION;
    public static final ForgeConfigSpec.BooleanValue REDUCE_ENTITY_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue REDUCE_BIOME_BLEND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("adaptive_performance");

        ENABLED = builder
                .comment("Enable adaptive client-side performance tuning.")
                .define("enabled", true);
        TARGET_FPS = builder
                .comment("FPS goal used by the adaptive controller. It does not impose an FPS cap.")
                .defineInRange("targetFps", 60, 20, 360);
        FPS_MARGIN = builder
                .comment("Hysteresis around the FPS goal to avoid rapidly changing settings.")
                .defineInRange("fpsMargin", 10, 0, 60);
        MIN_RENDER_DISTANCE = builder
                .comment("Lowest render distance the mod may temporarily use.")
                .defineInRange("minimumRenderDistance", 6, 2, 32);
        SAMPLE_INTERVAL_TICKS = builder
                .comment("How often the FPS controller samples performance (20 ticks = about 1 second).")
                .defineInRange("sampleIntervalTicks", 40, 10, 200);
        RECOVERY_SAMPLES = builder
                .comment("Number of good FPS samples required before settings begin to recover.")
                .defineInRange("recoverySamples", 3, 1, 20);
        RECOVERY_STEP_TICKS = builder
                .comment("Ticks between one-block render-distance recovery steps.")
                .defineInRange("recoveryStepTicks", 100, 20, 1200);
        REDUCE_PARTICLES = builder
                .comment("Temporarily use minimal particles when FPS remains low.")
                .define("reduceParticles", true);
        DISABLE_ENTITY_SHADOWS = builder
                .comment("Temporarily disable entity shadows when FPS remains low.")
                .define("disableEntityShadows", true);
        DISABLE_CLOUDS = builder
                .comment("Temporarily hide clouds when FPS remains low.")
                .define("disableClouds", true);
        DISABLE_AMBIENT_OCCLUSION = builder
                .comment("Temporarily disable ambient occlusion when FPS remains low.")
                .define("disableAmbientOcclusion", true);
        REDUCE_ENTITY_DISTANCE = builder
                .comment("Temporarily reduce the entity render-distance multiplier when FPS remains low.")
                .define("reduceEntityDistance", true);
        REDUCE_BIOME_BLEND = builder
                .comment("Temporarily reduce biome color blending when FPS remains low.")
                .define("reduceBiomeBlend", true);

        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
