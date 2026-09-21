package com.planetaryfactory.core.machine;

import java.util.ArrayList;
import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.Placements;
import com.planetaryfactory.core.placement.PlansPlacement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.item.OritechGeoItem;
import rearth.oritech.util.ColorableMachine;

/**
 * One click places the whole Assembling Machine (#326, ADR-0069): the anchor at the aimed tile and
 * seven parts beside and above it, or nothing at all.
 *
 * <p>Extends Oritech's {@link OritechGeoItem} with the name {@code "assembler"}, so the item in the
 * hand and the inventory is Oritech's assembler model drawn by GeckoLib -- the same arguments
 * Oritech's own {@code BlockContent} passes it, read off the jar: scale 0.7 and the annotation's
 * default colour, orange.
 *
 * <p>This replaces {@code BlockItem.place} whole, for the reason {@code RigBlockItem} does: the
 * single-block flow has no hook for "and these seven others", and a partial machine that ate the
 * item is the failure every footprint refuses.
 */
public class AssemblingMachineItem extends OritechGeoItem implements PlansPlacement {

    public AssemblingMachineItem(Properties properties) {
        super(PFBlocks.ASSEMBLING_MACHINE.get(), properties, 0.7f, "assembler",
                ColorableMachine.ColorVariant.ORANGE);
    }

    /**
     * The whole footprint at the facing Oritech's {@code MachineBlock} would give the anchor --
     * towards the player -- refusing as one if any position is taken or out of the world.
     */
    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return null;
        }
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection().getOpposite();
        List<BlockPos> positions = AssemblingMachineHull.positions(context.getClickedPos(), facing);

        List<PlacementPlan.Placed> blocks = new ArrayList<>(positions.size());
        boolean fits = true;
        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = positions.get(i);
            blocks.add(new PlacementPlan.Placed(pos, stateAt(i, facing)));
            // isUnobstructed is vanilla's own entity check: without it the footprint closes
            // around a player or a mob standing in it.
            if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced()
                    || !level.isUnobstructed(stateAt(i, facing), pos,
                            net.minecraft.world.phys.shapes.CollisionContext.empty())) {
                fits = false;
            }
        }
        return fits
                ? PlacementPlan.accepted(blocks)
                : PlacementPlan.refused(blocks, PlacementPlan.Refusal.FOOTPRINT_BLOCKED);
    }

    /** Placing executes the plan (ADR-0069), so the preview and the click cannot disagree. */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        PlacementPlan plan = Placements.planFor(this, context);
        if (plan == null || plan.isRefused()) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        for (PlacementPlan.Placed placed : plan.blocks()) {
            level.setBlock(placed.pos(), placed.state(), Block.UPDATE_ALL);
        }

        PlacementPlan.Placed anchor = plan.blocks().getFirst();
        Player player = context.getPlayer();
        SoundType sound = anchor.state().getSoundType();
        level.playSound(player, anchor.pos(), sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, anchor.pos(), GameEvent.Context.of(player, anchor.state()));

        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }

    /**
     * The anchor is placed already assembled, which is what makes Oritech's multiblock paths return
     * early rather than scan for cores the pack never asks the player to place. Public for the
     * GameTests, which stand a whole machine up without a player's click.
     */
    public static BlockState stateAt(int index, Direction facing) {
        if (index == 0) {
            return PFBlocks.ASSEMBLING_MACHINE.get().defaultBlockState()
                    .setValue(AssemblingMachineBlock.FACING, facing)
                    .setValue(MultiblockMachine.ASSEMBLED, true);
        }
        return PFBlocks.ASSEMBLING_MACHINE_PART.get().defaultBlockState()
                .setValue(AssemblingMachinePartBlock.FACING, facing)
                .setValue(AssemblingMachinePartBlock.PART, index);
    }
}
