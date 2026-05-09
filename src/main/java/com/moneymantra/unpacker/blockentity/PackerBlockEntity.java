package com.moneymantra.unpacker.blockentity;

import com.moneymantra.unpacker.block.PackerBlock;
import com.moneymantra.unpacker.menu.PackerMenu;
import com.moneymantra.unpacker.registry.ModBlockEntities;
import com.moneymantra.unpacker.util.ContainerItemUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PackerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ITEM_INPUT_SLOTS = 18;
    public static final int CONTAINER_INPUT_SLOTS = 6;
    public static final int OUTPUT_SLOTS = 6;
    public static final int CONTAINER_START = ITEM_INPUT_SLOTS;
    public static final int OUTPUT_START = ITEM_INPUT_SLOTS + CONTAINER_INPUT_SLOTS;
    public static final int TOTAL_SLOTS = ITEM_INPUT_SLOTS + CONTAINER_INPUT_SLOTS + OUTPUT_SLOTS;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_WORKING = 1;
    public static final int STATUS_WAITING_FOR_ITEMS = 2;
    public static final int STATUS_WAITING_FOR_CONTAINER = 3;
    public static final int STATUS_OUTPUT_FULL = 4;
    public static final int STATUS_NO_ITEM_FITS = 5;

    private static final String ITEMS_TAG = "Items";
    private static final int PACK_INTERVAL_TICKS = 2;

    private int trackedContainerSlot = -1;
    private int trackedPackedItemCount = 0;
    private int trackedFillPercent = 0;
    private int trackedStatus = STATUS_IDLE;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if(slot == trackedContainerSlot) {
                ItemStack trackedStack = getStackInSlot(slot);
                if(trackedStack.isEmpty() || !ContainerItemUtil.isSupportedContainer(trackedStack)) {
                    resetProgressTracking();
                }
            }
            changed();
        }

        @Override
        public int getSlotLimit(int slot) {
            if(slot >= CONTAINER_START) {
                return 1;
            }
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if(slot < 0 || slot >= TOTAL_SLOTS || stack.isEmpty()) {
                return false;
            }
            if(slot < ITEM_INPUT_SLOTS) {
                return !ContainerItemUtil.isSupportedContainer(stack);
            }
            if(slot < OUTPUT_START) {
                return ContainerItemUtil.isSupportedContainer(stack);
            }
            return false;
        }
    };

    private final IItemHandler topItemInputHandler = new TopItemInputHandler();
    private final IItemHandler frontContainerInputHandler = new FrontContainerInputHandler();
    private final IItemHandler bottomOutputHandler = new BottomOutputHandler();

    public PackerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.PACKER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PackerBlockEntity blockEntity) {
        if(level.getGameTime() % PACK_INTERVAL_TICKS == 0L) {
            blockEntity.processPackingCycle();
        }
        blockEntity.updateProgressTracking();
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if(side == null) {
            return null;
        }
        if(side == Direction.UP) {
            return topItemInputHandler;
        }
        if(side == Direction.DOWN) {
            return bottomOutputHandler;
        }
        if(side == getFrontDirection()) {
            return frontContainerInputHandler;
        }
        return null;
    }

    public ItemStackHandler getInternalItems() {
        return items;
    }

    public int getTrackedContainerSlot() {
        updateProgressTracking();
        return trackedContainerSlot;
    }

    public int getTrackedPackedItemCount() {
        updateProgressTracking();
        return trackedPackedItemCount;
    }

    public int getTrackedFillPercent() {
        updateProgressTracking();
        return trackedFillPercent;
    }

    public int getTrackedStatus() {
        updateProgressTracking();
        return trackedStatus;
    }

    public List<ItemStack> drainStoredItems() {
        List<ItemStack> drops = new ArrayList<>();
        for(int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if(!stack.isEmpty()) {
                drops.add(stack.copy());
                items.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        resetProgressTracking();
        return drops;
    }

    public int getComparatorSignal() {
        int occupied = 0;
        for(int slot = 0; slot < TOTAL_SLOTS; slot++) {
            if(!items.getStackInSlot(slot).isEmpty()) {
                occupied++;
            }
        }
        return occupied == 0 ? 0 : Math.max(1, Math.round((occupied / (float)TOTAL_SLOTS) * 15.0F));
    }

    private Direction getFrontDirection() {
        if(getBlockState().hasProperty(PackerBlock.FACING)) {
            return getBlockState().getValue(PackerBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Nullable
    private ActiveContainer findActiveContainer() {
        for(int slot = CONTAINER_START; slot < OUTPUT_START; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if(stack.isEmpty() || !ContainerItemUtil.isSupportedContainer(stack)) {
                continue;
            }
            return new ActiveContainer(slot, stack);
        }
        return null;
    }

    private void processPackingCycle() {
        ActiveContainer active = findActiveContainer();
        if(active == null) {
            trackedStatus = hasPackableInputItems() ? STATUS_WAITING_FOR_CONTAINER : STATUS_IDLE;
            return;
        }

        if(ContainerItemUtil.isContainerFull(active.stack())) {
            moveContainerToOutput(active);
            return;
        }

        if(!hasPackableInputItems()) {
            trackedStatus = STATUS_WAITING_FOR_ITEMS;
            return;
        }

        for(int inputSlot = 0; inputSlot < ITEM_INPUT_SLOTS; inputSlot++) {
            ItemStack input = items.getStackInSlot(inputSlot);
            if(input.isEmpty() || ContainerItemUtil.isSupportedContainer(input)) {
                continue;
            }

            int inserted = ContainerItemUtil.insertContent(active.stack(), input, false);
            if(inserted > 0) {
                input.shrink(inserted);
                if(input.isEmpty()) {
                    items.setStackInSlot(inputSlot, ItemStack.EMPTY);
                }
                items.setStackInSlot(active.slot(), active.stack());
                trackedStatus = STATUS_WORKING;
                changed();

                if(ContainerItemUtil.isContainerFull(active.stack())) {
                    moveContainerToOutput(active);
                }
                return;
            }
        }

        trackedStatus = STATUS_NO_ITEM_FITS;
        moveContainerToOutput(active);
    }

    private boolean moveContainerToOutput(ActiveContainer active) {
        int outputSlot = findFirstEmptyOutputSlot();
        if(outputSlot < 0) {
            trackedStatus = STATUS_OUTPUT_FULL;
            return false;
        }

        items.setStackInSlot(outputSlot, active.stack().copy());
        items.setStackInSlot(active.slot(), ItemStack.EMPTY);
        if(trackedContainerSlot == active.slot()) {
            resetProgressTracking();
        }
        changed();
        return true;
    }

    private int findFirstEmptyOutputSlot() {
        for(int slot = OUTPUT_START; slot < TOTAL_SLOTS; slot++) {
            if(items.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private boolean hasPackableInputItems() {
        for(int slot = 0; slot < ITEM_INPUT_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if(!stack.isEmpty() && !ContainerItemUtil.isSupportedContainer(stack)) {
                return true;
            }
        }
        return false;
    }

    private void updateProgressTracking() {
        ActiveContainer active = findActiveContainer();
        if(active == null) {
            trackedContainerSlot = -1;
            trackedPackedItemCount = 0;
            trackedFillPercent = 0;
            trackedStatus = hasPackableInputItems() ? STATUS_WAITING_FOR_CONTAINER : STATUS_IDLE;
            return;
        }

        trackedContainerSlot = active.slot();
        trackedPackedItemCount = ContainerItemUtil.countContentItems(active.stack());
        trackedFillPercent = Math.round(ContainerItemUtil.getFillProgress(active.stack()) * 100.0F);

        if(ContainerItemUtil.isContainerFull(active.stack())) {
            trackedStatus = findFirstEmptyOutputSlot() < 0 ? STATUS_OUTPUT_FULL : STATUS_WORKING;
        } else if(!hasPackableInputItems()) {
            trackedStatus = STATUS_WAITING_FOR_ITEMS;
        } else if(findFirstEmptyOutputSlot() < 0 && !canAnyInputItemFit(active.stack())) {
            trackedStatus = STATUS_OUTPUT_FULL;
        } else if(!canAnyInputItemFit(active.stack())) {
            trackedStatus = STATUS_NO_ITEM_FITS;
        } else if(trackedStatus != STATUS_OUTPUT_FULL) {
            trackedStatus = STATUS_WORKING;
        }
    }

    private boolean canAnyInputItemFit(ItemStack container) {
        for(int slot = 0; slot < ITEM_INPUT_SLOTS; slot++) {
            ItemStack input = items.getStackInSlot(slot);
            if(!input.isEmpty() && !ContainerItemUtil.isSupportedContainer(input) && ContainerItemUtil.canInsertContent(container, input)) {
                return true;
            }
        }
        return false;
    }

    private void resetProgressTracking() {
        trackedContainerSlot = -1;
        trackedPackedItemCount = 0;
        trackedFillPercent = 0;
        trackedStatus = STATUS_IDLE;
    }

    private void changed() {
        setChanged();
        if(level != null && !level.isClientSide) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(ITEMS_TAG, items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if(tag.contains(ITEMS_TAG)) {
            items.deserializeNBT(registries, tag.getCompound(ITEMS_TAG));
        }
        resetProgressTracking();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.unpacker.packer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PackerMenu(containerId, playerInventory, this, worldPosition);
    }

    private record ActiveContainer(int slot, ItemStack stack) {
    }

    private class TopItemInputHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return ITEM_INPUT_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if(slot < 0 || slot >= ITEM_INPUT_SLOTS) {
                return ItemStack.EMPTY;
            }
            return items.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if(slot < 0 || slot >= ITEM_INPUT_SLOTS || stack.isEmpty() || ContainerItemUtil.isSupportedContainer(stack)) {
                return stack;
            }
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot >= 0 && slot < ITEM_INPUT_SLOTS && !ContainerItemUtil.isSupportedContainer(stack);
        }
    }

    private class FrontContainerInputHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return CONTAINER_INPUT_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if(slot < 0 || slot >= CONTAINER_INPUT_SLOTS) {
                return ItemStack.EMPTY;
            }
            return items.getStackInSlot(CONTAINER_START + slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if(slot < 0 || slot >= CONTAINER_INPUT_SLOTS || !ContainerItemUtil.isSupportedContainer(stack)) {
                return stack;
            }
            return items.insertItem(CONTAINER_START + slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot >= 0 && slot < CONTAINER_INPUT_SLOTS && ContainerItemUtil.isSupportedContainer(stack);
        }
    }

    private class BottomOutputHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return OUTPUT_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if(slot < 0 || slot >= OUTPUT_SLOTS) {
                return ItemStack.EMPTY;
            }
            return items.getStackInSlot(OUTPUT_START + slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if(slot < 0 || slot >= OUTPUT_SLOTS) {
                return ItemStack.EMPTY;
            }
            return items.extractItem(OUTPUT_START + slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    }
}
