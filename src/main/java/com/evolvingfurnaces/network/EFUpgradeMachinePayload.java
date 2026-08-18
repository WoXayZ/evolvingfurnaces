package com.evolvingfurnaces.network;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EFUpgradeHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EFUpgradeMachinePayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<EFUpgradeMachinePayload> TYPE = new Type<>(EFConstants.id("upgrade_machine"));

    public static final StreamCodec<FriendlyByteBuf, EFUpgradeMachinePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeBlockPos(payload.pos()),
                    buf -> new EFUpgradeMachinePayload(buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EFUpgradeMachinePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            EFUpgradeHandler.tryUpgrade(player, payload.pos());
        }
    }
}
