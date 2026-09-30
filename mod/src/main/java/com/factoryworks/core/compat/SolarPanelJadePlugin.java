package com.factoryworks.core.compat;

import com.factoryworks.core.FactoryWorksCore;
import com.factoryworks.core.energy.SolarPanelBlock;
import com.factoryworks.core.energy.SolarPanelBlockEntity;
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

/** The Solar Panel's HUD (#531): what it is making this tick, so a night reads apart from a fault. */
@WailaPlugin
public class SolarPanelJadePlugin implements IWailaPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, "solar_panel");

    private static final String OUTPUT = "SolarPanelOutput";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof SolarPanelBlockEntity panel) {
                tag.putLong(OUTPUT, panel.currentOutputFe());
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
            CompoundTag data = accessor.getServerData();
            if (!data.contains(OUTPUT)) {
                return;
            }
            long fe = data.getLongOr(OUTPUT, 0L);
            tooltip.add(Component.translatable("gui.factoryworks.solar_panel.output", fe)
                    .withStyle(fe > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, SolarPanelBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, SolarPanelBlock.class);
    }
}
