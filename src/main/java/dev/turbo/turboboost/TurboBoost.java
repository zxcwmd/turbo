package dev.turbo.turboboost;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(TurboBoost.MOD_ID)
public final class TurboBoost {
    public static final String MOD_ID = "turboboost";

    public TurboBoost(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, TurboConfig.SPEC);
    }
}
