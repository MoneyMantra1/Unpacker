package com.moneymantra.unpacker.blockentity;

import com.moneymantra.unpacker.block.UnpackerBlock;
import com.moneymantra.unpacker.menu.UnpackerMenu;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UnpackerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 18;
    public static final int OUTPUT_SLOTS = 18;
    public static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;
    public static final int OUTPUT_START = INPUT_SLOTS;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_WORKING = 1;
    public static final int STATUS_WAITING_FOR_HOPPER = 2;
    public static final int STATUS_OUTPUT_FULL = 3;
    public static final int STATUS_FRONT_OUTPUT_BLOCKED = 4;

    private static final String ITEMS_TAG = "Items";
    private static final int FRONT_OUTPUT_INTERVAL_TICKS = 8;
    private static final int WAITING_FOR_HOPPER_TICKS = 40;

    private int trackedInputSlot = -1;
    private int trackedStartingItemCount = 0;
    private int trackedRemainingItemCount = 0;
    private int trackedStatus = STATUS_IDLE;
    private long lastContentExtractionGameTime = 0L;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if(slot == trackedInputSlot) {
                ItemStack trackedStack = getStackInSlot(slot);
                if(trackedStack.isEmpty() || !ContainerItemUtil.isSupportedContainer(trackedStack)) {
                    resetProgressTracking();
                }
            }
            changed();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < INPUT_SLOTS && ContainerItemUtil.isSupportedContainer(stack);
        }
    };

    private final IItemHandler topInputHandler = new TopInputHandler();
    private final IItemHandler frontOutputHandler = new FrontOutputHandler();
    private final IItemHandler bottomContentHandler = new BottomContentHandler();

    public UnpackerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.UNPACKER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, UnpackerBlockEntity blockEntity) {
        blockEntity.moveCompletedInputsToOutput();

        if(level.getGameTime() % FRONT_OUTPUT_INTERVAL_TICKS == 0L) {
            blockEntity.pushOutputToFront();
        }

        blockEntity.updateProgressTracking();
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if(side == null) {
            return null;
        }
        if(side == Direction.UP) {
            return topInputHandler;
        }
        if(side == Direction.DOWN) {
            return bottomContentHandler;
        }
        if(side == getFrontDirection()) {
            return frontOutputHandler;
        }
        return null;
    }

    public ItemStackHandler getInternalItems() {
        return items;
    }

    public int getTrackedInputSlot() {
        updateProgressTracking();
        return trackedInputSlot;
    }

    public int getTrackedStartingItemCount() {
        updateProgressTracking();
        return trackedStartingItemCount;
    }

    public int getTrackedRemainingItemCount() {
        updateProgressTracking();
        return trackedRemainingItemCount;
    }

    public int getTrackedStatus() {
        updateProgressTracking();
        return trackedStatus;
    }

    public List<ItemStack> drainStoredContainerItems() {
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
        if(getBlockState().hasProperty(UnpackerBlock.FACING)) {
            return getBlockState().getValue(UnpackerBlock.FACING);
        }
        return Direction.NORTH;
    }

    private void moveCompletedInputsToOutput() {
        for(int inputSlot = 0; inputSlot < INPUT_SLOTS; inputSlot++) {
            ItemStack stack = items.getStackInSlot(inputSlot);
            if(stack.isEmpty()) {
                continue;
            }
            if(!ContainerItemUtil.isSupportedContainer(stack)) {
                continue;
            }
            if(!ContainerItemUtil.isContainerEmpty(stack)) {
                continue;
            }

            int outputSlot = findFirstEmptyOutputSlot();
            if(outputSlot < 0) {
                return;
            }

            items.setStackInSlot(outputSlot, stack.copy());
            items.setStackInSlot(inputSlot, ItemStack.EMPTY);
            if(trackedInputSlot == inputSlot) {
                resetProgressTracking();
            }
        }
    }

    private int findFirstEmptyOutputSlot() {
        for(int slot = OUTPUT_START; slot < TOTAL_SLOTS; slot++) {
            if(items.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private boolean hasOutputItems() {
        for(int slot = OUTPUT_START; slot < TOTAL_SLOTS; slot++) {
            if(!items.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private ActiveInput findActiveInput() {
        for(int inputSlot = 0; inputSlot < INPUT_SLOTS; inputSlot++) {
            ItemStack stack = items.getStackInSlot(inputSlot);
            if(stack.isEmpty() || !ContainerItemUtil.isSupportedContainer(stack)) {
                continue;
            }
            return new ActiveInput(inputSlot, stack);
        }
        return null;
    }

    private void pushOutputToFront() {
        if(level == null || level.isClientSide) {
            return;
        }

        Direction front = getFrontDirection();
        BlockPos targetPos = worldPosition.relative(front);
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, front.getOpposite());
        if(target == null) {
            return;
        }

        for(int slot = OUTPUT_START; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if(stack.isEmpty()) {
                continue;
            }

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
            if(remainder.isEmpty()) {
                items.setStackInSlot(slot, ItemStack.EMPTY);
            } else if(remainder.getCount() != stack.getCount()) {
                items.setStackInSlot(slot, remainder.copy());
            }
            return;
        }
    }

    private boolean isFrontOutputBlocked() {
        if(level == null || level.isClientSide || !hasOutputItems()) {
            return false;
        }

        Direction front = getFrontDirection();
        BlockPos targetPos = worldPosition.relative(front);
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, front.getOpposite());
        if(target == null) {
            return false;
        }

        for(int slot = OUTPUT_START; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if(stack.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack.copy(), true);
            return !remainder.isEmpty() && remainder.getCount() == stack.getCount();
        }
        return false;
    }

    private void updateProgressTracking() {
        ActiveInput active = findActiveInput();
        if(active == null) {
            trackedInputSlot = -1;
            trackedStartingItemCount = 0;
            trackedRemainingItemCount = 0;
            trackedStatus = isFrontOutputBlocked() ? STATUS_FRONT_OUTPUT_BLOCKED : STATUS_IDLE;
            return;
        }

        int remaining = ContainerItemUtil.countContentItems(active.stack());
        if(trackedInputSlot != active.slot()) {
            trackedInputSlot = active.slot();
            trackedStartingItemCount = Math.max(remaining, 1);
            lastContentExtractionGameTime = level == null ? 0L : level.getGameTime();
        } else if(remaining > trackedStartingItemCount) {
            trackedStartingItemCount = remaining;
        }

        trackedRemainingItemCount = remaining;

        if(remaining <= 0) {
            trackedStatus = findFirstEmptyOutputSlot() < 0 ? STATUS_OUTPUT_FULL : STATUS_WORKING;
            return;
        }

        if(level != null && level.getGameTime() - lastContentExtractionGameTime > WAITING_FOR_HOPPER_TICKS) {
            trackedStatus = STATUS_WAITING_FOR_HOPPER;
        } else {
            trackedStatus = STATUS_WORKING;
        }
    }

    private void resetProgressTracking() {
        trackedInputSlot = -1;
        trackedStartingItemCount = 0;
        trackedRemainingItemCount = 0;
        trackedStatus = STATUS_IDLE;
        lastContentExtractionGameTime = level == null ? 0L : level.getGameTime();
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
        return Component.translatable("container.unpacker.unpacker");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new UnpackerMenu(containerId, playerInventory, this, worldPosition);
    }

    private record ActiveInput(int slot, ItemStack stack) {
    }

    private class TopInputHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return INPUT_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if(slot < 0 || slot >= INPUT_SLOTS) {
                return ItemStack.EMPTY;
            }
            return items.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if(slot < 0 || slot >= INPUT_SLOTS || !ContainerItemUtil.isSupportedContainer(stack)) {
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
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot >= 0 && slot < INPUT_SLOTS && ContainerItemUtil.isSupportedContainer(stack);
        }
    }

    private class FrontOutputHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return OUTPUT_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            int internalSlot = OUTPUT_START + slot;
            if(slot < 0 || slot >= OUTPUT_SLOTS) {
                return ItemStack.EMPTY;
            }
            return items.getStackInSlot(internalSlot);
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

    private class BottomContentHandler implements IItemHandler {
        @Override
        public int getSlots() {
            ActiveInput active = findActiveInput();
            return active == null ? 0 : ContainerItemUtil.getContentSlots(active.stack());
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            ActiveInput active = findActiveInput();
            return active == null ? ItemStack.EMPTY : ContainerItemUtil.getContentStack(active.stack(), slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            ActiveInput active = findActiveInput();
            if(active == null) {
                return ItemStack.EMPTY;
            }

            ItemStack extracted = ContainerItemUtil.extractContent(active.stack(), slot, amount, simulate);
            if(!simulate && !extracted.isEmpty()) {
                items.setStackInSlot(active.slot(), active.stack());
                lastContentExtractionGameTime = level == null ? 0L : level.getGameTime();
                changed();
                moveCompletedInputsToOutput();
                updateProgressTracking();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    }
}
