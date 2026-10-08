package com.evolvingfurnaces.tier;

import com.mojang.serialization.Codec;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

public record EFDisplayNames(Map<String, String> byLocale) {

    public static final Codec<EFDisplayNames> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .xmap(EFDisplayNames::new, EFDisplayNames::byLocale);

    public String resolve(String locale) {
        String key = locale == null ? "en_us" : locale.toLowerCase(Locale.ROOT);
        String exact = byLocale.get(key);
        if (exact != null && !exact.isBlank()) {
            return exact;
        }
        String en = byLocale.get("en_us");
        if (en != null && !en.isBlank()) {
            return en;
        }
        return byLocale.values().stream().filter(s -> s != null && !s.isBlank()).findFirst().orElse("Evolving Machine");
    }

    public String resolveForPlayer(@Nullable Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return resolve(serverPlayer.getLanguage());
        }
        return resolve(clientLocale());
    }

    public Component toComponent() {
        return Component.literal(resolve(clientLocale()));
    }

    public Component toComponent(@Nullable Player player) {
        return Component.literal(resolveForPlayer(player));
    }

    private static String clientLocale() {
        if (!FMLEnvironment.getDist().isClient()) {
            return "en_us";
        }
        try {
            Class<?> clazz = Class.forName("com.evolvingfurnaces.tier.EFClientDisplayNames");
            Object value = clazz.getDeclaredMethod("selectedLanguage").invoke(null);
            return value instanceof String locale ? locale : "en_us";
        } catch (ReflectiveOperationException ignored) {
            return "en_us";
        }
    }
}
