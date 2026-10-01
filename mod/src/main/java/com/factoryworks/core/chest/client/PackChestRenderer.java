package com.factoryworks.core.chest.client;

import com.factoryworks.core.chest.ChestTier;
import com.factoryworks.core.chest.PackChestBlock;
import com.factoryworks.core.chest.PackChestBlockEntity;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

/** Draws each tier's own sprite from the chest sheet, which atlases every namespace's `entity/chest` (#541). */
public class PackChestRenderer extends ChestRenderer<PackChestBlockEntity> {

    public PackChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SpriteId getCustomSprite(PackChestBlockEntity chest, ChestRenderState state) {
        ChestTier tier = ((PackChestBlock) chest.getBlockState().getBlock()).tier();
        return new SpriteId(Sheets.CHEST_SHEET,
                Identifier.fromNamespaceAndPath(tier.spriteNamespace(), "entity/chest/" + tier.spritePath()));
    }
}
