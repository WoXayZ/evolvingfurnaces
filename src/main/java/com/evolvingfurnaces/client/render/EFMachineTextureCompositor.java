package com.evolvingfurnaces.client.render;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EFEnergyModule;
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
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class EFMachineTextureCompositor {

    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> FRAME_COUNTS = new ConcurrentHashMap<>();
    private static final int GRID = 16;

    private EFMachineTextureCompositor() {
    }

    public static Identifier texture(
            EFMachineKind kind,
            EFMachineTier tier,
            Direction face,
            boolean lit,
            EFEnergyModule module
    ) {
        int frame = moduleFrameIndex(kind, face, module, lit);
        String key = kind.id()
                + "/" + tier.level()
                + "/" + face.getSerializedName()
                + "/" + lit
                + "/" + module.getSerializedName()
                + "/" + frame
                + "/overlay";
        return CACHE.computeIfAbsent(key, ignored -> compose(kind, tier, face, lit, module, frame, key));
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        CACHE.values().forEach(id -> minecraft.getTextureManager().release(id));
        CACHE.clear();
        FRAME_COUNTS.clear();
    }

    private static Identifier compose(
            EFMachineKind kind,
            EFMachineTier tier,
            Direction face,
            boolean lit,
            EFEnergyModule module,
            int frame,
            String key
    ) {
        Identifier id = Identifier.fromNamespaceAndPath("evolvingfurnaces", "dynamic/" + key);
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resources = minecraft.getResourceManager();
        try (NativeImage style = loadStyle(resources, tier.style(), face)) {
            NativeImage result = new NativeImage(GRID, GRID, true);
            applyModuleOverlay(result, resources, kind, face, module, lit, frame);
            if (style != null) {
                applyStyle(result, style, kind, face);
            }
            DynamicTexture texture = new DynamicTexture(() -> "evolvingfurnaces", result);
            minecraft.getTextureManager().register(id, texture);
            return id;
        } catch (IOException e) {
            return Identifier.withDefaultNamespace("textures/misc/white.png");
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

    private static void applyModuleOverlay(
            NativeImage dest,
            ResourceManager resources,
            EFMachineKind kind,
            Direction face,
            EFEnergyModule module,
            boolean lit,
            int frame
    ) {
        if (!usesModuleOverlay(kind, face, module)) {
            return;
        }
        // 26.1 orientable models draw the north face without a mirrored UV.
        boolean flipX = false;
        if (module == EFEnergyModule.ELECTRIC && lit) {
            applyPng(dest, resources, kind.vanillaFrontOffPng(), 0, true, flipX);
            applyPng(dest, resources, moduleFront(kind, module, false), 0, false, flipX);
            applyPng(dest, resources, moduleFront(kind, module, true), frame, false, flipX);
            return;
        }
        applyPng(dest, resources, moduleFront(kind, module, lit), frame, false, flipX);
    }

    private static void applyPng(
            NativeImage dest,
            ResourceManager resources,
            Identifier png,
            int frame,
            boolean copyOpaqueBase,
            boolean flipX
    ) {
        try (NativeImage overlay = load(resources, png)) {
            if (overlay == null) {
                return;
            }
            int srcY = Math.min(Math.max(0, overlay.getHeight() - GRID), frame * GRID);
            int width = Math.min(GRID, overlay.getWidth());
            int height = Math.min(GRID, overlay.getHeight() - srcY);
            for (int py = 0; py < height; py++) {
                for (int px = 0; px < width; px++) {
                    int pixel = overlay.getPixel(px, srcY + py);
                    if (copyOpaqueBase) {
                        pixel |= 0xFF000000;
                    } else if (((pixel >> 24) & 0xFF) == 0) {
                        continue;
                    }
                    int destX = flipX ? width - 1 - px : px;
                    dest.setPixel(destX, py, pixel);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static int moduleFrameIndex(EFMachineKind kind, Direction face, EFEnergyModule module, boolean lit) {
        if (!usesModuleOverlay(kind, face, module)) {
            return 0;
        }
        Identifier png = moduleFront(kind, module, lit);
        String cacheKey = png.toString();
        int frames = FRAME_COUNTS.computeIfAbsent(cacheKey, ignored -> {
            try (NativeImage overlay = load(Minecraft.getInstance().getResourceManager(), png)) {
                if (overlay == null || overlay.getHeight() <= GRID) {
                    return 1;
                }
                return Math.max(1, overlay.getHeight() / GRID);
            } catch (IOException e) {
                return 1;
            }
        });
        Minecraft minecraft = Minecraft.getInstance();
        long time = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        return (int) ((time / Math.max(1, moduleFrametime(png))) % Math.max(1, frames));
    }

    private static int moduleFrametime(Identifier png) {
        Identifier mcmeta = Identifier.fromNamespaceAndPath(png.getNamespace(), png.getPath() + ".mcmeta");
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

    private static boolean usesModuleOverlay(EFMachineKind kind, Direction face, EFEnergyModule module) {
        if (face != Direction.NORTH || !module.installed()) {
            return false;
        }
        return module == EFEnergyModule.GENERATOR ? kind == EFMachineKind.FURNACE : kind.isCooking();
    }

    private static Identifier moduleFront(EFMachineKind kind, EFEnergyModule module, boolean lit) {
        String suffix = lit ? "_on" : "";
        if (module == EFEnergyModule.GENERATOR) {
            return EFConstants.id("textures/block/generator_front" + suffix + ".png");
        }
        return EFConstants.id("textures/block/" + kind.id() + "_electric_front" + suffix + ".png");
    }

    private static void applyStyle(NativeImage dest, NativeImage style, EFMachineKind kind, Direction face) {
        for (int py = 0; py < GRID; py++) {
            for (int px = 0; px < GRID; px++) {
                if (!isStylePixel(kind, face, px, py)) {
                    continue;
                }
                int sx = Math.min(style.getWidth() - 1, px * style.getWidth() / GRID);
                int sy = Math.min(style.getHeight() - 1, py * style.getHeight() / GRID);
                dest.setPixel(px, py, style.getPixel(sx, sy) | 0xFF000000);
            }
        }
    }

    public static Identifier brewingBase(EFMachineTier tier) {
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
        Identifier blockId = tier.style().blockId();
        Identifier mcmeta = Identifier.fromNamespaceAndPath(
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

    private static Identifier composeBrewingBase(EFMachineTier tier, String key) {
        Identifier id = Identifier.fromNamespaceAndPath("evolvingfurnaces", "dynamic/" + key);
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resources = minecraft.getResourceManager();
        try (NativeImage base = load(resources, Identifier.withDefaultNamespace("textures/block/brewing_stand_base.png"));
                NativeImage style = loadStyle(resources, tier.style(), Direction.NORTH)) {
            NativeImage result = new NativeImage(GRID, GRID, true);
            if (base != null && style != null) {
                int frame = frameIndex(tier);
                int styleY = Math.min(style.getHeight() - GRID, frame * GRID);
                for (int py = 0; py < GRID; py++) {
                    for (int px = 0; px < GRID; px++) {
                        int basePixel = base.getPixel(px, py);
                        int alpha = (basePixel >> 24) & 0xFF;
                        if (alpha == 0) {
                            result.setPixel(px, py, 0);
                            continue;
                        }
                        int sx = Math.min(style.getWidth() - 1, px * style.getWidth() / GRID);
                        int sy = Math.min(style.getHeight() - 1, styleY + py * Math.min(GRID, style.getHeight()) / GRID);
                        int stylePixel = style.getPixel(sx, sy);
                        result.setPixel(px, py, (stylePixel & 0x00FFFFFF) | (alpha << 24));
                    }
                }
            }
            DynamicTexture texture = new DynamicTexture(() -> "evolvingfurnaces", result);
            minecraft.getTextureManager().register(id, texture);
            return id;
        } catch (IOException e) {
            return Identifier.withDefaultNamespace("textures/block/brewing_stand_base.png");
        }
    }

    private static NativeImage loadStyle(ResourceManager resources, EFStyleRef style, Direction face) throws IOException {
        EFTextureFace requested = style.texture();
        String name = requested == EFTextureFace.SIDE ? "side" : requested.getSerializedName();
        Identifier guessed = Identifier.fromNamespaceAndPath(
                style.blockId().getNamespace(),
                "textures/block/" + style.blockId().getPath() + "_" + name + ".png");
        NativeImage sided = load(resources, guessed);
        if (sided != null) {
            return sided;
        }
        return load(resources, Identifier.fromNamespaceAndPath(
                style.blockId().getNamespace(),
                "textures/block/" + style.blockId().getPath() + ".png"));
    }

    private static NativeImage load(ResourceManager resources, Identifier id) throws IOException {
        var optional = resources.getResource(id);
        if (optional.isEmpty()) {
            return null;
        }
        try (InputStream stream = optional.get().open()) {
            return NativeImage.read(stream);
        }
    }
}
