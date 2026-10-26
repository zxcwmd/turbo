package dev.turbo.turboboost;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Client bootstrap and main-thread performance controller for Minecraft 1.21.1. */
@Mod(value = TurboBoost.MOD_ID, dist = Dist.CLIENT)
public final class TurboBoostClient {
    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.turboboost.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.turboboost"
    );

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Map<Integer, VisibilityResult> visibilityCache = new HashMap<>();

    private long clientTicks;
    private int visibilityTestsThisTick;
    private int ticksUntilSample;
    private int recoveryTicks;
    private int lowSamples;
    private int goodSamples;
    private int preferredRenderDistance;
    private int preferredSimulationDistance;
    private ParticleStatus preferredParticles;
    private CloudStatus preferredClouds;
    private double preferredEntityDistance;
    private int preferredBiomeBlend;
    private boolean preferredAmbientOcclusion;
    private boolean preferredEntityShadows;
    private boolean initialized;
    private boolean wasInWorld;

    public TurboBoostClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::registerKey);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLivingRender);
    }

    private void registerKey(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_KEY);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        clientTicks++;
        visibilityTestsThisTick = 0;

        while (TOGGLE_KEY.consumeClick()) {
            boolean enabled = !TurboConfig.ENABLED.get();
            TurboConfig.ENABLED.set(enabled);
            if (!enabled) {
                restoreUserOptions();
                initialized = false;
            }
            minecraft.gui.getChat().addMessage(Component.literal(
                    enabled ? "TurboBoost: turbo profile enabled" : "TurboBoost: restored your video settings"
            ));
        }

        if (minecraft.player == null || minecraft.level == null) {
            if (wasInWorld) {
                restoreUserOptions();
                initialized = false;
                visibilityCache.clear();
            }
            wasInWorld = false;
            return;
        }
        wasInWorld = true;

        if (!initialized) {
            rememberUserOptions();
        }

        if (!TurboConfig.ENABLED.get()) {
            restoreUserOptions();
            initialized = false;
            return;
        }

        if (clientTicks % 100 == 0) {
            removeExpiredVisibilityResults();
        }

        if (--ticksUntilSample > 0) {
            return;
        }
        ticksUntilSample = TurboConfig.SAMPLE_INTERVAL_TICKS.get();
        sampleFrameRate();
    }

    private void sampleFrameRate() {
        int fps = minecraft.getFps();
        int target = TurboConfig.TARGET_FPS.get();
        int margin = TurboConfig.FPS_MARGIN.get();

        if (fps < target - margin) {
            lowSamples++;
            goodSamples = 0;
            if (lowSamples >= 2) {
                reduceVideoWork(fps < target * 0.65);
            }
            return;
        }

        if (fps > target + margin) {
            goodSamples++;
            lowSamples = 0;
            if (goodSamples >= TurboConfig.RECOVERY_SAMPLES.get()) {
                restoreAdaptiveVideoOptions();
                recoverRenderDistance();
            }
            return;
        }

        lowSamples = 0;
        goodSamples = 0;
    }

    private void rememberUserOptions() {
        preferredRenderDistance = minecraft.options.renderDistance().get();
        preferredSimulationDistance = minecraft.options.simulationDistance().get();
        preferredParticles = minecraft.options.particles().get();
        preferredClouds = minecraft.options.cloudStatus().get();
        preferredEntityDistance = minecraft.options.entityDistanceScaling().get();
        preferredBiomeBlend = minecraft.options.biomeBlendRadius().get();
        preferredAmbientOcclusion = minecraft.options.ambientOcclusion().get();
        preferredEntityShadows = minecraft.options.entityShadows().get();
        initialized = true;
    }

    private void reduceVideoWork(boolean severeDrop) {
        int renderDistance = minecraft.options.renderDistance().get();
        int distanceFloor = Math.min(preferredRenderDistance, TurboConfig.MIN_RENDER_DISTANCE.get());
        int nextDistance = Math.max(distanceFloor, renderDistance - (severeDrop ? 2 : 1));
        if (nextDistance != renderDistance) {
            minecraft.options.renderDistance().set(nextDistance);
        }

        if (TurboConfig.ADAPTIVE_PARTICLES.get()) {
            minecraft.options.particles().set(ParticleStatus.MINIMAL);
        }
        if (TurboConfig.ADAPTIVE_CLOUDS.get()) {
            minecraft.options.cloudStatus().set(CloudStatus.OFF);
        }
        if (TurboConfig.ADAPTIVE_AMBIENT_OCCLUSION.get()) {
            minecraft.options.ambientOcclusion().set(false);
        }
        if (TurboConfig.ADAPTIVE_ENTITY_DISTANCE.get()) {
            minecraft.options.entityDistanceScaling().set(Math.min(preferredEntityDistance, 0.5));
        }
        if (TurboConfig.ADAPTIVE_BIOME_BLEND.get()) {
            minecraft.options.biomeBlendRadius().set(0);
        }
        if (TurboConfig.ADAPTIVE_ENTITY_SHADOWS.get()) {
            minecraft.options.entityShadows().set(false);
        }

        if (TurboConfig.LOWER_SIMULATION_DISTANCE_IN_SINGLEPLAYER.get() && minecraft.hasSingleplayerServer()) {
            int current = minecraft.options.simulationDistance().get();
            int floor = Math.min(preferredSimulationDistance, TurboConfig.MIN_SINGLEPLAYER_SIMULATION_DISTANCE.get());
            minecraft.options.simulationDistance().set(Math.max(floor, current - (severeDrop ? 2 : 1)));
        }
    }

    private void restoreAdaptiveVideoOptions() {
        if (TurboConfig.ADAPTIVE_PARTICLES.get()) {
            minecraft.options.particles().set(preferredParticles);
        }
        if (TurboConfig.ADAPTIVE_CLOUDS.get()) {
            minecraft.options.cloudStatus().set(preferredClouds);
        }
        if (TurboConfig.ADAPTIVE_AMBIENT_OCCLUSION.get()) {
            minecraft.options.ambientOcclusion().set(preferredAmbientOcclusion);
        }
        if (TurboConfig.ADAPTIVE_ENTITY_DISTANCE.get()) {
            minecraft.options.entityDistanceScaling().set(preferredEntityDistance);
        }
        if (TurboConfig.ADAPTIVE_BIOME_BLEND.get()) {
            minecraft.options.biomeBlendRadius().set(preferredBiomeBlend);
        }
        if (TurboConfig.ADAPTIVE_ENTITY_SHADOWS.get()) {
            minecraft.options.entityShadows().set(preferredEntityShadows);
        }
        if (TurboConfig.LOWER_SIMULATION_DISTANCE_IN_SINGLEPLAYER.get() && minecraft.hasSingleplayerServer()) {
            minecraft.options.simulationDistance().set(preferredSimulationDistance);
        }
    }

    private void recoverRenderDistance() {
        recoveryTicks += TurboConfig.SAMPLE_INTERVAL_TICKS.get();
        if (recoveryTicks < TurboConfig.RECOVERY_STEP_TICKS.get()) {
            return;
        }
        recoveryTicks = 0;

        int current = minecraft.options.renderDistance().get();
        if (current < preferredRenderDistance) {
            minecraft.options.renderDistance().set(current + 1);
        }
        if (TurboConfig.LOWER_SIMULATION_DISTANCE_IN_SINGLEPLAYER.get() && minecraft.hasSingleplayerServer()) {
            int simulationDistance = minecraft.options.simulationDistance().get();
            if (simulationDistance < preferredSimulationDistance) {
                minecraft.options.simulationDistance().set(simulationDistance + 1);
            }
        }
    }

    private void restoreUserOptions() {
        if (!initialized) {
            return;
        }
        minecraft.options.renderDistance().set(preferredRenderDistance);
        minecraft.options.simulationDistance().set(preferredSimulationDistance);
        minecraft.options.particles().set(preferredParticles);
        minecraft.options.cloudStatus().set(preferredClouds);
        minecraft.options.entityDistanceScaling().set(preferredEntityDistance);
        minecraft.options.biomeBlendRadius().set(preferredBiomeBlend);
        minecraft.options.ambientOcclusion().set(preferredAmbientOcclusion);
        minecraft.options.entityShadows().set(preferredEntityShadows);
        lowSamples = 0;
        goodSamples = 0;
        recoveryTicks = 0;
    }

    private void onLivingRender(RenderLivingEvent.Pre event) {
        if (!TurboConfig.ENABLED.get() || !TurboConfig.ENTITY_OCCLUSION_CULLING.get()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (shouldKeepEntityVisible(entity)) {
            return;
        }

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double distanceSquared = camera.distanceToSqr(entity.position());
        double minDistance = TurboConfig.OCCLUSION_MIN_DISTANCE.get();
        double maxDistance = TurboConfig.OCCLUSION_MAX_DISTANCE.get();
        if (distanceSquared < minDistance * minDistance || distanceSquared > maxDistance * maxDistance) {
            return;
        }

        VisibilityResult cached = visibilityCache.get(entity.getId());
        if (cached != null
                && cached.expiresAt >= clientTicks
                && cached.entityPosition.distanceToSqr(entity.position()) < 0.25
                && cached.cameraPosition.distanceToSqr(camera) < 1.0) {
            if (cached.occluded) {
                event.setCanceled(true);
            }
            return;
        }

        if (visibilityTestsThisTick >= TurboConfig.MAX_OCCLUSION_TESTS_PER_TICK.get()) {
            return;
        }
        visibilityTestsThisTick++;

        boolean occluded = isFullyOccluded(entity, camera);
        visibilityCache.put(entity.getId(), new VisibilityResult(
                occluded,
                clientTicks + TurboConfig.OCCLUSION_CACHE_TICKS.get(),
                entity.position(),
                camera
        ));
        if (occluded) {
            event.setCanceled(true);
        }
    }

    private boolean shouldKeepEntityVisible(LivingEntity entity) {
        return minecraft.level == null
                || minecraft.player == null
                || entity == minecraft.player
                || entity == minecraft.getCameraEntity()
                || entity instanceof Player
                || entity.isCurrentlyGlowing()
                || entity.isOnFire()
                || entity.hasCustomName()
                || entity.isInvisible();
    }

    private boolean isFullyOccluded(LivingEntity entity, Vec3 camera) {
        AABB box = entity.getBoundingBox();
        Vec3 center = box.getCenter();
        double halfWidth = Math.max(0.08, Math.min(entity.getBbWidth() * 0.28, 0.35));
        Vec3[] samples = {
                new Vec3(center.x, box.minY + (box.maxY - box.minY) * 0.30, center.z),
                new Vec3(center.x, box.minY + (box.maxY - box.minY) * 0.72, center.z),
                new Vec3(center.x - halfWidth, center.y, center.z),
                new Vec3(center.x + halfWidth, center.y, center.z)
        };

        for (Vec3 sample : samples) {
            BlockHitResult hit = minecraft.level.clip(new ClipContext(
                    camera,
                    sample,
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    entity
            ));
            if (hit.getType() == BlockHitResult.Type.MISS
                    || !minecraft.level.getBlockState(hit.getBlockPos()).canOcclude()) {
                return false;
            }
        }
        return true;
    }

    private void removeExpiredVisibilityResults() {
        Iterator<Map.Entry<Integer, VisibilityResult>> iterator = visibilityCache.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt < clientTicks) {
                iterator.remove();
            }
        }
    }

    private record VisibilityResult(boolean occluded, long expiresAt, Vec3 entityPosition, Vec3 cameraPosition) {
    }
}
