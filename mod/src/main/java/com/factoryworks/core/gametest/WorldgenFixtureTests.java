package com.factoryworks.core.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.factoryworks.core.worldgen.WaterCensus;
import com.factoryworks.core.worldgen.WaterFixture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.slf4j.Logger;

/**
 * The world-load fixture harness (#356): each {@link WaterFixture} row, on each of its seeds,
 * sampled through the datapack's own generator without generating a chunk.
 *
 * <p>The GameTest world is flat, so the dimension is decoded from the datapack's stem file rather
 * than read off the level.
 */
final class WorldgenFixtureTests {

    private static final Logger LOGGER = LogUtils.getLogger();

    private WorldgenFixtureTests() {
    }

    static void register(PFGameTests.Registrar registrar) {
        for (WaterFixture row : WaterFixture.ROWS) {
            for (long seed : row.seeds()) {
                registrar.test("worldgen_" + row.body() + "_seed_" + seed, 20, helper -> survey(helper, row, seed));
            }
        }
    }

    private static void survey(GameTestHelper helper, WaterFixture row, long seed) {
        var access = helper.getLevel().registryAccess();
        LevelStem stem;
        try (var reader = helper.getLevel().getServer().getResourceManager()
                .openAsReader(Identifier.withDefaultNamespace(row.stem()))) {
            stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access),
                    JsonParser.parseReader(reader)).getOrThrow();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!(stem.generator() instanceof NoiseBasedChunkGenerator generator)) {
            helper.fail(row.stem() + " is not a noise generator: " + stem.generator().getClass());
            return;
        }
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        int seaLevel = settings.seaLevel();
        LevelHeightAccessor height = LevelHeightAccessor.create(
                settings.noiseSettings().minY(), settings.noiseSettings().height());
        RandomState random = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed);
        var biomes = generator.getBiomeSource();
        var sampler = random.sampler();

        WaterCensus census = new WaterCensus(row, seaLevel);
        int side = 2 * row.radius() / row.step() + 1;
        double[] continents = new double[side * side];
        int n = 0;
        for (int x = -row.radius(); x <= row.radius(); x += row.step()) {
            for (int z = -row.radius(); z <= row.radius(); z += row.step()) {
                int surface = surface(generator, x, z, height, random);
                Holder<Biome> biome = biomes.getNoiseBiome(
                        x >> 2, Math.max(surface, seaLevel) >> 2, z >> 2, sampler);
                census.column(surface, is(biome, row.seaBiome()), is(biome, row.shoreBiome()));
                // Logged as quantiles, for tuning the terrain's thresholds against.
                continents[n++] = random.router().continents().compute(new DensityFunction.SinglePointContext(x, 0, z));
            }
        }

        BlockPos spawn = sampler.findSpawnPosition();
        // The jigsaw's reach is a box, not a disc.
        int reach = row.startReach();
        for (int dx = -reach; dx <= reach; dx += row.startStep()) {
            for (int dz = -reach; dz <= reach; dz += row.startStep()) {
                census.startColumn(surface(generator, spawn.getX() + dx, spawn.getZ() + dz, height, random));
            }
        }

        Arrays.sort(continents);
        StringBuilder quantiles = new StringBuilder();
        for (int q = 1; q < 100; q++) {
            quantiles.append(String.format(Locale.ROOT, "%d=%.4f ", q, continents[q * n / 100]));
        }
        LOGGER.info("FIXTURE {} seed {} continents quantiles: {}", row.body(), seed, quantiles);
        LOGGER.info("FIXTURE {} seed {} spawn {}: {}", row.body(), seed, spawn.toShortString(), census.summary());
        List<String> failures = census.failures();
        if (!failures.isEmpty()) {
            helper.fail(row.body() + " seed " + seed + ": " + String.join("; ", failures));
            return;
        }
        helper.succeed();
    }

    private static int surface(NoiseBasedChunkGenerator generator, int x, int z, LevelHeightAccessor height,
            RandomState random) {
        return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, random);
    }

    private static boolean is(Holder<Biome> biome, String id) {
        return biome.unwrapKey().map(key -> key.identifier().toString().equals(id)).orElse(false);
    }
}
