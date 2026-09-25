package com.planetaryfactory.core.placement.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;
import io.github._5thlayer.beltworks.ComponentContent;
import io.github._5thlayer.beltworks.items.BeltTileItem;

/**
 * The start a held tile stack has stored (#393): an outline round the tile there and an arrow the
 * way the stretch will run from it, and an outline round each corner added since. Drawn whatever
 * the aim, so they stay visible while the player looks for the end. Loaded only when the fork is.
 */
final class PreviewStretchStart {

    private static final int COLOUR = 0xFFFFD040;
    private static final float WIDTH = 2.5F;
    private static final double TOP = 6.0 / 16.0 + 0.01;
    private static final VoxelShape OUTLINE = Shapes.create(new AABB(0, 0, 0, 1, TOP, 1));

    private PreviewStretchStart() {
    }

    static void draw(SubmitCustomGeometryEvent event, ItemStack stack) {
        if (!(stack.getItem() instanceof BeltTileItem)) {
            return;
        }
        BlockPos start = stack.get(ComponentContent.BELT_START.get());
        Direction look = stack.get(ComponentContent.BELT_DIR.get());
        if (start == null || look == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (BlockPos corner : stack.getOrDefault(ComponentContent.STRETCH_CORNERS.get(), List.<BlockPos>of())) {
            poseStack.pushPose();
            poseStack.translate(corner.getX() - camera.x(), corner.getY() - camera.y(), corner.getZ() - camera.z());
            collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> outline(buffer, pose));
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(start.getX() - camera.x(), start.getY() - camera.y(), start.getZ() - camera.z());
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            outline(buffer, pose);
            double backX = 0.5 - 0.35 * look.getStepX();
            double backZ = 0.5 - 0.35 * look.getStepZ();
            double tipX = 0.5 + 0.35 * look.getStepX();
            double tipZ = 0.5 + 0.35 * look.getStepZ();
            line(buffer, pose, backX, TOP, backZ, tipX, TOP, tipZ);
            Direction left = look.getCounterClockWise();
            for (int side : new int[] {1, -1}) {
                line(buffer, pose, tipX, TOP, tipZ,
                        tipX - 0.2 * look.getStepX() + side * 0.2 * left.getStepX(), TOP,
                        tipZ - 0.2 * look.getStepZ() + side * 0.2 * left.getStepZ());
            }
        });
        poseStack.popPose();
    }

    private static void outline(VertexConsumer buffer, PoseStack.Pose pose) {
        OUTLINE.forAllEdges((x1, y1, z1, x2, y2, z2) -> line(buffer, pose, x1, y1, z1, x2, y2, z2));
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose,
                             double x1, double y1, double z1, double x2, double y2, double z2) {
        Vector3f normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(COLOUR).setNormal(pose, normal).setLineWidth(WIDTH);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(COLOUR).setNormal(pose, normal).setLineWidth(WIDTH);
    }
}
