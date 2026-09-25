package com.planetaryfactory.core.machine;

import java.util.ArrayList;
import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.machine.footprint.FootprintItem;
import com.planetaryfactory.core.machine.footprint.FootprintMachine;
import com.planetaryfactory.core.machine.footprint.FootprintPartBlock;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import com.planetaryfactory.core.placement.PackRefusal;
import com.planetaryfactory.core.placement.ReplaceGroups;
import com.planetaryfactory.core.placement.ReplaceHandoff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import rearth.oritech.util.ColorableMachine;

/**
 * An Assembling Machine in the hand: its footprint's placement, or a Fast Replace when plainly aimed
 * at any block of a machine of another tier (ADR-0082). Scale 0.7 and the {@code "assembler"} model
 * are Oritech's own {@code BlockContent} arguments; the paint is the tier's (ADR-0075).
 */
public class AssemblingMachineItem extends FootprintItem {

    private static final String NO_ROOM_KEY = "message.planetaryfactory.replace.no_room";

    private final AssemblingTier tier;

    public AssemblingMachineItem(Properties properties, AssemblingTier tier) {
        super(properties, PFBlocks.assemblingFootprint(tier), 0.7f, "assembler",
                ColorableMachine.ColorVariant.valueOf(tier.paint()));
        this.tier = tier;
    }

    public AssemblingTier tier() {
        return tier;
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        if (context.isSecondaryUseActive()) {
            return super.plan(context);
        }
        Level level = context.getLevel();
        BlockPos anchor = anchorAimedAt(level, Placements.aimedPos(context));
        if (anchor == null || !(level.getBlockEntity(anchor) instanceof AssemblingMachineBlockEntity machine)
                || !ReplaceGroups.get().canReplace(id(getBlock()), id(machine.getBlockState().getBlock()))) {
            return super.plan(context);
        }
        Direction facing = machine.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        FootprintMachine footprint = PFBlocks.assemblingFootprint(tier);
        List<BlockPos> positions = footprint.positions(anchor, facing);
        List<PlacementPlan.Placed> blocks = new ArrayList<>(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            blocks.add(new PlacementPlan.Placed(positions.get(i), footprint.stateAt(i, facing)));
        }
        Player player = context.getPlayer();
        // The client cannot resolve the Held recipe, so its plan never counts the inputs a cleared
        // recipe hands back; the server's plan is the one the click executes (ADR-0082).
        boolean fits = player == null || ReplaceHandoff.fits(player, context.getHand(),
                new ItemStack(PFItems.assemblingMachine(machine.tier()).get()), machine.retierExtras(tier));
        return PlacementPlan.replacing(blocks, fits ? null : PackRefusal.NO_ROOM_TO_RETURN);
    }

    /**
     * Executes a replace plan from the anchor's {@code useItemOn}. The block entity is kept across
     * the swap (see {@link AssemblingMachineBlock}), and no block's removal hook runs, since the
     * anchor's would tear the footprint down.
     */
    InteractionResult replace(PlacementPlan plan, Level level, BlockPos anchor, Player player, InteractionHand hand) {
        if (plan.isRefused()) {
            if (plan.refusal() == PackRefusal.NO_ROOM_TO_RETURN && player instanceof ServerPlayer server) {
                server.sendSystemMessage(Component.translatable(NO_ROOM_KEY), true);
            }
            // CONSUME rather than FAIL: a failed use falls through to the item, which would place beside (ADR-0082).
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide() || !(level.getBlockEntity(anchor) instanceof AssemblingMachineBlockEntity machine)) {
            return InteractionResult.SUCCESS;
        }
        AssemblingTier from = machine.tier();
        List<ItemStack> extras = machine.retierTo(tier);
        for (PlacementPlan.Placed placed : plan.blocks()) {
            level.setBlock(placed.pos(), placed.state(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
            level.invalidateCapabilities(placed.pos());
        }
        machine.setChanged();
        ReplaceHandoff.execute(player, hand, new ItemStack(PFItems.assemblingMachine(from).get()), extras);
        BlockState state = plan.blocks().getFirst().state();
        SoundType sound = state.getSoundType(level, anchor, player);
        level.playSound(null, anchor, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        return InteractionResult.SUCCESS;
    }

    private static @Nullable BlockPos anchorAimedAt(Level level, BlockPos aimed) {
        BlockState state = level.getBlockState(aimed);
        if (state.getBlock() instanceof AssemblingMachineBlock) {
            return aimed;
        }
        if (state.getBlock() instanceof FootprintPartBlock part) {
            BlockPos anchor = part.machine().anchorOf(aimed, state);
            return level.getBlockState(anchor).getBlock() instanceof AssemblingMachineBlock ? anchor : null;
        }
        return null;
    }

    private static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }
}
