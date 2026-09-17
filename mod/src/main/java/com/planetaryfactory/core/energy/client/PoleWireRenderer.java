package com.planetaryfactory.core.energy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.planetaryfactory.core.energy.ClientPoles;
import com.planetaryfactory.core.energy.PoleColumn;
import com.planetaryfactory.core.energy.PoleLinks;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.PoleWires;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The wire between linked poles (#281, ADR-0062): cosmetic, and drawn with vanilla's leash geometry.
 *
 * <p>Each base pole draws the wires {@link PoleWires#drawnBy} gives it, from the top of its column
 * to the top of the other's. Links are re-derived every frame from the poles the client has loaded,
 * so breaking a pole, or turning it into an extension, takes its wires with it on the next frame.
 */
public final class PoleWireRenderer
        implements BlockEntityRenderer<SupplyAreaPoleBlockEntity, PoleWireRenderer.State> {

    /** Just under the top face of the top segment, where Factorio hangs its wire off the pole's head. */
    private static final double ATTACH_HEIGHT = 0.9;

    public static final class State extends BlockEntityRenderState {
        final List<EntityRenderState.LeashState> wires = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SupplyAreaPoleBlockEntity pole, State state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pole, state, partialTicks, cameraPosition, breakProgress);
        state.wires.clear();
        Level level = pole.getLevel();
        if (level == null || !PoleColumn.isBase(level, pole.getBlockPos())) {
            return;
        }
        Map<PoleLinks.Pole, BlockPos> bases = new HashMap<>();
        for (SupplyAreaPoleBlockEntity other : ClientPoles.loadedIn(level)) {
            BlockPos pos = other.getBlockPos();
            if (!other.isRemoved() && PoleColumn.isBase(level, pos)) {
                bases.put(other.shape(), pos);
            }
        }
        BlockPos from = pole.getBlockPos();
        Vec3 start = attachPoint(level, from);
        for (PoleLinks.Pole other : PoleWires.drawnBy(pole.shape(), bases.keySet())) {
            BlockPos to = bases.get(other);
            EntityRenderState.LeashState wire = new EntityRenderState.LeashState();
            wire.start = start;
            wire.end = attachPoint(level, to);
            wire.offset = start.subtract(Vec3.atLowerCornerOf(from));
            BlockPos startTop = BlockPos.containing(start);
            BlockPos endTop = BlockPos.containing(wire.end);
            wire.startBlockLight = level.getBrightness(LightLayer.BLOCK, startTop);
            wire.endBlockLight = level.getBrightness(LightLayer.BLOCK, endTop);
            wire.startSkyLight = level.getBrightness(LightLayer.SKY, startTop);
            wire.endSkyLight = level.getBrightness(LightLayer.SKY, endTop);
            wire.slack = true;
            state.wires.add(wire);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (EntityRenderState.LeashState wire : state.wires) {
            collector.submitLeash(poseStack, wire);
        }
    }

    /** A wire leaves the frustum long after the pole that draws it does. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    /**
     * Only one end draws a wire, so that end must stay drawn while the other is in view: the
     * default distance plus the longest span.
     */
    @Override
    public int getViewDistance() {
        return BlockEntityRenderer.super.getViewDistance() + (int) Math.ceil(PoleTier.SUBSTATION.wireReach());
    }

    @Override
    public AABB getRenderBoundingBox(SupplyAreaPoleBlockEntity pole) {
        double reach = PoleTier.SUBSTATION.wireReach();
        return new AABB(pole.getBlockPos()).inflate(reach, reach, reach);
    }

    private static Vec3 attachPoint(Level level, BlockPos base) {
        BlockPos top = PoleColumn.topOf(level, base);
        if (top == null) {
            top = base;
        }
        return new Vec3(top.getX() + 0.5, top.getY() + ATTACH_HEIGHT, top.getZ() + 0.5);
    }
}
