package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Personal Assembler's round trip (ADR-0038, #160).
 *
 * <p>Four of the seven Assembler packets go client-to-server, which is the shape the ADR demands: the
 * plan is server truth, so the client asks and the server decides. The other three go back: the open
 * Crafting Plan re-resolved, the queue's display view, and the set of recipe ids the Assembler can
 * plan at all -- and nothing about a plan crosses in that direction except what is drawn.
 *
 * <p>The first plan is the Crafting Plan menu's own opening data, so the answer and the screen arrive
 * together. Every later one is {@code PlanUpdatePacket}, because the dialog stays up while the queue
 * it feeds spends the inventory under it (#287).
 */
public final class PFNetwork {

    /** Bumped when a payload's shape changes; clients on the old shape are refused, not confused. */
    private static final String VERSION = "7";

    private PFNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(FillRecipePacket.TYPE, FillRecipePacket.STREAM_CODEC, FillRecipePacket::handle);
        registrar.playToServer(HoldRecipePacket.TYPE, HoldRecipePacket.STREAM_CODEC, HoldRecipePacket::handle);
        registrar.playToServer(PlanCraftPacket.TYPE, PlanCraftPacket.STREAM_CODEC, PlanCraftPacket::handle);
        registrar.playToServer(PlanCancelPacket.TYPE, PlanCancelPacket.STREAM_CODEC, PlanCancelPacket::handle);
        registrar.playToServer(RotateHeldPacket.TYPE, RotateHeldPacket.STREAM_CODEC, RotateHeldPacket::handle);
        registrar.playToClient(PlanUpdatePacket.TYPE, PlanUpdatePacket.STREAM_CODEC, PlanUpdatePacket::handle);
        registrar.playToClient(QueueSyncPacket.TYPE, QueueSyncPacket.STREAM_CODEC, QueueSyncPacket::handle);
        registrar.playToClient(HandRecipeSetPacket.TYPE, HandRecipeSetPacket.STREAM_CODEC, HandRecipeSetPacket::handle);
        // Not the Assembler's: a data pack is server truth, and the fuel table has to reach a
        // client for an item to say what it is worth (ADR-0047).
        // Not the Assembler's either: the wires a client draws (ADR-0068).
        registrar.playToClient(PoleWiresPacket.TYPE, PoleWiresPacket.STREAM_CODEC, PoleWiresPacket::handle);
        registrar.playToClient(FuelTablePacket.TYPE, FuelTablePacket.STREAM_CODEC, FuelTablePacket::handle);
        registrar.playToClient(RadarChunkPacket.TYPE, RadarChunkPacket.STREAM_CODEC, RadarChunkPacket::handle);
        registrar.playToClient(RadarMarkersPacket.TYPE, RadarMarkersPacket.STREAM_CODEC, RadarMarkersPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
