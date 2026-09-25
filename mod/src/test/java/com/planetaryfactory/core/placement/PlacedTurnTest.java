package com.planetaryfactory.core.placement;

import java.util.List;
import java.util.function.UnaryOperator;

import com.planetaryfactory.core.placement.PlacedTurn.Denial;
import com.planetaryfactory.core.placement.PlacedTurn.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlacedTurnTest {

    private static final UnaryOperator<String> VANILLA = state -> state + "+turned";
    private static final List<Denial<String>> NONE_DENIED = List.of();
    private static final PlacedTurn.Refit<String> FITS = turned -> null;

    @Test
    @DisplayName("a block with no contract takes vanilla's turn")
    void vanillaByDefault() {
        assertEquals(PlacedTurn.turned("tile+turned"), PlacedTurn.decide("tile", NONE_DENIED, null, VANILLA, FITS));
    }

    @Test
    @DisplayName("vanilla's turn that changes nothing is not applied")
    void unchangedIsUnturned() {
        assertEquals(PlacedTurn.unturned(), PlacedTurn.decide("stone", NONE_DENIED, null, state -> state, FITS));
    }

    @Test
    @DisplayName("a block's own contract answers instead of vanilla")
    void contractAnswers() {
        assertEquals(PlacedTurn.turned("machine, contents kept"),
                PlacedTurn.decide("machine", NONE_DENIED, state -> PlacedTurn.turned(state + ", contents kept"), VANILLA, FITS));
        assertEquals(PlacedTurn.refused("footprint"),
                PlacedTurn.decide("machine", NONE_DENIED, state -> PlacedTurn.refused("footprint"), VANILLA, FITS));
    }

    @Test
    @DisplayName("a contract's turn that changes nothing is not applied")
    void contractUnchangedIsUnturned() {
        assertEquals(PlacedTurn.unturned(),
                PlacedTurn.decide("machine", NONE_DENIED, PlacedTurn::turned, VANILLA, FITS));
    }

    @Test
    @DisplayName("a denied block is refused with its reason, whatever vanilla or its contract would do")
    void denialRefuses() {
        List<Denial<String>> denials = List.of(new Denial<>(state -> state.startsWith("splitter"), "splitter"));
        assertEquals(PlacedTurn.refused("splitter"), PlacedTurn.decide("splitter half", denials, null, VANILLA, FITS));
        assertEquals(PlacedTurn.refused("splitter"),
                PlacedTurn.decide("splitter half", denials, PlacedTurn::turned, VANILLA, FITS));
        assertEquals(PlacedTurn.turned("tile+turned"), PlacedTurn.decide("tile", denials, null, VANILLA, FITS));
    }

    @Test
    @DisplayName("the first denial that matches names the reason")
    void firstDenialWins() {
        List<Denial<String>> denials = List.of(
                new Denial<>(state -> state.contains("slope"), "slope"),
                new Denial<>(state -> state.contains("tile"), "tile"));
        assertEquals(PlacedTurn.refused("slope"), PlacedTurn.decide("tile on a slope", denials, null, VANILLA, FITS));
    }

    @Test
    @DisplayName("vanilla's turn that does not fit where it stands is refused with the refit's reason")
    void refitRefuses() {
        PlacedTurn.Refit<String> sloping = turned -> turned.startsWith("corner") ? "slope turns" : null;
        assertEquals(PlacedTurn.refused("slope turns"), PlacedTurn.decide("corner", NONE_DENIED, null, VANILLA, sloping));
        assertEquals(PlacedTurn.turned("tile+turned"), PlacedTurn.decide("tile", NONE_DENIED, null, VANILLA, sloping));
    }

    @Test
    @DisplayName("the refit is not asked of a turn that changes nothing, nor of a block's own contract")
    void refitAskedOnlyOfVanillaTurns() {
        PlacedTurn.Refit<String> refuses = turned -> "refused";
        assertEquals(PlacedTurn.unturned(), PlacedTurn.decide("stone", NONE_DENIED, null, state -> state, refuses));
        assertEquals(PlacedTurn.turned("machine, contents kept"), PlacedTurn.decide("machine", NONE_DENIED,
                state -> PlacedTurn.turned(state + ", contents kept"), VANILLA, refuses));
    }
}
