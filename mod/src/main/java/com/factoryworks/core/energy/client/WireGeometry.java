package com.factoryworks.core.energy.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

import com.factoryworks.core.energy.PoleColumn;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;

/**
 * A wire's geometry, drawn from one end to the other: vanilla's {@code LeashFeatureRenderer} shape
 * with the colour chosen by the caller.
 *
 * <p>Vanilla's own leash colour is fixed, and every wire the pack draws that is not plain brown --
 * the hand gesture's slack ({@link PoleWireRenderer}) and the Placement Preview's would-be wires
 * (#298) -- needs its own. One copy of the curve rather than one per caller, so a wire drawn by the
 * preview hangs exactly where the wire it promises will hang.
 */
public final class WireGeometry {

    /** Vanilla's leash segment count and width, so every wire here reads as the same wire. */
    static final int STEPS = 24;
    static final float WIDTH = 0.05F;

    /** Just under the top face of the top segment, where Factorio hangs its wire off the pole's head. */
    public static final double ATTACH_HEIGHT = 0.9;

    private WireGeometry() {
    }

    /** Where a wire hangs off the column based at {@code base}: the top segment's head. */
    public static Vec3 attachPoint(Level level, BlockPos base) {
        BlockPos top = PoleColumn.topOf(level, base);
        if (top == null) {
            top = base;
        }
        return new Vec3(top.getX() + 0.5, top.getY() + ATTACH_HEIGHT, top.getZ() + 0.5);
    }

    /**
     * @param poseIn the pose at the drawing block's own origin; {@code leash.offset} carries it to
     *               the wire's start, as vanilla's does
     */
    public static void draw(Matrix4f poseIn, VertexConsumer buffer, EntityRenderState.LeashState leash,
            float r, float g, float b, float alpha) {
        Matrix4f pose = new Matrix4f(poseIn).translate((float) leash.offset.x, (float) leash.offset.y,
                (float) leash.offset.z);
        float dx = (float) (leash.end.x - leash.start.x);
        float dy = (float) (leash.end.y - leash.start.y);
        float dz = (float) (leash.end.z - leash.start.z);
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        float offsetFactor = horizontal == 0.0F ? 0.0F : WIDTH / 2.0F / horizontal;
        float dxOff = dz * offsetFactor;
        float dzOff = dx * offsetFactor;
        for (int k = 0; k <= STEPS; k++) {
            vertices(buffer, pose, dx, dy, dz, WIDTH, dxOff, dzOff, k, false, leash, r, g, b, alpha);
        }
        for (int k = STEPS; k >= 0; k--) {
            vertices(buffer, pose, dx, dy, dz, 0.0F, dxOff, dzOff, k, true, leash, r, g, b, alpha);
        }
    }

    private static void vertices(VertexConsumer buffer, Matrix4f pose, float dx, float dy, float dz,
            float fudge, float dxOff, float dzOff, int k, boolean backwards, EntityRenderState.LeashState leash,
            float r, float g, float b, float alpha) {
        float progress = k / (float) STEPS;
        int block = (int) (leash.startBlockLight + (leash.endBlockLight - leash.startBlockLight) * progress);
        int sky = (int) (leash.startSkyLight + (leash.endSkyLight - leash.startSkyLight) * progress);
        int light = LightCoordsUtil.pack(block, sky);
        float shade = k % 2 == (backwards ? 1 : 0) ? 0.7F : 1.0F;
        float x = dx * progress;
        // The sag: vanilla's own curve, so a previewed wire hangs where the stored one will.
        float y = dy > 0.0F ? dy * progress * progress : dy - dy * (1.0F - progress) * (1.0F - progress);
        float z = dz * progress;
        buffer.addVertex(pose, x - dxOff, y + fudge, z + dzOff)
                .setColor(r * shade, g * shade, b * shade, alpha).setLight(light);
        buffer.addVertex(pose, x + dxOff, y + WIDTH - fudge, z - dzOff)
                .setColor(r * shade, g * shade, b * shade, alpha).setLight(light);
    }
}
