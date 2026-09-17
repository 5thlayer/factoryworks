package com.planetaryfactory.core.energy;

import com.planetaryfactory.core.PFDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Wiring and unwiring poles by hand with the Engineer's Pick (#296, ADR-0068).
 *
 * <p>The first click on a pole holds its base on the Pick; the second is {@link LevelWires#click}.
 * Feedback is in the world and never text: a sound per outcome, and particles along a wire made or
 * cut. The rules are {@link PoleWiring} and {@link PendingEnd}; this is where they meet a click.
 */
public final class PoleWireGesture {

    /** Particles per block of wire. */
    private static final double PARTICLES_PER_BLOCK = 2.0;

    private PoleWireGesture() {
    }

    /** A right-click with the Pick on a block; passes on anything that is not a pole. */
    public static InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (!(level.getBlockState(clicked).getBlock() instanceof SupplyAreaPoleBlock)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack pick = context.getItemInHand();
        BlockPos base = PoleColumn.baseOf(level, clicked);
        GlobalPos pending = pick.get(PFDataComponents.PENDING_WIRE.get());
        if (pending == null || !pending.dimension().equals(level.dimension())) {
            pick.set(PFDataComponents.PENDING_WIRE.get(), GlobalPos.of(level.dimension(), base));
            return InteractionResult.SUCCESS_SERVER;
        }
        PoleWiring.Click click = LevelWires.of(server).click(server, pending.pos(), base);
        switch (click) {
            case WIRED -> {
                pick.remove(PFDataComponents.PENDING_WIRE.get());
                play(server, base, SoundEvents.TRIPWIRE_ATTACH);
                alongWire(server, pending.pos(), base, ParticleTypes.ELECTRIC_SPARK);
            }
            case CUT -> {
                pick.remove(PFDataComponents.PENDING_WIRE.get());
                play(server, base, SoundEvents.TRIPWIRE_DETACH);
                alongWire(server, pending.pos(), base, ParticleTypes.SMOKE);
            }
            case CANCELLED -> pick.remove(PFDataComponents.PENDING_WIRE.get());
            // The end stays held: a refusal changes nothing, the held end included.
            // Not DISPENSER_FAIL: it plays random/click, the same file as TRIPWIRE_ATTACH. Played at
            // the player, not the pole: vanilla attenuates the crafter's fail over 3 blocks, and a
            // pole beyond wire reach is always further than that.
            case REFUSED -> {
                Vec3 at = context.getPlayer() != null ? context.getPlayer().position() : context.getClickLocation();
                server.playSound(null, at.x, at.y, at.z, SoundEvents.CRAFTER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Lets go of a held end the moment {@link PendingEnd} says it is no longer held. */
    public static void inventoryTick(ItemStack pick, ServerLevel level, Entity holder, EquipmentSlot slot) {
        GlobalPos pending = pick.get(PFDataComponents.PENDING_WIRE.get());
        if (pending == null) {
            return;
        }
        boolean sameDimension = pending.dimension().equals(level.dimension());
        BlockPos anchor = pending.pos();
        boolean standing = sameDimension && level.isLoaded(anchor)
                && level.getBlockState(anchor).getBlock() instanceof SupplyAreaPoleBlock
                && PoleColumn.isBase(level, anchor);
        PoleTier tier = standing
                ? ((SupplyAreaPoleBlock) level.getBlockState(anchor).getBlock()).tier()
                : PoleTier.SMALL;
        PoleLinks.Pole pole = new PoleLinks.Pole(anchor.getX(), anchor.getY(), anchor.getZ(), tier);
        double range = holder instanceof net.minecraft.world.entity.player.Player player
                ? player.blockInteractionRange() : 0.0;
        if (!PendingEnd.stillHeld(pole, holder.getX(), holder.getY(), holder.getZ(), range,
                standing, slot == EquipmentSlot.MAINHAND, sameDimension)) {
            pick.remove(PFDataComponents.PENDING_WIRE.get());
            // The snap: dropping an end is heard at the player, since nothing else shows it. A
            // chain's break, so it is none of the made, cut or refused sounds.
            level.playSound(null, holder.blockPosition(), SoundEvents.CHAIN_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void play(ServerLevel level, BlockPos at, SoundEvent sound) {
        level.playSound(null, at, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void alongWire(ServerLevel level, BlockPos fromBase, BlockPos toBase, ParticleOptions particle) {
        Vec3 from = attachPoint(level, fromBase);
        Vec3 to = attachPoint(level, toBase);
        int count = Math.max(2, (int) Math.ceil(from.distanceTo(to) * PARTICLES_PER_BLOCK));
        for (int i = 0; i <= count; i++) {
            Vec3 p = from.lerp(to, (double) i / count);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** The top of a column, where a wire hangs from. */
    private static Vec3 attachPoint(Level level, BlockPos base) {
        BlockPos top = PoleColumn.topOf(level, base);
        if (top == null) {
            top = base;
        }
        return new Vec3(top.getX() + 0.5, top.getY() + 0.9, top.getZ() + 0.5);
    }
}
