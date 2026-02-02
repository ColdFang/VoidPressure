package de.coldfang.voidpressure.events;

import de.coldfang.voidpressure.VoidPressure;
import de.coldfang.voidpressure.config.CommonConfig;
import de.coldfang.voidpressure.data.VoidPressureSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = VoidPressure.MODID)
@SuppressWarnings("unused")
public class VoidPressureDecayEvents {

    // Counts server ticks to apply decay once per 60s of game time (20 * 60 = 1200 ticks)
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        // Decay rate (per minute)
        double decayPerMin = CommonConfig.PRESSURE_DECAY_PER_MINUTE.get();
        if (decayPerMin <= 0.0) return;

        // Apply decay in coarse 60s steps
        tickCounter++;
        if (tickCounter < 20 * 60) return;
        tickCounter = 0;

        // Convert to milli-units (1 = 0.001 pressure)
        long decayMilli = Math.round(decayPerMin * 1000.0);
        if (decayMilli <= 0) return;

        // Pressure is treated as world-global and stored on the overworld
        ServerLevel level = server.overworld();
        VoidPressureSavedData data = VoidPressureSavedData.get(level);

        // Pass the level so the rolling 60s stats can track the loss correctly
        data.removePressureMilli(level, decayMilli);
    }
}
