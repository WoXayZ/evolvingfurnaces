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
