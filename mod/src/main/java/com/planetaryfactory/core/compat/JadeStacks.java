package com.planetaryfactory.core.compat;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;

/**
 * One stack in and out of a Jade sync tag.
 *
 * <p>26.1 took away the stack's {@code save}/{@code parse} pair, and Jade's transport is still a
 * {@link CompoundTag}, so the codec is applied against {@link NbtOps} here, once for every plugin.
 *
 * <p>A stack that will not encode is left out of the tag, and a tag that will not decode reads as
 * empty, which each plugin draws as "nothing" rather than crashing the HUD.
 */
final class JadeStacks {

    private JadeStacks() {
    }

    /** The registry-aware NBT ops both directions need; stated once. */
    private static RegistryOps<Tag> ops(BlockAccessor accessor) {
        return accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
    }

    static void put(CompoundTag tag, String key, ItemStack stack, BlockAccessor accessor) {
        ItemStack.CODEC.encodeStart(ops(accessor), stack)
                .result()
                .ifPresent(encoded -> tag.put(key, encoded));
    }

    static void putComponent(CompoundTag tag, String key, Component component, BlockAccessor accessor) {
        ComponentSerialization.CODEC.encodeStart(ops(accessor), component)
                .result()
                .ifPresent(encoded -> tag.put(key, encoded));
    }

    static Optional<Component> readComponent(CompoundTag tag, String key, BlockAccessor accessor) {
        Tag encoded = tag.get(key);
        return encoded == null ? Optional.empty() : ComponentSerialization.CODEC.parse(ops(accessor), encoded).result();
    }

    static ItemStack read(CompoundTag tag, String key, BlockAccessor accessor) {
        Tag encoded = tag.get(key);
        if (encoded == null) {
            return ItemStack.EMPTY;
        }
        return ItemStack.CODEC.parse(ops(accessor), encoded)
                .result()
                .orElse(ItemStack.EMPTY);
    }
}
