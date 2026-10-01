package com.factoryworks.core.mixin.oritech;

import io.github._5thlayer.wireworks.EnergyOwner;
import com.factoryworks.core.fluid.SteamChainCorpus;
import com.factoryworks.core.fluid.SteamEngineSpec;
import com.factoryworks.core.machine.footprint.FootprintPartBlock;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.oritech.block.base.entity.MultiblockGeneratorBlockEntity;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.util.Geometry;

/**
 * Oritech's Steam Engine, calibrated to Factorio (#282, ADR-0062).
 *
 * <p>Oritech's shape is kept on purpose: chaining, the fill-driven speed and the efficiency curve.
 * Two of its methods are replaced whole, because each change reaches into the middle of one.
 *
 * <ul>
 *   <li><b>{@code tickMaster}</b> burns and makes what {@link SteamEngineSpec} says, with the half
 *       millibucket carried rather than floored, returns no water -- #282's
 *       "Returned water is 0" holds because no insert exists, so there is no number to test --
 *       burns only what the FE buffer has room for, and sizes the row's tank and FE
 *       buffer to the row on every tick. It consults no recipe: the sweep removes Oritech's, and
 *       the spec already owns everything one would say.
 *   <li><b>{@code setupMaster}</b> is Oritech's scan with one more stop: an engine already answering
 *       to another live master is a boundary, not a slave. Oritech let two masters that received
 *       steam before either scanned both claim the empty engines between them, counting each twice.
 *       It finds the pack's engine by class and resolves a part to its anchor (ADR-0077).
 * </ul>
 *
 * <p>It is also an {@link EnergyOwner} (#292): a slave holds no FE, so a pole that reaches one draws
 * from its master, wherever the master stands.
 *
 * <p>Both were read off the installed 2.0.0-exp6 jar, not the 1.21.1 source clone; the signatures
 * differ. Extends Oritech's base class only so the protected members it inherits are reachable.
 */
