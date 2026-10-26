package dev.turbo.turboboost;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Client-only adaptive controller. It only changes normal video options. */
public final class ClientOptimizer {
    private final Minecraft minecraft = Minecraft.getInstance();

    private int ticksUntilSample;
    private int recoveryTicks;
    private int lowSamples;
    private int goodSamples;
    private int preferredRenderDistance;
    private ParticleStatus preferredParticles;
    private CloudStatus preferredCloudStatus;
    private double preferredEntityDistanceScaling;
    private int preferredBiomeBlendRadius;
    private boolean preferredAmbientOcclusion;
    private boolean preferredEntityShadows;
    private boolean initialized;

    public ClientOptimizer() {
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(this::onLogout);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (!initialized) {
            rememberUserOptions();
        }

        if (!ClientConfig.ENABLED.get()) {
            restoreUserOptions();
            initialized = false;
            return;
        }

        if (--ticksUntilSample > 0) {
            return;
        }
        ticksUntilSample = ClientConfig.SAMPLE_INTERVAL_TICKS.get();

        int fps = minecraft.getFps();
        int target = ClientConfig.TARGET_FPS.get();
        int margin = ClientConfig.FPS_MARGIN.get();

        if (fps < target - margin) {
            lowSamples++;
            goodSamples = 0;
            if (lowSamples >= 2) {
                lowerRenderDistance(fps < target * 0.65);
                enableLowCostOptions();
            }
            return;
        }

        if (fps > target + margin) {
            goodSamples++;
            lowSamples = 0;
            if (goodSamples >= ClientConfig.RECOVERY_SAMPLES.get()) {
                restoreLowCostOptions();
                recoverRenderDistance();
            }
            return;
        }

        // In the dead band, hold the current settings instead of oscillating.
        lowSamples = 0;
        goodSamples = 0;
    }

    private void rememberUserOptions() {
        preferredRenderDistance = minecraft.options.renderDistance().get();
        preferredParticles = minecraft.options.particles().get();
        preferredCloudStatus = minecraft.options.cloudStatus().get();
        preferredEntityDistanceScaling = minecraft.options.entityDistanceScaling().get();
        preferredBiomeBlendRadius = minecraft.options.biomeBlendRadius().get();
        preferredAmbientOcclusion = minecraft.options.ambientOcclusion().get();
        preferredEntityShadows = minecraft.options.entityShadows().get();
        initialized = true;
    }

    private void lowerRenderDistance(boolean severeDrop) {
        int current = minecraft.options.renderDistance().get();
        int floor = Math.min(preferredRenderDistance, ClientConfig.MIN_RENDER_DISTANCE.get());
        int step = severeDrop ? 2 : 1;
        int next = Math.max(floor, current - step);
        if (next != current) {
            minecraft.options.renderDistance().set(next);
        }
    }

    private void enableLowCostOptions() {
        if (ClientConfig.REDUCE_PARTICLES.get()) {
            minecraft.options.particles().set(ParticleStatus.MINIMAL);
        }
        if (ClientConfig.DISABLE_ENTITY_SHADOWS.get()) {
            minecraft.options.entityShadows().set(false);
        }
        if (ClientConfig.DISABLE_CLOUDS.get()) {
            minecraft.options.cloudStatus().set(CloudStatus.OFF);
        }
        if (ClientConfig.DISABLE_AMBIENT_OCCLUSION.get()) {
            minecraft.options.ambientOcclusion().set(false);
        }
        if (ClientConfig.REDUCE_ENTITY_DISTANCE.get()) {
            minecraft.options.entityDistanceScaling().set(Math.min(preferredEntityDistanceScaling, 0.5));
        }
        if (ClientConfig.REDUCE_BIOME_BLEND.get()) {
            minecraft.options.biomeBlendRadius().set(0);
        }
    }

    private void restoreLowCostOptions() {
        if (ClientConfig.REDUCE_PARTICLES.get()) {
            minecraft.options.particles().set(preferredParticles);
        }
        if (ClientConfig.DISABLE_ENTITY_SHADOWS.get()) {
            minecraft.options.entityShadows().set(preferredEntityShadows);
        }
        if (ClientConfig.DISABLE_CLOUDS.get()) {
            minecraft.options.cloudStatus().set(preferredCloudStatus);
        }
        if (ClientConfig.DISABLE_AMBIENT_OCCLUSION.get()) {
            minecraft.options.ambientOcclusion().set(preferredAmbientOcclusion);
        }
        if (ClientConfig.REDUCE_ENTITY_DISTANCE.get()) {
            minecraft.options.entityDistanceScaling().set(preferredEntityDistanceScaling);
        }
        if (ClientConfig.REDUCE_BIOME_BLEND.get()) {
            minecraft.options.biomeBlendRadius().set(preferredBiomeBlendRadius);
        }
    }

    private void recoverRenderDistance() {
        recoveryTicks += ClientConfig.SAMPLE_INTERVAL_TICKS.get();
        if (recoveryTicks < ClientConfig.RECOVERY_STEP_TICKS.get()) {
            return;
        }
        recoveryTicks = 0;
        int current = minecraft.options.renderDistance().get();
        if (current < preferredRenderDistance) {
            minecraft.options.renderDistance().set(current + 1);
        }
    }

    private void restoreUserOptions() {
        if (!initialized) {
            return;
        }
        minecraft.options.renderDistance().set(preferredRenderDistance);
        minecraft.options.particles().set(preferredParticles);
        minecraft.options.cloudStatus().set(preferredCloudStatus);
        minecraft.options.entityDistanceScaling().set(preferredEntityDistanceScaling);
        minecraft.options.biomeBlendRadius().set(preferredBiomeBlendRadius);
        minecraft.options.ambientOcclusion().set(preferredAmbientOcclusion);
        minecraft.options.entityShadows().set(preferredEntityShadows);
        lowSamples = 0;
        goodSamples = 0;
    }

    private void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        restoreUserOptions();
        initialized = false;
        ticksUntilSample = 0;
        recoveryTicks = 0;
    }
}
