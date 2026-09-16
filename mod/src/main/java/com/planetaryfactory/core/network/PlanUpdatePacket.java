package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.assembler.CraftingPlanMenu;
import com.planetaryfactory.core.assembler.PlanDisplay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The open Crafting Plan, re-resolved (#287).
 *
 * <p>The plan used to arrive once, as the menu's opening data. Now the screen stays up while the
 * queue spends the inventory under it, so the server sends the plan again after a press and on the
 * queue's sync cadence, and the menu takes it in place. Reopening the menu instead would reset the
 * screen, and with it the cursor.
 *
 * <p>{@code containerId} is there so an update resolved for a menu the player has since closed
 * lands on nothing rather than on whatever screen replaced it.
 */
public record PlanUpdatePacket(int containerId, PlanDisplay display, int largestAffordable)
        implements CustomPacketPayload {

    public static final Type<PlanUpdatePacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_plan_update"));

    public static final StreamCodec<ByteBuf, PlanUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlanUpdatePacket::containerId,
            PlanDisplay.STREAM_CODEC, PlanUpdatePacket::display,
            ByteBufCodecs.VAR_INT, PlanUpdatePacket::largestAffordable,
            PlanUpdatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlanUpdatePacket packet, IPayloadContext context) {
        if (context.player().containerMenu instanceof CraftingPlanMenu menu
                && menu.containerId == packet.containerId()) {
            menu.update(packet.display(), packet.largestAffordable());
        }
    }
}
