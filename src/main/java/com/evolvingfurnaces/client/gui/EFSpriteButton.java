package com.evolvingfurnaces.client.gui;

import com.evolvingfurnaces.EFConstants;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class EFSpriteButton extends Button {

    public static final int SIZE = 16;

    private static final ResourceLocation BUTTON = EFConstants.widget("button");
    private static final ResourceLocation BUTTON_HOVER = EFConstants.widget("button_highlighted");
    private static final ResourceLocation BUTTON_DISABLED = EFConstants.widget("button_disabled");
    private static final ResourceLocation BUTTON_DISABLED_HOVER = EFConstants.widget("button_disabled_highlighted");

    private final Supplier<ResourceLocation> icon;
    private final Supplier<Boolean> disabledChrome;
    private final boolean disabledHover;

    public EFSpriteButton(
            int x,
            int y,
            Supplier<ResourceLocation> icon,
            OnPress onPress,
            Component narration
    ) {
        this(x, y, icon, () -> false, false, onPress, narration);
    }

    public EFSpriteButton(
            int x,
            int y,
            Supplier<ResourceLocation> icon,
            Supplier<Boolean> disabledChrome,
            boolean disabledHover,
            OnPress onPress,
            Component narration
    ) {
        super(x, y, SIZE, SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.disabledChrome = disabledChrome;
        this.disabledHover = disabledHover;
        setTooltip(Tooltip.create(narration));
    }

    public static void blit(GuiGraphics graphics, int x, int y, ResourceLocation icon, boolean hover, boolean disabled) {
        blit(graphics, x, y, icon, hover, disabled, true);
    }

    public static void blit(
            GuiGraphics graphics,
            int x,
            int y,
            ResourceLocation icon,
            boolean hover,
            boolean disabled,
            boolean disabledHover
    ) {
        ResourceLocation background;
        if (disabled) {
            background = hover && disabledHover ? BUTTON_DISABLED_HOVER : BUTTON_DISABLED;
        } else {
            background = hover ? BUTTON_HOVER : BUTTON;
        }
        graphics.blit(background, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
        graphics.blit(icon, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean disabled = disabledChrome.get();
        blit(graphics, getX(), getY(), icon.get(), isHoveredOrFocused(), disabled, disabledHover);
    }
}
