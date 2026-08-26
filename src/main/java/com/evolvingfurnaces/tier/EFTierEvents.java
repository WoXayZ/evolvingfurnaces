package com.evolvingfurnaces.tier;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.network.EFNetwork;
import com.evolvingfurnaces.network.EFSyncTiersPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = EFConstants.MOD_ID)
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
            EFNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        } else {
            EFNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), payload);
        }
    }
}
