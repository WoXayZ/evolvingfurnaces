package com.evolvingfurnaces.client.render;

import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFStyleRef;
import com.evolvingfurnaces.tier.EFTextureFace;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class EFMachineTextureCompositor {

    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> FRAME_COUNTS = new ConcurrentHashMap<>();
    private static final int GRID = 16;

    private EFMachineTextureCompositor() {
    }

    public static ResourceLocation texture(EFMachineKind kind, EFMachineTier tier, Direction face, boolean lit) {
        String key = kind.id() + "/" + tier.level() + "/" + face.getSerializedName() + "/" + lit + "/overlay";
        return CACHE.computeIfAbsent(key, ignored -> compose(kind, tier, face, key));
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        CACHE.values().forEach(id -> minecraft.getTextureManager().release(id));
        CACHE.clear();
        FRAME_COUNTS.clear();
    }

    private static ResourceLocation compose(EFMachineKind kind, EFMachineTier tier, Direction face, String key) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("evolvingfurnaces", "dynamic/" + key);
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resources = minecraft.getResourceManager();
        try (NativeImage style = loadStyle(resources, tier.style(), face)) {
            NativeImage result = new NativeImage(GRID, GRID, true);
            if (style != null) {
                applyStyle(result, style, kind, face);
            }
            DynamicTexture texture = new DynamicTexture(result);
            minecraft.getTextureManager().register(id, texture);
            return id;
        } catch (IOException e) {
            return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
        }
    }

    static boolean isStylePixel(EFMachineKind kind, Direction face, int px, int py) {
        return switch (kind) {
            case FURNACE, BLAST_FURNACE -> px == 0 || py == 0 || px == 15 || py == 15;
            case SMOKER -> smokerBorder(face, px, py);
            case BREWING_STAND -> false;
        };
    }

    private static boolean smokerBorder(Direction face, int px, int py) {
        boolean topBar = py == 0 && px >= 2 && px <= 13;
        boolean bottomBar = py == 15 && px >= 2 && px <= 13;
        if (face.getAxis() != Direction.Axis.Y) {
            return topBar || bottomBar;
        }
        boolean leftBar = px == 0 && py >= 2 && py <= 13;
        boolean rightBar = px == 15 && py >= 2 && py <= 13;
        return topBar || bottomBar || leftBar || rightBar;
    }

    private static void applyStyle(NativeImage dest, NativeImage style, EFMachineKind kind, Direction face) {
        for (int py = 0; py < GRID; py++) {
            for (int px = 0; px < GRID; px++) {
                if (!isStylePixel(kind, face, px, py)) {
                    continue;
                }
                int sx = Math.min(style.getWidth() - 1, px * style.getWidth() / GRID);
                int sy = Math.min(style.getHeight() - 1, py * style.getHeight() / GRID);
                dest.setPixelRGBA(px, py, style.getPixelRGBA(sx, sy) | 0xFF000000);
            }
        }
    }

    public static ResourceLocation brewingBase(EFMachineTier tier) {
        String key = "brewing_stand/base/" + tier.level() + "/" + frameIndex(tier);
        return CACHE.computeIfAbsent(key, ignored -> composeBrewingBase(tier, key));
    }

    private static int frameIndex(EFMachineTier tier) {
        int frames = FRAME_COUNTS.computeIfAbsent("brewing/" + tier.level() + "/" + tier.style().blockId(), ignored -> {
            try (NativeImage style = loadStyle(Minecraft.getInstance().getResourceManager(), tier.style(), Direction.NORTH)) {
                if (style == null || style.getHeight() <= GRID) {
                    return 1;
                }
                return Math.max(1, style.getHeight() / GRID);
            } catch (IOException e) {
                return 1;
            }
        });
        Minecraft minecraft = Minecraft.getInstance();
        long time = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        return (int) ((time / Math.max(1, frametime(tier))) % Math.max(1, frames));
    }

    private static int frametime(EFMachineTier tier) {
        ResourceLocation blockId = tier.style().blockId();
        ResourceLocation mcmeta = ResourceLocation.fromNamespaceAndPath(
                blockId.getNamespace(), "textures/block/" + blockId.getPath() + ".png.mcmeta");
        var resource = Minecraft.getInstance().getResourceManager().getResource(mcmeta);
        if (resource.isEmpty()) {
            return 1;
        }
        try (var reader = resource.get().openAsReader()) {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("animation") && json.getAsJsonObject("animation").has("frametime")) {
                return Math.max(1, json.getAsJsonObject("animation").get("frametime").getAsInt());
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    private static ResourceLocation composeBrewingBase(EFMachineTier tier, String key) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("evolvingfurnaces", "dynamic/" + key);
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resources = minecraft.getResourceManager();
        try (NativeImage base = load(resources, ResourceLocation.withDefaultNamespace("textures/block/brewing_stand_base.png"));
                NativeImage style = loadStyle(resources, tier.style(), Direction.NORTH)) {
            NativeImage result = new NativeImage(GRID, GRID, true);
            if (base != null && style != null) {
                int frame = frameIndex(tier);
                int styleY = Math.min(style.getHeight() - GRID, frame * GRID);
                for (int py = 0; py < GRID; py++) {
                    for (int px = 0; px < GRID; px++) {
                        int basePixel = base.getPixelRGBA(px, py);
                        int alpha = (basePixel >> 24) & 0xFF;
                        if (alpha == 0) {
                            result.setPixelRGBA(px, py, 0);
                            continue;
                        }
                        int sx = Math.min(style.getWidth() - 1, px * style.getWidth() / GRID);
                        int sy = Math.min(style.getHeight() - 1, styleY + py * Math.min(GRID, style.getHeight()) / GRID);
                        int stylePixel = style.getPixelRGBA(sx, sy);
                        result.setPixelRGBA(px, py, (stylePixel & 0x00FFFFFF) | (alpha << 24));
                    }
                }
            }
            DynamicTexture texture = new DynamicTexture(result);
            minecraft.getTextureManager().register(id, texture);
            return id;
        } catch (IOException e) {
            return ResourceLocation.withDefaultNamespace("textures/block/brewing_stand_base.png");
        }
    }

    private static NativeImage loadStyle(ResourceManager resources, EFStyleRef style, Direction face) throws IOException {
        Block block = style.block();
        BlockState state = block.defaultBlockState();
        ResourceLocation particle = Minecraft.getInstance().getBlockRenderer()
                .getBlockModelShaper()
                .getParticleIcon(state)
                .contents()
                .name();
        ResourceLocation png = ResourceLocation.fromNamespaceAndPath(particle.getNamespace(), "textures/" + particle.getPath() + ".png");
        NativeImage image = load(resources, png);
        if (image != null) {
            return image;
        }
        EFTextureFace requested = style.texture();
        String name = requested == EFTextureFace.SIDE ? "side" : requested.getSerializedName();
        ResourceLocation guessed = ResourceLocation.fromNamespaceAndPath(
                style.blockId().getNamespace(),
                "textures/block/" + style.blockId().getPath() + "_" + name + ".png");
        NativeImage sided = load(resources, guessed);
        if (sided != null) {
            return sided;
        }
        return load(resources, ResourceLocation.fromNamespaceAndPath(
                style.blockId().getNamespace(),
                "textures/block/" + style.blockId().getPath() + ".png"));
    }

    private static NativeImage load(ResourceManager resources, ResourceLocation id) throws IOException {
        var optional = resources.getResource(id);
        if (optional.isEmpty()) {
            return null;
        }
        try (InputStream stream = optional.get().open()) {
            return NativeImage.read(stream);
        }
    }
}
