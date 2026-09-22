package com.planetaryfactory.core.radar;

/** Factorio's chunk: 32x32 blocks on a 32-block grid, which is four Minecraft chunks (#368). */
public record Sector(int x, int z) {

    public static final int SIZE = 32;
    public static final int CHUNKS_PER_SIDE = SIZE / 16;

    public static Sector ofBlock(int blockX, int blockZ) {
        return new Sector(Math.floorDiv(blockX, SIZE), Math.floorDiv(blockZ, SIZE));
    }

    public Sector offset(int dx, int dz) {
        return new Sector(x + dx, z + dz);
    }

    public int minBlockX() {
        return x * SIZE;
    }

    public int minBlockZ() {
        return z * SIZE;
    }

    public int minChunkX() {
        return x * CHUNKS_PER_SIDE;
    }

    public int minChunkZ() {
        return z * CHUNKS_PER_SIDE;
    }

    public long pack() {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static Sector unpack(long packed) {
        return new Sector((int) (packed >> 32), (int) packed);
    }
}
