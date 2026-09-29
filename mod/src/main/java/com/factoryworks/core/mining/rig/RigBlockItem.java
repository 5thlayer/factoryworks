package com.factoryworks.core.mining.rig;

import java.util.List;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.placement.PackRefusal;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.PlansPlacement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

/**
 * One click places all four or nine blocks (#192, ADR-0043): the anchor at the targeted tile and
 * its parts extending away from the player by their horizontal look direction.
 *
 * <p>This entirely replaces {@link BlockItem#place}, rather than overriding {@code getStateForPlacement}
 * on the anchor block, because the default single-block flow has no hook for "and also place these
 * other N blocks" -- the door and bed idiom this mirrors does the same thing in vanilla.
 *
 * <p><b>Placement is refused with nothing consumed where the footprint does not fit.</b> Every
 * target tile is checked before any block is placed and before the stack is shrunk; a partial
 * rig that ate the item on a failed placement is exactly ADR-0043's "bad first machine". Note the
 * vertical extent makes this likelier, not less so: a rig placed under a low ceiling now fails on a
 * tile the player cannot see from above.
 */
public class RigBlockItem extends BlockItem implements PlansPlacement {

    private final RigTier tier;

    public RigBlockItem(RigTier tier, Item.Properties properties) {
        super(PFBlocks.rig(tier).get(), properties);
        this.tier = tier;
    }

    /**
     * The rig's plan (#297, ADR-0069): the whole footprint, at the anchor's facing, refusing as one.
     *
     * <p><b>One blocked position refuses the lot.</b> Drawing one part red and the rest translucent
     * would promise a partial placement the game never performs -- {@link #place} is all-or-nothing
     * and always has been, because a partial rig that ate the item is ADR-0043's "bad first
     * machine". The refused plan still carries every position, so the whole footprint draws red,
     * which is also the only way the player sees the part they cannot see: the vertical extent
     * means a rig under a low ceiling fails on a block above their line of sight.
     */
    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos anchorPos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection();
        RigFacing rigFacing = RigDirections.toRigFacing(facing);
        RigCorpus.Row row = RigCorpus.get().rowOf(tier);
        List<RigGeometry.Offset> offsets = RigGeometry.footprint(
                row.width(), row.height(), tier.blocksTall(), rigFacing);

        BlockState anchorState = PFBlocks.rig(tier).get().defaultBlockState()
                .setValue(RigBlock.FACING, facing);
        BlockState partState = PFBlocks.rigPart(tier).get().defaultBlockState()
                .setValue(RigPartBlock.FACING, facing);

        List<PlacementPlan.Placed> blocks = new ArrayList<>(offsets.size());
        boolean fits = true;
        for (RigGeometry.Offset offset : offsets) {
            BlockPos pos = anchorPos.offset(offset.dx(), offset.dy(), offset.dz());
            boolean anchor = offset.dx() == 0 && offset.dy() == 0 && offset.dz() == 0;
            blocks.add(new PlacementPlan.Placed(pos, anchor ? anchorState : partState));
            if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced()) {
                fits = false;
            }
        }
        return fits
                ? PlacementPlan.accepted(blocks)
                : PlacementPlan.refused(blocks, PackRefusal.FOOTPRINT_BLOCKED);
    }

    /**
     * Placing <em>executes</em> the plan (ADR-0069), so the preview and the click cannot disagree
     * about where the footprint lands, which way it faces or whether it fits.
     */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        PlacementPlan plan = Placements.planFor(this, context);
        if (plan == null || plan.isRefused()) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        // The anchor is always the plan's first block -- RigGeometry.footprint puts it at index 0,
        // deliberately, and every part is told where it is.
        PlacementPlan.Placed anchor = plan.blocks().getFirst();
        BlockPos anchorPos = anchor.pos();

        level.setBlock(anchorPos, anchor.state(), Block.UPDATE_ALL);
        for (PlacementPlan.Placed placed : plan.blocks().subList(1, plan.blocks().size())) {
            level.setBlock(placed.pos(), placed.state(), Block.UPDATE_ALL);
            if (level.getBlockEntity(placed.pos()) instanceof RigPartBlockEntity part) {
                part.setAnchorPos(anchorPos);
            }
        }

        Player player = context.getPlayer();
        SoundType sound = anchor.state().getSoundType();
        level.playSound(player, anchorPos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, anchorPos, GameEvent.Context.of(player, anchor.state()));

        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
