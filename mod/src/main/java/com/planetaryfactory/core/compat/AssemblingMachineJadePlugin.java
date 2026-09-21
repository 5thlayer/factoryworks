package com.planetaryfactory.core.compat;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.machine.AssemblingMachineBlock;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineRecipes;
import com.planetaryfactory.core.machine.AssemblingStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

/**
 * The Assembling Machine's Held recipe, status, energy and progress on the HUD (#333). The status
 * is {@link AssemblingMachineBlockEntity#status}, the screen's rule; a locked recipe is its LOCKED.
 */
@WailaPlugin
public class AssemblingMachineJadePlugin implements IWailaPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembling_machine");

    private static final String STATUS = "AssemblingStatus";
    private static final String PRODUCT = "AssemblingProduct";
    private static final String UNKNOWN = "AssemblingUnknown";
    private static final String PROGRESS = "AssemblingProgress";
    private static final String DURATION = "AssemblingDuration";
    private static final String ENERGY = "AssemblingEnergy";
    private static final String CAPACITY = "AssemblingCapacity";

    private static final String PROGRESS_KEY = "tooltip.planetaryfactory.assembling_machine.jade.progress";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof AssemblingMachineBlockEntity machine)
                    || !(machine.getLevel() instanceof ServerLevel server)) {
                return;
            }
            tag.putInt(STATUS, machine.status().ordinal());
            tag.putLong(ENERGY, machine.energyStorage.getAmountAsLong());
            tag.putLong(CAPACITY, machine.energyStorage.getCapacityAsLong());
            if (machine.heldRecipe().id().isEmpty()) {
                return;
            }
            AssemblingMachineRecipes.resolve(server, machine.heldRecipe()).ifPresentOrElse(holder -> {
                ItemStack product = holder.value().assemble(null);
                if (!product.isEmpty()) {
                    JadeStacks.put(tag, PRODUCT, product, accessor);
                }
                tag.putInt(PROGRESS, machine.craftProgress());
                tag.putInt(DURATION, machine.craftDuration());
            }, () -> tag.putBoolean(UNKNOWN, true));
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
            if (data.getBooleanOr(UNKNOWN, false)) {
                tooltip.add(Component.translatable("gui.planetaryfactory.assembling_machine.unknown_recipe"));
            } else if (data.contains(DURATION)) {
                ItemStack product = JadeStacks.read(data, PRODUCT, accessor);
                if (product.isEmpty()) {
                    tooltip.add(progress(data));
                } else {
                    JadeLayout.line(tooltip, JadeUI.item(product), product.getHoverName());
                    JadeLayout.appendFigure(tooltip, progress(data));
                    // Padded to "100%", or the tooltip narrows every time the figure loses a digit.
                    var font = Minecraft.getInstance().font;
                    tooltip.append(JadeUI.spacer(Math.max(0, font.width(Component.translatable(PROGRESS_KEY, 100))
                            - font.width(progress(data))), 0));
                }
            }
            AssemblingStatus status = AssemblingStatus.fromOrdinal(data.getIntOr(STATUS, -1));
            tooltip.add(Component.translatable(status.langKey())
                    .withStyle(status.problem() ? ChatFormatting.RED : ChatFormatting.RESET));
            tooltip.add(Component.translatable("gui.planetaryfactory.assembling_machine.energy",
                    String.format("%,d", data.getLongOr(ENERGY, 0L)),
                    String.format("%,d", data.getLongOr(CAPACITY, 0L))));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    private static Component progress(CompoundTag data) {
        int duration = data.getIntOr(DURATION, 0);
        int progress = data.getIntOr(PROGRESS, 0);
        return Component.translatable(PROGRESS_KEY,
                duration <= 0 ? 0 : Math.round(Math.min(progress, duration) * 100F / duration));
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, AssemblingMachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, AssemblingMachineBlock.class);
    }
}
