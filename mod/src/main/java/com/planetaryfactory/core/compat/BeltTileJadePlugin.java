package com.planetaryfactory.core.compat;

import java.util.LinkedHashMap;
import java.util.Map;

import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.neoforged.fml.ModList;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import io.github._5thlayer.beltworks.blocks.BeltTileBlock;
import io.github._5thlayer.beltworks.blocks.BeltTileBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * A belt tile's HUD (#398): the rate the line through it runs at, and what the tile under the
 * crosshair itself carries.
 *
 * <p>The items are sent from the server rather than read on the client: a line's contents are the
 * head tile's, and the client is not sent them (#395).
 */
@WailaPlugin
public class BeltTileJadePlugin implements IWailaPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "belt_tile");

    private static final String LINE_TILES = "BeltLineTiles";
    private static final String LINE_RATE = "BeltLineRate";
    private static final String HELD = "BeltTileHeld";
    private static final String STACK = "BeltTileStack";
    private static final String KINDS = "BeltTileKinds";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof BeltTileBlockEntity tile)) {
                return;
            }
            var line = tile.line();
            if (line != null) {
                tag.putInt(LINE_TILES, line.tileCount());
                tag.putInt(LINE_RATE, (int) Math.round(line.itemsPerSecond()));
            }
            Map<String, ItemStack> kinds = new LinkedHashMap<>();
            int held = 0;
            for (ItemStack stack : tile.heldHere()) {
                held += stack.getCount();
                kinds.merge(stack.getItem().toString(), stack.copy(), (first, next) -> {
                    first.grow(next.getCount());
                    return first;
                });
            }
            tag.putInt(HELD, held);
            tag.putInt(KINDS, kinds.size());
            kinds.values().stream().findFirst()
                    .ifPresent(stack -> JadeStacks.put(tag, STACK, stack, accessor));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    private static final IBlockComponentProvider TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlock() instanceof BeltTileBlock belt)) {
                return;
            }
            tooltip.add(Component.translatable("gui.planetaryfactory.belt_tile.tier",
                    belt.tier().number(), (int) Math.round(belt.tier().itemsPerSecond()))
                    .withStyle(ChatFormatting.GRAY));

            CompoundTag data = accessor.getServerData();
            if (data.contains(LINE_TILES)) {
                tooltip.add(Component.translatable("gui.planetaryfactory.belt_tile.line",
                        data.getIntOr(LINE_TILES, 0), data.getIntOr(LINE_RATE, 0))
                        .withStyle(ChatFormatting.GRAY));
            }
            int held = data.getIntOr(HELD, 0);
            if (held == 0) {
                tooltip.add(Component.translatable("gui.planetaryfactory.belt_tile.empty")
                        .withStyle(ChatFormatting.DARK_GRAY));
                return;
            }
            ItemStack first = JadeStacks.read(data, STACK, accessor);
            // A tile holds eight items at most, so one name and a count says the whole of it.
            if (data.getIntOr(KINDS, 0) == 1 && !first.isEmpty()) {
                tooltip.add(Component.translatable("gui.planetaryfactory.belt_tile.holds_one",
                        held, first.getHoverName()));
            } else {
                tooltip.add(Component.translatable("gui.planetaryfactory.belt_tile.holds", held));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    // The fork is an optional dependency, and naming its classes here is what would load them, so
    // a Jade install without it gets no provider rather than a NoClassDefFoundError in its scan.
    @Override
    public void register(IWailaCommonRegistration registration) {
        if (!ModList.get().isLoaded("beltworks")) {
            return;
        }
        registration.registerBlockDataProvider(DATA, BeltTileBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        if (!ModList.get().isLoaded("beltworks")) {
            return;
        }
        registration.registerBlockComponent(TOOLTIP, BeltTileBlock.class);
    }
}
