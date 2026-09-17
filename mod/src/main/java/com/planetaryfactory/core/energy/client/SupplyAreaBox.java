package com.planetaryfactory.core.energy.client;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;

import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyArea;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import org.joml.Vector3f;

/**
 * The Supply Area Box (#158, ADR-0070): a pole's supply area drawn as the volume it covers.
 *
 * <p>A pole's area used to be invisible, and #147 answered that with text, which states the
 * footprint without saying where it lands. This is the third thing the Placement Preview shows,
 * after ADR-0069's translucent block and #298's wires.
 *
 * <h2>Why a volume and not a surface</h2>
 *
 * <p>The ticket was first written as a 2D overlay draped per column, resting on the highest block of
 * each. It cannot be made honest. A column with nothing solid inside the band gets no quad, so the
 * sheet has a hole wherever terrain falls away -- and a hole reads as "the area stops here", which
 * is false: a platform built out over that drop is inside the band and would be powered. The
 * converse is no better: a column covering two levels at once, a machine on a platform and a floor
 * below it, can only draw the upper, leaving a powered floor invisible. Minecraft is three
 * dimensional and the area is a three dimensional region, so the box is drawn as one.
 *
 * <h2>One builder, two call sites</h2>
 *
 * <p>{@link #offsets} is the whole geometry, and it is a function of the tier alone -- nothing here
 * reads the world. The held item's preview and the placed pole's renderer both draw what it
 * returns, so they cannot disagree about where the area is, which is ADR-0069's rule applied to the
 * overlay. The bounds come from {@link SupplyArea#bounds}: the substation's even-sided area takes
 * its extra block on the negative side, and a renderer that centred the box instead would be wrong
 * by half a block on the one tier where it shows.
 *
 * <h2>Edges only, and drawn through terrain</h2>
 *
 * <p>No fill. The player is almost always <em>inside</em> the volume -- the band is two blocks
 * either way and they stand on the ground the pole does -- so a fill would be a full-screen colour
 * wash over exactly the machines being positioned, and an 18-wide substation seen from outside
 * would be a wall between the player and their own base. Vanilla draws the structure block's
 * bounding box as edges only at the same scale, for the same reason.
 *
 * <p>The edges ignore depth. The box's lower edges sit two blocks below the base, so on flat ground
 * they are inside the terrain; depth-tested, the box reads as an open-bottomed cage and the ground
 * extent -- the thing the overlay exists to show -- is the part that goes missing. No stock line
 * type passes depth unconditionally, so {@link #PIPELINE} is vanilla's own {@code LINES_SNIPPET}
 * with {@link CompareOp#ALWAYS_PASS} and no depth write. It costs nothing extra: the same twelve
 * segments and the same arithmetic, with the test off.
 */
public final class SupplyAreaBox {

    /**
     * Bright yellow, Factorio's own electric-network colour, and the one tint no other pole gesture
     * uses: green, orange, red and brown are the Engineer's Pick's wire outcomes (ADR-0068) and
     * white and red are the placement preview's accepted and refused.
     *
     * <p><b>One state, never changing.</b> It does not turn red on a refused placement -- the red
     * block already says that -- and it must not come to react to network load when #157 lands. The
     * box describes an area, not an outcome; whether machines in it are being fed is the Jade line's
     * answer, on the machine.
     */
    private static final int COLOUR = 0xFFFFE04C;

    /** Vanilla's own line width for a world-space outline. */
    private static final float WIDTH = 2.0F;

    /**
     * Vanilla's line pipeline with the depth test passed unconditionally and no depth write.
     *
     * <p>Registered through NeoForge's {@link RegisterRenderPipelinesEvent} rather than built inline:
     * a pipeline has to be known before it is first used, and an unregistered one is a crash on the
     * frame the box is first drawn rather than at startup.
     */
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation("pipeline/planetaryfactory_supply_area_box")
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();

    private static final RenderType RENDER_TYPE = RenderType.create(
            "planetaryfactory_supply_area_box",
            RenderSetup.builder(PIPELINE).createRenderSetup());

    private SupplyAreaBox() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SupplyAreaBox::onRegisterPipelines);
    }

    private static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }

    /**
     * The box a pole of this tier covers, as offsets from its base.
     *
     * <p>The bounds are inclusive block offsets, so the far faces take a {@code +1} to enclose the
     * end blocks rather than stopping at their near corners.
     */
    private static AABB offsets(PoleTier tier) {
        SupplyArea.Bounds bounds = SupplyArea.bounds(tier);
        return new AABB(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1);
    }

    /** The same box in world space, for a pole of this tier based here. */
    private static AABB around(BlockPos base, PoleTier tier) {
        return offsets(tier).move(base.getX(), base.getY(), base.getZ());
    }

    /**
     * Draws the box for a pole based at the <em>pose's own origin</em>.
     *
     * <p>For a block entity renderer, whose pose is already translated to the block, this is the
     * call: the box is the bare offsets and needs no camera. See
     * {@link #drawAt} for a pose sitting at the camera instead.
     */
    public static void drawAtPose(SubmitNodeCollector collector, PoseStack poseStack, PoleTier tier) {
        draw(collector, poseStack, offsets(tier));
    }

    /**
     * Draws the box for a pole based at {@code base}, for a pose sitting at the camera.
     *
     * <p>The placement preview's call: its pose is the level's, so the box carries the world
     * position and the camera offset itself.
     */
    public static void drawAt(SubmitNodeCollector collector, PoseStack poseStack, Vec3 camera,
            BlockPos base, PoleTier tier) {
        draw(collector, poseStack, around(base, tier).move(-camera.x(), -camera.y(), -camera.z()));
    }

    /**
     * The edges come off a {@link net.minecraft.world.phys.shapes.VoxelShape}, the way every other
     * outline in the game is drawn, rather than from twelve hand-written segments that could differ
     * from one. Vanilla's own {@code ShapeRenderer} is not reused because it wants a whole
     * {@link PoseStack} while the collector hands out a single {@link PoseStack.Pose}, and the
     * callback may run after the stack has been popped.
     */
    private static void draw(SubmitNodeCollector collector, PoseStack poseStack, AABB box) {
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) ->
                Shapes.create(box).forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                    Vector3f normal = new Vector3f(
                            (float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
                    buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
                            .setColor(COLOUR).setNormal(pose, normal).setLineWidth(WIDTH);
                    buffer.addVertex(pose, (float) x2, (float) y2, (float) z2)
                            .setColor(COLOUR).setNormal(pose, normal).setLineWidth(WIDTH);
                }));
    }
}
