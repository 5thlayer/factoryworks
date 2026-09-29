package com.factoryworks.core.worldgen;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.oil.OilField;
import com.factoryworks.core.oil.OilFieldSource;
import com.factoryworks.core.oil.OilWellBlockEntity;
import com.factoryworks.core.ore.OutfieldDisc;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** A crude-oil field as a structure piece: its centre and the wells it drew (ADR-0081). */
public final class OilFieldPiece extends StructurePiece implements OilFieldSource {

    private final int centreX;
    private final int centreZ;
    private final List<OilField.Well> wells;

    OilFieldPiece(int centreX, int centreZ, int reach, List<OilField.Well> wells, LevelHeightAccessor height) {
        super(PFWorldgen.OIL_FIELD_PIECE.get(), 0, new BoundingBox(
                centreX - reach, height.getMinY(), centreZ - reach,
                centreX + reach, height.getMaxY(), centreZ + reach));
        this.centreX = centreX;
        this.centreZ = centreZ;
        this.wells = List.copyOf(wells);
    }

    public OilFieldPiece(CompoundTag tag) {
        super(PFWorldgen.OIL_FIELD_PIECE.get(), tag);
        this.centreX = tag.getIntOr("centre_x", 0);
        this.centreZ = tag.getIntOr("centre_z", 0);
        int[] xs = tag.getIntArray("well_x").orElse(new int[0]);
        int[] zs = tag.getIntArray("well_z").orElse(new int[0]);
        long[] amounts = tag.getLongArray("well_amount").orElse(new long[0]);
        List<OilField.Well> read = new ArrayList<>(xs.length);
        for (int i = 0; i < xs.length && i < zs.length && i < amounts.length; i++) {
            read.add(new OilField.Well(xs[i], zs[i], amounts[i]));
        }
        this.wells = List.copyOf(read);
    }

    @Override
    public int centreX() {
        return centreX;
    }

    @Override
    public int centreZ() {
        return centreZ;
    }

    @Override
    public List<OilField.Well> wells() {
        return wells;
    }

    /** Whether an outfield disc covers the column, whichever of the two structures places first (ADR-0081). */
    public static boolean oreAt(StructureManager structures, int x, int z) {
        for (StructureStart start : structures.startsForStructure(
                new ChunkPos(x >> 4, z >> 4), structure -> structure instanceof OutfieldDiscStructure)) {
            for (StructurePiece piece : start.getPieces()) {
                if (piece instanceof OutfieldDisc.Source disc && disc.covers(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("centre_x", centreX);
        tag.putInt("centre_z", centreZ);
        tag.putIntArray("well_x", wells.stream().mapToInt(OilField.Well::x).toArray());
        tag.putIntArray("well_z", wells.stream().mapToInt(OilField.Well::z).toArray());
        tag.putLongArray("well_amount", wells.stream().mapToLong(OilField.Well::amount).toArray());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
            RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
        // Only this chunk's wells: a WorldGenRegion refuses a structure lookup two chunks out.
        List<OilField.Well> here = wells.stream()
                .filter(well -> chunkBB.isInside(well.x(), chunkBB.minY(), well.z()))
                .toList();
        for (OilField.Well well : OilField.placeable(here, (x, z) -> oreAt(structureManager, x, z))) {
            BlockPos pos = new BlockPos(well.x(), GroundProcessor.ground(level, well.x(), well.z()), well.z());
            level.setBlock(pos, PFBlocks.OIL_WELL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(pos) instanceof OilWellBlockEntity entity) {
                entity.start(well.amount());
            }
        }
    }
}
