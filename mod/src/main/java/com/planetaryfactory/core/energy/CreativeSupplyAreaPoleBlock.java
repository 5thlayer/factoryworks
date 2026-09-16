package com.planetaryfactory.core.energy;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A supply-area pole that is also an unlimited generator for its network: the dev tool an energy face is checked with
 * (#272).
 *
 * <p>Place it, place a machine beside it, read Jade. Without it, checking "does the Electric
 * Furnace draw 90 FE/t" by hand means first building a working power chain, because the shipped
 * pole generates nothing -- so a two-block test would depend on a working steam chain.
 *
 * <h2>It is a subclass, not a fourth {@link PoleTier}</h2>
 *
 * <p>{@code PoleTier} is a footprint ladder taken from Factorio's own prototypes (ADR-0036,
 * ADR-0028's naming rule), and three places walk {@code PoleTier.values()}. A creative row in it
 * would put a non-Factorio entry into a corpus-derived ladder and drag it into the item map, the
 * recipe sweep and {@code docs/factorio-mechanics.md}, none of which have anything to say about a
 * dev tool. So this is a separate block wearing the substation's footprint -- the 18x18 is the
 * useful one for testing, since a machine can go anywhere nearby rather than having to touch the
 * pole.
 *
 * <p>Everything else is inherited and stays shared: the column, the scan, the water-fill
 * rationing, the Jade line and the network it links into. What is being tested has to be the thing
 * that ships, so the only difference is that {@link ElectricNetworks} counts it as a generator --
 * and that is decided from the blockstate rather than from here, because Minecraft rebuilds a block
 * entity from the type when a chunk loads and never asks the block again.
 *
 * <h2>It ships</h2>
 *
 * <p>Creative tab and {@code /give}, with no recipe emitted and none admitted by the sweep, which
 * is what keeps it out of survival -- the same arrangement vanilla's creative-only blocks have. It
 * is not gated behind a dev flag: a gate is another dial with its own failure mode, and the thing
 * it would protect against is a player who has already opened the creative menu.
 */
public class CreativeSupplyAreaPoleBlock extends SupplyAreaPoleBlock {

    /** The registry path. Not derived from a tier -- it is not on the ladder. */
    public static final String BLOCK_NAME = "creative_electric_pole";

    public CreativeSupplyAreaPoleBlock(BlockBehaviour.Properties props) {
        super(PoleTier.SUBSTATION, props);
    }
}
