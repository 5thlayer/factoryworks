package com.planetaryfactory.core.dismantle;

/**
 * The blocks one Dismantle takes up together, and which two of them are joined (ADR-0086). By
 * default two members are joined when they touch.
 */
public interface DismantleFamily<N> {

    boolean member(N node);

    /** The positions touching {@code node}, members or not. */
    Iterable<N> neighbours(N node);

    /** Whether two touching members are joined; asked only of members. */
    default boolean joined(N a, N b) {
        return true;
    }
}
