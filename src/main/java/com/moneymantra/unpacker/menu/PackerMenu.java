package com.moneymantra.unpacker.menu;

import com.moneymantra.unpacker.blockentity.PackerBlockEntity;
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

public class PackerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = PackerBlockEntity.TOTAL_SLOTS;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final PackerBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private int activeContainerSlot = -1;
    private int packedItemCount = 0;
    private int fillPercent = 0;
    private int status = PackerBlockEntity.STATUS_IDLE;

    public PackerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, readBlockEntity(playerInventory, data));
    }

    private PackerMenu(int containerId, Inventory playerInventory, MenuInit init) {
        this(containerId, playerInventory, init.blockEntity(), init.pos());
    }

    public PackerMenu(int containerId, Inventory playerInventory, PackerBlockEntity blockEntity, BlockPos pos) {
        super(ModMenuTypes.PACKER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);

        addMachineSlots();
        addPlayerInventorySlots(playerInventory);
        addProgressDataSlots();
    }

    private static MenuInit readBlockEntity(Inventory playerInventory, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if(blockEntity instanceof PackerBlockEntity packer) {
            return new MenuInit(packer, pos);
        }
        throw new IllegalStateException("Expected Packer block entity at " + pos + ", found " + blockEntity);
    }

    private record MenuInit(PackerBlockEntity blockEntity, BlockPos pos) {
    }

    private void addMachineSlots() {
        int startX = 29;

        for(int row = 0; row < 2; row++) {
            for(int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                this.addSlot(new SlotItemHandler(blockEntity.getInternalItems(), slot, startX + col * 18, 22 + row * 18));
            }
        }

        int containerStartX = 38;
        for(int row = 0; row < 2; row++) {
            for(int col = 0; col < 3; col++) {
                int containerSlot = PackerBlockEntity.CONTAINER_START + row * 3 + col;
                this.addSlot(new SlotItemHandler(blockEntity.getInternalItems(), containerSlot, containerStartX + col * 18, 115 + row * 18));
            }
        }

        int outputStartX = 128;
        for(int row = 0; row < 2; row++) {
            for(int col = 0; col < 3; col++) {
                int outputSlot = PackerBlockEntity.OUTPUT_START + row * 3 + col;
                this.addSlot(new LockedSlotItemHandler(blockEntity.getInternalItems(), outputSlot, outputStartX + col * 18, 115 + row * 18));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 29;

        for(int row = 0; row < 3; row++) {
            for(int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, 164 + row * 18));
            }
        }

        for(int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, 220));
        }
    }

    private void addProgressDataSlots() {
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.getTrackedContainerSlot();
            }

            @Override
            public void set(int value) {
                activeContainerSlot = value;
            }
        });

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.getTrackedPackedItemCount();
            }

            @Override
            public void set(int value) {
                packedItemCount = value;
            }
        });

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.getTrackedFillPercent();
            }

            @Override
            public void set(int value) {
                fillPercent = value;
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

    public int getActiveContainerSlot() {
        return activeContainerSlot;
    }

    public int getPackedItemCount() {
        return packedItemCount;
    }

    public int getFillPercent() {
        return fillPercent;
    }

    public int getStatus() {
        return status;
    }

    public ItemStack getActiveContainerStack() {
        if(activeContainerSlot < PackerBlockEntity.CONTAINER_START || activeContainerSlot >= PackerBlockEntity.OUTPUT_START) {
            return ItemStack.EMPTY;
        }
        return this.slots.get(activeContainerSlot).getItem();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PACKER.get());
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
                if(!this.moveItemStackTo(stack, PackerBlockEntity.CONTAINER_START, PackerBlockEntity.OUTPUT_START, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if(!this.moveItemStackTo(stack, 0, PackerBlockEntity.ITEM_INPUT_SLOTS, false)) {
                    if(index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
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
                }
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
