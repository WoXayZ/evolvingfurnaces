package com.evolvingfurnaces.network;

import com.evolvingfurnaces.machine.EFUpgradeHandler;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record EFUpgradeMachinePayload(BlockPos pos) {

    public static void encode(EFUpgradeMachinePayload payload, FriendlyByteBuf buf) {
        buf.writeBlockPos(payload.pos());
    }

    public static EFUpgradeMachinePayload decode(FriendlyByteBuf buf) {
        return new EFUpgradeMachinePayload(buf.readBlockPos());
    }

    public static void handle(EFUpgradeMachinePayload payload, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                EFUpgradeHandler.tryUpgrade(player, payload.pos());
            }
        });
        context.setPacketHandled(true);
    }
}
