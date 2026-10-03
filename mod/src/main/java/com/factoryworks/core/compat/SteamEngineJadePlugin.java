package com.factoryworks.core.compat;

import com.factoryworks.core.FactoryWorksCore;
import com.factoryworks.core.fluid.SteamEngineBlock;
import com.factoryworks.core.fluid.SteamEngineBlockEntity;
import com.factoryworks.core.fluid.SteamEngineStatus;
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
 * an engine to set. The charge is Jade's own row, read through the engine's energy face.
 */
@WailaPlugin
public class SteamEngineJadePlugin implements IWailaPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, "steam_engine");

    private static final String STATUS = "SteamEngineStatus";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof SteamEngineBlockEntity engine)) {
                return;
            }
            tag.putInt(STATUS, engine.status().map(Enum::ordinal).orElse(-1));
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
