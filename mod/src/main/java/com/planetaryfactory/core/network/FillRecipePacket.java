package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.assembler.PersonalAssembler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * EMI's {@code + Fill Recipe} was pressed: open the Crafting Plan for this recipe (#287).
 *
 * <p>No amount. EMI's shift-click {@code Integer.MAX_VALUE} used to seed Select Amount, which is
 * gone; opening queues nothing, so the plan opens on one and the screen's own buttons pick how many.
 */
public record FillRecipePacket(Identifier recipe) implements CustomPacketPayload {

    public static final Type<FillRecipePacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_fill_recipe"));

    public static final StreamCodec<ByteBuf, FillRecipePacket> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, FillRecipePacket::recipe,
            FillRecipePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FillRecipePacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PersonalAssembler.openPlan(player, packet.recipe());
        }
    }
}
