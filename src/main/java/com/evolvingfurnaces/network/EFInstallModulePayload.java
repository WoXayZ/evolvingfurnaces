package com.evolvingfurnaces.network;

import com.evolvingfurnaces.machine.EFEnergyModule;
import com.evolvingfurnaces.machine.EFUpgradeHandler;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record EFInstallModulePayload(BlockPos pos, EFEnergyModule module, boolean shift) {

    public static void encode(EFInstallModulePayload payload, FriendlyByteBuf buf) {
        buf.writeBlockPos(payload.pos());
        buf.writeEnum(payload.module());
        buf.writeBoolean(payload.shift());
    }

    public static EFInstallModulePayload decode(FriendlyByteBuf buf) {
        return new EFInstallModulePayload(buf.readBlockPos(), buf.readEnum(EFEnergyModule.class), buf.readBoolean());
    }

    public static void handle(EFInstallModulePayload payload, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                EFUpgradeHandler.tryInstallModule(player, payload.pos(), payload.module(), payload.shift());
            }
        });
        context.setPacketHandled(true);
    }
}
