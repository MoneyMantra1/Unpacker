package com.moneymantra.unpacker.registry;

import com.moneymantra.unpacker.Unpacker;
import com.moneymantra.unpacker.block.UnpackerBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Unpacker.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Unpacker.MOD_ID);

    public static final DeferredBlock<UnpackerBlock> UNPACKER = BLOCKS.register("unpacker", () -> new UnpackerBlock(
            BlockBehaviour.Properties.of()
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
    ));

    public static final DeferredItem<BlockItem> UNPACKER_ITEM = ITEMS.register("unpacker", () -> new BlockItem(UNPACKER.get(), new Item.Properties()));
}
