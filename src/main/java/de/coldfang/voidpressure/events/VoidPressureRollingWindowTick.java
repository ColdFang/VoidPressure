package de.coldfang.voidpressure.events;

import de.coldfang.voidpressure.VoidPressure;
import de.coldfang.voidpressure.data.VoidPressureSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = VoidPressure.MODID)
@SuppressWarnings("unused")
public final class VoidPressureRollingWindowTick {

    private VoidPressureRollingWindowTick() {
    }

    // Updates once per second (20 server ticks)
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();

        VoidPressureSavedData.get(level).tickRollingWindow(level);
    }
}
