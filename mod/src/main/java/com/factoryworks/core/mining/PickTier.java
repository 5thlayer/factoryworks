package com.factoryworks.core.mining;

/**
 * The two rungs of the Engineer's Pick, and Factorio's two mining speeds (ADR-0039).
 *
 * <p>Factorio's character mines at {@code 0.5}, and {@code steel-axe} carries a
 * {@code character-mining-speed} modifier of {@code 1}. That modifier is a <em>fraction</em> --
 * Factorio applies it as {@code base * (1 + modifier)} -- so it is +100% and leaves the character
 * at {@code 1.0}, not at {@code 1.5}. Those
 * two speeds are Factorio's and are kept; a resource's seconds-per-item is
 * {@code mining_time / mining_speed}, so the ratio between the tiers is Factorio's too.
 *
 * <p><b>The mining time is the pack's, not Factorio's.</b> Factorio gives Terra's four resources
 * {@code mining_time: 1}, which would be two seconds by hand -- and ADR-0039 shipped exactly that,
 * then failed its own human-on-delivery check. Two seconds is Factorio's number inside Factorio's
 * economy, where the engineer hand-mines perhaps thirty ore before a burner drill takes over; here
 * the starting area holds around 1150 ore blocks and every other block in the game breaks in well
 * under a second, so the ore alone felt three to five times heavier than the world around it. The
 * amendment halves the time and keeps everything else: still flat across the four resources, still
 * halved by {@code steel-axe}, still checkable as one number rather than vanilla's hardness spread.
 *
 * <p>{@code tests/factorio/test_resource_extract.py} asserts the speeds and the mining time in this
 * file against the character's {@code mining_speed}, {@code steel-axe}'s modifier (a fraction, not
 * an addend) and the resources' {@code mining_time}.
 */
public enum PickTier {
    /** Factorio's bare character: {@code mining_speed 0.5}, so one second an ore. */
    IRON("engineers_iron_pick", 0.5f),
    /** After {@code steel-axe}: {@code mining_speed 1.0}, so half of one. */
    STEEL("engineers_steel_pick", 1.0f);

    /**
     * The mining time every one of Terra's resources carries -- the pack's number, not Factorio's.
     *
     * <p>Factorio's is {@code 1}. This is half of it, and the halving is the whole of ADR-0039's
     * amendment: flat across the four resources as before, and still the only mining number in the
     * pack that is stated rather than inherited from a hardness table.
     */
    public static final float MINING_TIME = 0.5f;

    /**
     * Vanilla's own iron-pickaxe speed, expressed per unit of Factorio mining speed.
     *
     * <p>{@code 12 x 0.5 = 6}, which is exactly {@code Tiers.IRON}, so the Iron Pick is a vanilla
     * iron pickaxe everywhere the tag does not reach. The Steel Pick doubles it for the same
     * reason it halves the flat time: {@code steel-axe} adds 1 to a base mining speed of 0.5, and
     * ADR-0039 records the research's outcome as "mining doubles" rather than "ore mining
     * doubles" -- Factorio's own {@code character-mining-speed} applies to everything the
     * character mines, so stopping the boost at the tag would be the divergence, not carrying it.
     *
     * <p>That does put the Steel Pick at 12, above netherite's 9. Nothing is being outclassed:
     * ADR-0034's sweep leaves the pack no other pickaxe at any tier, so vanilla's ladder is not a
     * ceiling this has to fit under -- it is a ladder the pack does not have.
     */
    private static final float VANILLA_SPEED_PER_MINING_SPEED = 12.0f;

    private final String id;
    private final float miningSpeed;

    PickTier(String id, float miningSpeed) {
        this.id = id;
        this.miningSpeed = miningSpeed;
    }

    public String id() {
        return id;
    }

    public float miningSpeed() {
        return miningSpeed;
    }

    /** The speed this tier reports on a block outside Factorio's flat mining time. */
    public float vanillaSpeed() {
        return VANILLA_SPEED_PER_MINING_SPEED * miningSpeed;
    }

    /** Seconds to take one of Terra's resources, which is the whole number the player feels. */
    public float secondsPerResource() {
        return MINING_TIME / miningSpeed;
    }
}
