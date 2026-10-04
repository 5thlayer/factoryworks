package com.factoryworks.core;

import com.factoryworks.core.fluid.BoilerItem;
import com.factoryworks.core.chest.ChestTier;
import com.factoryworks.core.smelting.FurnaceItem;
import com.factoryworks.core.smelting.FurnaceTier;
import com.factoryworks.core.fluid.BarrelFluidHandler;
import com.factoryworks.core.fluid.BarrelItem;
import com.factoryworks.core.fluid.OffshorePumpItem;
import com.factoryworks.core.fluid.BarrelSpec;
import com.factoryworks.core.mining.EngineersPick;
import com.factoryworks.core.mining.PickTier;
import com.factoryworks.core.machine.AssemblingMachineItem;
import com.factoryworks.core.machine.AssemblingTier;
import com.factoryworks.core.machine.footprint.FootprintItem;
import com.factoryworks.core.mining.rig.RigBlockItem;
import com.factoryworks.core.mining.rig.RigTier;
import com.factoryworks.core.radar.RadarItem;
import com.factoryworks.core.oil.PumpjackItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Block items for what {@link PFBlocks} registers.
 *
 * <p>The saplings need one so they can be held, planted by hand and placed by a Create Deployer
 * through the normal use-on path.
 *
 * <p>The barrel is the exception: an item with no block behind it, and the only thing here that is a
 * mechanism rather than a way to hold a block. It is Factorio's barrel (ADR-0037), and it exists in
 * this jar rather than in KubeJS because a fluid capability is not something any scripting API in the
 * pack reaches.
 */
