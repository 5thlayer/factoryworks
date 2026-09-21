package com.planetaryfactory.core.ore;

/**
 * Factorio's regular-patch law for one resource: a spot's quantity and a tile's richness by
 * distance from world origin (ADR-0045).
 *
 * <p>The quantity stops growing at 1600 blocks because density does; richness has no cap.
 * The map-generation controls are Factorio's defaults of 1 and do not appear.
 */
public record OutfieldLaw(OreCorpus.Outfield spot, OreCorpus.DensityLaw density,
        OreCorpus.DistanceLaw richnessLaw) {

    public static OutfieldLaw of(OreResource resource) {
        OreCorpus corpus = OreCorpus.get();
        return new OutfieldLaw(resource.corpus().outfield(), corpus.densityLaw(), corpus.distanceLaw());
    }

    public double density(double distance) {
        return density.at(spot.baseDensity(), distance);
    }

    /** {@code regular_spot_quantity_base_at} times the spot's own size factor. */
    public double quantity(double sizeFactor, double distance) {
        return sizeFactor * 1_000_000 / spot.baseSpotsPerKm2() * density(distance);
    }

    public double richness(double distance) {
        return richnessLaw.richnessAt(distance);
    }

    public long total(double sizeFactor, double distance) {
        return (long) (quantity(sizeFactor, distance) * richness(distance));
    }
}
