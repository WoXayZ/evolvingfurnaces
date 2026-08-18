package com.evolvingfurnaces.tier;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.network.EFSyncTiersPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = EFConstants.MOD_ID)
public final class EFTierEvents {

    private EFTierEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EFTierReloadListener());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        EFSyncTiersPayload payload = EFSyncTiersPayload.from(EFTierManager.snapshot());
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            PacketDistributor.sendToPlayer(player, payload);
        } else {
            PacketDistributor.sendToAllPlayers(payload);
        }
    }
}
