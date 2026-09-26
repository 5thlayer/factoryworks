package com.planetaryfactory.core.compat;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.energy.AccumulatorBlock;
import com.planetaryfactory.core.energy.AccumulatorBlockEntity;
import com.planetaryfactory.core.energy.AccumulatorStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
 * The accumulator's HUD (#468), its only interface. The charge is Jade's own energy row, read
 * through the face.
 */
@WailaPlugin
public class AccumulatorJadePlugin implements IWailaPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "accumulator");

    private static final String STATUS = "AccumulatorStatus";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof AccumulatorBlockEntity accumulator) {
                tag.putInt(STATUS, accumulator.status().map(Enum::ordinal).orElse(-1));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    private static final IBlockComponentProvider TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            AccumulatorStatus.fromOrdinal(accessor.getServerData().getIntOr(STATUS, -1)).ifPresent(status ->
                    tooltip.add(Component.translatable(status.langKey())
                            .withStyle(status.problem() ? ChatFormatting.RED : ChatFormatting.GREEN)));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, AccumulatorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, AccumulatorBlock.class);
    }
}
