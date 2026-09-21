package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.machine.AssemblingMachineMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * EMI's {@code + Fill Recipe} was pressed on an Assembling Machine's screen (#330, ADR-0073).
 *
 * <p>A recipe id and nothing else: a machine holds one recipe, not an amount. It names no position
 * -- the machine is whichever one the player has open, so a packet can reach no other.
 */
public record HoldRecipePacket(Identifier recipe) implements CustomPacketPayload {

    public static final Type<HoldRecipePacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembling_machine_hold_recipe"));

    public static final StreamCodec<ByteBuf, HoldRecipePacket> STREAM_CODEC =
            Identifier.STREAM_CODEC.map(HoldRecipePacket::new, HoldRecipePacket::recipe);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HoldRecipePacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof AssemblingMachineMenu menu
                && menu.stillValid(player)) {
            menu.request(player, packet.recipe().toString());
        }
    }
}
