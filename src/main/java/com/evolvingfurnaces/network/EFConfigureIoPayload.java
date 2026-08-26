package com.evolvingfurnaces.network;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EFIoHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EFConfigureIoPayload(BlockPos pos, Direction face) implements CustomPacketPayload {

    public static final Type<EFConfigureIoPayload> TYPE = new Type<>(EFConstants.id("configure_io"));

    public static final StreamCodec<FriendlyByteBuf, EFConfigureIoPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.pos());
                        buf.writeEnum(payload.face());
                    },
                    buf -> new EFConfigureIoPayload(buf.readBlockPos(), buf.readEnum(Direction.class)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EFConfigureIoPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.level().isLoaded(payload.pos()) || player.distanceToSqr(payload.pos().getCenter()) > 64.0D) {
            return;
        }
        if (player.level().getBlockEntity(payload.pos()) instanceof EFIoHost machine) {
            machine.io().cycle(payload.face(), machine.energyModule());
            machine.setChanged();
            player.level().invalidateCapabilities(payload.pos());
            player.level().sendBlockUpdated(payload.pos(), machine.getBlockState(), machine.getBlockState(), 3);
        }
    }
}
