package com.planetaryfactory.core.assembler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A plan on the queue, and everything about it that survives a logout: which step it is on, how many
 * of that step's crafts are done, how far into the next one, and what it is holding.
 *
 * <p>The buffer is the plan's own pocket. It starts as the reservation Start took, loses a step's
 * inputs and gains its outputs as each craft finishes, and hands the player whatever no remaining
 * step needs. So it is exactly "the remaining reservation plus the intermediates already produced" --
 * which is what ADR-0038 says a cancellation refunds, without the queue having to reconstruct it.
 *
 * <p>Mutable, and deliberately not a record: it is ticked in place sixty times a second, and the
 * copy a record would demand per tick buys nothing.
 */
public final class QueuedPlan {

    private final CraftingPlan plan;
    private final ItemBag buffer;
    private int stepIndex;
    private int craftsDone;
    private int progressTicks;

    QueuedPlan(CraftingPlan plan, ItemBag buffer, int stepIndex, int craftsDone, int progressTicks) {
        this.plan = plan;
        this.buffer = buffer.copy();
        this.stepIndex = stepIndex;
        this.craftsDone = craftsDone;
        this.progressTicks = progressTicks;
    }

    /** Restores one, which is what the data attachment does on login. */
    public static QueuedPlan restored(
            CraftingPlan plan, Map<String, Integer> buffer, int stepIndex, int craftsDone, int progressTicks) {
        return new QueuedPlan(plan, ItemBag.of(buffer), stepIndex, craftsDone, progressTicks);
    }

    public CraftingPlan plan() {
        return plan;
    }

    /** What the plan is holding. A copy: the queue is the only thing that may change it. */
    public Map<String, Integer> buffer() {
        return buffer.asMap();
    }

    public int stepIndex() {
        return stepIndex;
    }

    /** How many of the current step's crafts are finished. */
    public int craftsDone() {
        return craftsDone;
    }

    public int progressTicks() {
        return progressTicks;
    }

    /** The step being crafted, or empty once the last one is done. */
    public CraftStep currentStep() {
        return stepIndex < plan.steps().size() ? plan.steps().get(stepIndex) : null;
    }

    /** Zero to one, for a progress bar; one when there is nothing left to craft. */
    public float progress() {
        CraftStep step = currentStep();
        if (step == null) return 1.0f;
        if (step.durationTicks() <= 0) return 1.0f;
        return Math.min(1.0f, (float) progressTicks / step.durationTicks());
    }

    void advanceTick() {
        progressTicks++;
    }

    /**
     * Consumes one craft's share of the step's inputs and banks its share of the outputs, moving to
     * the next step once the last craft is done (#289).
     *
     * <p>It throws rather than under-consuming when the buffer cannot cover the craft. A resolved
     * plan is complete by construction (ADR-0038) and was paid for whole at Start, so a step that
     * cannot be fed is a resolver bug -- and the alternative to a loud failure is a plan that
     * silently crafts something out of nothing, which is a duplication bug nobody would ever trace
     * back to here.
     */
    void completeCraft() {
        CraftStep step = plan.steps().get(stepIndex);
        for (ItemAmount input : step.inputs()) {
            int wanted = step.share(input.count(), craftsDone);
            if (buffer.count(input.item()) < wanted) {
                throw new IllegalStateException("Plan " + plan.id() + " step " + stepIndex
                        + " wants " + wanted + " " + input.item()
                        + " but the plan is holding " + buffer.count(input.item()));
            }
        }
        for (ItemAmount input : step.inputs()) {
            buffer.remove(input.item(), step.share(input.count(), craftsDone));
        }
        for (ItemAmount output : step.outputs()) {
            buffer.add(output.item(), step.share(output.count(), craftsDone));
        }
        craftsDone++;
        progressTicks = 0;
        if (craftsDone >= step.crafts()) {
            stepIndex++;
            craftsDone = 0;
        }
    }

    /** Everything the plan is holding, as a list that survives the bag being changed. */
    List<ItemAmount> held() {
        return buffer.amounts();
    }

    void takeFromBuffer(String item, int count) {
        buffer.remove(item, count);
    }

    boolean finished() {
        return stepIndex >= plan.steps().size();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof QueuedPlan that
                && stepIndex == that.stepIndex
                && craftsDone == that.craftsDone
                && progressTicks == that.progressTicks
                && plan.equals(that.plan)
                && buffer.equals(that.buffer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plan, buffer, stepIndex, craftsDone, progressTicks);
    }

    @Override
    public String toString() {
        return "QueuedPlan[" + plan.rootItem() + " x" + plan.amount()
                + ", step " + stepIndex + "/" + plan.steps().size() + " craft " + craftsDone
                + ", " + progressTicks + " ticks, holding " + buffer + "]";
    }
}
