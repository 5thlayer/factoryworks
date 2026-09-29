package com.factoryworks.core.radar.ftb;

import com.factoryworks.core.oil.WellYield;
import com.factoryworks.core.ore.OreResource;
import com.factoryworks.core.radar.PatchAmount;
import com.factoryworks.core.radar.PatchMarker;
import com.factoryworks.core.radar.SectorPatches;
import com.factoryworks.core.radar.client.ChartMarkerRenderer;
import com.factoryworks.core.radar.client.RadarMapClient;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.AddMapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.neoforge.FTBChunksClientEvent;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.util.TooltipList;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Draws the patch markers as FTB Chunks map icons, through its public icon API (ADR-0079). The
 * icons are rebuilt from {@link RadarMapClient} on each refresh. Client only, loaded only when
 * {@code ftbchunks} is.
 */
public final class FtbMapMarkers implements ChartMarkerRenderer {

    /** Oritech's crude bucket, the item Factorio's crude-oil icon maps to (ADR-0081). */
    private static final String OIL_ICON = "oritech:still_oil_bucket";

    private FtbMapMarkers() {
    }

    public static ChartMarkerRenderer create() {
        NeoForge.EVENT_BUS.addListener(FtbMapMarkers::onMapIcon);
        return new FtbMapMarkers();
    }

    @Override
    public void markersChanged() {
        FTBChunksAPI.clientApi().requestMinimapIconRefresh();
    }

    private static void onMapIcon(FTBChunksClientEvent.MapIcon event) {
        AddMapIconEvent.Data data = event.getEventData();
        RadarMapClient.markers(data.dimension()).forEach((marker, amount) -> data.add(new PatchIcon(marker, amount)));
    }

    private static final class PatchIcon extends MapIcon.SimpleMapIcon {

        private final Component name;
        private final String amount;

        private final boolean oil;

        private PatchIcon(PatchMarker marker, long amount) {
            super(new Vec3(marker.x() + 0.5, marker.y(), marker.z() + 0.5),
                    ItemIcon.ofItem(BuiltInRegistries.ITEM.getValue(Identifier.parse(icon(marker)))));
            this.name = Component.translatable("map.factoryworks.patch." + marker.resource());
            this.oil = marker.resource().equals(SectorPatches.CRUDE_OIL);
            this.amount = oil ? PatchAmount.yield(amount, WellYield.fromCorpus().normal()) : PatchAmount.format(amount);
        }

        private static String icon(PatchMarker marker) {
            return marker.resource().equals(SectorPatches.CRUDE_OIL)
                    ? OIL_ICON
                    : OreResource.of(marker.resource()).drop();
        }

        @Override
        public void addTooltip(TooltipList list) {
            list.add(Component.translatable(oil ? "map.factoryworks.patch.yield" : "map.factoryworks.patch.amount",
                    name, amount));
        }
    }
}
