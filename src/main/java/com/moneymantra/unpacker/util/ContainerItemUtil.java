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


    public static int countContentItems(ItemStack stack) {
        int total = 0;
        int slots = getContentSlots(stack);
        for(int slot = 0; slot < slots; slot++) {
            ItemStack contentStack = getContentStack(stack, slot);
            if(!contentStack.isEmpty()) {
                total += contentStack.getCount();
            }
        }
        return total;
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


    public static boolean isContainerFull(ItemStack stack) {
        return switch(getKind(stack)) {
            case TRAVELERS_BACKPACK -> isTravelerBackpackStorageFull(stack);
            case SHULKER_BOX -> isShulkerFull(stack);
            case BUNDLE -> isBundleFull(stack);
            case UNSUPPORTED -> true;
        };
    }

    public static float getFillProgress(ItemStack stack) {
        return switch(getKind(stack)) {
            case TRAVELERS_BACKPACK -> getTravelerBackpackFillProgress(stack);
            case SHULKER_BOX -> getShulkerFillProgress(stack);
            case BUNDLE -> getBundleFillProgress(stack);
            case UNSUPPORTED -> 0.0F;
        };
    }

    public static boolean canInsertContent(ItemStack container, ItemStack input) {
        return insertContent(container, input, true) > 0;
    }

    public static int insertContent(ItemStack container, ItemStack input, boolean simulate) {
        if(container.isEmpty() || input.isEmpty() || isSupportedContainer(input)) {
            return 0;
        }

        return switch(getKind(container)) {
            case TRAVELERS_BACKPACK -> insertTravelerBackpackContent(container, input, simulate);
            case SHULKER_BOX -> insertShulkerContent(container, input, simulate);
            case BUNDLE -> insertBundleContent(container, input, simulate);
            case UNSUPPORTED -> 0;
        };
    }

    public static String getKindDisplayName(ItemStack stack) {
        return switch(getKind(stack)) {
            case TRAVELERS_BACKPACK -> "Backpack";
            case SHULKER_BOX -> "Shulker Box";
            case BUNDLE -> "Bundle";
            case UNSUPPORTED -> "Container";
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

    private static boolean isTravelerBackpackStorageFull(ItemStack stack) {
        IItemHandler handler = getTravelerBackpackStorage(stack);
        if(handler == null || handler.getSlots() <= 0) {
            return true;
        }
        for(int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stored = handler.getStackInSlot(slot);
            if(stored.isEmpty()) {
                return false;
            }
            int limit = Math.min(handler.getSlotLimit(slot), stored.getMaxStackSize());
            if(stored.getCount() < limit) {
                return false;
            }
        }
        return true;
    }

    private static float getTravelerBackpackFillProgress(ItemStack stack) {
        IItemHandler handler = getTravelerBackpackStorage(stack);
        if(handler == null || handler.getSlots() <= 0) {
            return 0.0F;
        }
        int filledSlots = 0;
        for(int slot = 0; slot < handler.getSlots(); slot++) {
            if(!handler.getStackInSlot(slot).isEmpty()) {
                filledSlots++;
            }
        }
        return filledSlots / (float)handler.getSlots();
    }

    private static int insertTravelerBackpackContent(ItemStack container, ItemStack input, boolean simulate) {
        IItemHandler handler = getTravelerBackpackStorage(container);
        if(handler == null) {
            return 0;
        }

        ItemStack remainder = input.copy();
        for(int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, simulate);
        }
        return input.getCount() - remainder.getCount();
    }

    private static boolean isShulkerFull(ItemStack stack) {
        NonNullList<ItemStack> items = getShulkerItems(stack);
        for(ItemStack stored : items) {
            if(stored.isEmpty()) {
                return false;
            }
            if(stored.getCount() < stored.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private static float getShulkerFillProgress(ItemStack stack) {
        NonNullList<ItemStack> items = getShulkerItems(stack);
        int filledSlots = 0;
        for(ItemStack stored : items) {
            if(!stored.isEmpty()) {
                filledSlots++;
            }
        }
        return filledSlots / (float)SHULKER_SLOTS;
    }

    private static int insertShulkerContent(ItemStack container, ItemStack input, boolean simulate) {
        if(input.isEmpty()) {
            return 0;
        }

        NonNullList<ItemStack> items = getShulkerItems(container);
        ItemStack remainder = input.copy();

        for(int slot = 0; slot < SHULKER_SLOTS && !remainder.isEmpty(); slot++) {
            ItemStack stored = items.get(slot);
            if(stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, remainder)) {
                continue;
            }

            int limit = stored.getMaxStackSize();
            int toMove = Math.min(remainder.getCount(), limit - stored.getCount());
            if(toMove <= 0) {
                continue;
            }

            if(!simulate) {
                stored.grow(toMove);
            }
            remainder.shrink(toMove);
        }

        for(int slot = 0; slot < SHULKER_SLOTS && !remainder.isEmpty(); slot++) {
            ItemStack stored = items.get(slot);
            if(!stored.isEmpty()) {
                continue;
            }

            int toMove = Math.min(remainder.getCount(), remainder.getMaxStackSize());
            if(toMove <= 0) {
                continue;
            }

            if(!simulate) {
                items.set(slot, remainder.copyWithCount(toMove));
            }
            remainder.shrink(toMove);
        }

        int inserted = input.getCount() - remainder.getCount();
        if(!simulate && inserted > 0) {
            container.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
        return inserted;
    }

    private static boolean isBundleFull(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return contents.weight().floatValue() >= 0.999F;
    }

    private static float getBundleFillProgress(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return Math.max(0.0F, Math.min(1.0F, contents.weight().floatValue()));
    }

    private static int insertBundleContent(ItemStack container, ItemStack input, boolean simulate) {
        if(input.isEmpty()) {
            return 0;
        }

        BundleContents contents = container.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        int inserted = mutable.tryInsert(input.copy());
        if(!simulate && inserted > 0) {
            container.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        }
        return inserted;
    }
}
