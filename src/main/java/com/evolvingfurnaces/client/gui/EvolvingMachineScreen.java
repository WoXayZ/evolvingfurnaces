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
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class EvolvingMachineScreen extends AbstractFurnaceScreen<EvolvingMachineMenu> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier LIT_PROGRESS = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier BURN_PROGRESS = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final Identifier IO_PANEL = EFConstants.id("textures/gui/io_panel.png");
    private static final Identifier INFO_PANEL = EFConstants.id("textures/gui/info_panel.png");

    private static final Identifier IO = EFConstants.id("widget/io");
    private static final Identifier INFO = EFConstants.id("widget/info");
    private static final Identifier UPGRADE = EFConstants.id("widget/upgrade");
    private static final Identifier UPGRADE_READY = EFConstants.id("widget/upgrade_ready");
    private static final Identifier UPGRADE_MAX = EFConstants.id("widget/upgrade_max");
    private static final Identifier GENERATOR = EFConstants.id("widget/generator");
    private static final Identifier GENERATOR_READY = EFConstants.id("widget/generator_ready");
    private static final Identifier ELECTRIC = EFConstants.id("widget/electric");
    private static final Identifier ELECTRIC_READY = EFConstants.id("widget/electric_ready");
    private static final Identifier ENERGY_EMPTY = EFConstants.id("widget/energy_empty");
    private static final Identifier ENERGY_FULL = EFConstants.id("widget/energy_full");

    static final int IO_W = 62;
    static final int IO_H = 67;
    static final int INFO_W = 150;
    static final int INFO_H = 116;
    private static final int PANEL_TEX = 256;
    private static final int FACE = 8;
    private static final int FACE_GAP = 4;
    private static final int TEXT = 0xFF404040;
    private static final int ENERGY_W = 18;
    private static final int ENERGY_H = 35;
    private static final int SLOT_INPUT_Y = 17;
    private static final int SLOT_FUEL_X = 56;
    private static final int SLOT_OUTPUT_X = 116;

    private static boolean rememberedInfoOpen;
    private static boolean rememberedIoOpen;

    private boolean infoOpen;
    private boolean ioOpen;
    private boolean bookVisible;
    private EFSpriteButton ioButton;
    private EFSpriteButton infoButton;
    private EFSpriteButton upgradeButton;
    private EFSpriteButton generatorButton;
    private EFSpriteButton electricButton;

    public EvolvingMachineScreen(EvolvingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, filterName(menu.kind()), TEXTURE, LIT_PROGRESS, BURN_PROGRESS, tabs(menu.kind()));
        this.infoOpen = rememberedInfoOpen;
        this.ioOpen = rememberedIoOpen;
        if (this.ioOpen) {
            this.infoOpen = false;
        }
    }

    private static Component filterName(EFMachineKind kind) {
        return switch (kind) {
            case BLAST_FURNACE -> Component.translatable("gui.recipebook.toggleRecipes.blastable");
            case SMOKER -> Component.translatable("gui.recipebook.toggleRecipes.smokable");
            default -> Component.translatable("gui.recipebook.toggleRecipes.smeltable");
        };
    }

    private static List<RecipeBookComponent.TabInfo> tabs(EFMachineKind kind) {
        return switch (kind) {
            case BLAST_FURNACE -> List.of(
                    new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.BLAST_FURNACE),
                    new RecipeBookComponent.TabInfo(Items.REDSTONE_ORE, RecipeBookCategories.BLAST_FURNACE_BLOCKS),
                    new RecipeBookComponent.TabInfo(Items.IRON_SHOVEL, Items.GOLDEN_LEGGINGS, RecipeBookCategories.BLAST_FURNACE_MISC));
            case SMOKER -> List.of(
                    new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.SMOKER),
                    new RecipeBookComponent.TabInfo(Items.PORKCHOP, RecipeBookCategories.SMOKER_FOOD));
            default -> List.of(
                    new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.FURNACE),
                    new RecipeBookComponent.TabInfo(Items.PORKCHOP, RecipeBookCategories.FURNACE_FOOD),
                    new RecipeBookComponent.TabInfo(Items.STONE, RecipeBookCategories.FURNACE_BLOCKS),
                    new RecipeBookComponent.TabInfo(Items.LAVA_BUCKET, Items.EMERALD, RecipeBookCategories.FURNACE_MISC));
        };
    }

    @Override
    public void init() {
        super.init();
        bookVisible = false;
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
                        ClientPacketDistributor.sendToServer(new EFUpgradeMachinePayload(menu.blockPos()));
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
    protected void onRecipeBookButtonClick() {
        bookVisible = !bookVisible;
        if (bookVisible && ioOpen) {
            ioOpen = false;
            rememberPanels();
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        applyLayout();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (infoOpen) {
            renderInfo(graphics, mouseX, mouseY);
        }
        if (ioOpen) {
            renderIo(graphics, mouseX, mouseY);
        }
        if (menu.energyModule().installed() && inEnergyBar(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                    font,
                    List.of(Component.translatable(
                            "evolvingfurnaces.gui.energy",
                            menu.energyStored(),
                            menu.energyCapacity())),
                    Optional.empty(),
                    mouseX,
                    mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        EFEnergyModule module = menu.energyModule();
        if (!module.installed()) {
            super.extractBackground(graphics, mouseX, mouseY, partialTick);
            return;
        }
        this.extractTransparentBackground(graphics);
        this.minecraft.gui.extractDeferredSubtitles();
        int i = leftPos;
        int j = topPos;
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        coverUnusedSlots(graphics);
        if (module == EFEnergyModule.GENERATOR) {
            if (menu.isLit()) {
                int flame = Mth.ceil(menu.getLitProgress() * 13.0F) + 1;
                graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, LIT_PROGRESS, 14, 14, 0, 14 - flame, i + 56, j + 36 + 14 - flame, 14, flame);
            }
        } else {
            int cook = Mth.ceil(menu.getBurnProgress() * 24.0F);
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, BURN_PROGRESS, 24, 16, 0, 0, i + 79, j + 34, cook, 16);
        }
        blitEnergyBar(graphics, energyBarX(), energyBarY(), menu.energyStored(), menu.energyCapacity());
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
            blitCooksBar(graphics, barX, barY, machine);
            if (inCooksBar(barX, barY, mouseX, mouseY)) {
                int required = EFMachineTooltips.progressGoal(menu.kind(), machine.machineLevel());
                graphics.setTooltipForNextFrame(
                        font,
                        List.of(Component.translatable("evolvingfurnaces.gui.cooks_short", machine.cooks(), required)),
                        Optional.empty(),
                        mouseX,
                        mouseY);
            }
        }
    }

    private void blitCooksBar(GuiGraphicsExtractor graphics, int x, int y, EvolvingMachineBlockEntity machine) {
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

    private void renderIo(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = ioPanelX();
        int y = ioPanelY();
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, IO_PANEL, x, y, 0.0F, 0.0F, IO_W, IO_H, PANEL_TEX, PANEL_TEX);
        Component title = Component.translatable("evolvingfurnaces.gui.io_config");
        graphics.text(font, title, x + (IO_W - font.width(title)) / 2, y + 5, TEXT, false);
        EvolvingMachineBlockEntity machine = machine();
        if (machine == null) {
            return;
        }
        Direction hovered = ioFaceAt(mouseX, mouseY);
        IoFace hoveredCell = null;
        for (IoFace cell : ioFaces()) {
            boolean hover = cell.world == hovered;
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ioSprite(machine.io().get(cell.world), machine.energyModule(), hover), cell.x, cell.y, FACE, FACE);
            if (hover) {
                hoveredCell = cell;
            }
        }
        if (hovered != null && hoveredCell != null) {
            graphics.setTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable("evolvingfurnaces.gui.io.rel." + hoveredCell.label),
                            ioRoleLabel(machine.io().get(hovered), machine.energyModule())
                    ),
                    Optional.empty(),
                    (int) mouseX,
                    (int) mouseY);
        }
    }

    private void applyLayout() {
        boolean bookOpen = bookVisible && width >= 379;
        if (bookOpen) {
            leftPos = 177 + (width - imageWidth - 200) / 2;
        } else {
            leftPos = (width - imageWidth) / 2;
        }
        if (!bookVisible) {
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
        if (!bookVisible) {
            return;
        }
        for (var child : children()) {
            if (child instanceof ImageButton image && image.getWidth() == 20 && image.getHeight() == 18) {
                image.onPress(null);
                return;
            }
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

    private static Identifier ioSprite(EFIoRole role, EFEnergyModule module, boolean hover) {
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
        boolean shift = minecraft != null && minecraft.hasShiftDown();
        if (menu.energyModule() == module) {
            if (shift) {
                ClientPacketDistributor.sendToServer(new EFInstallModulePayload(menu.blockPos(), EFEnergyModule.NONE, true));
            }
            return;
        }
        if (menu.energyModule().installed() && !shift) {
            return;
        }
        ClientPacketDistributor.sendToServer(new EFInstallModulePayload(menu.blockPos(), module, shift));
    }

    private Identifier moduleIcon(EFEnergyModule module) {
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
            return Component.translatable(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(cost.itemId().get())
                    .map(holder -> holder.value().getDescriptionId())
                    .orElse("item.minecraft.air"));
        }
        return Component.literal("#" + cost.tagId().map(Object::toString).orElse("tag"));
    }

    private void blitEnergyBar(GuiGraphicsExtractor graphics, int x, int y, int stored, int capacity) {
        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ENERGY_EMPTY, x, y, ENERGY_W, ENERGY_H);
        if (stored <= 0 || capacity <= 0) {
            return;
        }
        int filled = Mth.clamp(stored * ENERGY_H / capacity, stored > 0 ? 1 : 0, ENERGY_H);
        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ENERGY_FULL, ENERGY_W, ENERGY_H, 0, ENERGY_H - filled, x, y + ENERGY_H - filled, ENERGY_W, filled);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        if (slotCovered(slot) && slot.getItem().isEmpty()) {
            return;
        }
        super.extractSlot(graphics, slot, mouseX, mouseY);
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

    private void coverUnusedSlots(GuiGraphicsExtractor graphics) {
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

    private void coverWithAdjacent(GuiGraphicsExtractor graphics, int localX, int localY, int width, int height) {
        int srcU = localX > 0 ? localX - 1 : localX + width;
        int x = leftPos + localX;
        int y = topPos + localY;
        for (int dx = 0; dx < width; dx++) {
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, x + dx, y, srcU, localY, 1, height, 256, 256);
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
