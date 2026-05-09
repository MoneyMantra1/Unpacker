package com.moneymantra.unpacker.menu;

import com.moneymantra.unpacker.blockentity.UnpackerBlockEntity;
import com.moneymantra.unpacker.registry.ModBlocks;
import com.moneymantra.unpacker.registry.ModMenuTypes;
import com.moneymantra.unpacker.util.ContainerItemUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class UnpackerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = UnpackerBlockEntity.TOTAL_SLOTS;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final UnpackerBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private int activeInputSlot = -1;
    private int startingItemCount = 0;
    private int remainingItemCount = 0;
    private int status = UnpackerBlockEntity.STATUS_IDLE;

    public UnpackerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, readBlockEntity(playerInventory, data));
    }

    private UnpackerMenu(int containerId, Inventory playerInventory, MenuInit init) {
        this(containerId, playerInventory, init.blockEntity(), init.pos());
    }

    public UnpackerMenu(int containerId, Inventory playerInventory, UnpackerBlockEntity blockEntity, BlockPos pos) {
        super(ModMenuTypes.UNPACKER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);

        addMachineSlots();
        addPlayerInventorySlots(playerInventory);
        addProgressDataSlots();
    }

    private static MenuInit readBlockEntity(Inventory playerInventory, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if(blockEntity instanceof UnpackerBlockEntity unpacker) {
            return new MenuInit(unpacker, pos);
        }
        throw new IllegalStateException("Expected Unpacker block entity at " + pos + ", found " + blockEntity);
    }

    private record MenuInit(UnpackerBlockEntity blockEntity, BlockPos pos) {
    }

    private void addMachineSlots() {
        int startX = 90;

        for(int row = 0; row < 2; row++) {
            for(int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                this.addSlot(new SlotItemHandler(blockEntity.getInternalItems(), slot, startX + col * 18, 28 + row * 18));
            }
        }

        for(int row = 0; row < 2; row++) {
            for(int col = 0; col < 9; col++) {
                int slot = UnpackerBlockEntity.OUTPUT_START + row * 9 + col;
                this.addSlot(new LockedSlotItemHandler(blockEntity.getInternalItems(), slot, startX + col * 18, 137 + row * 18));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 90;

        for(int row = 0; row < 3; row++) {
            for(int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, 181 + row * 18));
            }
        }

        for(int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, 236));
        }
    }

    private void addProgressDataSlots() {
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.getTrackedInputSlot();
            }

            @Override
            public void set(int value) {
                activeInputSlot = value;
            }
        });

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.getTrackedStartingItemCount();
            }

            @Override
            public void set(int value) {
                startingItemCount = value;
            }
        });

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.getTrackedRemainingItemCount();
            }

            @Override
            public void set(int value) {
                remainingItemCount = value;
            }
        });

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.getTrackedStatus();
            }

            @Override
            public void set(int value) {
                status = value;
            }
        });
    }

    public int getActiveInputSlot() {
        return activeInputSlot;
    }

    public int getStartingItemCount() {
        return startingItemCount;
    }

    public int getRemainingItemCount() {
        return remainingItemCount;
    }

    public int getStatus() {
        return status;
    }

    public ItemStack getActiveInputStack() {
        if(activeInputSlot < 0 || activeInputSlot >= UnpackerBlockEntity.INPUT_SLOTS) {
            return ItemStack.EMPTY;
        }
        return this.slots.get(activeInputSlot).getItem();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.UNPACKER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if(slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if(index < MACHINE_SLOT_COUNT) {
                if(!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if(ContainerItemUtil.isSupportedContainer(stack)) {
                if(!this.moveItemStackTo(stack, 0, UnpackerBlockEntity.INPUT_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else if(index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
                if(!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if(index >= HOTBAR_START && index < HOTBAR_END) {
                if(!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if(stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if(stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return result;
    }
}
