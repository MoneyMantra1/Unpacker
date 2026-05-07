package com.moneymantra.unpacker.registry;

import com.moneymantra.unpacker.Unpacker;
import com.moneymantra.unpacker.blockentity.UnpackerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Unpacker.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UnpackerBlockEntity>> UNPACKER = BLOCK_ENTITIES.register(
            "unpacker",
            () -> BlockEntityType.Builder.of(UnpackerBlockEntity::new, ModBlocks.UNPACKER.get()).build(null)
    );
}
