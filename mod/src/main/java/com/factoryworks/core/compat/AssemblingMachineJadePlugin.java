package com.factoryworks.core.compat;

import net.neoforged.neoforge.transfer.fluid.FluidResource;
import snownee.jade.api.fluid.JadeFluidObject;
import com.factoryworks.core.FactoryWorksCore;
import com.factoryworks.core.machine.ChassisMachineBlock;
import com.factoryworks.core.machine.AssemblingMachineBlockEntity;
import com.factoryworks.core.machine.AssemblingMachineRecipes;
import com.factoryworks.core.machine.AssemblingStatus;
import com.factoryworks.core.machine.AssemblingStatusText;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
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
 * The Assembling Machine's Held recipe, status, progress and tank on the HUD (#333, #295). The status
 * is {@link AssemblingMachineBlockEntity#status}, the screen's rule; a locked recipe is its LOCKED.
 */
@WailaPlugin
public class AssemblingMachineJadePlugin implements IWailaPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, "assembling_machine");

    private static final String STATUS = "AssemblingStatus";
    private static final String PRODUCT = "AssemblingProduct";
    private static final String NAME = "AssemblingName";
    private static final String OUTPUT_FLUID = "AssemblingOutputFluid";
    private static final String UNKNOWN = "AssemblingUnknown";
    private static final String PROGRESS = "AssemblingProgress";
    private static final String DURATION = "AssemblingDuration";
    private static final String FLUID = "AssemblingFluid";
    private static final String TANK = "AssemblingTank";
    private static final String TANK_CAPACITY = "AssemblingTankCapacity";

    private static final String PROGRESS_KEY = "tooltip.factoryworks.assembling_machine.jade.progress";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof AssemblingMachineBlockEntity machine)
                    || !(machine.getLevel() instanceof ServerLevel server)) {
                return;
            }
            tag.putInt(STATUS, machine.status().ordinal());
            if (!machine.spec().fluidInputs().isEmpty()) {
                tag.putLong(TANK, machine.tank().getAmountAsLong(0));
                tag.putInt(TANK_CAPACITY, machine.spec().fluidInputVolume(0));
            }
            machine.heldFluid().ifPresent(fluid -> tag.putString(FLUID, BuiltInRegistries.FLUID.getKey(fluid).toString()));
            if (machine.heldRecipe().id().isEmpty()) {
                return;
            }
            AssemblingMachineRecipes.resolve(server, machine.heldRecipe(), machine.spec()).ifPresentOrElse(holder -> {
                ItemStack product = holder.value().assemble(null);
                if (!product.isEmpty()) {
                    JadeStacks.put(tag, PRODUCT, product, accessor);
                }
                JadeStacks.putComponent(tag, NAME, AssemblingMachineRecipes.name(holder), accessor);
                holder.value().fluidResults().stream().findFirst().ifPresent(result -> tag.putString(OUTPUT_FLUID,
                        BuiltInRegistries.FLUID.getKey(FluidResource.of(result).getFluid()).toString()));
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
                tooltip.add(Component.translatable("gui.factoryworks.assembling_machine.unknown_recipe"));
            } else if (data.contains(DURATION)) {
                ItemStack product = JadeStacks.read(data, PRODUCT, accessor);
                Optional<Component> name = JadeStacks.readComponent(data, NAME, accessor);
                if (name.isEmpty()) {
                    tooltip.add(progress(data));
                } else {
                    Optional<Fluid> outputFluid = data.getString(OUTPUT_FLUID).map(Identifier::tryParse)
                            .flatMap(BuiltInRegistries.FLUID::getOptional);
                    if (!product.isEmpty()) {
                        JadeLayout.line(tooltip, JadeUI.item(product), name.get());
                    } else if (outputFluid.isPresent()) {
                        JadeLayout.line(tooltip, JadeUI.fluid(JadeFluidObject.of(outputFluid.get())).size(16, 16), name.get());
                    } else {
                        tooltip.add(name.get());
                    }
                    JadeLayout.appendFigure(tooltip, progress(data));
                    // Padded to "100%", or the tooltip narrows every time the figure loses a digit.
                    var font = Minecraft.getInstance().font;
                    tooltip.append(JadeUI.spacer(Math.max(0, font.width(Component.translatable(PROGRESS_KEY, 100))
                            - font.width(progress(data))), 0));
                }
            }
            Optional<Fluid> fluid = data.getString(FLUID).map(Identifier::tryParse)
                    .flatMap(BuiltInRegistries.FLUID::getOptional);
            AssemblingStatus status = AssemblingStatus.fromOrdinal(data.getIntOr(STATUS, -1));
            tooltip.add(AssemblingStatusText.of(status, fluid).copy()
                    .withStyle(status.problem() ? ChatFormatting.RED : ChatFormatting.RESET));
            if (data.contains(TANK_CAPACITY)) {
                tooltip.add(AssemblingStatusText.tank(fluid, data.getLongOr(TANK, 0L), data.getIntOr(TANK_CAPACITY, 0)));
            }
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
        registration.registerBlockComponent(TOOLTIP, ChassisMachineBlock.class);
    }
}
