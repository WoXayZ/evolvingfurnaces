package com.evolvingfurnaces.network;

import com.evolvingfurnaces.EFConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EFNetwork {

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EFConstants.MOD_ID, "main"),
            () -> "1", s -> s.equals("1"), s -> s.equals("1"));

    private EFNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, EFSyncTiersPayload.class, EFSyncTiersPayload::encode, EFSyncTiersPayload::decode, EFSyncTiersPayload::handle);
        CHANNEL.registerMessage(id++, EFUpgradeMachinePayload.class, EFUpgradeMachinePayload::encode, EFUpgradeMachinePayload::decode, EFUpgradeMachinePayload::handle);
        CHANNEL.registerMessage(id++, EFConfigureIoPayload.class, EFConfigureIoPayload::encode, EFConfigureIoPayload::decode, EFConfigureIoPayload::handle);
    }
}
