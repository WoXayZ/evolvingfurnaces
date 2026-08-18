package com.evolvingfurnaces.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class EFNetwork {

    private EFNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(EFSyncTiersPayload.TYPE, EFSyncTiersPayload.STREAM_CODEC, EFSyncTiersPayload::handle);
        registrar.playToServer(EFUpgradeMachinePayload.TYPE, EFUpgradeMachinePayload.STREAM_CODEC, EFUpgradeMachinePayload::handle);
        registrar.playToServer(EFConfigureIoPayload.TYPE, EFConfigureIoPayload.STREAM_CODEC, EFConfigureIoPayload::handle);
    }
}
