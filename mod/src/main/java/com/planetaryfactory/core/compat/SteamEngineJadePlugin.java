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
 * The Steam Engine's status, steam, output and charge on the HUD (#352). A slave's figures are its
 * Master Engine's, since its own tank and buffer are empty by design (ADR-0077).
 */
@WailaPlugin
public class SteamEngineJadePlugin implements IWailaPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "steam_engine");

    private static final String STATUS = "SteamEngineStatus";
    private static final String CHAINED = "SteamEngineChained";
    private static final String STEAM = "SteamEngineSteam";
    private static final String STEAM_CAPACITY = "SteamEngineSteamCapacity";
    private static final String OUTPUT = "SteamEngineOutput";
    private static final String ENERGY = "SteamEngineEnergy";
    private static final String CAPACITY = "SteamEngineCapacity";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof SteamEngineBlockEntity engine)) {
                return;
            }
            tag.putInt(STATUS, engine.status().ordinal());
            tag.putBoolean(CHAINED, engine.inSlaveMode());
            tag.putLong(STEAM, engine.steam());
            tag.putLong(STEAM_CAPACITY, engine.steamCapacity());
            tag.putLong(OUTPUT, engine.outputPerTick());
            tag.putLong(ENERGY, engine.energy());
            tag.putLong(CAPACITY, engine.energyCapacity());
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
                    .withStyle(status.problem() ? ChatFormatting.RED : ChatFormatting.RESET));
            if (data.getBooleanOr(CHAINED, false)) {
                tooltip.add(Component.translatable("gui.planetaryfactory.steam_engine.chained"));
            }
            tooltip.add(Component.translatable("gui.planetaryfactory.steam_engine.steam",
                    figure(data, STEAM), figure(data, STEAM_CAPACITY)));
            tooltip.add(Component.translatable("gui.planetaryfactory.steam_engine.output",
                    figure(data, OUTPUT)));
            tooltip.add(Component.translatable("gui.planetaryfactory.steam_engine.energy",
                    figure(data, ENERGY), figure(data, CAPACITY)));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    private static String figure(CompoundTag data, String key) {
        return String.format("%,d", data.getLongOr(key, 0L));
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, SteamEngineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, SteamEngineBlock.class);
    }
}
