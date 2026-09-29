package com.factoryworks.core.smelting;

import javax.annotation.Nullable;

import com.factoryworks.core.PFBlockEntities;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import com.factoryworks.core.placement.PackRefusal;
import com.factoryworks.core.placement.ReplaceHandoff;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;

/**
 * One of Factorio's three furnaces (#155). The tier is the block's, so the three ids point at one
 * block entity and one behaviour.
 *
 * <p>{@code FACING} is for the front texture and nothing else -- the inventory is unsided, so
 * which way the block looks never changes what an inserter or a funnel gets. {@code LIT} drives
 * the lit front and the glow, on all three tiers: the Electric Furnace lights when it is running,
 * which is the only thing distinguishing "powered and working" from "waiting for the pole".
 */
public class FurnaceBlock extends BaseEntityBlock {

    private static final String NO_ROOM_KEY = "message.factoryworks.replace.no_room";

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /**
     * A block codec is only ever read by data generation, which this pack does not run -- but
     * {@link BaseEntityBlock} makes it abstract, so it carries the one field that distinguishes
     * the three registrations rather than a stand-in tier that would be wrong for two of them.
     */
    public static final com.mojang.serialization.MapCodec<FurnaceBlock> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                    com.mojang.serialization.Codec.STRING
                            .xmap(FurnaceTier::byName, FurnaceTier::serializedName)
                            .fieldOf("tier")
                            .forGetter(FurnaceBlock::tier),
                    // 26.1 carries the block's id on its Properties, so a block codec has to state
                    // them rather than conjure a fresh set -- `propertiesCodec()` is vanilla's own
                    // half of that pair.
                    propertiesCodec()
            ).apply(instance, FurnaceBlock::new));

    private final FurnaceTier tier;

    public FurnaceBlock(FurnaceTier tier, BlockBehaviour.Properties props) {
        super(props
                .mapColor(MapColor.STONE)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(LIT) ? 13 : 0));
        this.tier = tier;
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(LIT, false));
    }

    public FurnaceTier tier() {
        return tier;
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurnaceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, PFBlockEntities.FURNACE.get(),
                (tickLevel, pos, tickState, entity) -> entity.serverTick());
    }

    /**
     * A furnace of another tier in the same Replace Group swaps this one in place, executing the
     * plan the preview drew (ADR-0082); anything else falls through to the screen.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FurnaceItem item)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        PlacementPlan plan = Placements.planFor(item, new BlockPlaceContext(level, player, hand, stack, hit));
        if (plan == null || !plan.isReplace()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        // CONSUME rather than FAIL: a failed use falls through to the item, which would place beside (ADR-0082).
        if (plan.isRefused()) {
            if (plan.refusal() == PackRefusal.NO_ROOM_TO_RETURN && player instanceof ServerPlayer server) {
                server.sendSystemMessage(Component.translatable(NO_ROOM_KEY), true);
            }
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof FurnaceBlockEntity old)) {
            return InteractionResult.SUCCESS;
        }
        PlacementPlan.Placed placed = plan.blocks().getFirst();
        FurnaceBlockEntity.Handover handover = old.handOver(item.tier());
        old.clearContent();
        level.setBlock(pos, placed.state(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof FurnaceBlockEntity fresh) {
            fresh.receive(handover);
        }
        ReplaceHandoff.execute(player, hand, new ItemStack(state.getBlock()), handover.extras());
        SoundType sound = placed.state().getSoundType(level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FurnaceBlockEntity furnace) {
            // The tier travels with the opening packet: the client has no block entity to ask, and
            // the tier is what decides whether the screen has a fuel slot or an energy bar.
            player.openMenu(furnace, buf -> buf.writeEnum(furnace.tier()));
        }
        return InteractionResult.CONSUME;
    }

    /** A broken furnace pays back what it held. Nothing here is a resource sink (ADR-0041). */
}
