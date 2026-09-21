package com.planetaryfactory.core.machine.footprint;

import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.Placements;
import com.planetaryfactory.core.placement.PlansPlacement;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;
import rearth.oritech.item.OritechGeoItem;
import rearth.oritech.util.ColorableMachine;

/**
 * One click places a whole {@link FootprintMachine} (ADR-0069), or nothing at all.
 *
 * <p>An {@link OritechGeoItem}, so the item in the hand is the Oritech model GeckoLib draws, with
 * the scale and default colour Oritech's own {@code BlockContent} gives that model, read off the
 * jar.
 *
 * <p>Replaces {@code BlockItem.place} whole, for the reason {@code RigBlockItem} does: the
 * single-block flow has no hook for the other blocks, and a partial machine that ate the item is
 * the failure every footprint refuses.
 */
public class FootprintItem extends OritechGeoItem implements PlansPlacement {

    private final FootprintMachine machine;

    public FootprintItem(Properties properties, FootprintMachine machine, float scale, String model) {
        super(machine.anchor().get(), properties, scale, model, ColorableMachine.ColorVariant.ORANGE);
        this.machine = machine;
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        return machine.plan(context);
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
}
