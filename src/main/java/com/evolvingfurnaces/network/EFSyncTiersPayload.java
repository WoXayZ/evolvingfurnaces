package com.evolvingfurnaces.network;

import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFTierList;
import com.evolvingfurnaces.tier.EFTierManager;
import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record EFSyncTiersPayload(Map<EFMachineKind, EFTierList> tiers) {

    private static final Codec<Map<EFMachineKind, EFTierList>> MAP_CODEC =
            Codec.unboundedMap(Codec.STRING.xmap(EFSyncTiersPayload::kindOf, EFMachineKind::id), EFTierList.CODEC);

    public static EFSyncTiersPayload from(Map<EFMachineKind, EFTierList> tiers) {
        return new EFSyncTiersPayload(new EnumMap<>(tiers));
    }

    public static void encode(EFSyncTiersPayload payload, FriendlyByteBuf buf) {
        CompoundTag tag = (CompoundTag) MAP_CODEC.encodeStart(NbtOps.INSTANCE, payload.tiers()).result().orElseThrow();
        buf.writeNbt(tag);
    }

    public static EFSyncTiersPayload decode(FriendlyByteBuf buf) {
        return new EFSyncTiersPayload(new EnumMap<>(MAP_CODEC.parse(NbtOps.INSTANCE, buf.readNbt()).result().orElseThrow()));
    }

    public static void handle(EFSyncTiersPayload payload, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> EFTierManager.replaceClient(payload.tiers()));
        context.setPacketHandled(true);
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
