package com.factoryworks.core.chest;

/** The two chests above vanilla's 27 slots (#540), each a block of nine columns. */
public enum ChestTier {
    IRON("iron_chest", 4, 2.5f, "minecraft", "copper_exposed"),
    STEEL("steel_chest", 6, 3.0f, "railcraft", "void_chest");

    private final String blockName;
    private final int rows;
    private final float strength;
    private final String spriteNamespace;
    private final String spritePath;

    ChestTier(String blockName, int rows, float strength, String spriteNamespace, String spritePath) {
        this.blockName = blockName;
        this.rows = rows;
        this.strength = strength;
        this.spriteNamespace = spriteNamespace;
        this.spritePath = spritePath;
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

    /** The sprite under `entity/chest/` on the chest sheet, in its owner's namespace (#541). */
    public String spriteNamespace() {
        return spriteNamespace;
    }

    public String spritePath() {
        return spritePath;
    }

    public int slots() {
        return rows * 9;
    }
}
