package com.planetaryfactory.core.compat;

import java.util.UUID;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.radar.ChartOwners;
import com.planetaryfactory.core.radar.RadarBlock;
import com.planetaryfactory.core.radar.RadarBlockEntity;
import com.planetaryfactory.core.radar.RadarChartData;
import com.planetaryfactory.core.radar.Sector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * A developer's view of a Radar's charting, off by default in Jade's plugin config: until the chart
 * reaches FTB Chunks' map (#369) nothing else in game shows it. Untranslated for that reason.
 */
@WailaPlugin
public class RadarJadePlugin implements IWailaPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "radar_debug");

    private static final String OWNER = "RadarOwner";
    private static final String NEXT_X = "RadarNextX";
    private static final String NEXT_Z = "RadarNextZ";
    private static final String CURSOR = "RadarCursor";
    private static final String SWEEP = "RadarSweep";
    private static final String PROGRESS = "RadarProgress";
    private static final String CHARTED = "RadarCharted";

    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof RadarBlockEntity radar)
                    || !(accessor.getLevel() instanceof ServerLevel level)) {
                return;
            }
            Sector next = radar.nextSector(level);
            tag.putInt(NEXT_X, next.x());
            tag.putInt(NEXT_Z, next.z());
            tag.putInt(CURSOR, radar.nextIndex(level));
            tag.putInt(SWEEP, radar.sweepSize());
            tag.putLong(PROGRESS, radar.progress());
            UUID owner = radar.owner();
            if (owner != null) {
                tag.putString(OWNER, owner.toString());
                tag.putInt(CHARTED, RadarChartData.get(level.getServer())
                        .sectors(ChartOwners.teamOf(owner), level.dimension().identifier().toString())
                        .size());
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    private static final IBlockComponentProvider TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(CURSOR)) {
                return;
            }
            long perSector = RadarBlockEntity.spec().fePerSector();
            long perTick = RadarBlockEntity.spec().fePerTick();
            long left = perSector - data.getLongOr(PROGRESS, 0L);
            int x = data.getIntOr(NEXT_X, 0);
            int z = data.getIntOr(NEXT_Z, 0);
            tooltip.add(Component.literal("Next sector (%d, %d), blocks %d..%d, %d..%d".formatted(
                    x, z, x * Sector.SIZE, x * Sector.SIZE + Sector.SIZE - 1,
                    z * Sector.SIZE, z * Sector.SIZE + Sector.SIZE - 1)));
            tooltip.add(Component.literal("Long-range sector %d of %d, due in %.1f s at full power".formatted(
                    data.getIntOr(CURSOR, 0) + 1, data.getIntOr(SWEEP, 0), left / (perTick * 20.0))));
            tooltip.add(data.contains(OWNER)
                    ? Component.literal("Team chart: %d sectors here, owner %s".formatted(
                            data.getIntOr(CHARTED, 0), data.getStringOr(OWNER, "")))
                    : Component.literal("No owner: charts nothing"));
        }

        @Override
        public boolean enabledByDefault() {
            return false;
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, RadarBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, RadarBlock.class);
    }
}
