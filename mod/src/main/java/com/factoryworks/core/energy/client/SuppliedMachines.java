package com.factoryworks.core.energy.client;

import java.util.ArrayList;
import java.util.List;

import com.factoryworks.core.energy.PoleTier;
import com.factoryworks.core.energy.SupplyAreaScan;
import com.factoryworks.core.energy.SupplyScan;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Which machines a pole reaches, for the Supply Area Box to outline (#158, ADR-0070), cached.
 *
 * <p>The answer is {@link SupplyAreaScan}'s -- the same scan the pole runs to build its network, so
 * the outlines cannot claim a machine the network would not feed, or miss one it would.
 *
 * <h2>Why this is cached rather than asked per frame</h2>
 *
 * <p>A substation's area is 18x18x5 = 1620 blocks and the scan is a capability lookup per block.
 * That is the cost the pole's own scan already refuses to pay every tick, and a renderer pays it
 * sixty times a second rather than twenty. So the same answer is kept for
 * {@link #RESCAN_INTERVAL} -- the pole's own interval, for the pole's own reason: machines do not
 * appear and vanish every tick, and a newly placed one waits at most two seconds to be outlined.
 *
 * <p>The cache holds one entry, because only one box is ever drawn in a frame. A held pole that
 * would extend a column draws no box from the hand (the column's own renderer draws it), a pole
 * aimed at a wrong-tier pole is refused, and a placed pole draws only while looked at -- so the two
 * call sites cannot both want a different base at once. If that ever stops being true this becomes
 * a thrashing single-entry cache rather than a wrong one.
 */
public final class SuppliedMachines {

    /** Ticks between rescans. The pole's own {@code RESCAN_INTERVAL}: two seconds. */
    private static final int RESCAN_INTERVAL = 40;

    private static @Nullable Key key;
    private static long scannedAt = Long.MIN_VALUE;
    private static List<BlockPos> reached = List.of();

    private SuppliedMachines() {
    }

    private record Key(BlockPos base, PoleTier tier, ResourceKey<Level> dimension) {
    }

    /**
     * Every block a pole of this tier based here reaches, in no particular order.
     *
     * <p>All three roles together: a consumer, a generator and an accumulator are all things this
     * pole is connected to, and telling them apart is the Jade line's job rather than a second
     * colour's (ADR-0070 keeps the box one colour). An owner standing <em>outside</em> the area is
     * included where it stands -- a slave Steam Engine's master is what the network actually draws,
     * so an outline beyond the box is the truth about the row rather than a leak.
     */
    public static List<BlockPos> around(Level level, BlockPos base, PoleTier tier) {
        Key now = new Key(base.immutable(), tier, level.dimension());
        long time = level.getGameTime();
        if (!now.equals(key) || time - scannedAt >= RESCAN_INTERVAL || time < scannedAt) {
            SupplyScan.Roles<BlockPos> roles = SupplyAreaScan.of(level, base, tier);
            List<BlockPos> found = new ArrayList<>(roles.consumers());
            found.addAll(roles.generators());
            found.addAll(roles.accumulators());
            key = now;
            scannedAt = time;
            reached = List.copyOf(found);
        }
        return reached;
    }

    /** Dropped on level unload, so a cached answer never outlives the world it was read from. */
    public static void clear() {
        key = null;
        scannedAt = Long.MIN_VALUE;
        reached = List.of();
    }
}
