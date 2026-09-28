package com.planetaryfactory.core.start;

import java.util.List;

/**
 * The first-join message: the opening's two surprises, for a player who never opens the book (#494).
 * A message, not a script (#100).
 */
public final class Welcome {

    public static final List<String> LINES = List.of(
            "message.planetaryfactory.welcome.crafting",
            "message.planetaryfactory.welcome.reach");

    private Welcome() {
    }
}
