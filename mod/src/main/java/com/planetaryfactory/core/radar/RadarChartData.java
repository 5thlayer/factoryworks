package com.planetaryfactory.core.radar;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every team's chart, and what of it each player's map has been sent, saved with the world on the
 * overworld's data storage (#368, #369, ADR-0079).
 */
public final class RadarChartData extends SavedData {

    public static final SavedDataType<RadarChartData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "radar_charts"),
            RadarChartData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    RadarCharts.CODEC.fieldOf("charts").forGetter(data -> data.charts),
                    ChartDelivery.CODEC.optionalFieldOf("delivered", new ChartDelivery())
                            .forGetter(data -> data.delivery))
                    .apply(instance, RadarChartData::new)));

    private final RadarCharts charts;
    private final ChartDelivery delivery;

    public RadarChartData() {
        this(new RadarCharts(), new ChartDelivery());
    }

    private RadarChartData(RadarCharts charts, ChartDelivery delivery) {
        this.charts = charts;
        this.delivery = delivery;
    }

    public static RadarChartData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean chart(UUID team, String dimension, Sector sector) {
        boolean added = charts.chart(team, dimension, sector);
        if (added) {
            delivery.charted(team, dimension, sector);
            setDirty();
        }
        return added;
    }

    public void observe(UUID player, UUID team, String dimension) {
        delivery.observe(player, team, dimension, charts);
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
    }

    public Set<Sector> sectors(UUID team, String dimension) {
        return charts.sectors(team, dimension);
    }
}
