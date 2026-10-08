package com.evolvingfurnaces.network;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFTierList;
import com.evolvingfurnaces.tier.EFTierManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.netty.handler.codec.DecoderException;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EFSyncTiersPayload(Map<EFMachineKind, EFTierList> tiers) implements CustomPacketPayload {

    public static final Type<EFSyncTiersPayload> TYPE = new Type<>(EFConstants.id("sync_tiers"));

    private static final Codec<Map<EFMachineKind, EFTierList>> MAP_CODEC =
            Codec.unboundedMap(Codec.STRING.xmap(EFSyncTiersPayload::kindOf, EFMachineKind::id), EFTierList.CODEC);

    public static final StreamCodec<FriendlyByteBuf, EFSyncTiersPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeJsonWithCodec(MAP_CODEC, payload.tiers()),
            buf -> {
                JsonElement json = JsonParser.parseString(buf.readUtf());
                Map<EFMachineKind, EFTierList> map = MAP_CODEC.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(DecoderException::new);
                return new EFSyncTiersPayload(new EnumMap<>(map));
            }
    );

    public static EFSyncTiersPayload from(Map<EFMachineKind, EFTierList> tiers) {
        return new EFSyncTiersPayload(new EnumMap<>(tiers));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EFSyncTiersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EFTierManager.replaceClient(payload.tiers()));
    }

    private static EFMachineKind kindOf(String id) {
        for (EFMachineKind kind : EFMachineKind.values()) {
            if (kind.id().equals(id)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown machine kind " + id);
    }
}
