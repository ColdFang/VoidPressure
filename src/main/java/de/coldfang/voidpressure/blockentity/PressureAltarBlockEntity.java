package de.coldfang.voidpressure.blockentity;

import de.coldfang.voidpressure.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PressureAltarBlockEntity extends BlockEntity {

    private static final String TAG_OFFERED = "Offered";
    private static final String TAG_LAST_RITUAL_GAME_TIME = "LastRitualGameTime";

    private ItemStack offered = ItemStack.EMPTY;
    private long lastRitualGameTime = -1;

    public PressureAltarBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        super(ModBlockEntities.PRESSURE_ALTAR.get(), pos, state);
    }

    public @NotNull ItemStack getOffered() {
        return offered;
    }

    public boolean hasOffered() {
        return !offered.isEmpty();
    }

    // Allows placing exactly one item
    public boolean tryPlaceOne(@NotNull ItemStack inHand) {
        if (hasOffered()) return false;
        if (inHand.isEmpty()) return false;

        ItemStack one = inHand.copy();
        one.setCount(1);
        offered = one;

        markDirtyAndSync();
        return true;
    }

    public @NotNull ItemStack removeOffered() {
        ItemStack out = offered;
        offered = ItemStack.EMPTY;

        markDirtyAndSync();
        return out;
    }

    @SuppressWarnings("unused")
    public long getLastRitualGameTime() {
        return lastRitualGameTime;
    }

    @SuppressWarnings("unused")
    public void setLastRitualGameTime(long t) {
        lastRitualGameTime = t;
        markDirtyAndSync();
    }

    private void markDirtyAndSync() {
        setChanged();

        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.getChunk(worldPosition).setUnsaved(true);

            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    // Disk persistence

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains(TAG_OFFERED)) {
            offered = ItemStack.parseOptional(registries, tag.getCompound(TAG_OFFERED));
        } else {
            offered = ItemStack.EMPTY;
        }

        lastRitualGameTime = tag.getLong(TAG_LAST_RITUAL_GAME_TIME);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (!offered.isEmpty()) {
            tag.put(TAG_OFFERED, offered.save(registries));
        }

        tag.putLong(TAG_LAST_RITUAL_GAME_TIME, lastRitualGameTime);
    }

    // Client sync

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            @NotNull Connection net,
            @NotNull ClientboundBlockEntityDataPacket pkt,
            @NotNull HolderLookup.Provider registries
    ) {
        loadAdditional(pkt.getTag(), registries);
    }
}
