package com.planetaryfactory.core.gametest;

import java.util.Set;
import java.util.UUID;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.radar.RadarBlockEntity;
import com.planetaryfactory.core.radar.RadarChartData;
import com.planetaryfactory.core.radar.Sector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A pole-fed Radar charts its own sector into its owner's chart after 10 MJ, and a starved one
 * charts nothing (#368). The tick figures are typed: 100,000 FE at 150 FE/t is 667 ticks.
 */
final class RadarTests {

    private static final BlockPos FLOOR = new BlockPos(5, 0, 3);
    /** Two blocks past the footprint's edge, where a pole's area reaches its nearest parts. */
    private static final BlockPos POLE = new BlockPos(8, 1, 3);

    private static final int BEFORE_TEN_MEGAJOULES = 640;
    private static final int AFTER_TEN_MEGAJOULES = 720;

    private RadarTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("fed_radar_charts_its_first_sector_after_ten_megajoules", 800, RadarTests::fedRadarCharts);
        tests.test("starved_radar_charts_nothing", 800, RadarTests::starvedRadarChartsNothing);
    }

    private static void fedRadarCharts(GameTestHelper helper) {
        UUID owner = place(helper);
        helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        BlockPos anchor = helper.absolutePos(FLOOR.above());
        helper.startSequence()
                .thenIdle(BEFORE_TEN_MEGAJOULES)
                .thenExecute(() -> {
                    if (!chart(helper, owner).isEmpty()) {
                        helper.fail("charted a sector before 10 MJ reached the radar", FLOOR.above());
                    }
                    if (radar(helper).progress() == 0L) {
                        helper.fail("a pole-fed radar drew nothing", FLOOR.above());
                    }
                })
                .thenIdle(AFTER_TEN_MEGAJOULES - BEFORE_TEN_MEGAJOULES)
                .thenExecute(() -> {
                    Set<Sector> expected = Set.of(Sector.ofBlock(anchor.getX(), anchor.getZ()));
                    if (!chart(helper, owner).equals(expected)) {
                        helper.fail("after 10 MJ the chart holds " + chart(helper, owner)
                                + " where it should hold only the radar's own " + expected, FLOOR.above());
                    }
                })
                .thenSucceed();
    }

    private static void starvedRadarChartsNothing(GameTestHelper helper) {
        UUID owner = place(helper);
        helper.startSequence()
                .thenIdle(AFTER_TEN_MEGAJOULES)
                .thenExecute(() -> {
                    if (!chart(helper, owner).isEmpty() || radar(helper).progress() != 0L) {
                        helper.fail("a radar with no pole charted " + chart(helper, owner)
                                + " and banked " + radar(helper).progress() + " FE", FLOOR.above());
                    }
                })
                .thenSucceed();
    }

    /** Places the Radar with its own item, the way a player does; the player is its owner. */
    private static UUID place(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PFItems.RADAR.get()));
        BlockPos absolute = helper.absolutePos(FLOOR);
        helper.useBlock(FLOOR, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
        if (!player.getUUID().equals(radar(helper).owner())) {
            helper.fail("the placing player does not own the radar", FLOOR.above());
        }
        return player.getUUID();
    }

    private static RadarBlockEntity radar(GameTestHelper helper) {
        if (helper.getLevel().getBlockEntity(helper.absolutePos(FLOOR.above())) instanceof RadarBlockEntity radar) {
            return radar;
        }
        helper.fail("no radar stands on the floor", FLOOR.above());
        throw new IllegalStateException("unreachable");
    }

    private static Set<Sector> chart(GameTestHelper helper, UUID owner) {
        return RadarChartData.get(helper.getLevel().getServer())
                .sectors(owner, helper.getLevel().dimension().identifier().toString());
    }
}
