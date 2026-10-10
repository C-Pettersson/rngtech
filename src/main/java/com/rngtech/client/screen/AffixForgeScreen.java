package com.rngtech.client.screen;

import com.rngtech.content.blockentity.AffixForgeBlockEntity;
import com.rngtech.content.item.RefinementConsumableItem;
import com.rngtech.content.item.RefinementLensItem;
import com.rngtech.content.item.RefinementModifierItem;
import com.rngtech.content.menu.AffixForgeMenu;
import com.rngtech.rpg.CorruptionOutcome;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierLensTag;
import com.rngtech.rpg.ModifierLensTargets;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.corruption.CorruptionCatalog;
import com.rngtech.rpg.corruption.CorruptionPreview;
import com.rngtech.rpg.corruption.CorruptionText;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementModifier;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementResult;
import com.rngtech.rpg.refinement.RefinementSelection;
import com.rngtech.rpg.refinement.RefinementTargets;
import com.rngtech.rpg.unique.UniqueItems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AffixForgeScreen extends AbstractContainerScreen<AffixForgeMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int TEXT_GOOD = 0xFF2F6B3F;
    private static final int TEXT_BAD = 0xFF8A3C32;
    private static final int ROW = 0xFFE0E0E0;
    private static final int BUTTON = 0xFF6C7658;
    private static final int BUTTON_DARK = 0xFF3E4634;
    private static final int BUTTON_LIGHT = 0xFFA5B17E;
    private static final int REFORGE_X = 19;
    private static final int REFORGE_Y = 64;
    private static final int REFORGE_WIDTH = 64;
    private static final int REFORGE_HEIGHT = 18;
    private static final int RP_X = 19;
    private static final int RP_Y = 90;
    private static final int RP_WIDTH = 64;
    private static final int RP_HEIGHT = 16;
    private static final int UPGRADE_Y = 143;
    private static final int PLAYER_INVENTORY_X = 132;
    private static final int PLAYER_INVENTORY_Y = 143;
    private static final int HOTBAR_Y = 201;
    private static final int MOD_PANEL_X = 104;
    private static final int MOD_PANEL_Y = 22;
    private static final int MOD_PANEL_WIDTH = 92;
    private static final int OUTCOME_PANEL_X = 204;
    private static final int OUTCOME_PANEL_Y = 22;
    private static final int OUTCOME_PANEL_WIDTH = 124;
    private static final int PANEL_HEIGHT = 100;
    private static final int ROW_HEIGHT = 14;
    private static final int MAX_ROWS = 6;
    private static final int MAX_OUTCOME_LINES = 6;
    private static final int OUTCOME_LINE_GAP = 4;
    private static final String NO_POTENTIAL = "rngtech.refinement.failure.no_potential";

    public AffixForgeScreen(AffixForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 342;
        imageHeight = 226;
        inventoryLabelX = PLAYER_INVENTORY_X;
        inventoryLabelY = 130;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderPanelTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderSlotFrame(guiGraphics, 13, 31);
        renderSlotFrame(guiGraphics, 42, 31);
        renderSlotFrame(guiGraphics, 71, 31);
        renderSlotFrame(guiGraphics, 13, UPGRADE_Y - 1);
        renderSlotFrame(guiGraphics, 42, UPGRADE_Y - 1);
        renderSlotFrame(guiGraphics, 71, UPGRADE_Y - 1);
        renderReforgeButton(guiGraphics);
        renderRpReadout(guiGraphics);
        renderSelectionPanel(guiGraphics);
        renderOutcomePanel(guiGraphics);
        renderPlayerInventoryFrames(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.refinement.target.short"), 13, 20, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.refinement.catalyst.short"), 42, 20, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.refinement.modifier.short"), 71, 20, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.affix_forge.upgrades"), 13, 130, TEXT_MUTED, false);
        drawRpLabel(guiGraphics);
        guiGraphics.drawString(font, Component.translatable("rngtech.refinement.modifiers"), MOD_PANEL_X, 12, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.refinement.outcome"), OUTCOME_PANEL_X, 12, TEXT_MUTED, false);
        Component reforge = Component.translatable("rngtech.refinement.reforge");
        guiGraphics.drawString(
                font,
                reforge,
                REFORGE_X + (REFORGE_WIDTH - font.width(reforge)) / 2,
                REFORGE_Y + 6,
                0xFFFFFFFF,
                false
        );

        drawSelectionLabels(guiGraphics);
        drawOutcomeLabels(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handleReforgeClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleReforgeClick(double mouseX, double mouseY) {
        int x = leftPos + REFORGE_X;
        int y = topPos + REFORGE_Y;
        if (mouseX < x || mouseX >= x + REFORGE_WIDTH || mouseY < y || mouseY >= y + REFORGE_HEIGHT) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null
                && menu.clickMenuButton(minecraft.player, menu.reforgeButtonId())) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.reforgeButtonId());
        }
        return true;
    }

    private void renderSelectionPanel(GuiGraphics guiGraphics) {
        int x = leftPos + MOD_PANEL_X - 2;
        int y = topPos + MOD_PANEL_Y - 2;
        guiGraphics.fill(x, y, x + MOD_PANEL_WIDTH + 4, y + PANEL_HEIGHT, 0xFF9C9C9C);
        guiGraphics.fill(x + 1, y + 1, x + MOD_PANEL_WIDTH + 3, y + PANEL_HEIGHT - 1, 0xFFD5D5D5);

        for (SelectionRow row : selectionRows()) {
            int rowX = leftPos + MOD_PANEL_X;
            int rowY = topPos + row.y();
            guiGraphics.fill(rowX, rowY, rowX + MOD_PANEL_WIDTH, rowY + ROW_HEIGHT - 1, ROW);
        }
    }

    private void drawSelectionLabels(GuiGraphics guiGraphics) {
        List<SelectionRow> rows = selectionRows();
        if (rows.isEmpty()) {
            drawClipped(
                    guiGraphics,
                    Component.translatable("rngtech.refinement.no_target_modifiers"),
                    MOD_PANEL_X + 3,
                    MOD_PANEL_Y + 4,
                    MOD_PANEL_WIDTH - 6,
                    TEXT_MUTED
            );
            return;
        }

        for (SelectionRow row : rows) {
            drawClipped(guiGraphics, row.title(), MOD_PANEL_X + 3, row.y() + 2, MOD_PANEL_WIDTH - 6, TEXT);
        }
    }

    private void renderOutcomePanel(GuiGraphics guiGraphics) {
        int x = leftPos + OUTCOME_PANEL_X - 2;
        int y = topPos + OUTCOME_PANEL_Y - 2;
        guiGraphics.fill(x, y, x + OUTCOME_PANEL_WIDTH + 4, y + PANEL_HEIGHT, 0xFF9C9C9C);
        guiGraphics.fill(x + 1, y + 1, x + OUTCOME_PANEL_WIDTH + 3, y + PANEL_HEIGHT - 1, 0xFFD5D5D5);
    }

    private void drawOutcomeLabels(GuiGraphics guiGraphics) {
        for (OutcomeBlock block : outcomeLayout()) {
            for (int row = 0; row < block.rows().size(); row++) {
                guiGraphics.drawString(
                        font,
                        block.rows().get(row),
                        OUTCOME_PANEL_X + 3,
                        block.y() + row * font.lineHeight,
                        block.line().color(),
                        false
                );
            }
        }
    }

    private List<OutcomeBlock> outcomeLayout() {
        List<OutcomeBlock> blocks = new ArrayList<>();
        int y = OUTCOME_PANEL_Y + 4;
        int bottom = OUTCOME_PANEL_Y + PANEL_HEIGHT - 5;
        for (OutcomeLine line : outcomeLines()) {
            List<FormattedCharSequence> rows = font.split(line.text(), OUTCOME_PANEL_WIDTH - 6);
            if (y + rows.size() * font.lineHeight > bottom) {
                break;
            }
            blocks.add(new OutcomeBlock(line, rows, y));
            y += rows.size() * font.lineHeight + OUTCOME_LINE_GAP;
        }
        return blocks;
    }

    private List<SelectionRow> selectionRows() {
        List<SelectionRow> rows = new ArrayList<>();
        ItemStack target = targetStack();
        if (!RefinementTargets.canRefine(target)) {
            return rows;
        }

        int y = MOD_PANEL_Y + 4;
        List<MachineModifier> affixes = affixes(targetTraits());
        for (int affixIndex = 0; affixIndex < affixes.size() && rows.size() < MAX_ROWS; affixIndex++) {
            MachineModifier modifier = affixes.get(affixIndex);
            rows.add(new SelectionRow(
                    MachineModifierText.displayName(modifier),
                    MachineModifierText.tooltipLine(modifier),
                    y
            ));
            y += ROW_HEIGHT;
        }
        return rows;
    }

    private List<OutcomeLine> outcomeLines() {
        ItemStack target = targetStack();
        if (!RefinementTargets.canRefine(target)) {
            return List.of(new OutcomeLine(Component.translatable("rngtech.refinement.failure.invalid_target"), TEXT_BAD));
        }

        RefinementOperation operation = operation();
        if (operation == null) {
            return List.of(new OutcomeLine(Component.translatable("rngtech.refinement.failure.invalid_consumable"), TEXT_BAD));
        }

        String lockedMessage = menu.lockedMessage(operation);
        if (lockedMessage != null) {
            return lockedOperationLines(lockedMessage);
        }
        String focusMessage = focusLockedMessage();
        if (focusMessage != null) {
            return lockedOperationLines(focusMessage);
        }

        RefinementModifier refinementModifier = modifier();
        Set<ModifierLensTag> lensTags = lensTags();
        if (targetTraits().isCorrupted()) {
            return List.of(new OutcomeLine(Component.translatable("rngtech.refinement.failure.corrupted"), TEXT_BAD));
        }
        if (operation == RefinementOperation.CORRUPT && !RefinementTargets.canCorrupt(target)) {
            return List.of(new OutcomeLine(Component.translatable("rngtech.refinement.failure.cannot_corrupt"), TEXT_BAD));
        }
        if (!lensTags.isEmpty() && !RefinementEngine.operationSupportsLens(operation)) {
            return focusFailureLines("rngtech.refinement.failure.lens_requires_add_or_upgrade");
        }
        if (refinementModifier.requiresCorrupt() && operation != RefinementOperation.CORRUPT) {
            return List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.failure.modifier_requires_corrupt"), TEXT_BAD),
                    modifierPreviewLine(refinementModifier)
            );
        }
        if (refinementModifier.requiresRandomUpgrade() && operation != RefinementOperation.UPGRADE_RANDOM_MODIFIER) {
            return List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.failure.modifier_requires_affix_modifier"), TEXT_BAD),
                    modifierPreviewLine(refinementModifier)
            );
        }
        if (refinementModifier != RefinementModifier.NONE
                && !refinementModifier.requiresRandomUpgrade()
                && !refinementModifier.requiresCorrupt()
                && !RefinementEngine.canUseModifier(operation.action())) {
            return List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.failure.modifier_requires_add_or_upgrade"), TEXT_BAD),
                    modifierPreviewLine(refinementModifier)
            );
        }
        if (refinementModifier.requiresSelectedUpgrade() && operation != RefinementOperation.UPGRADE_SELECTED_MODIFIER) {
            return List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.failure.modifier_requires_upgrade"), TEXT_BAD),
                    modifierPreviewLine(refinementModifier)
            );
        }

        RefinementResult dryRun = dryRun(target, operation, refinementModifier, lensTags);
        if (!dryRun.success()) {
            List<OutcomeLine> lines = focusFailureLines(dryRun.messageKey());
            if (NO_POTENTIAL.equals(dryRun.messageKey()) && hasCostRange(operation)) {
                lines.add(1, costRangeLine());
            }
            return lines;
        }
        return crystalOutcomeLines(operation);
    }

    private static RefinementResult dryRun(
            ItemStack target,
            RefinementOperation operation,
            RefinementModifier refinementModifier,
            Set<ModifierLensTag> lensTags
    ) {
        return RefinementEngine.apply(
                RefinementTargets.eligibilityProfile(target),
                RefinementTargets.storedTraits(target),
                operation,
                RefinementTargets.modifierRollComponentStage(target),
                RefinementSelection.none(),
                refinementModifier,
                lensTags,
                RandomSource.create(0L)
        );
    }

    private List<OutcomeLine> lockedOperationLines(String messageKey) {
        return List.of(
                new OutcomeLine(Component.translatable(messageKey), TEXT_BAD),
                new OutcomeLine(Component.translatable("rngtech.refinement.preview.install_upgrade"), TEXT_MUTED)
        );
    }

    private List<OutcomeLine> focusFailureLines(String messageKey) {
        List<OutcomeLine> lines = new ArrayList<>();
        lines.add(new OutcomeLine(Component.translatable(messageKey), TEXT_BAD));
        addFocusPreviewLine(lines);
        return lines;
    }

    private List<OutcomeLine> crystalOutcomeLines(RefinementOperation operation) {
        return switch (operation.action()) {
            case FULL_REROLL -> List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.full_reroll"), TEXT_GOOD),
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.cost_random"), TEXT_MUTED)
            );
            case FILL_OPEN_SLOTS -> List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.fill_slots"), TEXT_GOOD),
                    costRangeEachLine()
            );
            case RANDOM_UPGRADE -> upgradeLines(false);
            case SELECTED_UPGRADE -> upgradeLines(true);
            case RANDOM_REMOVE -> List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.random_remove"), TEXT_GOOD),
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.cost_random"), TEXT_MUTED)
            );
            case RANDOM_ADD -> candidateLines(candidateDefinitions(List.of()));
            case ASCEND_RARITY -> List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.ascend"), TEXT_GOOD),
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.upgrade_existing"), TEXT_MUTED),
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.cost_all"), TEXT_MUTED)
            );
            case CATALYZE_ASCENSION -> List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.catalyze_ascension"), TEXT_GOOD),
                    costFixedLine(RefinementEngine.ascensionCatalystPotentialCost())
            );
            case TARGETED_ADD_OR_UPGRADE -> candidateLines(candidateDefinitions(operation.targetStats()));
            case CORRUPT -> corruptionLines();
        };
    }

    /** The Volatile Catalyst table for this target; hovering Blessed or Blighted lists the implicits its pool can grant. */
    private List<OutcomeLine> corruptionLines() {
        boolean warded = modifier() == RefinementModifier.CORRUPTION_WARD;
        ItemStack target = targetStack();
        List<OutcomeLine> lines = new ArrayList<>();
        for (CorruptionPreview.Row row : CorruptionPreview.rows(
                CorruptionCatalog.active(),
                RefinementTargets.eligibilityProfile(target).id(),
                RefinementTargets.storedTraits(target),
                warded,
                UniqueItems.definition(target)
        )) {
            if (row.percent() <= 0.0 && !(warded && row.outcome() == CorruptionOutcome.BLIGHTED)) {
                continue;
            }
            lines.add(new OutcomeLine(
                    CorruptionText.chance(row, warded),
                    CorruptionText.outcomeTooltip(row, warded),
                    corruptionColor(row, warded)
            ));
        }
        lines.add(new OutcomeLine(Component.translatable("rngtech.corruption.preview.no_cost"), TEXT_MUTED));
        return lines;
    }

    private static int corruptionColor(CorruptionPreview.Row row, boolean warded) {
        if (warded && row.outcome() == CorruptionOutcome.BLIGHTED) {
            return TEXT_MUTED;
        }
        return switch (row.outcome()) {
            case UNTOUCHED -> TEXT_MUTED;
            case BLESSED -> TEXT_GOOD;
            case REFORGED, WARPED -> TEXT;
            case BLIGHTED -> TEXT_BAD;
        };
    }

    private List<OutcomeLine> upgradeLines(boolean allowTuning) {
        if (targetTraits().refinementPotential() < RefinementEngine.minPotentialCost(operation())) {
            return List.of(new OutcomeLine(Component.translatable("rngtech.refinement.failure.no_potential"), TEXT_BAD));
        }

        List<MachineModifier> upgradeable = upgradeableAffixes(allowTuning);
        if (upgradeable.isEmpty()) {
            return List.of(new OutcomeLine(Component.translatable("rngtech.refinement.failure.no_matching_modifier"), TEXT_BAD));
        }
        Set<ModifierLensTag> lensTags = lensTags();
        if (!lensTags.isEmpty()
                && upgradeable.stream().noneMatch(modifier -> definitionFor(modifier).matchesLensTags(lensTags))) {
            return focusFailureLines("rngtech.refinement.failure.no_matching_lens_affix");
        }

        List<OutcomeLine> lines = new ArrayList<>();
        lines.add(new OutcomeLine(
                Component.translatable(allowTuning
                        ? "rngtech.refinement.preview.greater_upgrade"
                        : "rngtech.refinement.preview.upgrade_existing"),
                TEXT_GOOD
        ));
        lines.add(costRangeLine());
        addModifierFocusPreviewLine(lines, upgradeable);
        return lines;
    }

    private List<OutcomeLine> candidateLines(List<ModifierDefinition> definitions) {
        if (definitions.isEmpty()) {
            return List.of(noAddableAffixLine());
        }

        Set<ModifierLensTag> lensTags = lensTags();
        if (!lensTags.isEmpty() && definitions.stream().noneMatch(definition -> definition.matchesLensTags(lensTags))) {
            return focusFailureLines("rngtech.refinement.failure.no_matching_lens_affix");
        }

        List<ModifierDefinition> previewDefinitions = definitions.stream()
                .sorted(Comparator.comparing((ModifierDefinition definition) -> definition.matchesLensTags(lensTags)).reversed())
                .toList();
        List<OutcomeLine> lines = new ArrayList<>();
        lines.add(new OutcomeLine(Component.translatable("rngtech.refinement.preview.can_roll"), TEXT_GOOD));
        lines.add(costRangeLine());
        addDefinitionFocusPreviewLine(lines, definitions);
        for (ModifierDefinition definition : previewDefinitions) {
            if (lines.size() >= MAX_OUTCOME_LINES) {
                break;
            }
            int color = !lensTags.isEmpty()
                            ? (definition.matchesLensTags(lensTags) ? TEXT_GOOD : TEXT_MUTED)
                            : TEXT;
            lines.add(new OutcomeLine(candidateName(definition), candidateTooltip(definition), color));
        }
        return lines;
    }

    private List<ModifierDefinition> candidateDefinitions(List<MachineStat> targetStats) {
        List<ModifierDefinition> definitions = new ArrayList<>();
        definitions.addAll(candidateDefinitions(ModifierSlot.PREFIX, targetStats));
        definitions.addAll(candidateDefinitions(ModifierSlot.SUFFIX, targetStats));
        return definitions;
    }

    private List<ModifierDefinition> candidateDefinitions(ModifierSlot slot, List<MachineStat> targetStats) {
        ItemStack target = targetStack();
        if (!RefinementTargets.canRefine(target) || !hasOpenSlot(slot)) {
            return List.of();
        }

        ModifierEligibilityProfile profile = RefinementTargets.eligibilityProfile(target);
        return profile.rollableDefinitions(slot).stream()
                .filter(definition -> targetStats.isEmpty()
                        || (definition.canTargetWithLens() && targetStats.contains(definition.stat())))
                .filter(definition -> affixes(targetTraits()).stream().noneMatch(definition::conflictsWith))
                .toList();
    }

    private List<MachineModifier> upgradeableAffixes(boolean allowTuning) {
        List<MachineModifier> upgradeable = upgradeableAffixesAllowingTuning(false);
        if (allowTuning && upgradeable.isEmpty()) {
            return upgradeableAffixesAllowingTuning(true);
        }
        return upgradeable;
    }

    private List<MachineModifier> upgradeableAffixesAllowingTuning(boolean allowTuning) {
        return affixes(targetTraits()).stream()
                .filter(modifier -> {
                    ModifierDefinition definition = definitionFor(modifier);
                    if (definition == null) {
                        return false;
                    }
                    int currentTier = Math.max(1, modifier.tier());
                    int maxUpgradeTier = Math.min(Math.max(1, definition.maxTier()), MachineTraitRoller.maxUpgradeTier());
                    return allowTuning || currentTier < maxUpgradeTier;
                })
                .toList();
    }

    private ModifierDefinition definitionFor(MachineModifier modifier) {
        ItemStack target = targetStack();
        if (!RefinementTargets.canRefine(target)) {
            return null;
        }
        ModifierEligibilityProfile profile = RefinementTargets.eligibilityProfile(target);
        return profile.definitions().stream()
                .filter(ModifierDefinition::canRoll)
                .filter(definition -> definition.matches(modifier))
                .findFirst()
                .orElse(null);
    }

    private boolean hasOpenSlot(ModifierSlot slot) {
        return RefinementEngine.hasOpenAffixSlot(targetTraits(), slot);
    }

    private OutcomeLine noAddableAffixLine() {
        return new OutcomeLine(
                Component.translatable(RefinementEngine.noAddableAffixFailureKey(targetTraits(), false)),
                TEXT_BAD
        );
    }

    private Component candidateName(ModifierDefinition definition) {
        return Component.translatable(
                "rngtech.refinement.candidate",
                MachineModifierText.slotLabel(definition.slot()),
                MachineModifierText.displayName(definition)
        );
    }

    private Component candidateTooltip(ModifierDefinition definition) {
        return Component.translatable(
                "rngtech.refinement.candidate",
                MachineModifierText.slotLabel(definition.slot()),
                MachineModifierText.definitionStats(definition)
        );
    }

    private RefinementOperation operation() {
        ItemStack consumable = menu.getSlot(AffixForgeBlockEntity.SLOT_CONSUMABLE).getItem();
        return consumable.getItem() instanceof RefinementConsumableItem refinementItem ? refinementItem.operation() : null;
    }

    private RefinementModifier modifier() {
        if (!menu.hasModifierSocket()) {
            return RefinementModifier.NONE;
        }
        ItemStack stack = modifierStack();
        return stack.getItem() instanceof RefinementModifierItem refinementModifierItem
                ? refinementModifierItem.modifier()
                : RefinementModifier.NONE;
    }

    private Set<ModifierLensTag> lensTags() {
        if (!menu.hasLensArray()) {
            return Set.of();
        }
        ItemStack stack = modifierStack();
        return stack.getItem() instanceof RefinementLensItem lensItem ? lensItem.lensTags() : Set.of();
    }

    private String focusLockedMessage() {
        ItemStack stack = modifierStack();
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof RefinementLensItem) {
            return menu.hasLensArray() ? null : "rngtech.refinement.failure.requires_lens_array";
        }
        if (stack.getItem() instanceof RefinementModifierItem) {
            return menu.hasModifierSocket() ? null : "rngtech.refinement.failure.requires_modifier_socket";
        }
        return "rngtech.refinement.failure.invalid_focus";
    }

    private ItemStack modifierStack() {
        return menu.getSlot(AffixForgeBlockEntity.SLOT_MODIFIER).getItem();
    }

    private OutcomeLine costRangeLine() {
        RefinementOperation operation = operation();
        return new OutcomeLine(
                Component.translatable(
                        "rngtech.refinement.preview.cost_range",
                        RefinementEngine.minPotentialCost(operation),
                        RefinementEngine.maxPotentialCost(operation)
                ),
                TEXT_MUTED
        );
    }

    private OutcomeLine costRangeEachLine() {
        RefinementOperation operation = operation();
        return new OutcomeLine(
                Component.translatable(
                        "rngtech.refinement.preview.cost_range_each",
                        RefinementEngine.minPotentialCost(operation),
                        RefinementEngine.maxPotentialCost(operation)
                ),
                TEXT_MUTED
        );
    }

    private OutcomeLine costFixedLine(int cost) {
        return new OutcomeLine(Component.translatable("rngtech.refinement.preview.cost_rp", cost), TEXT_MUTED);
    }

    private void addFocusPreviewLine(List<OutcomeLine> lines) {
        if (lines.size() >= MAX_OUTCOME_LINES) {
            return;
        }
        Set<ModifierLensTag> lensTags = lensTags();
        if (!lensTags.isEmpty()) {
            lines.add(lensBiasLine(
                    new ModifierLensTargets.MatchSummary(0, 0),
                    List.of(Component.translatable("rngtech.refinement.preview.lens_bias.tooltip"))
            ));
            return;
        }
        RefinementModifier refinementModifier = modifier();
        if (refinementModifier != RefinementModifier.NONE) {
            lines.add(modifierPreviewLine(refinementModifier));
        }
    }

    private void addDefinitionFocusPreviewLine(List<OutcomeLine> lines, List<ModifierDefinition> definitions) {
        if (lines.size() >= MAX_OUTCOME_LINES) {
            return;
        }
        Set<ModifierLensTag> lensTags = lensTags();
        if (!lensTags.isEmpty()) {
            lines.add(lensBiasLine(
                    ModifierLensTargets.summarizeDefinitions(definitions, lensTags),
                    lensDefinitionTooltip(definitions, lensTags)
            ));
            return;
        }
        addFocusPreviewLine(lines);
    }

    private void addModifierFocusPreviewLine(List<OutcomeLine> lines, List<MachineModifier> modifiers) {
        if (lines.size() >= MAX_OUTCOME_LINES) {
            return;
        }
        Set<ModifierLensTag> lensTags = lensTags();
        if (!lensTags.isEmpty()) {
            ModifierEligibilityProfile profile = RefinementTargets.eligibilityProfile(targetStack());
            lines.add(lensBiasLine(
                    ModifierLensTargets.summarizeModifiers(profile, modifiers, lensTags),
                    lensModifierTooltip(profile, modifiers, lensTags)
            ));
            return;
        }
        addFocusPreviewLine(lines);
    }

    private OutcomeLine lensBiasLine(ModifierLensTargets.MatchSummary summary, List<Component> tooltip) {
        return new OutcomeLine(
                Component.translatable(
                        "rngtech.refinement.preview.lens_bias_targeted",
                        summary.matchingCount(),
                        summary.stillPossibleCount()
                ),
                tooltip,
                TEXT_MUTED
        );
    }

    private List<Component> lensDefinitionTooltip(List<ModifierDefinition> definitions, Set<ModifierLensTag> lensTags) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("rngtech.refinement.preview.lens_bias.tooltip").withStyle(ChatFormatting.GRAY));
        List<ModifierDefinition> matching = ModifierLensTargets.matchingDefinitions(definitions, lensTags);
        if (matching.isEmpty()) {
            tooltip.add(Component.translatable("rngtech.refinement.preview.lens_bias.no_current_targets")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return tooltip;
        }
        tooltip.add(Component.translatable("rngtech.refinement.preview.lens_bias.current_targets")
                .withStyle(ChatFormatting.DARK_AQUA));
        for (ModifierDefinition definition : matching) {
            tooltip.add(candidateName(definition).copy().withStyle(ChatFormatting.BLUE));
        }
        return tooltip;
    }

    private List<Component> lensModifierTooltip(
            ModifierEligibilityProfile profile,
            List<MachineModifier> modifiers,
            Set<ModifierLensTag> lensTags
    ) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("rngtech.refinement.preview.lens_bias.tooltip").withStyle(ChatFormatting.GRAY));
        List<MachineModifier> matching = ModifierLensTargets.matchingModifiers(profile, modifiers, lensTags);
        if (matching.isEmpty()) {
            tooltip.add(Component.translatable("rngtech.refinement.preview.lens_bias.no_current_targets")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return tooltip;
        }
        tooltip.add(Component.translatable("rngtech.refinement.preview.lens_bias.current_targets")
                .withStyle(ChatFormatting.DARK_AQUA));
        for (MachineModifier modifier : matching) {
            tooltip.add(Component.translatable(
                    "rngtech.refinement.candidate",
                    MachineModifierText.slotLabel(modifier.slot()),
                    MachineModifierText.displayName(modifier)
            ).withStyle(ChatFormatting.BLUE));
        }
        return tooltip;
    }

    private OutcomeLine modifierPreviewLine(RefinementModifier refinementModifier) {
        return new OutcomeLine(
                Component.translatable("rngtech.refinement.preview.modifier." + refinementModifier.name().toLowerCase(Locale.ROOT)),
                TEXT_MUTED
        );
    }

    private static boolean hasCostRange(RefinementOperation operation) {
        return operation == RefinementOperation.ADD_MODIFIER
                || operation == RefinementOperation.UPGRADE_RANDOM_MODIFIER
                || operation == RefinementOperation.UPGRADE_SELECTED_MODIFIER;
    }

    private ItemStack targetStack() {
        return menu.getSlot(AffixForgeBlockEntity.SLOT_TARGET).getItem();
    }

    private MachineTraits targetTraits() {
        ItemStack target = targetStack();
        return RefinementTargets.canRefine(target) ? RefinementTargets.traits(target) : MachineTraits.EMPTY;
    }

    private List<MachineModifier> affixes(MachineTraits traits) {
        return traits.modifiers().stream().filter(modifier -> modifier.slot().isAffix()).toList();
    }

    private void renderPanelTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        if (hoveredRpReadout(mouseX, mouseY) && targetTraits().isCorrupted()) {
            guiGraphics.renderTooltip(font, Component.translatable("rngtech.tooltip.corrupted"), mouseX, mouseY);
            return;
        }
        if (hoveredRpReadout(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.tooltip.refinement_potential", targetTraits().refinementPotential()),
                    mouseX,
                    mouseY
            );
            return;
        }

        Component upgradeTooltip = hoveredUpgradeSlotTooltip(mouseX, mouseY);
        if (upgradeTooltip != null) {
            guiGraphics.renderTooltip(font, upgradeTooltip, mouseX, mouseY);
            return;
        }

        Component inputTooltip = hoveredEmptyInputTooltip(mouseX, mouseY);
        if (inputTooltip != null) {
            guiGraphics.renderTooltip(font, inputTooltip, mouseX, mouseY);
            return;
        }

        SelectionRow selectionRow = hoveredSelectionRow(mouseX, mouseY);
        if (selectionRow != null) {
            guiGraphics.renderComponentTooltip(
                    font,
                    List.of(selectionRow.title(), selectionRow.tooltip().copy().withStyle(ChatFormatting.GRAY)),
                    mouseX,
                    mouseY
            );
            return;
        }

        OutcomeLine outcomeLine = hoveredOutcomeLine(mouseX, mouseY);
        if (outcomeLine != null) {
            guiGraphics.renderComponentTooltip(font, outcomeLine.tooltip(), mouseX, mouseY);
        }
    }

    private boolean hoveredRpReadout(int mouseX, int mouseY) {
        int x = leftPos + RP_X;
        int y = topPos + RP_Y;
        return mouseX >= x && mouseX < x + RP_WIDTH && mouseY >= y && mouseY < y + RP_HEIGHT;
    }

    private Component hoveredUpgradeSlotTooltip(int mouseX, int mouseY) {
        if (hoveredSlot(mouseX, mouseY, 14, UPGRADE_Y)
                && !menu.getSlot(AffixForgeBlockEntity.SLOT_LENS_ARRAY).hasItem()) {
            return Component.translatable("rngtech.affix_forge.upgrade.lens_array.tooltip");
        }
        if (hoveredSlot(mouseX, mouseY, 43, UPGRADE_Y)
                && !menu.getSlot(AffixForgeBlockEntity.SLOT_MODIFIER_SOCKET).hasItem()) {
            return Component.translatable("rngtech.affix_forge.upgrade.modifier_socket.tooltip");
        }
        if (hoveredSlot(mouseX, mouseY, 72, UPGRADE_Y)
                && !menu.getSlot(AffixForgeBlockEntity.SLOT_RESONANCE_MATRIX).hasItem()) {
            return Component.translatable("rngtech.affix_forge.upgrade.resonance_matrix.tooltip");
        }
        return null;
    }

    private Component hoveredEmptyInputTooltip(int mouseX, int mouseY) {
        if (hoveredSlot(mouseX, mouseY, 14, 32) && !menu.getSlot(AffixForgeBlockEntity.SLOT_TARGET).hasItem()) {
            return Component.translatable("rngtech.refinement.failure.invalid_target");
        }
        if (hoveredSlot(mouseX, mouseY, 43, 32) && !menu.getSlot(AffixForgeBlockEntity.SLOT_CONSUMABLE).hasItem()) {
            return Component.translatable("rngtech.refinement.failure.invalid_consumable");
        }
        if (hoveredSlot(mouseX, mouseY, 72, 32) && !menu.getSlot(AffixForgeBlockEntity.SLOT_MODIFIER).hasItem()) {
            return Component.translatable("rngtech.refinement.failure.invalid_focus");
        }
        return null;
    }

    private boolean hoveredSlot(int mouseX, int mouseY, int slotX, int slotY) {
        int x = leftPos + slotX;
        int y = topPos + slotY;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private SelectionRow hoveredSelectionRow(int mouseX, int mouseY) {
        for (SelectionRow row : selectionRows()) {
            int x = leftPos + MOD_PANEL_X;
            int y = topPos + row.y();
            if (mouseX >= x && mouseX < x + MOD_PANEL_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                return row;
            }
        }
        return null;
    }

    private OutcomeLine hoveredOutcomeLine(int mouseX, int mouseY) {
        int x = leftPos + OUTCOME_PANEL_X;
        for (OutcomeBlock block : outcomeLayout()) {
            int y = topPos + block.y();
            int height = block.rows().size() * font.lineHeight;
            if (mouseX >= x && mouseX < x + OUTCOME_PANEL_WIDTH && mouseY >= y && mouseY < y + height) {
                return block.line();
            }
        }
        return null;
    }

    private void drawClipped(GuiGraphics guiGraphics, Component component, int x, int y, int width, int color) {
        String clipped = font.plainSubstrByWidth(component.getString(), width);
        guiGraphics.drawString(font, Component.literal(clipped), x, y, color, false);
    }

    private void renderPanel(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        guiGraphics.fill(x, y, x + imageWidth, y + 1, PANEL_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + imageHeight, PANEL_LIGHT);
        guiGraphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        guiGraphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, PANEL_DARK);
    }

    private void renderReforgeButton(GuiGraphics guiGraphics) {
        int x = leftPos + REFORGE_X;
        int y = topPos + REFORGE_Y;
        guiGraphics.fill(x, y, x + REFORGE_WIDTH, y + REFORGE_HEIGHT, BUTTON_DARK);
        guiGraphics.fill(x + 1, y + 1, x + REFORGE_WIDTH - 1, y + REFORGE_HEIGHT - 1, BUTTON);
        guiGraphics.fill(x + 1, y + 1, x + REFORGE_WIDTH - 1, y + 2, BUTTON_LIGHT);
    }

    private void renderRpReadout(GuiGraphics guiGraphics) {
        int x = leftPos + RP_X;
        int y = topPos + RP_Y;
        guiGraphics.fill(x, y, x + RP_WIDTH, y + RP_HEIGHT, 0xFF595959);
        guiGraphics.fill(x + 1, y + 1, x + RP_WIDTH - 1, y + RP_HEIGHT - 1, 0xFFD5D5D5);
        guiGraphics.fill(x + 1, y + 1, x + RP_WIDTH - 1, y + 2, PANEL_LIGHT);
    }

    private void drawRpLabel(GuiGraphics guiGraphics) {
        if (targetTraits().isCorrupted()) {
            drawClipped(guiGraphics, Component.translatable("rngtech.tooltip.corrupted"), RP_X + 4, RP_Y + 4, RP_WIDTH - 8, TEXT_BAD);
            return;
        }
        Component label = Component.translatable("rngtech.refinement.rp", targetTraits().refinementPotential());
        drawClipped(
                guiGraphics,
                label,
                RP_X + 4,
                RP_Y + 4,
                RP_WIDTH - 8,
                targetTraits().refinementPotential() > 0 ? TEXT : TEXT_BAD
        );
    }

    private void renderPlayerInventoryFrames(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(
                        guiGraphics,
                        PLAYER_INVENTORY_X - 1 + column * 18,
                        PLAYER_INVENTORY_Y - 1 + row * 18
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, PLAYER_INVENTORY_X - 1 + column * 18, HOTBAR_Y - 1);
        }
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private record SelectionRow(Component title, Component tooltip, int y) {
    }

    private record OutcomeBlock(OutcomeLine line, List<FormattedCharSequence> rows, int y) {
    }

    private record OutcomeLine(Component text, List<Component> tooltip, int color) {
        private OutcomeLine(Component text, int color) {
            this(text, List.of(text), color);
        }

        private OutcomeLine(Component text, Component tooltip, int color) {
            this(text, List.of(tooltip), color);
        }
    }
}
