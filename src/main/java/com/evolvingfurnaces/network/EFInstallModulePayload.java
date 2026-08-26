package com.evolvingfurnaces.network;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EFEnergyModule;
import com.evolvingfurnaces.machine.EFUpgradeHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EFInstallModulePayload(BlockPos pos, EFEnergyModule module, boolean shift) implements CustomPacketPayload {

    public static final Type<EFInstallModulePayload> TYPE = new Type<>(EFConstants.id("install_module"));

    public static final StreamCodec<FriendlyByteBuf, EFInstallModulePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.pos());
                        buf.writeEnum(payload.module());
                        buf.writeBoolean(payload.shift());
                    },
                    buf -> new EFInstallModulePayload(
                            buf.readBlockPos(),
                            buf.readEnum(EFEnergyModule.class),
                            buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EFInstallModulePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            EFUpgradeHandler.tryInstallModule(player, payload.pos(), payload.module(), payload.shift());
        }
    }
}
