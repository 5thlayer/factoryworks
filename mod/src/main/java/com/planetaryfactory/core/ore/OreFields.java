package com.planetaryfactory.core.ore;

import com.mojang.serialization.Codec;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The starting fields this world actually dealt, and what one of their blocks is worth.
 *
 * <p>ADR-0041 keeps the patch total as the invariant and makes the per-block amount the quotient
 * over the blocks in the field. Only the world knows the second number: the jigsaw deals one of
 * three size variants per resource, and vanilla drops an overlapping child silently, so the field
 * that got placed is the only field worth dividing by. So the blocks are counted once, at the
 * moment the starting area is stamped, and what is written down is four small records rather than
 * a counter per ore block.
 *
 * <p>This is not the parallel counter ADR-0020 refused. It holds no remaining amount and never
 * changes as the patch is mined; it is the *initial* amount's derivation, fixed at placement.
 * What is left in a given block is the block's own business ({@link OreDelta}).
 *
 * <p>A block outside every recorded field -- an outfield vein, a block someone placed by hand --
 * falls back on the same arithmetic scaled by Factorio's distance law, which is flat inside 1600
 * blocks of spawn and rises beyond it.
 */
public final class OreFields extends SavedData {

    /**
     * The whole of this data as one codec. 26.1 serialises saved data through a codec rather than
     * through a {@code save}/{@code load} pair, so the field list is the only thing stated and the
     * two directions can no longer disagree.
     *
     * <p><b>No unit test, deliberately.</b> {@link Field} is built on {@link BlockPos} and
     * {@link BoundingBox}, so this codec cannot be reached from the Minecraft-free test source set
     * that holds {@code AssemblerCodecsTest}. What it would assert -- that the derivation survives
     * a save -- is instead covered by the arithmetic tests under {@code core/ore/}, which take the
     * field list directly.
     */
    public static final Codec<OreFields> CODEC = Field.CODEC.listOf()
            .xmap(OreFields::of, OreFields::fields)
            .fieldOf("fields")
            .codec();

    public static final SavedDataType<OreFields> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "ore_fields"),
            OreFields::new, CODEC);

    private final List<Field> fields = new ArrayList<>();

    public OreFields() {
    }

    private static OreFields of(List<Field> fields) {
        OreFields data = new OreFields();
        data.fields.addAll(fields);
        return data;
    }

    /**
     * Record a placed field: the resource, where it landed, and how many ore blocks it holds.
     *
     * <p>The amount is {@link OreField}'s quotient, so the arithmetic that decides it is the one
     * the unit tests cover rather than a second copy of it here. The placed record is handed back
     * so a caller wanting to report the amount reads it rather than dividing a second time.
     */
    public Field record(OreResource resource, BoundingBox box, int blocks) {
        OreField field = new OreField(resource.key(), resource.corpus().startingAmount(), blocks);
        Field placed = new Field(resource.key(), box, field.amountPerBlock());
        fields.add(placed);
        setDirty();
        return placed;
    }

    public List<Field> fields() {
        return List.copyOf(fields);
    }

    /**
     * The initial amount of the block at {@code pos}, or {@code 0} where nothing here places one.
     *
     * <p>A position inside a recorded field takes that field's amount. Anything else -- an outfield
     * vein -- takes the same resource's starting-field amount scaled by Factorio's own richness
     * term, which is 1.0 everywhere inside 1600 blocks of spawn. That keeps one arithmetic for the
     * whole planet: ADR-0020's "the starting patches and the outfield differ by size and access,
     * not by mechanic".
     */
    public int initialAmount(OreResource resource, BlockPos pos, BlockPos spawn) {
        for (Field field : fields) {
            if (field.resource().equals(resource.key()) && field.box().isInside(pos)) {
                return field.amountPerBlock();
            }
        }
        int reference = referenceAmount(resource);
        if (reference <= 0) {
            return 0;
        }
        double distance = Math.sqrt(pos.distSqr(spawn));
        return Math.max(1, (int) Math.round(reference * OreCorpus.get().distanceLaw().richnessAt(distance)));
    }

    /**
     * What one block of this resource's starting field holds, for a world that dealt one.
     *
     * <p>A resource with no starting field -- uranium, which Factorio gives none -- falls back on
     * the smallest amount any <em>other</em> resource's field recorded, rather than on nothing:
     * an outfield vein of blocks holding zero is a vein that pays out nothing at all. The smallest
     * is deliberate and not arbitrary -- uranium is the scarcest thing on the planet, so the
     * leanest field on record is the closest honest stand-in for a total Factorio never states.
     * The minimum is order-independent, so which field was stamped first does not decide it.
     */
    private int referenceAmount(OreResource resource) {
        int smallestOther = 0;
        for (Field field : fields) {
            if (field.resource().equals(resource.key())) {
                return field.amountPerBlock();
            }
            smallestOther = smallestOther == 0
                    ? field.amountPerBlock()
                    : Math.min(smallestOther, field.amountPerBlock());
        }
        return smallestOther;
    }

    /**
     * One placed field.
     *
     * @param resource the corpus key
     * @param box where the field's template landed
     * @param amountPerBlock the patch total over the blocks that were actually placed
     */
    public record Field(String resource, BoundingBox box, int amountPerBlock) {

        public static final Codec<Field> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("resource").forGetter(Field::resource),
                BoundingBox.CODEC.fieldOf("box").forGetter(Field::box),
                Codec.INT.fieldOf("amount").forGetter(Field::amountPerBlock)
        ).apply(instance, Field::new));
    }
}
