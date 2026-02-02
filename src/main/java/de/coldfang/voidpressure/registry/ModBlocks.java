package de.coldfang.voidpressure.registry;

import de.coldfang.voidpressure.VoidPressure;
import de.coldfang.voidpressure.block.PressureAltarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(VoidPressure.MODID);

    public static final DeferredBlock<Block> PRESSURE_ALTAR =
            BLOCKS.register("pressure_altar",
                    () -> new PressureAltarBlock(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion()));
}
