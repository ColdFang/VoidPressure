package de.coldfang.voidpressure.registry;

import de.coldfang.voidpressure.VoidPressure;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VoidPressure.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VOIDPRESSURE_TAB =
            TABS.register("voidpressure", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.voidpressure"))
                    .icon(() -> new ItemStack(ModItems.PRESSURE_ALTAR.get()))
                    .displayItems((params, output) ->
                            output.accept(ModItems.PRESSURE_ALTAR.get())
                    )
                    .build());
}
