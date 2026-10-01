package com.factoryworks.core.chest;

/** The two chests above vanilla's 27 slots (#540), each a block of nine columns. */
public enum ChestTier {
    IRON("iron_chest", 4, 2.5f),
    STEEL("steel_chest", 6, 3.0f);

    private final String blockName;
    private final int rows;
    private final float strength;

    ChestTier(String blockName, int rows, float strength) {
        this.blockName = blockName;
        this.rows = rows;
        this.strength = strength;
    }

    public String blockName() {
        return blockName;
    }

    public int rows() {
        return rows;
    }

    public float strength() {
        return strength;
    }

    public int slots() {
        return rows * 9;
    }
}
