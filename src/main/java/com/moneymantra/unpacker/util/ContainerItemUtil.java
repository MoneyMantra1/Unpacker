package com.moneymantra.unpacker.util;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ContainerItemUtil {
    private static final int SHULKER_SLOTS = 27;
    private static final String TRAVELERS_BACKPACK_NAMESPACE = "travelersbackpack";

    private ContainerItemUtil() {
    }

    public enum ContainerKind {
        TRAVELERS_BACKPACK,
        SHULKER_BOX,
        BUNDLE,
        UNSUPPORTED
    }

    public static boolean isSupportedContainer(ItemStack stack) {
        return getKind(stack) != ContainerKind.UNSUPPORTED;
    }

    public static ContainerKind getKind(ItemStack stack) {
        if(stack.isEmpty()) {
            return ContainerKind.UNSUPPORTED;
        }
        if(isTravelersBackpackItem(stack)) {
            return ContainerKind.TRAVELERS_BACKPACK;
        }
        if(isShulkerBox(stack)) {
            return ContainerKind.SHULKER_BOX;
        }
        if(stack.getItem() instanceof BundleItem) {
            return ContainerKind.BUNDLE;
        }
        return ContainerKind.UNSUPPORTED;
    }

    public static boolean isContainerEmpty(ItemStack stack) {
        return switch(getKind(stack)) {
            case TRAVELERS_BACKPACK -> isTravelerBackpackStorageEmpty(stack);
            case SHULKER_BOX -> isShulkerEmpty(stack);
            case BUNDLE -> isBundleEmpty(stack);
            case UNSUPPORTED -> true;
        };
    }

    public static int getContentSlots(ItemStack stack) {
        return switch(getKind(stack)) {
            case TRAVELERS_BACKPACK -> {
                IItemHandler handler = getTravelerBackpackStorage(stack);
                yield handler == null ? 0 : handler.getSlots();
            }
            case SHULKER_BOX -> SHULKER_SLOTS;
            case BUNDLE -> getBundleItems(stack).size();
            case UNSUPPORTED -> 0;
        };
    }

    public static ItemStack getContentStack(ItemStack stack, int slot) {
        if(slot < 0) {
            return ItemStack.EMPTY;
        }

        return switch(getKind(stack)) {
            case TRAVELERS_BACKPACK -> {
                IItemHandler handler = getTravelerBackpackStorage(stack);
                yield handler == null || slot >= handler.getSlots() ? ItemStack.EMPTY : handler.getStackInSlot(slot).copy();
            }
            case SHULKER_BOX -> getShulkerStack(stack, slot);
            case BUNDLE -> getBundleStack(stack, slot);
            case UNSUPPORTED -> ItemStack.EMPTY;
        };
    }

    public static ItemStack extractContent(ItemStack stack, int slot, int amount, boolean simulate) {
        if(stack.isEmpty() || slot < 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }

        return switch(getKind(stack)) {
            case TRAVELERS_BACKPACK -> extractTravelerBackpackContent(stack, slot, amount, simulate);
            case SHULKER_BOX -> extractShulkerContent(stack, slot, amount, simulate);
            case BUNDLE -> extractBundleContent(stack, slot, amount, simulate);
            case UNSUPPORTED -> ItemStack.EMPTY;
        };
    }

    private static boolean isTravelersBackpackItem(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!TRAVELERS_BACKPACK_NAMESPACE.equals(id.getNamespace())) {
            return false;
        }

        String path = id.getPath();
        return !path.endsWith("_sleeping_bag")
                && !path.endsWith("_upgrade")
                && !path.equals("backpack_tank")
                && !path.equals("hose")
                && !path.equals("hose_nozzle");
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    @Nullable
    private static IItemHandler getTravelerBackpackStorage(ItemStack stack) {
        return stack.getCapability(Capabilities.ItemHandler.ITEM);
    }

    private static boolean isTravelerBackpackStorageEmpty(ItemStack stack) {
        IItemHandler handler = getTravelerBackpackStorage(stack);
        if(handler == null) {
            return true;
        }
        for(int slot = 0; slot < handler.getSlots(); slot++) {
            if(!handler.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static ItemStack extractTravelerBackpackContent(ItemStack stack, int slot, int amount, boolean simulate) {
        IItemHandler handler = getTravelerBackpackStorage(stack);
        if(handler == null || slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        return handler.extractItem(slot, amount, simulate);
    }

    private static ItemContainerContents getShulkerContents(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    private static NonNullList<ItemStack> getShulkerItems(ItemStack stack) {
        NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SLOTS, ItemStack.EMPTY);
        getShulkerContents(stack).copyInto(items);
        return items;
    }

    private static boolean isShulkerEmpty(ItemStack stack) {
        return getShulkerContents(stack).nonEmptyStream().findAny().isEmpty();
    }

    private static ItemStack getShulkerStack(ItemStack stack, int slot) {
        if(slot >= SHULKER_SLOTS) {
            return ItemStack.EMPTY;
        }
        return getShulkerItems(stack).get(slot).copy();
    }

    private static ItemStack extractShulkerContent(ItemStack stack, int slot, int amount, boolean simulate) {
        if(slot >= SHULKER_SLOTS) {
            return ItemStack.EMPTY;
        }

        NonNullList<ItemStack> items = getShulkerItems(stack);
        ItemStack stored = items.get(slot);
        if(stored.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = stored.copy();
        extracted.setCount(Math.min(amount, stored.getCount()));
        if(!simulate) {
            stored.shrink(extracted.getCount());
            if(stored.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
            if(items.stream().allMatch(ItemStack::isEmpty)) {
                stack.remove(DataComponents.CONTAINER);
            } else {
                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            }
        }
        return extracted;
    }

    private static List<ItemStack> getBundleItems(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        List<ItemStack> items = new ArrayList<>();
        for(ItemStack itemStack : contents.itemsCopy()) {
            if(!itemStack.isEmpty()) {
                items.add(itemStack.copy());
            }
        }
        return items;
    }

    private static boolean isBundleEmpty(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).isEmpty();
    }

    private static ItemStack getBundleStack(ItemStack stack, int slot) {
        List<ItemStack> items = getBundleItems(stack);
        if(slot >= items.size()) {
            return ItemStack.EMPTY;
        }
        return items.get(slot).copy();
    }

    private static ItemStack extractBundleContent(ItemStack stack, int slot, int amount, boolean simulate) {
        List<ItemStack> items = getBundleItems(stack);
        if(slot >= items.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack stored = items.get(slot);
        ItemStack extracted = stored.copy();
        extracted.setCount(Math.min(amount, stored.getCount()));
        if(!simulate) {
            stored.shrink(extracted.getCount());
            if(stored.isEmpty()) {
                items.remove(slot);
            }
            stack.set(DataComponents.BUNDLE_CONTENTS, items.isEmpty() ? BundleContents.EMPTY : new BundleContents(items));
        }
        return extracted;
    }
}
