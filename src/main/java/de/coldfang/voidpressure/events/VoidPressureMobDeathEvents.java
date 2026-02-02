package de.coldfang.voidpressure.events;

import de.coldfang.voidpressure.VoidPressure;
import de.coldfang.voidpressure.config.VoidPressureBalance;
import de.coldfang.voidpressure.data.VoidPressureSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = VoidPressure.MODID)
@SuppressWarnings("unused")
public class VoidPressureMobDeathEvents {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        // Nur "Mobs" zählen
        if (!(event.getEntity() instanceof Mob mob)) return;

        boolean killedByPlayer = event.getSource().getEntity() instanceof ServerPlayer;

        // Player kills: allow per-mob overwrite (implemented in VoidPressureBalance next)
        long gainMilli = killedByPlayer
                ? VoidPressureBalance.playerKillGainMilli(mob)
                : VoidPressureBalance.passiveKillGainMilli();

        var data = VoidPressureSavedData.get(level);
        data.addPressureMilliAndTrigger(level, gainMilli);
    }
}
