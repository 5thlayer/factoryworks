package com.planetaryfactory.core.radar;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every team's chart, the outfield patches of its sectors, and what of it each player's map has been
 * sent, saved with the world on the overworld's data storage (#368, #369, #370, ADR-0079). Which
 * markers a player has been sent is not saved: the client forgets them at logout.
 */
public final class RadarChartData extends SavedData {

    public static final SavedDataType<RadarChartData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "radar_charts"),
            RadarChartData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    RadarCharts.CODEC.fieldOf("charts").forGetter(data -> data.charts),
                    // Not optionalFieldOf(name, default): every older save would share that one
                    // mutable default, and a value equal to the default is never written (#369).
                    ChartDelivery.CODEC.optionalFieldOf("delivered")
                            .forGetter(data -> Optional.of(data.delivery)),
                    SectorPatches.CODEC.optionalFieldOf("patches")
                            .forGetter(data -> Optional.of(data.patches)))
                    .apply(instance, (charts, delivery, patches) -> new RadarChartData(charts,
                            delivery.orElseGet(ChartDelivery::new), patches.orElseGet(SectorPatches::new)))));

    private final RadarCharts charts;
    private final ChartDelivery delivery;
    private final SectorPatches patches;
    private final MarkerDelivery markers = new MarkerDelivery();

    public RadarChartData() {
        this(new RadarCharts(), new ChartDelivery(), new SectorPatches());
    }

    private RadarChartData(RadarCharts charts, ChartDelivery delivery, SectorPatches patches) {
        this.charts = charts;
        this.delivery = delivery;
        this.patches = patches;
    }

    public static RadarChartData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Records the sector and the outfield patches found centred in it; true if the team lacked it. */
    public boolean chart(UUID team, String dimension, Sector sector, List<PatchMarker> found) {
        boolean added = charts.chart(team, dimension, sector);
        if (added) {
            patches.record(dimension, sector, found);
            delivery.charted(team, dimension, sector);
            markers.charted(team, dimension, found);
            setDirty();
        }
        return added;
    }

    public void observe(UUID player, UUID team, String dimension) {
        delivery.observe(player, team, dimension, charts);
        markers.observe(player, team, dimension, charts, patches);
    }

    public List<PatchMarker> takeMarkers(UUID player) {
        return markers.take(player);
    }

    public List<Sector> takeDeliveries(UUID player, int max) {
        List<Sector> taken = delivery.take(player, max);
        if (!taken.isEmpty()) {
            setDirty();
        }
        return taken;
    }

    public void logout(UUID player) {
        delivery.logout(player);
        markers.logout(player);
    }

    public boolean isCharted(UUID team, String dimension, Sector sector) {
        return charts.isCharted(team, dimension, sector);
    }

    public Set<Sector> sectors(UUID team, String dimension) {
        return charts.sectors(team, dimension);
    }
}
