package com.planetaryfactory.core.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;

/**
 * One stack in and out of a Jade sync tag.
 *
 * <p>Both providers carry an {@link ItemStack} from the server to the tooltip, and 26.1 took away
 * the {@code save}/{@code parse} pair they used: a stack is written through its codec now. Jade's
 * own transport is still a {@link CompoundTag}, so the codec is applied against {@link NbtOps}
 * here rather than twice, differently, in two plugins.
 *
 * <p>A stack that will not encode is left out of the tag, and a tag that will not decode reads as
 * empty. Both providers already draw an empty stack as the dash, so a failure here is a tooltip
 * line that says "nothing" rather than a crashed HUD.
 */
final class JadeStacks {

    private JadeStacks() {
    }

    static void put(CompoundTag tag, String key, ItemStack stack, BlockAccessor accessor) {
        ItemStack.CODEC
                .encodeStart(accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), stack)
                .result()
                .ifPresent(encoded -> tag.put(key, encoded));
    }

    static ItemStack read(CompoundTag tag, String key, BlockAccessor accessor) {
        Tag encoded = tag.get(key);
        if (encoded == null) {
            return ItemStack.EMPTY;
        }
        return ItemStack.CODEC
                .parse(accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), encoded)
                .result()
                .orElse(ItemStack.EMPTY);
    }
}
