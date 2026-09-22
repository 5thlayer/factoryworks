package com.planetaryfactory.core.radar.ftb;

import com.planetaryfactory.core.ore.OreResource;
import com.planetaryfactory.core.radar.PatchMarker;
import com.planetaryfactory.core.radar.client.ChartMarkerRenderer;
import com.planetaryfactory.core.radar.client.RadarMapClient;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.AddMapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.neoforge.FTBChunksClientEvent;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.util.TooltipList;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Draws the patch markers as FTB Chunks map icons, through its public icon API (ADR-0079). The
 * icons are rebuilt from {@link RadarMapClient} on each refresh. Client only, loaded only when
 * {@code ftbchunks} is.
 */
public final class FtbMapMarkers implements ChartMarkerRenderer {

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
        for (PatchMarker marker : RadarMapClient.markers(data.dimension())) {
            data.add(new PatchIcon(marker));
        }
    }

    private static final class PatchIcon extends MapIcon.SimpleMapIcon {

        private final Component name;

        private PatchIcon(PatchMarker marker) {
            super(new Vec3(marker.x() + 0.5, marker.y(), marker.z() + 0.5),
                    Color4I.rgb(OreResource.of(marker.resource()).corpus().mapColor()).withBorder(Color4I.WHITE, false));
            this.name = Component.translatable("map.planetaryfactory.patch." + marker.resource());
        }

        @Override
        public void addTooltip(TooltipList list) {
            list.add(name);
        }
    }
}
