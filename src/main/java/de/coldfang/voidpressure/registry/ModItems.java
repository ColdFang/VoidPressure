package de.coldfang.voidpressure.registry;

import de.coldfang.voidpressure.VoidPressure;
import de.coldfang.voidpressure.item.PressureAltarItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(VoidPressure.MODID);

    public static final DeferredItem<Item> PRESSURE_ALTAR =
            ITEMS.register("pressure_altar",
                    () -> new PressureAltarItem(
                            ModBlocks.PRESSURE_ALTAR.get(),
                            new Item.Properties()
                    ));
}
