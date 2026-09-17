package com.planetaryfactory.core.energy;

import com.planetaryfactory.core.PFBlockEntities;
import java.util.List;

import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.Placements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;

/**
 * A Factorio electric pole (ADR-0036, ADR-0062): it supplies every machine standing in its area, and
 * it links to every pole within wire reach into one Electric Network.
 *
 * <p>The network is recomputed from the poles standing rather than stored -- no propagation, no
 * topology persisted across chunk unloads. {@link ElectricNetworks} has the why.
 *
 * <p>One class serves all three tiers; they differ by the {@link PoleTier} handed to the
 * constructor and by nothing else.
 *
 * <h2>The pole is a column</h2>
 *
 * <p>A pole stands as tall as the player builds it, up to {@link PoleColumn#MAX_SEGMENTS}, and the
 * supply area is measured at the base whatever the height. {@link PoleColumn} has the why. This
 * class owns the two world-facing consequences: extending a column and breaking one.
 */
public class SupplyAreaPoleBlock extends Block implements EntityBlock {

    /** A pole is a post, not a cube -- 6/16 square and full height. */
    private static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);

    private final PoleTier tier;

    public SupplyAreaPoleBlock(PoleTier tier, BlockBehaviour.Properties props) {
        super(props
                .strength(1.5F)
                .sound(SoundType.COPPER)
                // No tool requirement: the pack registers no mining-tool tags for its own blocks,
                // and requiring one here would make a pole break to nothing by hand.
                .noOcclusion());
        this.tier = tier;
    }

    public PoleTier tier() {
        return tier;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Right-clicking any segment with this tier's own pole grows the column by one.
     *
     * <p>Clicking <em>any</em> segment rather than only the top is the point: the new block lands on
     * top of the column, so a player standing on the ground raises a pole past their own reach by
     * clicking the base repeatedly. That matters because {@link PoleColumn#MAX_SEGMENTS} is chosen
     * so the top stays reachable -- if extending also demanded reaching the top, the cap would have
     * had to be smaller still.
     *
     * <p><b>The pole in hand is not consumed.</b> A column is one pole however tall it is: its height
     * is a wiring decision, not a cost, and paying a pole per segment was punishing enough in play
     * that poles were left short. Breaking the column pays back the one item it cost, since a
     * segment dropped by the cascade carries no loot.
     *
     * <p>A pole of a <em>different</em> tier does nothing at all, rather than falling through to
     * ordinary placement. Falling through would set a second, separate pole against the side of this
     * column, with its own supply area and its own block entity, and it would look exactly like the
     * extension the player was asking for.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(stack.getItem() instanceof SupplyAreaPoleItem item)) {
            // Includes the empty hand and every other item, which must fall through to their own
            // placement. Removing the top segment bare-handed would be the natural inverse of this,
            // and is deliberately absent: breaking is already how blocks come off, and a bare-hand
            // interaction that deletes part of a build loses substations to misclicks.
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        // The column rule is asked for, not restated (ADR-0069): this executes the same plan the
        // preview draws, so the two cannot disagree about where a segment lands or whether one
        // may. A different tier is refused here rather than falling through, which would set a
        // second, separate pole against the side of this column looking exactly like the extension
        // the player asked for.
        PlacementPlan plan = Placements.planFor(item, new BlockPlaceContext(level, player, hand, stack, hit));
        if (plan == null || plan.isRefused() || plan.blocks().size() != 1) {
            return InteractionResult.CONSUME;
        }
        PlacementPlan.Placed segment = plan.blocks().getFirst();

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        level.setBlockAndUpdate(segment.pos(), segment.state());
        level.playSound(null, segment.pos(), getSoundType(state, level, segment.pos(), player).getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        // The item is not consumed: a column is one pole, however tall, and height is a wiring
        // decision rather than a cost (ADR-0036). Paying a pole per segment made raising one
        // punishing enough that players left poles short. The other half of "one pole" is the
        // teardown below: a broken column pays out exactly the one item it cost.
        return InteractionResult.SUCCESS;
    }

    /**
     * Breaking any segment drops the column above it.
     *
     * <p>Chains and scaffolding both do this, so the muscle memory is already there,
     * and the alternative -- leaving segments floating where their base was -- is a lie about a
     * structure the player thinks of as one object. Recursion is via {@code destroyBlock}, which
     * re-enters here for the block above, so the column unwinds one segment at a time.
     */
    /** A placed base wires itself (ADR-0068). */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                           boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            LevelWires.of(server).placed(server, pos);
        }
    }

    /**
     * Only a base drops a pole: a column is one item however tall (ADR-0036).
     *
     * <p>Without this, extending for free and breaking the top segment back off would be a pole
     * duplicator, and it would look like ordinary play rather than an exploit. The loot table is the
     * same one either way; what changes is whether it is asked at all.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockPos pos = BlockPos.containing(params.getOptionalParameter(LootContextParams.ORIGIN));
        if (params.getLevel().getBlockState(pos.below()).is(this)) {
            return List.of();
        }
        return super.getDrops(state, params);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        // A base's wires go with it; an extension holds none (ADR-0068).
        if (!level.getBlockState(pos.below()).is(this)) {
            LevelWires.of(level).broken(level, pos);
        }
        // 26.1 calls this only when the block is genuinely gone, so the old
        // `!state.is(newState.getBlock())` guard is the caller's job now.
        BlockPos above = pos.above();
        if (level.getBlockState(above).is(this)) {
            // Dropped without loot: extending a column costs nothing, so a segment must pay nothing
            // back, or a tall pole broken is a pole duplicator. The block the player actually broke
            // pays out through its own loot table, which is the single item the column cost.
            level.destroyBlock(above, false);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyAreaPoleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        // Server only: the pole has nothing to animate, and pushing energy on the client would be
        // pushing it into a copy of the world.
        if (level.isClientSide() || type != PFBlockEntities.SUPPLY_AREA_POLE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            // Only the base's block entity is ticked. An extension is given one by Minecraft --
            // a block entity is built from the blockstate, which cannot see the block below -- and
            // it stays inert. This is the gate that makes a five-tall pole cost what a one-tall
            // pole costs.
            if (be instanceof SupplyAreaPoleBlockEntity pole && PoleColumn.isBase(lvl, pos)) {
                pole.serverTick();
            }
        };
    }
}
