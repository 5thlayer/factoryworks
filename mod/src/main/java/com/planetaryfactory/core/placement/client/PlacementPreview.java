package com.planetaryfactory.core.placement.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.planetaryfactory.core.energy.PoleColumn;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlock;
import com.planetaryfactory.core.energy.client.SupplyAreaBox;
import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.Placements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

/**
 * Factorio's build preview (#297, ADR-0069): the block a held item would place, drawn translucent
 * where it would land and red where placing would be refused.
 *
 * <p>It draws a {@link PlacementPlan} and decides nothing itself. That is the whole point of
 * ADR-0069 -- the renderer asks the same question the click does, so the preview cannot promise a
 * placement the game will not perform. Where there is no plan there is nothing drawn, which is most
 * of vanilla's refusals: the ray hits a block and placement simply picks another spot.
 *
 * <p>Always on while a previewable item is in the main hand and the aim hits a block in reach, with
 * no keybind and no toggle. Vanilla's white outline is left alone -- it marks what the player is
 * aiming at, which is still true.
 *
 * <h2>Why this hook</h2>
 *
 * <p>ADR-0069 names {@code RenderLevelStageEvent.AfterTranslucentBlocks}, which is the right
 * <em>place</em> but no longer the right <em>door</em>: 26.1 moved level rendering behind the
 * submit-node collector, and that event hands out no collector. {@link SubmitCustomGeometryEvent}
 * is NeoForge's own answer -- "to submit custom geometry, use this instead" -- and the translucent
 * render type still sorts the quads into the translucent pass. It is still no mixin, which is what
 * the decision was actually about.
 *
 * <h2>The cache</h2>
 *
 * <p>A rig's plan is a few hundred block reads and this runs every frame, so the plan is kept until
 * the item, the aimed position, the hit face or the player's horizontal facing changes -- the four
 * things a plan is a function of. A world edit under a still cursor is a stale frame and resolves
 * on the next change; a preview is not authoritative and the click re-asks on the server.
 */
public final class PlacementPreview {

    /** How solid the preview is. Enough to read the block's own texture, thin enough to see through. */
    private static final int ALPHA = 0x80;

    /** White: the block's own colours, just faint. */
    private static final int ACCEPTED_TINT = (ALPHA << 24) | 0xFFFFFF;

    /** Red: any reason placing would be refused, the pack's and vanilla's alike. */
    private static final int REFUSED_TINT = (ALPHA << 24) | 0xFF4040;

    private static @Nullable Key key;
    private static @Nullable PlacementPlan cached;

    private PlacementPreview() {
    }

    /** The four things a plan is a function of, so the cache turns over exactly when it must. */
    private record Key(ItemStack stack, BlockPos aimed, Direction face, Direction facing) {
    }

    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            key = null;
            cached = null;
            return;
        }
        ItemStack stack = player.getMainHandItem();
        Key now = new Key(stack, hit.getBlockPos(), hit.getDirection(), player.getDirection());
        if (key == null || !sameKey(key, now)) {
            key = now;
            cached = Placements.planFor(level, player, InteractionHand.MAIN_HAND, stack, hit);
        }
        PlacementPlan plan = cached;
        if (plan == null || plan.blocks().isEmpty()) {
            return;
        }
        draw(event, level, plan);
        drawSupplyArea(event, level, plan);
    }

    /**
     * The Supply Area Box for a held pole (#158, ADR-0070), drawn on top of the block itself.
     *
     * <p>Anchored at the <em>base</em> of the column the pole would belong to, not at the plan's own
     * position. For an extension the plan names the new <em>top</em> segment, so a box drawn there
     * would be wrong by the column's height -- the area is measured at the base whatever the pole's
     * height (#147).
     *
     * <p><b>The walk down only happens when the placement really is an extension</b>, which is when
     * the block below is that same pole. Asking {@link PoleColumn#baseOf} unconditionally answers
     * about any pole it finds, of any tier, and the aim does not have to land on one to end up above
     * one: a snow layer or a plant sitting on a column's top segment is replaceable, so a small pole
     * aimed at it takes the ordinary vanilla plan and stands on top of a <em>medium</em> column. Its
     * box would then be drawn at that column's base, two blocks below where the pole would actually
     * stand -- a preview lying about the one thing this overlay exists to show.
     *
     * <p>An extension draws nothing here, because the column it joins is a placed pole the player is
     * by definition looking at, and its own renderer is already drawing that exact box. Drawing it
     * again would be two submissions a frame of identical geometry.
     *
     * <p>Only a pole draws one, and only an accepted plan: a refused placement puts nothing down, so
     * there is no area to describe, and the red block already says the placement is refused.
     */
    private static void drawSupplyArea(SubmitCustomGeometryEvent event, ClientLevel level, PlacementPlan plan) {
        if (plan.isRefused()) {
            return;
        }
        for (PlacementPlan.Placed placed : plan.blocks()) {
            if (!(placed.state().getBlock() instanceof SupplyAreaPoleBlock pole)) {
                continue;
            }
            if (level.getBlockState(placed.pos().below()).is(placed.state().getBlock())) {
                return;
            }
            SupplyAreaBox.drawAt(event.getSubmitNodeCollector(), event.getPoseStack(), level,
                    event.getLevelRenderState().cameraRenderState.pos, placed.pos(), pole.tier());
            return;
        }
    }

    /**
     * Stacks are compared by item and components rather than by identity, because the hand's stack
     * object is not stable across frames, and by count deliberately <em>not</em>: placing the last
     * one of a stack must not make the preview flicker on the frame before it is gone.
     */
    private static boolean sameKey(Key a, Key b) {
        return ItemStack.isSameItemSameComponents(a.stack(), b.stack())
                && a.aimed().equals(b.aimed())
                && a.face() == b.face()
                && a.facing() == b.facing();
    }

    private static void draw(SubmitCustomGeometryEvent event, ClientLevel level, PlacementPlan plan) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        int tint = plan.isRefused() ? REFUSED_TINT : ACCEPTED_TINT;
        QuadInstance instance = new QuadInstance();
        instance.setColor(tint);

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        RandomSource random = RandomSource.create();

        for (PlacementPlan.Placed placed : plan.blocks()) {
            BlockPos pos = placed.pos();
            BlockStateModel model = Minecraft.getInstance().getModelManager()
                    .getBlockStateModelSet().get(placed.state());
            List<BlockStateModelPart> parts = new ArrayList<>();
            random.setSeed(placed.state().getSeed(pos));
            // The real position, not BlockPos.ZERO: a model whose parts depend on where it stands
            // must be asked about where it would stand. The getter is empty because the block is
            // not there yet -- a model that reads its neighbours previews unconnected, which is
            // honest, since nothing has connected to it.
            model.collectParts(BlockAndTintGetter.EMPTY, pos, placed.state(), random, parts);
            if (parts.isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x(), pos.getY() - camera.y(), pos.getZ() - camera.z());
            collector.submitCustomGeometry(poseStack,
                    RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
                    (pose, buffer) -> {
                        for (BlockStateModelPart part : parts) {
                            emit(buffer, pose, part.getQuads(null), instance);
                            for (Direction direction : Direction.values()) {
                                emit(buffer, pose, part.getQuads(direction), instance);
                            }
                        }
                    });
            poseStack.popPose();
        }
    }

    private static void emit(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                             List<BakedQuad> quads, QuadInstance instance) {
        for (BakedQuad quad : quads) {
            buffer.putBakedQuad(pose, quad, instance);
        }
    }
}
