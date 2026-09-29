package com.factoryworks.core.radar;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.factoryworks.core.FactoryWorksCore;
import com.factoryworks.core.ore.PatchId;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every team's chart, the outfield patches of its sectors and of what each player has walked, and
 * what of it each player's map has been sent, saved with the world on the overworld's data storage
 * (#368, #369, #370, ADR-0079). Which markers a player has been sent is not saved: the client
 * forgets them at logout.
 */
public final class RadarChartData extends SavedData {

    public static final SavedDataType<RadarChartData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, "radar_charts"),
            RadarChartData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    RadarCharts.CODEC.fieldOf("charts").forGetter(data -> data.charts),
                    // Not optionalFieldOf(name, default): every older save would share that one
                    // mutable default, and a value equal to the default is never written (#369).
                    ChartDelivery.CODEC.optionalFieldOf("delivered")
                            .forGetter(data -> Optional.of(data.delivery)),
                    SectorPatches.CODEC.optionalFieldOf("patches")
                            .forGetter(data -> Optional.of(data.patches)),
                    WalkedPatches.CODEC.optionalFieldOf("walked")
                            .forGetter(data -> Optional.of(data.walked)))
                    .apply(instance, (charts, delivery, patches, walked) -> new RadarChartData(charts,
                            delivery.orElseGet(ChartDelivery::new), patches.orElseGet(SectorPatches::new),
                            walked.orElseGet(WalkedPatches::new)))));

    private final RadarCharts charts;
    private final ChartDelivery delivery;
    private final SectorPatches patches;
    private final WalkedPatches walked;
    private final MarkerDelivery markers = new MarkerDelivery();

    public RadarChartData() {
        this(new RadarCharts(), new ChartDelivery(), new SectorPatches(), new WalkedPatches());
    }

    private RadarChartData(RadarCharts charts, ChartDelivery delivery, SectorPatches patches, WalkedPatches walked) {
        this.charts = charts;
        this.delivery = delivery;
        this.patches = patches;
        this.walked = walked;
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
        markers.observe(player, team, dimension, charts, patches, walked);
    }

    /** A Radar looking again at sectors its team has charted refreshes their markers' amounts. */
    public void rescanned(UUID team, String dimension, Collection<Sector> sectors) {
        markers.charted(team, dimension, patches.in(dimension, sectors));
    }

    /** The outfield patches centred in a chunk just sent to the player. */
    public void walked(UUID player, String dimension, List<PatchMarker> found) {
        if (!walked.add(player, dimension, found).isEmpty()) {
            setDirty();
        }
        markers.found(player, dimension, found);
    }

    public void ranOut(PatchId patch) {
        markers.ranOut(patch);
    }

    public List<MarkerDelivery.Update> takeMarkers(UUID player, MarkerDelivery.Amounts amounts) {
        return markers.take(player, amounts);
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
