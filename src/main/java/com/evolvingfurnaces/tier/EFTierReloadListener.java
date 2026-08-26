package com.evolvingfurnaces.tier;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.EvolvingFurnaces;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class EFTierReloadListener extends SimplePreparableReloadListener<Map<EFMachineKind, EFTierList>> {

    @Override
    protected Map<EFMachineKind, EFTierList> prepare(ResourceManager manager, ProfilerFiller profiler) {
        EnumMap<EFMachineKind, EFTierList> result = new EnumMap<>(EFMachineKind.class);
        for (EFMachineKind kind : EFMachineKind.values()) {
            ResourceLocation fileId = new ResourceLocation(EFConstants.MOD_ID, "evolving_furnaces/" + kind.dataPath() + ".json");
            Optional<Resource> resource = manager.getResource(fileId);
            if (resource.isEmpty()) {
                EvolvingFurnaces.LOGGER.warn("Evolving Furnaces: missing {}, using defaults", fileId);
                result.put(kind, EFDefaultTiers.forKind(kind));
                continue;
            }
            try (Reader reader = resource.get().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                var decoded = EFTierList.CODEC.parse(JsonOps.INSTANCE, json);
                decoded.resultOrPartial(error -> EvolvingFurnaces.LOGGER.error(
                        "Evolving Furnaces: invalid tier file {}: {}", fileId, error));
                result.put(kind, decoded.result().orElseThrow(() -> new IllegalStateException("invalid tiers")));
            } catch (Exception e) {
                EvolvingFurnaces.LOGGER.error(
                        "Evolving Furnaces: invalid tier file {}, using defaults. {}",
                        fileId,
                        e.getMessage(),
                        e);
                result.put(kind, EFDefaultTiers.forKind(kind));
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<EFMachineKind, EFTierList> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        EFTierManager.replaceServer(object);
    }
}
