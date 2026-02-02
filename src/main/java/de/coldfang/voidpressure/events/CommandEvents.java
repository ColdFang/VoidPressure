package de.coldfang.voidpressure.events;

import de.coldfang.voidpressure.VoidPressure;
import de.coldfang.voidpressure.command.VoidPressureCommands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = VoidPressure.MODID)
@SuppressWarnings("unused")
public class CommandEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        VoidPressureCommands.register(event.getDispatcher());
    }
}

