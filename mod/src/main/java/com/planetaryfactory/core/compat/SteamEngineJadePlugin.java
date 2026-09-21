package com.planetaryfactory.core.compat;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.fluid.SteamEngineBlock;
import com.planetaryfactory.core.fluid.SteamEngineBlockEntity;
import com.planetaryfactory.core.fluid.SteamEngineStatus;
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
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * The Steam Engine's HUD (#352), which is its only interface: no screen, since there is nothing on
 * an engine to set. The steam tank is Jade's own fluid row, read through the engine's fluid face,
 * which is its Master Engine's tank on a slave (ADR-0077). Jade's FE row is dropped: it shows the
 * buffer, which reads as output and is not, and the status already says when it is full.
 */
@WailaPlugin
public class SteamEngineJadePlugin implements IWailaPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "steam_engine");

    private static final String STATUS = "SteamEngineStatus";
    private static final String CHAINED = "SteamEngineChained";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof SteamEngineBlockEntity engine)) {
                return;
            }
            tag.putInt(STATUS, engine.status().ordinal());
            tag.putBoolean(CHAINED, engine.inSlaveMode());
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    private static final IBlockComponentProvider TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(STATUS)) {
                return;
            }
            SteamEngineStatus status = SteamEngineStatus.fromOrdinal(data.getIntOr(STATUS, -1));
            tooltip.add(Component.translatable(status.langKey())
                    .withStyle(status.problem() ? ChatFormatting.RED : ChatFormatting.GREEN));
            if (data.getBooleanOr(CHAINED, false)) {
                tooltip.add(Component.translatable("gui.planetaryfactory.steam_engine.chained")
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, SteamEngineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, SteamEngineBlock.class);
        registration.addTooltipCollectedCallback((box, accessor) -> {
            if (accessor instanceof BlockAccessor block && block.getBlock() instanceof SteamEngineBlock) {
                box.getTooltip().remove(JadeIds.UNIVERSAL_ENERGY_STORAGE);
            }
        });
    }
}
