package com.evolvingfurnaces.client.gui;

import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EFEnergyModule;
import com.evolvingfurnaces.machine.EFMachineTooltips;
import com.evolvingfurnaces.machine.EFUpgradeHandler;
import com.evolvingfurnaces.machine.EvolvingMachineBlockEntity;
import com.evolvingfurnaces.machine.EvolvingMachineMenu;
import com.evolvingfurnaces.network.EFConfigureIoPayload;
import com.evolvingfurnaces.network.EFInstallModulePayload;
import com.evolvingfurnaces.network.EFUpgradeMachinePayload;
import com.evolvingfurnaces.tier.EFCostIngredient;
import com.evolvingfurnaces.tier.EFIoRole;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import com.evolvingfurnaces.tier.EFUpgradeCost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.screens.recipebook.AbstractFurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.BlastingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SmeltingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SmokingRecipeBookComponent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.neoforged.neoforge.network.PacketDistributor;

public class EvolvingMachineScreen extends AbstractFurnaceScreen<EvolvingMachineMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation LIT_PROGRESS = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN_PROGRESS = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final ResourceLocation IO_PANEL = EFConstants.id("textures/gui/io_panel.png");
    private static final ResourceLocation INFO_PANEL = EFConstants.id("textures/gui/info_panel.png");

    private static final ResourceLocation IO = EFConstants.id("widget/io");
    private static final ResourceLocation INFO = EFConstants.id("widget/info");
    private static final ResourceLocation UPGRADE = EFConstants.id("widget/upgrade");
    private static final ResourceLocation UPGRADE_READY = EFConstants.id("widget/upgrade_ready");
    private static final ResourceLocation UPGRADE_MAX = EFConstants.id("widget/upgrade_max");
    private static final ResourceLocation GENERATOR = EFConstants.id("widget/generator");
    private static final ResourceLocation GENERATOR_READY = EFConstants.id("widget/generator_ready");
    private static final ResourceLocation ELECTRIC = EFConstants.id("widget/electric");
    private static final ResourceLocation ELECTRIC_READY = EFConstants.id("widget/electric_ready");
    private static final ResourceLocation ENERGY_EMPTY = EFConstants.id("widget/energy_empty");
    private static final ResourceLocation ENERGY_FULL = EFConstants.id("widget/energy_full");

    static final int IO_W = 62;
    static final int IO_H = 67;
    static final int INFO_W = 150;
    static final int INFO_H = 116;
    private static final int PANEL_TEX = 256;
    private static final int FACE = 8;
    private static final int FACE_GAP = 4;
    private static final int TEXT = 0x404040;
    private static final int ENERGY_W = 18;
    private static final int ENERGY_H = 35;
    private static final int SLOT_INPUT_Y = 17;
    private static final int SLOT_FUEL_X = 56;
    private static final int SLOT_OUTPUT_X = 116;

    private static boolean rememberedInfoOpen;
    private static boolean rememberedIoOpen;

    private boolean infoOpen;
    private boolean ioOpen;
    private boolean lastBookVisible;
    private EFSpriteButton ioButton;
    private EFSpriteButton infoButton;
    private EFSpriteButton upgradeButton;
    private EFSpriteButton generatorButton;
    private EFSpriteButton electricButton;

    public EvolvingMachineScreen(EvolvingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, recipeBook(menu.kind()), inventory, title, TEXTURE, LIT_PROGRESS, BURN_PROGRESS);
        this.infoOpen = rememberedInfoOpen;
        this.ioOpen = rememberedIoOpen;
        if (this.ioOpen) {
            this.infoOpen = false;
        }
    }

    private static AbstractFurnaceRecipeBookComponent recipeBook(EFMachineKind kind) {
        return switch (kind) {
            case FURNACE -> new SmeltingRecipeBookComponent();
            case BLAST_FURNACE -> new BlastingRecipeBookComponent();
            case SMOKER -> new SmokingRecipeBookComponent();
            case BREWING_STAND -> new SmeltingRecipeBookComponent();
        };
    }

    @Override
    public void init() {
        super.init();
        lastBookVisible = recipeBookComponent.isVisible();
        applyLayout();
        ioButton = addRenderableWidget(new EFSpriteButton(
                ioButtonX(),
                buttonY(),
                () -> IO,
                button -> {
                    ioOpen = !ioOpen;
                    if (ioOpen) {
                        infoOpen = false;
                        hideRecipeBook();
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
                        PacketDistributor.sendToServer(new EFUpgradeMachinePayload(menu.blockPos()));
                    }
                },
                Component.translatable("evolvingfurnaces.gui.upgrade")));
        generatorButton = addRenderableWidget(new EFSpriteButton(
                generatorButtonX(),
                buttonY(),
                () -> moduleIcon(EFEnergyModule.GENERATOR),
                () -> menu.energyModule() == EFEnergyModule.GENERATOR,
                true,
                button -> tryInstall(EFEnergyModule.GENERATOR),
                Component.translatable("evolvingfurnaces.gui.module.generator")));
        electricButton = addRenderableWidget(new EFSpriteButton(
                electricButtonX(),
                buttonY(),
                () -> moduleIcon(EFEnergyModule.ELECTRIC),
                () -> menu.energyModule() == EFEnergyModule.ELECTRIC,
                true,
                button -> tryInstall(EFEnergyModule.ELECTRIC),
                Component.translatable("evolvingfurnaces.gui.module.electric")));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (recipeBookComponent.isVisible() && !lastBookVisible && ioOpen) {
            ioOpen = false;
            rememberPanels();
        }
        lastBookVisible = recipeBookComponent.isVisible();
        applyLayout();
        if (upgradeButton != null) {
            upgradeButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(upgradeNarration()));
        }
        if (generatorButton != null) {
            generatorButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(moduleNarration(EFEnergyModule.GENERATOR)));
        }
        if (electricButton != null) {
            electricButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(moduleNarration(EFEnergyModule.ELECTRIC)));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        applyLayout();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (infoOpen) {
            renderInfo(graphics, mouseX, mouseY);
        }
        if (ioOpen) {
            renderIo(graphics, mouseX, mouseY);
        }
        if (menu.energyModule().installed() && inEnergyBar(mouseX, mouseY)) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(Component.translatable(
                            "evolvingfurnaces.gui.energy",
                            menu.energyStored(),
                            menu.energyCapacity())),
                    mouseX,
                    mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        EFEnergyModule module = menu.energyModule();
        if (!module.installed()) {
            super.renderBg(graphics, partialTick, mouseX, mouseY);
            return;
        }
        int i = leftPos;
        int j = topPos;
        graphics.blit(TEXTURE, i, j, 0, 0, imageWidth, imageHeight);
        coverUnusedSlots(graphics);
        if (module == EFEnergyModule.GENERATOR) {
            if (menu.isLit()) {
                int flame = Mth.ceil(menu.getLitProgress() * 13.0F) + 1;
                graphics.blitSprite(LIT_PROGRESS, 14, 14, 0, 14 - flame, i + 56, j + 36 + 14 - flame, 14, flame);
            }
        } else {
            int cook = Mth.ceil(menu.getBurnProgress() * 24.0F);
            graphics.blitSprite(BURN_PROGRESS, 24, 16, 0, 0, i + 79, j + 34, cook, 16);
        }
        blitEnergyBar(graphics, energyBarX(), energyBarY(), menu.energyStored(), menu.energyCapacity());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && ioOpen) {
            Direction face = ioFaceAt(mouseX, mouseY);
            if (face != null) {
                PacketDistributor.sendToServer(new EFConfigureIoPayload(menu.blockPos(), face));
                return true;
            }
            if (inIoPanel(mouseX, mouseY)) {
                return true;
            }
        }
        if (button == 0 && infoOpen && inInfoPanel(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        if (inExtraUi(mouseX, mouseY)) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    private void renderInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = infoPanelX();
        int y = infoPanelY();
        graphics.blit(INFO_PANEL, x, y, 0.0F, 0.0F, INFO_W, INFO_H, PANEL_TEX, PANEL_TEX);
        Component title = Component.translatable("evolvingfurnaces.gui.info");
        graphics.drawString(font, title, x + (INFO_W - font.width(title)) / 2, y + 5, TEXT, false);
        List<Component> lines = new ArrayList<>();
        EvolvingMachineBlockEntity machine = machine();
        if (machine != null) {
            EFMachineTooltips.appendInfo(
                    lines,
                    menu.kind(),
                    machine.machineLevel(),
                    machine.cooks(),
                    menu.energyModule(),
                    minecraft.player);
        }
        int lineY = y + 18;
        for (Component line : lines) {
            final int color;
            if (line.getStyle().getColor() != null
                    && ChatFormatting.GOLD.getColor() != null
                    && line.getStyle().getColor().getValue() == ChatFormatting.GOLD.getColor()) {
                color = 0xB36B00;
            } else {
                color = TEXT;
            }
            graphics.drawString(font, line.copy().withStyle(style -> style.withColor(color)), x + 8, lineY, color, false);
            lineY += 10;
        }
        if (machine != null) {
            int barX = x + 8;
            int barY = y + INFO_H - 16;
            blitCooksBar(graphics, barX, barY, machine);
            if (inCooksBar(barX, barY, mouseX, mouseY)) {
                int required = EFMachineTooltips.progressGoal(menu.kind(), machine.machineLevel());
                graphics.renderComponentTooltip(
                        font,
                        List.of(Component.translatable("evolvingfurnaces.gui.cooks_short", machine.cooks(), required)),
                        mouseX,
                        mouseY);
            }
        }
    }

    private void blitCooksBar(GuiGraphics graphics, int x, int y, EvolvingMachineBlockEntity machine) {
        boolean max = EFTierManager.nextClient(menu.kind(), machine.machineLevel()).isEmpty();
        int barW = INFO_W - 16;
        graphics.fill(x, y, x + barW, y + 5, 0xFF8B8B8B);
        graphics.fill(x, y, x + barW - 1, y + 4, 0xFF373737);
        int required = EFMachineTooltips.progressGoal(menu.kind(), machine.machineLevel());
        int filled = EFMachineTooltips.progressFill(machine.cooks(), required, barW - 2);
        int fill = max ? 0xFFB8860B : 0xFF3D7EFF;
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + 4, fill);
    }

    private static boolean inCooksBar(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + INFO_W - 16 && mouseY >= y && mouseY < y + 5;
    }

    private void renderIo(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = ioPanelX();
        int y = ioPanelY();
        graphics.blit(IO_PANEL, x, y, 0.0F, 0.0F, IO_W, IO_H, PANEL_TEX, PANEL_TEX);
        Component title = Component.translatable("evolvingfurnaces.gui.io_config");
        graphics.drawString(font, title, x + (IO_W - font.width(title)) / 2, y + 5, TEXT, false);
        EvolvingMachineBlockEntity machine = machine();
        if (machine == null) {
            return;
        }
        Direction hovered = ioFaceAt(mouseX, mouseY);
        IoFace hoveredCell = null;
        for (IoFace cell : ioFaces()) {
            boolean hover = cell.world == hovered;
            graphics.blitSprite(ioSprite(machine.io().get(cell.world), machine.energyModule(), hover), cell.x, cell.y, FACE, FACE);
            if (hover) {
                hoveredCell = cell;
            }
        }
        if (hovered != null && hoveredCell != null) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(
                            Component.translatable("evolvingfurnaces.gui.io.rel." + hoveredCell.label),
                            ioRoleLabel(machine.io().get(hovered), machine.energyModule())
                    ),
                    (int) mouseX,
                    (int) mouseY);
        }
    }

    private void applyLayout() {
        if (recipeBookComponent.isVisible() && ioOpen) {
            ioOpen = false;
            rememberPanels();
        }
        leftPos = recipeBookComponent.updateScreenPosition(width, imageWidth);
        if (!recipeBookComponent.isVisible()) {
            if (ioOpen) {
                leftPos += IO_W / 2;
            } else if (infoOpen) {
                leftPos -= INFO_W / 2;
            }
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
        boolean generatorVisible = EFConfig.moduleAvailable(menu.kind(), EFEnergyModule.GENERATOR);
        boolean electricVisible = EFConfig.moduleAvailable(menu.kind(), EFEnergyModule.ELECTRIC);
        if (generatorButton != null) {
            generatorButton.visible = generatorVisible;
            generatorButton.setPosition(generatorButtonX(), buttonY());
        }
        if (electricButton != null) {
            electricButton.visible = electricVisible;
            electricButton.setPosition(generatorVisible ? electricButtonX() : generatorButtonX(), buttonY());
        }
        for (var child : children()) {
            if (child instanceof ImageButton image && image.getWidth() == 20 && image.getHeight() == 18) {
                image.setPosition(leftPos + 20, height / 2 - 49);
            }
        }
    }

    private void hideRecipeBook() {
        if (recipeBookComponent.isVisible()) {
            recipeBookComponent.toggleVisibility();
            lastBookVisible = false;
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

    private int generatorButtonX() {
        return leftPos + 24;
    }

    private int electricButtonX() {
        return leftPos + 42;
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
        Direction facing = facing();
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

    private static ResourceLocation ioSprite(EFIoRole role, EFEnergyModule module, boolean hover) {
        String name = ioSpriteKey(role, module);
        String path = "widget/io_" + name;
        if (hover) {
            path += "_highlighted";
        }
        return EFConstants.id(path);
    }

    private static String ioSpriteKey(EFIoRole role, EFEnergyModule module) {
        if (role == EFIoRole.ENERGY) {
            return module == EFEnergyModule.GENERATOR ? "energy_output" : "energy_input";
        }
        return role.getSerializedName();
    }

    private Direction facing() {
        if (minecraft != null && minecraft.level != null) {
            var state = minecraft.level.getBlockState(menu.blockPos());
            if (state.hasProperty(AbstractFurnaceBlock.FACING)) {
                return state.getValue(AbstractFurnaceBlock.FACING);
            }
        }
        return Direction.NORTH;
    }

    private EvolvingMachineBlockEntity machine() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        if (minecraft.level.getBlockEntity(menu.blockPos()) instanceof EvolvingMachineBlockEntity machine) {
            return machine;
        }
        return null;
    }

    private ResourceLocation upgradeIcon() {
        if (isMaxLevel()) {
            return UPGRADE_MAX;
        }
        if (canUpgrade()) {
            return UPGRADE_READY;
        }
        return UPGRADE;
    }

    private boolean isMaxLevel() {
        return EFTierManager.nextClient(menu.kind(), menu.machineLevel()).isEmpty();
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
                && EFUpgradeHandler.canUpgradeClient(minecraft.player, menu.kind(), menu.machineLevel(), menu.cooks());
    }

    private void tryInstall(EFEnergyModule module) {
        boolean shift = hasShiftDown();
        if (menu.energyModule() == module) {
            if (shift) {
                PacketDistributor.sendToServer(new EFInstallModulePayload(menu.blockPos(), EFEnergyModule.NONE, true));
            }
            return;
        }
        if (menu.energyModule().installed() && !shift) {
            return;
        }
        PacketDistributor.sendToServer(new EFInstallModulePayload(menu.blockPos(), module, shift));
    }

    private ResourceLocation moduleIcon(EFEnergyModule module) {
        boolean ready = canInstall(module);
        if (module == EFEnergyModule.GENERATOR) {
            return ready ? GENERATOR_READY : GENERATOR;
        }
        return ready ? ELECTRIC_READY : ELECTRIC;
    }

    private boolean canInstall(EFEnergyModule module) {
        return minecraft != null && minecraft.player != null
                && EFUpgradeHandler.canInstallModuleClient(minecraft.player, menu.kind(), menu.energyModule(), module);
    }

    private Component moduleNarration(EFEnergyModule module) {
        Component name = Component.translatable("evolvingfurnaces.gui.module." + module.getSerializedName());
        if (menu.energyModule() == module) {
            List<Component> installed = new java.util.ArrayList<>();
            installed.add(Component.translatable("evolvingfurnaces.gui.module_installed", name));
            installed.add(Component.translatable("evolvingfurnaces.gui.module_remove"));
            Component joined = installed.getFirst();
            for (int i = 1; i < installed.size(); i++) {
                joined = joined.copy().append(Component.literal("\n")).append(installed.get(i));
            }
            return joined;
        }
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(name);
        if (menu.energyModule().installed()) {
            lines.add(Component.translatable("evolvingfurnaces.gui.module_swap"));
        }
        EFUpgradeCost cost = EFTierManager.listClient(menu.kind()).moduleCost(module);
        lines.add(Component.translatable("evolvingfurnaces.gui.xp_cost", cost.experienceLevels()));
        for (EFCostIngredient item : cost.items()) {
            lines.add(Component.literal("- " + item.count() + " ").append(costLabel(item)));
        }
        Component joined = lines.getFirst();
        for (int i = 1; i < lines.size(); i++) {
            joined = joined.copy().append(Component.literal("\n")).append(lines.get(i));
        }
        return joined;
    }

    private static Component costLabel(EFCostIngredient cost) {
        if (cost.itemId().isPresent()) {
            return Component.translatable(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(cost.itemId().get()).getDescriptionId());
        }
        return Component.literal("#" + cost.tagId().map(Object::toString).orElse("tag"));
    }

    private void blitEnergyBar(GuiGraphics graphics, int x, int y, int stored, int capacity) {
        graphics.blitSprite(ENERGY_EMPTY, x, y, ENERGY_W, ENERGY_H);
        if (stored <= 0 || capacity <= 0) {
            return;
        }
        int filled = Mth.clamp(stored * ENERGY_H / capacity, stored > 0 ? 1 : 0, ENERGY_H);
        graphics.blitSprite(ENERGY_FULL, ENERGY_W, ENERGY_H, 0, ENERGY_H - filled, x, y + ENERGY_H - filled, ENERGY_W, filled);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (slotCovered(slot) && slot.getItem().isEmpty()) {
            return;
        }
        super.renderSlot(graphics, slot);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (width == 16 && height == 16 && coveredEmptySlotAt(x, y)) {
            return false;
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    private boolean coveredEmptySlotAt(int x, int y) {
        int count = Math.min(3, menu.slots.size());
        for (int i = 0; i < count; i++) {
            Slot slot = menu.slots.get(i);
            if (slotCovered(slot) && slot.getItem().isEmpty() && slot.x == x && slot.y == y) {
                return true;
            }
        }
        return false;
    }

    private boolean slotCovered(Slot slot) {
        if (slot.index > 2) {
            return false;
        }
        return switch (menu.energyModule()) {
            case GENERATOR -> slot.index == 0 || slot.index == 2;
            case ELECTRIC -> slot.index == 1;
            case NONE -> false;
        };
    }

    private int energyBarX() {
        return menu.energyModule() == EFEnergyModule.ELECTRIC ? leftPos + SLOT_FUEL_X - 1 : leftPos + SLOT_OUTPUT_X;
    }

    private int energyBarY() {
        return menu.energyModule() == EFEnergyModule.ELECTRIC ? topPos + 36 : topPos + SLOT_INPUT_Y + 8;
    }

    private boolean inEnergyBar(int mouseX, int mouseY) {
        int x = energyBarX();
        int y = energyBarY();
        return mouseX >= x && mouseX < x + ENERGY_W && mouseY >= y && mouseY < y + ENERGY_H;
    }

    private void coverUnusedSlots(GuiGraphics graphics) {
        switch (menu.energyModule()) {
            case GENERATOR -> {
                coverWithAdjacent(graphics, 55, 16, 18, 18);
                coverWithAdjacent(graphics, 111, 30, 26, 26);
            }
            case ELECTRIC -> coverWithAdjacent(graphics, 55, 36, 18, 35);
            case NONE -> {
            }
        }
    }

    private void coverWithAdjacent(GuiGraphics graphics, int localX, int localY, int width, int height) {
        int srcU = localX > 0 ? localX - 1 : localX + width;
        int x = leftPos + localX;
        int y = topPos + localY;
        for (int dx = 0; dx < width; dx++) {
            graphics.blit(TEXTURE, x + dx, y, srcU, localY, 1, height);
        }
    }

    private static Component ioRoleLabel(EFIoRole role, EFEnergyModule module) {
        if (role == EFIoRole.ENERGY) {
            if (module == EFEnergyModule.GENERATOR) {
                return Component.translatable("evolvingfurnaces.gui.io.role.energy_output");
            }
            if (module == EFEnergyModule.ELECTRIC) {
                return Component.translatable("evolvingfurnaces.gui.io.role.energy_input");
            }
        }
        return Component.translatable("evolvingfurnaces.gui.io.role." + role.getSerializedName());
    }

    private record IoFace(int x, int y, Direction world, String label) {
    }
}
