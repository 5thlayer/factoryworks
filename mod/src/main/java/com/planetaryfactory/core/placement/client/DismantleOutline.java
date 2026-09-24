package com.planetaryfactory.core.placement.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;

/** A red outline round each block a Dismantle would take up. */
final class DismantleOutline {

    private static final int COLOUR = 0xFFFF4040;
    private static final float WIDTH = 2.5F;
    // A hair outside the block, or vanilla's own outline of the aimed block hides its edges.
    private static final VoxelShape OUTLINE = Shapes.create(new AABB(0, 0, 0, 1, 1, 1).inflate(0.004));

    private DismantleOutline() {
    }

    static void draw(SubmitCustomGeometryEvent event, List<BlockPos> positions) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (BlockPos pos : positions) {
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x(), pos.getY() - camera.y(), pos.getZ() - camera.z());
            collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) ->
                    OUTLINE.forAllEdges((x1, y1, z1, x2, y2, z2) -> line(buffer, pose, x1, y1, z1, x2, y2, z2)));
            poseStack.popPose();
        }
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose,
                             double x1, double y1, double z1, double x2, double y2, double z2) {
        Vector3f normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(COLOUR).setNormal(pose, normal).setLineWidth(WIDTH);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(COLOUR).setNormal(pose, normal).setLineWidth(WIDTH);
    }
}