@Mixin(SteamEngineEntity.class)
public abstract class SteamEngineEntityMixin extends MultiblockGeneratorBlockEntity
        implements EnergyOwner {

    /** Oritech's scan reach along the engine's facing axis, read from its class, not restated. */
    @Shadow
    @Final
    private static int MAX_CHAIN_SIZE;

    /** Oritech's top speed; a tank holding more than a shrunken row's capacity would exceed it. */
    @Shadow
    @Final
    private static int MAX_SPEED;

    @Shadow
    @Final
    private Set<SteamEngineEntity> slaves;

    @Shadow
    public SteamEngineEntity.SteamEngineSyncPacket clientStats;

    @Shadow
    private float getSteamProcessingSpeed() {
        throw new AssertionError();
    }

    @Shadow
    private float getSteamEnergyEfficiency(float speed) {
        throw new AssertionError();
    }

    @Shadow
    private void spawnParticles() {
        throw new AssertionError();
    }

    @Unique
    private SteamEngineSpec factoryworks$spec;

    @Unique
    private SteamEngineSpec.Carry factoryworks$carry = SteamEngineSpec.Carry.NONE;

    protected SteamEngineEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }

    @Override
    public BlockPos wireworks$energyOwner() {
        SteamEngineEntity self = (SteamEngineEntity) (Object) this;
        return self.inSlaveMode() ? self.master.getBlockPos() : null;
    }

    @Unique
    private SteamEngineSpec factoryworks$spec() {
        if (factoryworks$spec == null) {
            factoryworks$spec = SteamEngineSpec.fromCorpus(SteamChainCorpus.get(),
                    speed -> getSteamEnergyEfficiency((float) speed));
        }
        return factoryworks$spec;
    }

    /**
     * The FE buffer Oritech's own resize path reads. {@code updateEnergyContainer} runs on load and
     * on every addon change and sets the capacity from here, so answering Oritech's config would undo
     * the per-row size. {@code slaves} is still null while the super constructor calls this.
     */
    @Inject(method = "getDefaultCapacity", at = @At("HEAD"), cancellable = true)
    private void factoryworks$defaultCapacity(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(factoryworks$spec().bufferCapacity(slaves == null ? 1 : slaves.size() + 1));
    }

    /** A lone engine's tank is Factorio's from the moment it exists, before any steam reaches it. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void factoryworks$sizeAlone(BlockPos pos, BlockState state, CallbackInfo ci) {
        factoryworks$sizeTo(1);
    }

    @Unique
    private void factoryworks$sizeTo(int rowLength) {
        SteamEngineSpec spec = factoryworks$spec();
        ((FluidStacksCapacityAccessor) boilerStorage).factoryworks$setCapacity(
                spec.tankCapacity(rowLength));
        energyStorage.setCapacity(spec.bufferCapacity(rowLength));
    }

    @Inject(method = "tickMaster", at = @At("HEAD"), cancellable = true)
    private void factoryworks$tickMaster(CallbackInfo ci) {
        ci.cancel();
        int rowLength = slaves.size() + 1;
        factoryworks$sizeTo(rowLength);

        if (energyStorage.getAmountAsLong() >= energyStorage.getCapacityAsLong()
                && OritechConfig.generators.steamEngineData.stopOnEnergyFull.get()) {
            return;
        }
        // No recipe lookup. Oritech gates the engine on an `oritech:steam_engine` recipe, which
        // decides nothing SteamEngineSpec does not already decide -- the rate is the spec's and the
        // tank takes only #c:steam -- and the pack's default-deny sweep removes it, which left a full
        // tank burning nothing with no line in any log.

        SteamEngineSpec spec = factoryworks$spec();
        float speed = Math.min(getSteamProcessingSpeed(), MAX_SPEED);
        long room = energyStorage.getCapacityAsLong() - energyStorage.getAmountAsLong();
        SteamEngineSpec.Request asked = spec.request(speed, rowLength, factoryworks$carry, room);
        ResourceHandler<FluidResource> input = boilerStorage.getInputContainer();
        SteamEngineSpec.Tick made;
        try (Transaction transaction = Transaction.openRoot()) {
            int drawn = asked.steam() > 0
                    ? input.extract(input.getResource(0), asked.steam(), transaction)
                    : 0;
            made = spec.burn(drawn, speed, asked.carry(), room);
            energyStorage.internalInsert(made.energy(), transaction);
            transaction.commit();
        }
        factoryworks$carry = made.carry();
        if (made.steam() <= 0) {
            // Oritech's own early return: a tick that burnt nothing neither animates nor counts as work.
            return;
        }

        clientStats = new SteamEngineEntity.SteamEngineSyncPacket(worldPosition, speed,
                getSteamEnergyEfficiency(speed), made.energy(), made.steam(), slaves.size());
        spawnParticles();
        lastWorkedAt = level.getGameTime();
        progress.set((int) (speed * 100.0F));
    }

    @Inject(method = "setupMaster", at = @At("HEAD"), cancellable = true)
    private void factoryworks$setupMaster(CallbackInfo ci) {
        ci.cancel();
        SteamEngineEntity self = (SteamEngineEntity) (Object) this;
        slaves.clear();
        for (int direction = -1; direction <= 1; direction += 2) {
            for (int step = 1; step <= MAX_CHAIN_SIZE; step++) {
                BlockPos at = new BlockPos(Geometry.offsetToWorldPosition(getFacing(),
                        new Vec3i(step * direction, 0, 0), worldPosition));
                BlockState state = level.getBlockState(at);
                if (state.getBlock() instanceof FootprintPartBlock part) {
                    at = part.machine().anchorOf(at, state);
                }
                // By class, not by Oritech's type: the pack's engine has a type of its own (ADR-0077).
                if (!(level.getBlockEntity(at) instanceof SteamEngineEntity engine)) {
                    break;
                }
                if (!engine.isAssembled(engine.getBlockState())
                        || !engine.boilerStorage.getInStack().isEmpty()
                        || (engine.inSlaveMode() && engine.master != self)) {
                    break;
                }
                slaves.add(engine);
                engine.masterHeartbeat = level.getGameTime();
                engine.master = self;
            }
        }
    }
}
