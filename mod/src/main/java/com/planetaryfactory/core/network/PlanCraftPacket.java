package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.assembler.PersonalAssembler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * {@code +1}, {@code +5} or {@code all}: resolve this many and queue it now (#287).
 *
 * <p>It names a recipe and a count, not a plan id. The server resolves afresh against the inventory
 * as it is at the click and takes the reservation in the same step, so there is no held plan for a
 * stale click to pay for -- which is what the id guarded when Start stood between the two.
 */
public record PlanCraftPacket(Identifier recipe, int amount) implements CustomPacketPayload {

    public static final Type<PlanCraftPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_plan_craft"));

    public static final StreamCodec<ByteBuf, PlanCraftPacket> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, PlanCraftPacket::recipe,
            ByteBufCodecs.VAR_INT, PlanCraftPacket::amount,
            PlanCraftPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlanCraftPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PersonalAssembler.craft(player, packet.recipe(), packet.amount());
        }
    }
}
