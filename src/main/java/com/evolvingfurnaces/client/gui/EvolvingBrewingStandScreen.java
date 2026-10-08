package com.evolvingfurnaces.client.gui;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EFMachineTooltips;
import com.evolvingfurnaces.machine.EFUpgradeHandler;
import com.evolvingfurnaces.machine.EvolvingBrewingStandBlockEntity;
import com.evolvingfurnaces.machine.EvolvingBrewingStandMenu;
import com.evolvingfurnaces.network.EFConfigureIoPayload;
import com.evolvingfurnaces.network.EFUpgradeMachinePayload;
import com.evolvingfurnaces.tier.EFIoRole;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFTierManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class EvolvingBrewingStandScreen extends AbstractContainerScreen<EvolvingBrewingStandMenu> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/brewing_stand.png");
    private static final Identifier FUEL_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/fuel_length");
    private static final Identifier BREW_PROGRESS = Identifier.withDefaultNamespace("container/brewing_stand/brew_progress");
    private static final Identifier BUBBLES = Identifier.withDefaultNamespace("container/brewing_stand/bubbles");
    private static final int[] BUBBLE_LENGTHS = {29, 24, 20, 16, 11, 6, 0};

    private static final Identifier IO_PANEL = EFConstants.id("textures/gui/io_panel.png");
    private static final Identifier INFO_PANEL = EFConstants.id("textures/gui/info_panel.png");
    private static final Identifier IO = EFConstants.id("widget/io");
    private static final Identifier INFO = EFConstants.id("widget/info");
    private static final Identifier UPGRADE = EFConstants.id("widget/upgrade");
    private static final Identifier UPGRADE_READY = EFConstants.id("widget/upgrade_ready");
    private static final Identifier UPGRADE_MAX = EFConstants.id("widget/upgrade_max");

    static final int IO_W = 62;
    static final int IO_H = 67;
    static final int INFO_W = 150;
    static final int INFO_H = 116;
    private static final int PANEL_TEX = 256;
    private static final int FACE = 8;
    private static final int FACE_GAP = 4;
    private static final int TEXT = 0xFF404040;

    private static boolean rememberedInfoOpen;
    private static boolean rememberedIoOpen;

    private boolean infoOpen;
    private boolean ioOpen;
    private EFSpriteButton ioButton;
    private EFSpriteButton infoButton;
    private EFSpriteButton upgradeButton;

    public EvolvingBrewingStandScreen(EvolvingBrewingStandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.infoOpen = rememberedInfoOpen;
        this.ioOpen = rememberedIoOpen;
        if (this.ioOpen) {
            this.infoOpen = false;
        }
    }

    @Override
    protected void init() {
        super.init();
        applyLayout();
        ioButton = addRenderableWidget(new EFSpriteButton(
                ioButtonX(),
                buttonY(),
                () -> IO,
                button -> {
                    ioOpen = !ioOpen;
                    if (ioOpen) {
                        infoOpen = false;
                    }
                    rememberPanels();
                    applyLayout();
                },
                Component.translatable("evolvingfurnaces.gui.io")));
        infoButton = addRenderableWidget(new EFSpriteButton(
                infoButtonX(),
                buttonY(),
                () -> INFO,
                button -> {
                    infoOpen = !infoOpen;
                    if (infoOpen) {
                        ioOpen = false;
                    }
                    rememberPanels();
                    applyLayout();
                },
                Component.translatable("evolvingfurnaces.gui.info")));
        upgradeButton = addRenderableWidget(new EFSpriteButton(
                upgradeButtonX(),
                buttonY(),
                this::upgradeIcon,
                this::isMaxLevel,
                true,
                button -> {
                    if (!isMaxLevel()) {
                        ClientPacketDistributor.sendToServer(new EFUpgradeMachinePayload(menu.blockPos()));
                    }
                },
                Component.translatable("evolvingfurnaces.gui.upgrade")));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        applyLayout();
        if (upgradeButton != null) {
            upgradeButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(upgradeNarration()));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        applyLayout();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (infoOpen) {
            renderInfo(graphics, mouseX, mouseY);
        }
        if (ioOpen) {
            renderIo(graphics, mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        int fuelWidth = Mth.clamp((18 * menu.fuel() + EvolvingBrewingStandBlockEntity.FUEL_USES - 1)
                / EvolvingBrewingStandBlockEntity.FUEL_USES, 0, 18);
        if (fuelWidth > 0) {
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, FUEL_SPRITE, 18, 4, 0, 0, leftPos + 60, topPos + 44, fuelWidth, 4);
        }
        int brewTime = menu.brewTime();
        if (brewTime > 0) {
            int height = (int) (28.0F * (1.0F - (float) brewTime / (float) menu.brewTimeTotal()));
            if (height > 0) {
                graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, BREW_PROGRESS, 9, 28, 0, 0, leftPos + 97, topPos + 16, 9, height);
            }
            int bubble = BUBBLE_LENGTHS[brewTime / 2 % 7];
            if (bubble > 0) {
                graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, BUBBLES, 12, 29, 0, 29 - bubble, leftPos + 63, topPos + 14 + 29 - bubble, 12, bubble);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0 && ioOpen) {
            Direction face = ioFaceAt(mouseX, mouseY);
            if (face != null) {
                ClientPacketDistributor.sendToServer(new EFConfigureIoPayload(menu.blockPos(), face));
                return true;
            }
            if (inIoPanel(mouseX, mouseY)) {
                return true;
            }
        }
        if (event.button() == 0 && infoOpen && inInfoPanel(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop) {
        if (inExtraUi(mouseX, mouseY)) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop);
    }

    private void renderInfo(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = infoPanelX();
        int y = infoPanelY();
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, INFO_PANEL, x, y, 0.0F, 0.0F, INFO_W, INFO_H, PANEL_TEX, PANEL_TEX);
        Component title = Component.translatable("evolvingfurnaces.gui.info");
        graphics.text(font, title, x + (INFO_W - font.width(title)) / 2, y + 5, TEXT, false);
        List<Component> lines = new ArrayList<>();
        EvolvingBrewingStandBlockEntity machine = machine();
        if (machine != null) {
            EFMachineTooltips.append(lines, EFMachineKind.BREWING_STAND, machine.machineLevel(), machine.cooks(), minecraft.player);
        }
        int lineY = y + 18;
        for (Component line : lines) {
            final int color;
            if (line.getStyle().getColor() != null
                    && ChatFormatting.GOLD.getColor() != null
                    && line.getStyle().getColor().getValue() == ChatFormatting.GOLD.getColor()) {
                color = 0xFFB36B00;
            } else {
                color = TEXT;
            }
            graphics.text(font, line.copy().withStyle(style -> style.withColor(color)), x + 8, lineY, color, false);
            lineY += 10;
        }
        if (machine != null) {
            int barX = x + 8;
            int barY = y + INFO_H - 16;
            blitBrewsBar(graphics, barX, barY, machine);
            if (inBrewsBar(barX, barY, mouseX, mouseY)) {
                int required = EFMachineTooltips.progressGoal(EFMachineKind.BREWING_STAND, machine.machineLevel());
                graphics.setTooltipForNextFrame(
                        font,
                        List.of(Component.translatable("evolvingfurnaces.gui.cooks_short", machine.cooks(), required)),
                        Optional.empty(),
                        mouseX,
                        mouseY);
            }
        }
    }

    private void blitBrewsBar(GuiGraphicsExtractor graphics, int x, int y, EvolvingBrewingStandBlockEntity machine) {
        boolean max = EFTierManager.nextClient(EFMachineKind.BREWING_STAND, machine.machineLevel()).isEmpty();
        int barW = INFO_W - 16;
        graphics.fill(x, y, x + barW, y + 5, 0xFF8B8B8B);
        graphics.fill(x, y, x + barW - 1, y + 4, 0xFF373737);
        int required = EFMachineTooltips.progressGoal(EFMachineKind.BREWING_STAND, machine.machineLevel());
        int filled = EFMachineTooltips.progressFill(machine.cooks(), required, barW - 2);
        int fill = max ? 0xFFB8860B : 0xFF3D7EFF;
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + 4, fill);
    }

    private static boolean inBrewsBar(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + INFO_W - 16 && mouseY >= y && mouseY < y + 5;
    }

    private void renderIo(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = ioPanelX();
        int y = ioPanelY();
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, IO_PANEL, x, y, 0.0F, 0.0F, IO_W, IO_H, PANEL_TEX, PANEL_TEX);
        Component title = Component.translatable("evolvingfurnaces.gui.io_config");
        graphics.text(font, title, x + (IO_W - font.width(title)) / 2, y + 5, TEXT, false);
        EvolvingBrewingStandBlockEntity machine = machine();
        if (machine == null) {
            return;
        }
        Direction hovered = ioFaceAt(mouseX, mouseY);
        IoFace hoveredCell = null;
        for (IoFace cell : ioFaces()) {
            boolean hover = cell.world == hovered;
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ioSprite(machine.io().get(cell.world), hover), cell.x, cell.y, FACE, FACE);
            if (hover) {
                hoveredCell = cell;
            }
        }
        if (hovered != null && hoveredCell != null) {
                graphics.setTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable("evolvingfurnaces.gui.io.rel." + hoveredCell.label),
                            Component.translatable("evolvingfurnaces.gui.io.role." + machine.io().get(hovered).getSerializedName())
                    ),
                    Optional.empty(),
                    (int) mouseX,
                    (int) mouseY);
        }
    }

    private void applyLayout() {
        leftPos = (width - imageWidth) / 2;
        if (ioOpen) {
            leftPos += IO_W / 2;
        } else if (infoOpen) {
            leftPos -= INFO_W / 2;
        }
        if (ioButton != null) {
            ioButton.setPosition(ioButtonX(), buttonY());
        }
        if (infoButton != null) {
            infoButton.setPosition(infoButtonX(), buttonY());
        }
        if (upgradeButton != null) {
            upgradeButton.setPosition(upgradeButtonX(), buttonY());
        }
    }

    private void rememberPanels() {
        rememberedIoOpen = ioOpen;
        rememberedInfoOpen = infoOpen;
    }

    @Override
    public void onClose() {
        rememberPanels();
        super.onClose();
    }

    private int buttonY() {
        return topPos - 20;
    }

    private int ioButtonX() {
        return leftPos + 6;
    }

    private int infoButtonX() {
        return leftPos + imageWidth - 40;
    }

    private int upgradeButtonX() {
        return leftPos + imageWidth - 20;
    }

    private int infoPanelX() {
        return leftPos + imageWidth;
    }

    private int infoPanelY() {
        return topPos;
    }

    private int ioPanelX() {
        return leftPos - IO_W;
    }

    private int ioPanelY() {
        return topPos;
    }

    private boolean inExtraUi(double mouseX, double mouseY) {
        if (mouseY >= buttonY() && mouseY < topPos && mouseX >= ioButtonX() && mouseX < upgradeButtonX() + EFSpriteButton.SIZE) {
            return true;
        }
        if (ioOpen && inIoPanel(mouseX, mouseY)) {
            return true;
        }
        return infoOpen && inInfoPanel(mouseX, mouseY);
    }

    public List<Rect2i> guiExtraAreas() {
        List<Rect2i> areas = new ArrayList<>();
        areas.add(new Rect2i(ioButtonX(), buttonY(), upgradeButtonX() + EFSpriteButton.SIZE - ioButtonX(), EFSpriteButton.SIZE));
        if (infoOpen) {
            areas.add(new Rect2i(infoPanelX(), infoPanelY(), INFO_W, INFO_H));
        }
        if (ioOpen) {
            areas.add(new Rect2i(ioPanelX(), ioPanelY(), IO_W, IO_H));
        }
        return areas;
    }

    private boolean inIoPanel(double mouseX, double mouseY) {
        return mouseX >= ioPanelX() && mouseX < ioPanelX() + IO_W && mouseY >= ioPanelY() && mouseY < ioPanelY() + IO_H;
    }

    private boolean inInfoPanel(double mouseX, double mouseY) {
        return mouseX >= infoPanelX() && mouseX < infoPanelX() + INFO_W && mouseY >= infoPanelY() && mouseY < infoPanelY() + INFO_H;
    }

    private IoFace[] ioFaces() {
        Direction facing = Direction.NORTH;
        int x = ioPanelX();
        int y = ioPanelY();
        int step = FACE + FACE_GAP;
        int grid = FACE * 3 + FACE_GAP * 2;
        int originX = x + (IO_W - grid) / 2;
        int originY = y + 20;
        return new IoFace[] {
                new IoFace(originX + step, originY, Direction.UP, "top"),
                new IoFace(originX, originY + step, facing.getClockWise(), "left"),
                new IoFace(originX + step, originY + step, facing, "front"),
                new IoFace(originX + step * 2, originY + step, facing.getCounterClockWise(), "right"),
                new IoFace(originX + step, originY + step * 2, Direction.DOWN, "bottom"),
                new IoFace(originX + step * 2, originY + step * 2, facing.getOpposite(), "back")
        };
    }

    private Direction ioFaceAt(double mouseX, double mouseY) {
        for (IoFace cell : ioFaces()) {
            if (mouseX >= cell.x && mouseX < cell.x + FACE && mouseY >= cell.y && mouseY < cell.y + FACE) {
                return cell.world;
            }
        }
        return null;
    }

    private static Identifier ioSprite(EFIoRole role, boolean hover) {
        String name = role == EFIoRole.ENERGY ? "fuel" : role.getSerializedName();
        String path = "widget/io_" + name;
        if (hover) {
            path += "_highlighted";
        }
        return EFConstants.id(path);
    }

    private EvolvingBrewingStandBlockEntity machine() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        if (minecraft.level.getBlockEntity(menu.blockPos()) instanceof EvolvingBrewingStandBlockEntity machine) {
            return machine;
        }
        return null;
    }

    private Identifier upgradeIcon() {
        if (isMaxLevel()) {
            return UPGRADE_MAX;
        }
        if (canUpgrade()) {
            return UPGRADE_READY;
        }
        return UPGRADE;
    }

    private boolean isMaxLevel() {
        return EFTierManager.nextClient(EFMachineKind.BREWING_STAND, menu.machineLevel()).isEmpty();
    }

    private Component upgradeNarration() {
        if (isMaxLevel()) {
            return Component.translatable("evolvingfurnaces.gui.max_level");
        }
        if (canUpgrade()) {
            return Component.translatable("evolvingfurnaces.gui.upgrade_ready");
        }
        return Component.translatable("evolvingfurnaces.gui.upgrade");
    }

    private boolean canUpgrade() {
        return minecraft != null && minecraft.player != null
                && EFUpgradeHandler.canUpgradeClient(
                        minecraft.player, EFMachineKind.BREWING_STAND, menu.machineLevel(), menu.cooks());
    }

    private record IoFace(int x, int y, Direction world, String label) {
    }
}
