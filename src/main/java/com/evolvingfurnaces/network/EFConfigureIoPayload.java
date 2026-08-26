package com.evolvingfurnaces.network;

import com.evolvingfurnaces.machine.EFIoHost;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public record EFConfigureIoPayload(BlockPos pos, Direction face) {

    public static void encode(EFConfigureIoPayload payload, FriendlyByteBuf buf) {
        buf.writeBlockPos(payload.pos());
        buf.writeEnum(payload.face());
    }

    public static EFConfigureIoPayload decode(FriendlyByteBuf buf) {
        return new EFConfigureIoPayload(buf.readBlockPos(), buf.readEnum(Direction.class));
    }

    public static void handle(EFConfigureIoPayload payload, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (!player.level().isLoaded(payload.pos()) || player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos()) instanceof EFIoHost machine) {
                machine.io().cycle(payload.face());
                machine.setChanged();
                player.level().sendBlockUpdated(payload.pos(), machine.getBlockState(), machine.getBlockState(), 3);
            }
        });
        context.setPacketHandled(true);
    }
}
