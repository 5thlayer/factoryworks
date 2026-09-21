package com.planetaryfactory.core.ore;

/**
 * Factorio's regular-patch law for one resource: a spot's quantity, size and edge, and a tile's
 * richness, by distance from world origin (ADR-0045).
 *
 * <p>The quantity stops growing at 1600 blocks because density does; richness has no cap.
 * The map-generation controls are Factorio's defaults of 1 and do not appear.
 */
public record OutfieldLaw(OreCorpus.Outfield spot, OreCorpus.DensityLaw density,
        OreCorpus.DistanceLaw richnessLaw, OreCorpus.Edge edge) {

    public static OutfieldLaw of(OreResource resource) {
        OreCorpus corpus = OreCorpus.get();
        return new OutfieldLaw(resource.corpus().outfield(), corpus.densityLaw(), corpus.distanceLaw(),
                corpus.edge());
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

    /** {@code random_penalty_between(minimum, maximum, 1)} for a uniform {@code unit} in [0, 1]. */
    public double sizeFactor(double unit) {
        return spot.spotSizeMinimum() + (spot.spotSizeMaximum() - spot.spotSizeMinimum()) * unit;
    }

    public double radius(double sizeFactor, double distance) {
        return Math.min(edge.radiusCap(), spot.rqFactor() * Math.cbrt(quantity(sizeFactor, distance)));
    }

    /** The peak of the spot's cone: a cone of this radius whose volume is the spot's quantity. */
    public double height(double sizeFactor, double distance) {
        double radius = radius(sizeFactor, distance);
        return radius == 0 ? 0 : quantity(sizeFactor, distance) / (Math.PI / 3 * radius * radius);
    }

    /** {@code regular_spot_height_typical_at}: the cone of a mean-sized spot, ignoring the cap. */
    public double typicalHeight(double distance) {
        return Math.cbrt(quantity(spot.meanSpotSize(), distance))
                / (Math.PI / 3 * spot.rqFactor() * spot.rqFactor());
    }

    public double blobAmplitude(double distance) {
        return spot.blobAmplitudeMultiplier()
                * Math.min(typicalHeight(spot.blobAmplitudeMaximumDistance()), typicalHeight(distance));
    }
}
