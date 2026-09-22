package com.planetaryfactory.core.radar.ftb;

import com.planetaryfactory.core.ore.OreResource;
import com.planetaryfactory.core.radar.PatchAmount;
import com.planetaryfactory.core.radar.PatchMarker;
import com.planetaryfactory.core.radar.client.ChartMarkerRenderer;
import com.planetaryfactory.core.radar.client.RadarMapClient;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.AddMapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.api.neoforge.FTBChunksClientEvent;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.util.TooltipList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

        private PatchIcon(PatchMarker marker, long amount) {
            super(new Vec3(marker.x() + 0.5, marker.y(), marker.z() + 0.5),
                    ItemIcon.ofItem(BuiltInRegistries.ITEM.getValue(
                            Identifier.parse(OreResource.of(marker.resource()).drop()))));
            this.name = Component.translatable("map.planetaryfactory.patch." + marker.resource());
            this.amount = PatchAmount.format(amount);
        }

        @Override
        public void addTooltip(TooltipList list) {
            list.add(Component.translatable("map.planetaryfactory.patch.amount", name, amount));
        }

        /** Factorio labels a patch's amount on the map; the minimap is too small to carry it. */
        @Override
        public void draw(MapType mapType, GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                boolean outsideVisibleArea, int alpha) {
            super.draw(mapType, graphics, x, y, w, h, outsideVisibleArea, alpha);
            if (!mapType.isLargeMap() || outsideVisibleArea) {
                return;
            }
            Font font = Minecraft.getInstance().font;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + w / 2f, y + h);
            graphics.pose().scale(0.5f, 0.5f);
            graphics.text(font, amount, -font.width(amount) / 2, 1, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }
    }
}
