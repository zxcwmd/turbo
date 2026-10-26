package dev.turbo.turboboost;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;

@Mod(TurboBoost.MOD_ID)
public final class TurboBoost {
    public static final String MOD_ID = "turboboost";

    public TurboBoost() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientOptimizer::new);
    }
}
