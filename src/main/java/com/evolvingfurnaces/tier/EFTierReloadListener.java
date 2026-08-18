package com.evolvingfurnaces.tier;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.EvolvingFurnaces;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class EFTierReloadListener extends SimplePreparableReloadListener<Map<EFMachineKind, EFTierList>> {

    private static final FileToIdConverter CONVERTER = FileToIdConverter.json("evolving_furnaces");

    @Override
    protected Map<EFMachineKind, EFTierList> prepare(ResourceManager manager, ProfilerFiller profiler) {
        EnumMap<EFMachineKind, EFTierList> result = new EnumMap<>(EFMachineKind.class);
        for (EFMachineKind kind : EFMachineKind.values()) {
            ResourceLocation fileId = CONVERTER.idToFile(EFConstants.id(kind.dataPath()));
            var resources = manager.getResourceStack(fileId);
            if (resources.isEmpty()) {
                EvolvingFurnaces.LOGGER.warn("Evolving Furnaces: missing {}, using defaults", fileId);
                result.put(kind, EFDefaultTiers.forKind(kind));
                continue;
            }
            Resource resource = resources.getLast();
            try (Reader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                var decoded = EFTierList.CODEC.parse(JsonOps.INSTANCE, json);
                result.put(kind, decoded.getOrThrow(error -> new IllegalStateException(error)));
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
