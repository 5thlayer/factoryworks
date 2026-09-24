package com.planetaryfactory.core;

import com.mojang.serialization.Codec;
import com.planetaryfactory.core.placement.QuarterTurn;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components, which currently means the one the barrel carries its fluid in.
 *
 * <p>NeoForge supplies the content type and both codecs; what it does not supply is a registered
 * component to hang them on, so every mod with a fluid-holding item registers its own. This is that
 * one item's, and it is the whole reason a single barrel can stand in for Factorio's nine distinct
 * filled-barrel items (#93).
 */
public final class PFDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>>
            FLUID_CONTENT = DATA_COMPONENTS.register("fluid_content",
                    () -> DataComponentType.<SimpleFluidContent>builder()
                            .persistent(SimpleFluidContent.CODEC)
                            .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                            .build());

    /**
     * The first end of a wire the Engineer's Pick is holding: the anchor pole's base and its
     * dimension (ADR-0068). On the stack, so it survives a relog and a client can draw from it.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>>
            PENDING_WIRE = DATA_COMPONENTS.register("pending_wire",
                    () -> DataComponentType.<GlobalPos>builder()
                            .persistent(GlobalPos.CODEC)
                            .networkSynchronized(GlobalPos.STREAM_CODEC)
                            .build());

    /** A Dismantle's stored start (ADR-0086). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>>
            DISMANTLE_START = DATA_COMPONENTS.register("dismantle_start",
                    () -> DataComponentType.<GlobalPos>builder()
                            .persistent(GlobalPos.CODEC)
                            .networkSynchronized(GlobalPos.STREAM_CODEC)
                            .build());

    /**
     * How far the held stack's placement is turned from the look (ADR-0083). Absent means no turn,
     * and a turn back to none removes it, so a turned stack stacks again with an unturned one.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<QuarterTurn>>
            QUARTER_TURN = DATA_COMPONENTS.register("quarter_turn",
                    () -> DataComponentType.<QuarterTurn>builder()
                            .persistent(Codec.intRange(0, 3).xmap(QuarterTurn::new, QuarterTurn::quarters))
                            .networkSynchronized(ByteBufCodecs.VAR_INT.map(QuarterTurn::of, QuarterTurn::quarters))
                            .build());

    private PFDataComponents() {
    }

    static void register(IEventBus modBus) {
        DATA_COMPONENTS.register(modBus);
    }
}
