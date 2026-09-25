package com.planetaryfactory.core.placement;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

/**
 * What Rotate or Reverse Rotate does to a placed block (#405, ADR-0087), over any block state
 * {@code S}: a denied block is refused, a block with its own contract answers for itself, and any
 * other takes vanilla's turn unless it does not fit where it stands. A turn that changes nothing is
 * not applied.
 */
public final class PlacedTurn {

    private PlacedTurn() {
    }

    public sealed interface Verdict<S> {
    }

    public record Turned<S>(S state) implements Verdict<S> {
    }

    public record Refused<S>(String reason) implements Verdict<S> {
    }

    public record Unturned<S>() implements Verdict<S> {
    }

    /** A block whose vanilla turn is wrong, and the reason it is refused. */
    public record Denial<S>(Predicate<S> matches, String reason) {
    }

    /** A block's own answer to being turned. */
    @FunctionalInterface
    public interface Contract<S> {
        Verdict<S> turn(S state);
    }

    /** Why vanilla's turned state does not fit where it stands, or {@code null} if it does. */
    @FunctionalInterface
    public interface Refit<S> {
        @Nullable
        String refusal(S turned);
    }

    public static <S> Verdict<S> turned(S state) {
        return new Turned<>(state);
    }

    public static <S> Verdict<S> refused(String reason) {
        return new Refused<>(reason);
    }

    public static <S> Verdict<S> unturned() {
        return new Unturned<>();
    }

    public static <S> Verdict<S> decide(S state, List<Denial<S>> denials, @Nullable Contract<S> own,
                                        UnaryOperator<S> vanilla, Refit<S> refit) {
        for (Denial<S> denial : denials) {
            if (denial.matches().test(state)) {
                return refused(denial.reason());
            }
        }
        if (own != null) {
            return unlessUnchanged(state, own.turn(state));
        }
        S turned = vanilla.apply(state);
        if (turned.equals(state)) {
            return unturned();
        }
        String refusal = refit.refusal(turned);
        return refusal != null ? refused(refusal) : turned(turned);
    }

    private static <S> Verdict<S> unlessUnchanged(S state, Verdict<S> verdict) {
        return verdict instanceof Turned<S>(S turned) && turned.equals(state) ? unturned() : verdict;
    }
}