public final class PFItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(FactoryWorksCore.NAMESPACE);

    private static final List<DeferredHolder<Item, ? extends Item>> NATURAL = new ArrayList<>();
    private static final List<DeferredHolder<Item, ? extends Item>> FUNCTIONAL = new ArrayList<>();

    /**
     * Factorio's barrel: 50 mB, stacking to ten, holding any fluid.
     *
     * <p>Both numbers are Factorio's and neither is tunable here -- see {@link BarrelSpec}. Filling
     * and emptying were Create's Spout and Item Drain, natively and with no recipes at all, because
     * both key on the fluid capability this item carries (#93). Create left the pack with ADR-0060;
     * which block inherits that job is #262's to say. The capability
     * is what makes either work, and it is unchanged.
     *
     * <p>{@link BarrelItem}, not a plain {@link Item}: a filled and an empty barrel are otherwise
     * identical in the inventory, and the fluid a barrel carries is not a decoration but the reason
     * to hold one over another.
     */
    public static final DeferredHolder<Item, Item> BARREL = ITEMS.registerItem(
            "barrel", props -> new BarrelItem(props.stacksTo(BarrelSpec.STACK_SIZE)));

    /** Each Assembling Machine tier's item (#326, #295). */
    private static final Map<AssemblingTier, DeferredHolder<Item, AssemblingMachineItem>> ASSEMBLING_MACHINES =
            new EnumMap<>(AssemblingTier.class);

    static {
        for (AssemblingTier tier : AssemblingTier.values()) {
            ASSEMBLING_MACHINES.put(tier, ITEMS.registerItem(tier.blockName(),
                    props -> new AssemblingMachineItem(props, tier)));
        }
    }

    /** The Chemical Plant's item (ADR-0096), on Oritech's own {@code centrifuge} model at its 0.7. */
    public static final DeferredHolder<Item, FootprintItem> CHEMICAL_PLANT = ITEMS.registerItem(
            "chemical_plant",
            props -> new FootprintItem(props, PFBlocks.CHEMICAL_PLANT_FOOTPRINT, 0.7f, "centrifuge"));

    /** The Oil Refinery's item (ADR-0096), on Oritech's own {@code refinery} model at its 0.7. */
    public static final DeferredHolder<Item, FootprintItem> OIL_REFINERY = ITEMS.registerItem(
            "oil_refinery",
            props -> new FootprintItem(props, PFBlocks.OIL_REFINERY_FOOTPRINT, 0.7f, "refinery"));

    /** The Steam Engine's item (ADR-0077), on Oritech's own {@code steam_engine} model at its 0.7. */
    public static final DeferredHolder<Item, FootprintItem> STEAM_ENGINE = ITEMS.registerItem(
            "steam_engine",
            props -> new FootprintItem(props, PFBlocks.STEAM_ENGINE_FOOTPRINT, 0.7f, "steam_engine"));

    public static final DeferredHolder<Item, RadarItem> RADAR = ITEMS.registerItem("radar", RadarItem::new);

    public static final DeferredHolder<Item, BoilerItem> BOILER = ITEMS.registerItem("boiler", BoilerItem::new);

    public static final DeferredHolder<Item, PumpjackItem> PUMPJACK = ITEMS.registerItem("pumpjack", PumpjackItem::new);

    /**
     * The Engineer's Pick, in its two tiers (ADR-0039).
     *
     * <p>In the jar rather than in KubeJS because the answer it gives is
     * {@code isCorrectToolForDrops}, and that is a method on the item -- a block's
     * {@code requires_correct_tool_for_drops} is fixed at registration, so under ADR-0034's sweep,
     * which left the pack with no pickaxe recipe at all, there is nothing a datapack could have
     * said instead. Both are indestructible: no durability component, so no bar and no repair.
     */
    private static final Map<PickTier, DeferredHolder<Item, EngineersPick>> PICKS =
            new EnumMap<>(PickTier.class);

    /**
     * Both rigs' items (#192, ADR-0043). Each is a {@link RigBlockItem}, not
     * {@code registerSimpleBlockItem}: the anchor's default single-block placement would leave the
     * parts behind, so the item is what places the whole footprint in one click. There is no item
     * for the part block -- it is never held, placed, or offered in a creative tab, only ever
     * produced by this item's own placement.
     */
    private static final Map<RigTier, DeferredHolder<Item, RigBlockItem>> RIGS = new EnumMap<>(RigTier.class);

    static {
        for (PickTier tier : PickTier.values()) {
            PICKS.put(tier, ITEMS.registerItem(tier.id(),
                    props -> new EngineersPick(tier, props.stacksTo(1))));
        }
        NATURAL.add(ITEMS.registerSimpleBlockItem(PFBlocks.YUMAKO_SAPLING));
        NATURAL.add(ITEMS.registerSimpleBlockItem(PFBlocks.JELLYSTEM_SAPLING));
        for (FurnaceTier tier : FurnaceTier.values()) {
            FUNCTIONAL.add(ITEMS.registerItem(tier.blockName(),
                    props -> new FurnaceItem(PFBlocks.furnace(tier).get(), props)));
        }
        for (RigTier tier : RigTier.values()) {
            DeferredHolder<Item, RigBlockItem> item = ITEMS.registerItem(tier.blockName(),
                    props -> new RigBlockItem(tier, props));
            RIGS.put(tier, item);
            FUNCTIONAL.add(item);
        }
        FUNCTIONAL.add(BOILER);
        // Not registerSimpleBlockItem: the pump refuses to place away from water, and the refusal
        // is the item's, because by the time a block exists it is too late to decline.
        FUNCTIONAL.add(ITEMS.registerItem("offshore_pump",
                props -> new OffshorePumpItem(props)));
        for (ChestTier tier : ChestTier.values()) {
            FUNCTIONAL.add(ITEMS.registerSimpleBlockItem(PFBlocks.chest(tier)));
        }
        FUNCTIONAL.add(BARREL);
        ASSEMBLING_MACHINES.values().forEach(FUNCTIONAL::add);
        FUNCTIONAL.add(CHEMICAL_PLANT);
        FUNCTIONAL.add(OIL_REFINERY);
        FUNCTIONAL.add(STEAM_ENGINE);
        FUNCTIONAL.add(RADAR);
        FUNCTIONAL.add(PUMPJACK);
        // Tools sit with the machinery, not with the saplings: a pick is the first thing a player
        // reaches for and the last place they would look for it is NATURAL_BLOCKS.
        PICKS.values().forEach(FUNCTIONAL::add);
    }


    private PFItems() {
    }

    public static DeferredHolder<Item, AssemblingMachineItem> assemblingMachine(AssemblingTier tier) {
        return ASSEMBLING_MACHINES.get(tier);
    }

    public static DeferredHolder<Item, RigBlockItem> rig(RigTier tier) {
        return RIGS.get(tier);
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /**
     * The barrel's fluid face.
     *
     * <p>Registered against the item rather than built into it, which is how an {@code ItemStack}
     * capability works in NeoForge: the handler is constructed per stack, over the component that
     * stack carries.
     */
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, itemAccess) -> new BarrelFluidHandler(itemAccess),
                BARREL.get());
    }

    static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            NATURAL.forEach(entry -> event.accept(entry.get()));
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            FUNCTIONAL.forEach(entry -> event.accept(entry.get()));
        }
    }
}
