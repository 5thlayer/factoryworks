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
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

/**
 * The Steam Engine's HUD (#352), which is its only interface: no screen, since there is nothing on
 * an engine to set. A slave's figures are its Master Engine's, since its own tank is empty by design
 * (ADR-0077). Jade's own tank and FE rows are dropped: they repeat the steam gauge and show the
 * buffer, which reads as output and is not.
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
    private static final String MAX_OUTPUT = "SteamEngineMaxOutput";

    private static final int STEAM_COLOUR = 0xFFB4B4B4;
    private static final int POWER_COLOUR = 0xFF3CC83C;

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
            tag.putLong(MAX_OUTPUT, engine.maxOutputPerTick());
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
            bar(tooltip, data, STEAM, STEAM_CAPACITY, STEAM_COLOUR, "gui.planetaryfactory.steam_engine.steam");
            bar(tooltip, data, OUTPUT, MAX_OUTPUT, POWER_COLOUR, "gui.planetaryfactory.steam_engine.output");
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    /** One of Factorio's gauges: the figure written inside a bar filled to it. */
    private static void bar(ITooltip tooltip, CompoundTag data, String amountKey, String capacityKey,
                            int colour, String langKey) {
        long amount = data.getLongOr(amountKey, 0L);
        long capacity = data.getLongOr(capacityKey, 0L);
        float filled = capacity <= 0 ? 0F : Math.min(1F, amount / (float) capacity);
        Component text = Component.translatable(langKey, figure(amount), figure(capacity));
        tooltip.add(JadeUI.progress(new ProgressView(ProgressView.Part.of(filled, colour), text,
                JadeUI.progressStyle(), BoxStyle.DEFAULT_NESTED_BOX)));
    }

    private static String figure(long value) {
        return String.format("%,d", value);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, SteamEngineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, SteamEngineBlock.class);
        registration.addTooltipCollectedCallback((box, accessor) -> {
            if (accessor instanceof BlockAccessor block && block.getBlock() instanceof SteamEngineBlock) {
                box.getTooltip().remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
                box.getTooltip().remove(JadeIds.UNIVERSAL_ENERGY_STORAGE);
            }
        });
    }
}
