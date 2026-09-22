package com.planetaryfactory.core.radar;

import java.util.Set;
import java.util.UUID;

import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Every team's chart, saved with the world on the overworld's data storage (#368, ADR-0079). */
public final class RadarChartData extends SavedData {

    public static final SavedDataType<RadarChartData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "radar_charts"),
            RadarChartData::new,
            RadarCharts.CODEC.xmap(RadarChartData::new, data -> data.charts).fieldOf("charts").codec());

    private final RadarCharts charts;

    public RadarChartData() {
        this(new RadarCharts());
    }

    private RadarChartData(RadarCharts charts) {
        this.charts = charts;
    }

    public static RadarChartData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean chart(UUID team, String dimension, Sector sector) {
        boolean added = charts.chart(team, dimension, sector);
        if (added) {
            setDirty();
        }
        return added;
    }

    public Set<Sector> sectors(UUID team, String dimension) {
        return charts.sectors(team, dimension);
    }
}
