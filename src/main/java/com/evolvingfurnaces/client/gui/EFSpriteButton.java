package com.evolvingfurnaces.client.gui;

import com.evolvingfurnaces.EFConstants;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class EFSpriteButton extends Button {

    public static final int SIZE = 16;

    private static final Identifier BUTTON = EFConstants.id("widget/button");
    private static final Identifier BUTTON_HOVER = EFConstants.id("widget/button_highlighted");
    private static final Identifier BUTTON_DISABLED = EFConstants.id("widget/button_disabled");
    private static final Identifier BUTTON_DISABLED_HOVER = EFConstants.id("widget/button_disabled_highlighted");

    private final Supplier<Identifier> icon;
    private final Supplier<Boolean> disabledChrome;
    private final boolean disabledHover;

    public EFSpriteButton(
            int x,
            int y,
            Supplier<Identifier> icon,
            OnPress onPress,
            Component narration
    ) {
        this(x, y, icon, () -> false, false, onPress, narration);
    }

    public EFSpriteButton(
            int x,
            int y,
            Supplier<Identifier> icon,
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

    public static void blit(GuiGraphicsExtractor graphics, int x, int y, Identifier icon, boolean hover, boolean disabled) {
        blit(graphics, x, y, icon, hover, disabled, true);
    }

    public static void blit(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            Identifier icon,
            boolean hover,
            boolean disabled,
            boolean disabledHover
    ) {
        Identifier background;
        if (disabled) {
            background = hover && disabledHover ? BUTTON_DISABLED_HOVER : BUTTON_DISABLED;
        } else {
            background = hover ? BUTTON_HOVER : BUTTON;
        }
        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, background, x, y, SIZE, SIZE);
        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon, x, y, SIZE, SIZE);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean disabled = disabledChrome.get();
        blit(graphics, getX(), getY(), icon.get(), isHoveredOrFocused(), disabled, disabledHover);
    }
}
