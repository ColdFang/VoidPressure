package de.coldfang.voidpressure.events;

import de.coldfang.voidpressure.VoidPressure;
import de.coldfang.voidpressure.events.pressure.EffectEvents;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = VoidPressure.MODID)
@SuppressWarnings("unused")
public class VoidPressurePlayerEffectSyncEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        //noinspection resource
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        EffectEvents.syncAllFor(level);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        //noinspection resource
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        EffectEvents.syncAllFor(level);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        //noinspection resource
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        EffectEvents.syncAllFor(level);
    }
}

