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
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * The Steam Engine's HUD (#352), which is its only interface: no screen, since there is nothing on
 * an engine to set. The tank and the charge are Jade's own rows, read through the engine's faces,
 * which are its Master Engine's on a slave (ADR-0077).
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
            tag.putInt(STATUS, engine.status().map(Enum::ordinal).orElse(-1));
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
            SteamEngineStatus.fromOrdinal(data.getIntOr(STATUS, -1)).ifPresent(status ->
                    tooltip.add(Component.translatable(status.langKey()).withStyle(ChatFormatting.RED)));
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
    }
}
