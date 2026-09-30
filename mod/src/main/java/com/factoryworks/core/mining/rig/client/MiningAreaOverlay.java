package com.factoryworks.core.mining.rig.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.factoryworks.core.mining.rig.RigBlock;
import com.factoryworks.core.mining.rig.RigCorpus;
import com.factoryworks.core.mining.rig.RigDirections;
import com.factoryworks.core.mining.rig.RigFacing;
import com.factoryworks.core.mining.rig.RigGeometry.Offset;
import com.factoryworks.core.mining.rig.RigOutputTile;
import com.factoryworks.core.mining.rig.RigMiningArea;
import com.factoryworks.core.mining.rig.RigPartBlockEntity;
import com.factoryworks.core.ore.OreBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

/**
 * The mining-area overlay (#195, ADR-0043): the top face of every ore block a rig works, tinted
 * while the rig is looked at or held, and not otherwise.
 *
 * <p>Drawn from blockstate alone, since the amount never reaches the client. A field is one block
 * thick and flush with the ground, so its top face is what the player sees.
 */
public final class MiningAreaOverlay {

    private static final int TINT = 0x6060C0FF;

    /** Above the face it tints, so the two never z-fight. */
    private static final int ARROW = 0xD0FFA020;

    private static final float LIFT = 1.0F / 512;

    private MiningAreaOverlay() {
    }

    /** A placed rig, looked at through its anchor or any part. The held rig is the preview's call. */
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos anchor = anchorOf(level, hit.getBlockPos());
        if (anchor != null) {
            drawFor(event.getSubmitNodeCollector(), event.getPoseStack(), level,
                    event.getLevelRenderState().cameraRenderState.pos, anchor, level.getBlockState(anchor));
        }
    }

    private static @Nullable BlockPos anchorOf(ClientLevel level, BlockPos aimed) {
        if (level.getBlockState(aimed).getBlock() instanceof RigBlock) {
            return aimed;
        }
        if (level.getBlockEntity(aimed) instanceof RigPartBlockEntity part
                && part.anchorPos() != null
                && level.getBlockState(part.anchorPos()).getBlock() instanceof RigBlock) {
            return part.anchorPos();
        }
        return null;
    }

    /** The area of a rig whose anchor stands, or would stand, at {@code anchor} in {@code anchorState}. */
    public static void drawFor(SubmitNodeCollector collector, PoseStack poseStack, ClientLevel level,
            Vec3 camera, BlockPos anchor, BlockState anchorState) {
        if (!(anchorState.getBlock() instanceof RigBlock rig)) {
            return;
        }
        var ores = RigMiningArea.positions(anchor, rig.tier(), anchorState.getValue(RigBlock.FACING))
                .stream()
                .filter(pos -> level.getBlockState(pos).getBlock() instanceof OreBlock)
                .toList();
        if (ores.isEmpty()) {
            return;
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
            for (BlockPos pos : ores) {
                float x = (float) (pos.getX() - camera.x());
                float y = (float) (pos.getY() + 1 + LIFT - camera.y());
                float z = (float) (pos.getZ() - camera.z());
                buffer.addVertex(pose, x, y, z).setColor(TINT);
                buffer.addVertex(pose, x, y, z + 1).setColor(TINT);
                buffer.addVertex(pose, x + 1, y, z + 1).setColor(TINT);
                buffer.addVertex(pose, x + 1, y, z).setColor(TINT);
            }
        });
    }

    /**
     * An arrow on the Drop Position of a rig that would stand at {@code anchor}, pointing away from
     * it (#535). The tile is {@link RigOutputTile}'s, the one the rig ejects onto.
     */
    public static void drawDropPosition(SubmitNodeCollector collector, PoseStack poseStack, Vec3 camera,
            BlockPos anchor, BlockState anchorState) {
        if (!(anchorState.getBlock() instanceof RigBlock rig)) {
            return;
        }
        RigFacing facing = RigDirections.toRigFacing(anchorState.getValue(RigBlock.FACING));
        RigFacing right = facing.rightOf();
        RigCorpus.Row row = RigCorpus.get().rowOf(rig.tier());
        Offset tile = RigOutputTile.of(row.width(), row.height(), row.vectorX(), row.vectorY(), facing);
        float originX = (float) (anchor.getX() + tile.dx() + 0.5 - camera.x());
        float y = (float) (anchor.getY() + tile.dy() + LIFT - camera.y());
        float originZ = (float) (anchor.getZ() + tile.dz() + 0.5 - camera.z());

        // Tile-local (along, across) -> world; a triangle is a quad with a repeated corner.
        float[][][] quads = {
            {{-0.35F, -0.09F}, {-0.35F, 0.09F}, {0.05F, 0.09F}, {0.05F, -0.09F}},
            {{0.05F, -0.3F}, {0.05F, 0.3F}, {0.4F, 0.0F}, {0.4F, 0.0F}},
        };
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
            for (float[][] quad : quads) {
                for (float[] corner : quad) {
                    buffer.addVertex(pose,
                            originX + facing.dx() * corner[0] + right.dx() * corner[1], y,
                            originZ + facing.dz() * corner[0] + right.dz() * corner[1])
                            .setColor(ARROW);
                }
            }
        });
    }
}
