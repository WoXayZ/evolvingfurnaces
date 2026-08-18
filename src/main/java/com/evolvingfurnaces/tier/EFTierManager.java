package com.evolvingfurnaces.tier;

import com.evolvingfurnaces.EvolvingFurnaces;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

public final class EFTierManager {

    private static volatile Map<EFMachineKind, EFTierList> serverTiers = EFDefaultTiers.all();
    private static volatile Map<EFMachineKind, EFTierList> clientTiers = EFDefaultTiers.all();

    private EFTierManager() {
    }

    public static Map<EFMachineKind, EFTierList> current() {
        return Collections.unmodifiableMap(serverTiers);
    }

    public static Map<EFMachineKind, EFTierList> client() {
        return Collections.unmodifiableMap(clientTiers);
    }

    public static EFTierList list(EFMachineKind kind) {
        return serverTiers.getOrDefault(kind, EFDefaultTiers.forKind(kind));
    }

    public static EFTierList listClient(EFMachineKind kind) {
        return clientTiers.getOrDefault(kind, EFDefaultTiers.forKind(kind));
    }

    public static Optional<EFMachineTier> tier(EFMachineKind kind, int level) {
        return list(kind).byLevel(level);
    }

    public static Optional<EFMachineTier> tierClient(EFMachineKind kind, int level) {
        return listClient(kind).byLevel(level);
    }

    public static Optional<EFMachineTier> next(EFMachineKind kind, int currentLevel) {
        return list(kind).nextAfter(currentLevel);
    }

    public static Optional<EFMachineTier> nextClient(EFMachineKind kind, int currentLevel) {
        return listClient(kind).nextAfter(currentLevel);
    }

    public static synchronized void replaceServer(Map<EFMachineKind, EFTierList> next) {
        EnumMap<EFMachineKind, EFTierList> copy = new EnumMap<>(EFMachineKind.class);
        for (EFMachineKind kind : EFMachineKind.values()) {
            EFTierList list = next.get(kind);
            if (list == null || list.levels().isEmpty()) {
                EvolvingFurnaces.LOGGER.warn(
                        "Evolving Furnaces: missing or empty tier list for {}, using defaults",
                        kind.id());
                copy.put(kind, EFDefaultTiers.forKind(kind));
            } else {
                copy.put(kind, list);
            }
        }
        serverTiers = copy;
        EvolvingFurnaces.LOGGER.info(
                "Evolving Furnaces: loaded tiers (furnace={}, blast_furnace={}, smoker={})",
                copy.get(EFMachineKind.FURNACE).levels().size(),
                copy.get(EFMachineKind.BLAST_FURNACE).levels().size(),
                copy.get(EFMachineKind.SMOKER).levels().size());
    }

    public static void replaceClient(Map<EFMachineKind, EFTierList> next) {
        EnumMap<EFMachineKind, EFTierList> copy = new EnumMap<>(EFMachineKind.class);
        copy.putAll(EFDefaultTiers.all());
        copy.putAll(next);
        clientTiers = copy;
    }

    public static Map<EFMachineKind, EFTierList> snapshot() {
        return new EnumMap<>(serverTiers);
    }

    @Nullable
    public static EFMachineTier require(EFMachineKind kind, int level) {
        return tier(kind, level).orElse(null);
    }
}
