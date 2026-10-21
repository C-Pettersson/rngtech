package com.rngtech.content.blockentity;

import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.GlyphItem;
import com.rngtech.content.menu.PhaseCradleMenu;
import com.rngtech.content.recipe.PhaseArrayRecipe;
import com.rngtech.content.recipe.PhaseArrayRecipeInput;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.PhaseArrayRules;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.PhaseArrayAction;
import com.rngtech.rpg.refinement.PhaseLineStats;
import com.rngtech.rpg.refinement.PhaseOutcome;
import com.rngtech.rpg.refinement.PhasePulse;
import com.rngtech.rpg.refinement.PhaseStrain;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementResult;
import com.rngtech.rpg.refinement.RefinementSelection;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The Phase Cradle: the end of a Phase Array line. It holds the target, the Glyph, a donor, and the output, scans for
 * its line, charges a pulse from supply and its Battery Cell sockets, and fires the selected operation through the
 * refinement library. The block keeps the {@code rngtech:exotic_affix_forge} id it had as the Exotic Affix Forge.
 */
public class PhaseCradleBlockEntity extends BlockEntity implements MenuProvider, MachineInfoProvider {
    public static final int SLOT_TARGET = 0;
    public static final int SLOT_GLYPH = 1;
    public static final int SLOT_DONOR = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int CELL_SLOTS = 9;

    public static final int STATUS_NO_LINE = 0;
    public static final int STATUS_NO_RECIPE = 1;
    public static final int STATUS_NO_TARGET = 2;
    public static final int STATUS_LOCKED = 3;
    public static final int STATUS_NO_COILS = 4;
    public static final int STATUS_OUTPUT_FULL = 5;
    public static final int STATUS_NO_SELECTION = 6;
    public static final int STATUS_ILLEGAL = 7;
    public static final int STATUS_NO_GLYPH = 8;
    public static final int STATUS_NO_RP = 9;
    public static final int STATUS_IDLE = 10;
    public static final int STATUS_CHARGING = 11;
    public static final int STATUS_READY = 12;

    public static final int DATA_STATUS = 0;
    public static final int DATA_FORMED = 1;
    public static final int DATA_COIL_COUNT = 2;
    public static final int DATA_USABLE_COILS = 3;
    public static final int DATA_ACTION = 4;
    public static final int DATA_SELECTION = 5;
    public static final int DATA_DEFINITION = 6;
    public static final int DATA_DONOR_AFFIX = 7;
    public static final int DATA_CHARGE_LOW = 8;
    public static final int DATA_CHARGE_HIGH = 9;
    public static final int DATA_COST_LOW = 10;
    public static final int DATA_COST_HIGH = 11;
    public static final int DATA_CHARGE_RATE = 12;
    public static final int DATA_LAST_INPUT = 13;
    public static final int DATA_LAST_OUTCOME = 14;
    public static final int DATA_LAST_HELD = 15;
    public static final int DATA_LAST_SLIPPED = 16;
    public static final int DATA_STRAIN = 17;
    public static final int DATA_RP_COST = 18;
    public static final int DATA_GLYPH_TIER = 19;
    public static final int DATA_REQUIRED_COILS = 20;
    public static final int DATA_ODDS_SUCCESS = 21;
    public static final int DATA_ODDS_OVERHOLD = 22;
    public static final int DATA_ODDS_PARTIAL = 23;
    public static final int DATA_ODDS_MISS = 24;
    public static final int DATA_ODDS_FRACTURE = 25;
    public static final int DATA_PARTIAL_WINDOW = 26;
    public static final int DATA_STRAIN_TOLERANCE = 27;
    public static final int DATA_CHARGING = 28;
    public static final int DATA_HOLD_BASE = 29;
    public static final int DATA_COUNT = DATA_HOLD_BASE + PhaseArrayRules.MAX_COILS;
    /** Odds and hold chances cross the menu as thousandths. */
    public static final int ODDS_SCALE = 1000;

    private static final int LINE_RESCAN_INTERVAL = 80;
    private static final int NO_OUTCOME = -1;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_TARGET, SLOT_DONOR -> RefinementTargets.canRefine(stack);
                case SLOT_GLYPH -> GlyphItem.isGlyph(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_GLYPH ? 64 : 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_TARGET) {
                selection = RefinementSelection.none();
                definitionIndex = 0;
            }
            if (slot == SLOT_DONOR) {
                donorAffixIndex = 0;
            }
            setChanged();
        }
    };
    private final ItemStackHandler cells = new ItemStackHandler(CELL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return BatteryCellItem.isBatteryCell(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IEnergyStorage chargeStorage = new ChargeStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedChargeStorage = energyFlow.track(chargeStorage);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            PhaseArrayRecipe recipe = recipe();
            if (index >= DATA_HOLD_BASE) {
                int position = index - DATA_HOLD_BASE + 1;
                return position <= lineStats.coilCount()
                        ? (int) Math.round(lineStats.holdChance().get(position - 1) * ODDS_SCALE / 100.0)
                        : 0;
            }
            return switch (index) {
                case DATA_STATUS -> status(recipe);
                case DATA_FORMED -> line.complete() ? 1 : 0;
                case DATA_COIL_COUNT -> line.coilCount();
                case DATA_USABLE_COILS -> line.usableCoils();
                case DATA_ACTION -> action.ordinal();
                case DATA_SELECTION -> PhaseCradleMenu.selectionCode(selection);
                case DATA_DEFINITION -> definitionIndex;
                case DATA_DONOR_AFFIX -> donorAffixIndex;
                case DATA_CHARGE_LOW -> (int) charge;
                case DATA_CHARGE_HIGH -> (int) (charge >>> 32);
                case DATA_COST_LOW -> (int) cost(recipe);
                case DATA_COST_HIGH -> (int) (cost(recipe) >>> 32);
                case DATA_CHARGE_RATE -> lineStats.chargeRate();
                case DATA_LAST_INPUT -> energyFlow.lastInput();
                case DATA_LAST_OUTCOME -> lastOutcome == null ? NO_OUTCOME : lastOutcome.ordinal();
                case DATA_LAST_HELD -> lastHeld;
                case DATA_LAST_SLIPPED -> lastSlipped;
                case DATA_STRAIN -> PhaseStrain.get(targetStack());
                case DATA_RP_COST -> recipe == null ? 0 : recipe.refinementPotentialCost();
                case DATA_GLYPH_TIER -> recipe == null ? 0 : recipe.glyphTier();
                case DATA_REQUIRED_COILS -> recipe == null ? 0 : recipe.coils();
                case DATA_ODDS_SUCCESS -> thousandths(odds(recipe).success());
                case DATA_ODDS_OVERHOLD -> thousandths(odds(recipe).overhold());
                case DATA_ODDS_PARTIAL -> thousandths(odds(recipe).partial());
                case DATA_ODDS_MISS -> thousandths(odds(recipe).miss());
                case DATA_ODDS_FRACTURE -> thousandths(odds(recipe).fracture());
                case DATA_PARTIAL_WINDOW -> lineStats.partialWindow();
                case DATA_STRAIN_TOLERANCE -> lineStats.strainTolerance();
                case DATA_CHARGING -> charging ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private PhaseLine.Scan line = PhaseLine.Scan.none();
    private PhaseLineStats lineStats = PhaseLineStats.empty();
    private final Set<BlockPos> formedMembers = new HashSet<>();
    private boolean lineRescanRequested = true;
    private int lineRescanTimer;

    private long charge;
    private boolean charging;
    private long inputWindowTick = Long.MIN_VALUE;
    private int inputThisTick;
    private PhaseArrayAction action = PhaseArrayAction.REFORGE;
    private RefinementSelection selection = RefinementSelection.none();
    private int definitionIndex;
    private int donorAffixIndex;
    private PhasePulse.Outcome lastOutcome;
    private int lastHeld;
    private int lastSlipped;

    public PhaseCradleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.EXOTIC_AFFIX_FORGE.get(), pos, blockState);
    }

    // -- The line ------------------------------------------------------------------------------------------------------

    /** Asks for a line scan on the next tick, as the Wooden Dehumidifier does when a frame changes. */
    public void requestLineRescan() {
        lineRescanRequested = true;
    }

    public boolean isLineFormed() {
        return line.complete();
    }

    public int lineCoilCount() {
        return line.coilCount();
    }

    /** Coils, counted from the Emitter, whose inscribed tier meets their position; the operations a line can fire. */
    public int usableCoils() {
        return line.usableCoils();
    }

    public Direction lineAxis() {
        return line.axis();
    }

    public PhaseLine.Scan lineScan() {
        return line;
    }

    public PhaseLineStats lineStats() {
        return lineStats;
    }

    public boolean ownsMember(BlockPos pos) {
        return line.complete() && formedMembers.contains(pos);
    }

    /** The Cradle whose formed line includes {@code member}, for a Coil or Emitter forwarding a capability. */
    public static Optional<PhaseCradleBlockEntity> cradleForMember(Level level, BlockPos member) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int distance = 1; distance <= PhaseArrayRules.MAX_COILS + 1; distance++) {
                if (level.getBlockEntity(member.relative(direction, distance)) instanceof PhaseCradleBlockEntity cradle
                        && cradle.ownsMember(member)) {
                    return Optional.of(cradle);
                }
            }
        }
        return Optional.empty();
    }

    private void scanLine() {
        if (level == null) {
            return;
        }
        lineRescanRequested = false;
        PhaseLine.Scan scan = PhaseLine.scan(level, worldPosition);
        boolean changed = scan.complete() != line.complete()
                || scan.coilCount() != line.coilCount()
                || scan.usableCoils() != line.usableCoils();
        line = scan;
        lineStats = scan.complete() ? readLineStats(scan) : PhaseLineStats.empty();
        Set<BlockPos> next = scan.complete() ? scan.members() : Set.of();
        for (BlockPos member : Set.copyOf(formedMembers)) {
            if (!next.contains(member)) {
                PhaseLine.setFormed(level, member, false);
                formedMembers.remove(member);
            }
        }
        for (BlockPos member : next) {
            if (formedMembers.add(member)) {
                PhaseLine.setFormed(level, member, true);
            }
        }
        if (!scan.complete()) {
            charging = false;
        }
        if (changed) {
            setChanged();
        }
    }

    private PhaseLineStats readLineStats(PhaseLine.Scan scan) {
        MachineStatAccumulator emitter = level.getBlockEntity(scan.emitter()) instanceof PhaseEmitterBlockEntity emitterEntity
                ? emitterEntity.effectiveStats()
                : com.rngtech.rpg.MachineBaseStatCatalog.phaseEmitter();
        List<MachineStatAccumulator> coils = new ArrayList<>();
        for (BlockPos pos : scan.coilsFromEmitter()) {
            coils.add(level.getBlockEntity(pos) instanceof PhaseCoilBlockEntity coil
                    ? coil.effectiveStats()
                    : com.rngtech.rpg.MachineBaseStatCatalog.phaseCoil());
        }
        return PhaseLineStats.of(emitter, coils, scan.usableCoils());
    }

    /** Unlights every member; called when the Cradle is broken. */
    public void clearFormedMembers() {
        if (level == null) {
            return;
        }
        for (BlockPos member : Set.copyOf(formedMembers)) {
            PhaseLine.setFormed(level, member, false);
        }
        formedMembers.clear();
        line = PhaseLine.Scan.none();
        lineStats = PhaseLineStats.empty();
    }

    // -- Ticking and charge ----------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhaseCradleBlockEntity cradle) {
        if (cradle.lineRescanRequested || cradle.lineRescanTimer-- <= 0) {
            cradle.scanLine();
            cradle.lineRescanTimer = LINE_RESCAN_INTERVAL;
        }
        cradle.rollInputWindow(level.getGameTime());
        if (cradle.charging) {
            cradle.drainCells();
        }
    }

    private void rollInputWindow(long gameTime) {
        if (gameTime != inputWindowTick) {
            inputWindowTick = gameTime;
            inputThisTick = 0;
        }
    }

    /** FE the line still takes this tick: up to the pulse's cost, at the line's Charge Rate. */
    private int remainingInput() {
        if (!charging || !line.complete()) {
            return 0;
        }
        long toCost = cost(recipe()) - charge;
        long rate = lineStats.chargeRate() - inputThisTick;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, Math.min(toCost, rate)));
    }

    private void addCharge(int amount) {
        charge += amount;
        inputThisTick += amount;
        setChanged();
    }

    private void drainCells() {
        for (int slot = 0; slot < CELL_SLOTS && remainingInput() > 0; slot++) {
            ItemStack cell = cells.getStackInSlot(slot);
            IEnergyStorage storage = cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage == null) {
                continue;
            }
            int extracted = storage.extractEnergy(remainingInput(), false);
            if (extracted > 0) {
                addCharge(extracted);
                energyFlow.recordInput(extracted);
            }
        }
    }

    public long charge() {
        return charge;
    }

    public boolean isCharging() {
        return charging;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return trackedChargeStorage;
    }

    private final class ChargeStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int accepted = Math.min(toReceive, remainingInput());
            if (accepted > 0 && !simulate) {
                addCharge(accepted);
            }
            return Math.max(0, accepted);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, charge);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, Math.max(charge, lineStats.chargeCapacity()));
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return charging && line.complete();
        }
    }

    // -- Recipes, costs, and status ---------------------------------------------------------------------------------------

    public PhaseArrayRecipe recipe() {
        return recipeFor(action);
    }

    public PhaseArrayRecipe recipeFor(PhaseArrayAction wanted) {
        if (level == null) {
            return null;
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.PHASE_ARRAY_TYPE.get(), new PhaseArrayRecipeInput(targetStack(), wanted), level)
                .map(holder -> holder.value())
                .orElse(null);
    }

    /** The pulse's FE after the line's Thrift. */
    public long cost(PhaseArrayRecipe recipe) {
        if (recipe == null) {
            return 0L;
        }
        long base = recipe.feFor(targetStack());
        return Math.round(base * (1.0 - lineStats.feThrift() / 100.0));
    }

    private PhasePulse.Odds odds(PhaseArrayRecipe recipe) {
        if (recipe == null || !line.complete()) {
            return PhasePulse.Odds.NONE;
        }
        return PhasePulse.odds(lineStats, recipe.coils(), PhaseStrain.get(targetStack()));
    }

    private static int thousandths(double fraction) {
        return (int) Math.round(fraction * ODDS_SCALE);
    }

    public int status() {
        return status(recipe());
    }

    private int status(PhaseArrayRecipe recipe) {
        if (!line.complete()) {
            return STATUS_NO_LINE;
        }
        if (recipe == null) {
            return STATUS_NO_RECIPE;
        }
        ItemStack target = targetStack();
        if (target.isEmpty()) {
            return STATUS_NO_TARGET;
        }
        if (!RefinementTargets.canRefine(target) || isLocked(RefinementTargets.storedTraits(target))) {
            return STATUS_LOCKED;
        }
        if (line.usableCoils() < recipe.coils()) {
            return STATUS_NO_COILS;
        }
        if (!outputStack().isEmpty()) {
            return STATUS_OUTPUT_FULL;
        }
        if (!hasSelection(recipe.action())) {
            return STATUS_NO_SELECTION;
        }
        if (!operationLegal(recipe)) {
            return STATUS_ILLEGAL;
        }
        if (recipe.needsGlyph() && GlyphItem.tier(glyphStack()) < recipe.glyphTier()) {
            return STATUS_NO_GLYPH;
        }
        if (RefinementTargets.storedTraits(target).refinementPotential() < recipe.refinementPotentialCost()) {
            return STATUS_NO_RP;
        }
        if (charge >= cost(recipe)) {
            return STATUS_READY;
        }
        return charging ? STATUS_CHARGING : STATUS_IDLE;
    }

    private static boolean isLocked(MachineTraits traits) {
        return traits.isCorrupted() || traits.rarity() == Rarity.UNIQUE;
    }

    private boolean hasSelection(PhaseArrayAction wanted) {
        return switch (wanted.selection()) {
            case NONE -> true;
            case AFFIX -> selection.kind() == RefinementSelection.Kind.EXISTING_MODIFIER;
            case EMPTY_SLOT_AND_DEFINITION -> selection.kind() == RefinementSelection.Kind.EMPTY_SLOT
                    && definitionIndex < imprintCandidates().size();
            case EMPTY_SLOT_AND_DONOR_AFFIX -> selection.kind() == RefinementSelection.Kind.EMPTY_SLOT
                    && donorAffix() != null;
        };
    }

    /** Whether the selected operation can change the target at all, so a pulse is never fired at nothing. */
    private boolean operationLegal(PhaseArrayRecipe recipe) {
        ItemStack target = targetStack();
        MachineTraits traits = RefinementTargets.storedTraits(target);
        ModifierEligibilityProfile profile = RefinementTargets.eligibilityProfile(target);
        return switch (recipe.action()) {
            case REFORGE -> true;
            case ATTUNE -> traits.modifiers().stream().anyMatch(modifier -> modifier.slot().isAffix());
            case RESONATE -> RefinementEngine.phaseRaisedTier(profile, traits, selection.affixIndex(), 1,
                    MachineTraitRoller.maxUpgradeTier()) > 0;
            case EXCISE -> RefinementEngine.phaseRemove(profile, traits, selection.affixIndex(), RandomSource.create(0)) != null;
            case IMPRINT -> !imprintCandidates().isEmpty();
            case TRANSMUTE -> traits.modifiers().stream().anyMatch(modifier -> modifier.slot().isAffix());
            case TEMPER -> PhaseStrain.get(target) > 0;
            case EXALT -> RefinementTargets.componentStage(target) >= PhaseArrayRules.PART_STAGE
                    && RefinementEngine.phaseRaisedTier(profile, traits, selection.affixIndex(), 1,
                            MachineTraitRoller.maxUpgradeTier() + 1) == MachineTraitRoller.maxUpgradeTier() + 1;
            case GRAFT -> donorAffix() != null
                    && RefinementEngine.phaseCanGraft(profile, traits, donorAffix(), selection.emptySlot());
        };
    }

    /** The affixes Imprint can add to the selected empty slot, in profile order; the same list the screen shows. */
    public List<ModifierDefinition> imprintCandidates() {
        ItemStack target = targetStack();
        if (selection.kind() != RefinementSelection.Kind.EMPTY_SLOT || !RefinementTargets.canRefine(target)) {
            return List.of();
        }
        return RefinementEngine.phaseImprintCandidates(
                RefinementTargets.eligibilityProfile(target),
                RefinementTargets.storedTraits(target),
                selection.emptySlot()
        );
    }

    /** The donor affixes Graft can move, in the donor's affix order. */
    public static List<MachineModifier> donorAffixes(ItemStack donor) {
        if (!RefinementTargets.canRefine(donor)) {
            return List.of();
        }
        return RefinementTargets.storedTraits(donor).modifiers().stream()
                .filter(modifier -> modifier.slot().isAffix())
                .toList();
    }

    private MachineModifier donorAffix() {
        List<MachineModifier> affixes = donorAffixes(donorStack());
        return donorAffixIndex >= 0 && donorAffixIndex < affixes.size() ? affixes.get(donorAffixIndex) : null;
    }

    // -- Player actions --------------------------------------------------------------------------------------------------

    public void setAction(PhaseArrayAction wanted) {
        if (wanted != action) {
            action = wanted;
            charging = false;
            setChanged();
        }
    }

    public void setSelection(RefinementSelection wanted) {
        selection = wanted == null ? RefinementSelection.none() : wanted;
        definitionIndex = 0;
        setChanged();
    }

    public void setDefinitionIndex(int index) {
        definitionIndex = Math.max(0, index);
        setChanged();
    }

    public void setDonorAffixIndex(int index) {
        donorAffixIndex = Math.max(0, index);
        setChanged();
    }

    /** Charge starts a pulse's fill; pressed again it stops, keeping what has arrived. */
    public void toggleCharging() {
        int status = status();
        if (charging) {
            charging = false;
        } else if (status == STATUS_IDLE) {
            charging = true;
        }
        setChanged();
    }

    /** Fires the pulse when the charge is full. Phase 5 adds the hold sequence in front of this; here it is instant. */
    public boolean fire() {
        PhaseArrayRecipe recipe = recipe();
        if (level == null || status(recipe) != STATUS_READY) {
            return false;
        }
        ItemStack target = targetStack();
        RandomSource random = level.random;
        int strain = PhaseStrain.get(target);
        PhasePulse.Result result = PhasePulse.resolve(lineStats, recipe.coils(), strain, random);
        long pulseCost = cost(recipe);
        charge -= pulseCost;
        boolean slipped = result.outcome() == PhasePulse.Outcome.MISS || result.outcome() == PhasePulse.Outcome.PARTIAL;
        if (slipped) {
            charge += Math.round(pulseCost * lineStats.feRecovery() / 100.0);
        }
        boolean keepGlyph = result.outcome() == PhasePulse.Outcome.MISS
                && random.nextDouble() * 100.0 < lineStats.glyphSalvage();
        if (recipe.needsGlyph() && !keepGlyph) {
            glyphStack().shrink(1);
        }

        ItemStack refined = target.copy();
        MachineTraits traits = RefinementTargets.storedTraits(target);
        ModifierEligibilityProfile profile = RefinementTargets.eligibilityProfile(target);
        int stage = RefinementTargets.modifierRollComponentStage(target);
        int strainAfter = strain + (random.nextDouble() * 100.0 < lineStats.strainMercy() ? 0 : 1);
        Applied applied = switch (result.outcome()) {
            case FRACTURE -> new Applied(RefinementEngine.phaseFracture(profile, traits, random), false, null, strainAfter);
            case MISS -> new Applied(null, false, null, strainAfter);
            case SUCCESS, OVERHOLD, PARTIAL -> apply(recipe, result.outcome(), target, traits, profile, stage, strainAfter, random);
        };
        if (applied.traits() != null) {
            RefinementTargets.setTraits(refined, applied.traits());
            if (result.outcome() != PhasePulse.Outcome.FRACTURE) {
                int rp = applied.traits().refinementPotential() - recipe.refinementPotentialCost();
                RefinementTargets.setTraits(refined, withPotential(applied.traits(), Math.max(0, rp)));
            }
        }
        PhaseStrain.set(refined, applied.strain());
        if (BatteryCellItem.isBatteryCell(refined)) {
            BatteryCellItem.clampEnergy(refined, BatteryCellItem.energyCapacity(refined));
        }
        items.setStackInSlot(SLOT_OUTPUT, refined);
        items.setStackInSlot(SLOT_TARGET, ItemStack.EMPTY);
        if (applied.consumeDonor()) {
            items.setStackInSlot(SLOT_DONOR, ItemStack.EMPTY);
        } else if (applied.strippedDonor() != null) {
            ItemStack donor = donorStack().copy();
            RefinementTargets.setTraits(donor, applied.strippedDonor());
            items.setStackInSlot(SLOT_DONOR, donor);
        }
        lastOutcome = result.outcome();
        lastHeld = result.held();
        lastSlipped = result.slippedPosition();
        charging = false;
        playOutcome(result.outcome());
        setChanged();
        return true;
    }

    /** What a pulse did: the target's new traits (null for no change), what happens to the donor, and the Strain after. */
    private record Applied(MachineTraits traits, boolean consumeDonor, MachineTraits strippedDonor, int strain) {
    }

    private Applied apply(
            PhaseArrayRecipe recipe,
            PhasePulse.Outcome outcome,
            ItemStack target,
            MachineTraits traits,
            ModifierEligibilityProfile profile,
            int stage,
            int strainAfter,
            RandomSource random
    ) {
        boolean partial = outcome == PhasePulse.Outcome.PARTIAL;
        boolean overhold = outcome == PhasePulse.Outcome.OVERHOLD;
        PhaseOutcome.Partial partialKind = partial ? recipe.partial() : PhaseOutcome.Partial.NONE;
        PhaseOutcome.Overhold overholdKind = overhold ? recipe.overhold() : PhaseOutcome.Overhold.NONE;
        int index = selection.affixIndex();
        return switch (recipe.action()) {
            case REFORGE -> {
                if (partialKind == PhaseOutcome.Partial.REROLL_AFFIXES) {
                    yield new Applied(RefinementEngine.phaseRerollAffixes(profile, traits, stage, random), false, null, strainAfter);
                }
                RefinementResult first = RefinementEngine.reforgeTarget(target, random);
                MachineTraits rolled = first.traits();
                if (overholdKind == PhaseOutcome.Overhold.BEST_OF_TWO_RARITY) {
                    MachineTraits second = RefinementEngine.reforgeTarget(target, random).traits();
                    rolled = second.rarity().ordinal() > rolled.rarity().ordinal() ? second : rolled;
                }
                yield new Applied(first.success() ? rolled : null, false, null, strainAfter);
            }
            case ATTUNE -> changed(partialKind == PhaseOutcome.Partial.REROLL_ONE_VALUE
                    ? RefinementEngine.phaseRerollValue(profile, traits, -1, random)
                    : RefinementEngine.phaseRerollValues(profile, traits, overholdKind == PhaseOutcome.Overhold.BEST_OF_TWO_VALUES, random),
                    strainAfter);
            case RESONATE -> changed(partialKind == PhaseOutcome.Partial.REROLL_SELECTED_VALUE
                    ? RefinementEngine.phaseRerollValue(profile, traits, index, random)
                    : RefinementEngine.phaseRaiseTier(profile, traits, index, overholdKind == PhaseOutcome.Overhold.TWO_TIERS ? 2 : 1,
                            MachineTraitRoller.maxUpgradeTier(), random),
                    strainAfter);
            case EXCISE -> changed(RefinementEngine.phaseRemove(profile, traits,
                    partialKind == PhaseOutcome.Partial.REMOVE_RANDOM ? -1 : index, random),
                    overholdKind == PhaseOutcome.Overhold.REMOVE_STRAIN ? Math.max(0, strainAfter - 1) : strainAfter);
            case IMPRINT -> {
                List<ModifierDefinition> candidates = imprintCandidates();
                ModifierDefinition definition = candidates.get(Math.min(definitionIndex, candidates.size() - 1));
                yield changed(RefinementEngine.phaseAdd(profile, traits, selection.emptySlot(), definition, stage,
                        overholdKind == PhaseOutcome.Overhold.BEST_OF_TWO_TIERS,
                        partialKind == PhaseOutcome.Partial.ADD_RANDOM_OF_FAMILY, random), strainAfter);
            }
            case TRANSMUTE -> changed(RefinementEngine.phaseTransmute(profile, traits, index,
                    overholdKind == PhaseOutcome.Overhold.KEEP_VALUE_QUALITY && !partial, random), strainAfter);
            case TEMPER -> new Applied(traits, false, null,
                    partialKind == PhaseOutcome.Partial.HALVE_STRAIN ? strainAfter / 2 : 0);
            case EXALT -> changed(partialKind == PhaseOutcome.Partial.REROLL_SELECTED_VALUE
                    ? RefinementEngine.phaseRerollValue(profile, traits, index, random)
                    : RefinementEngine.phaseRaiseTier(profile, traits, index, 1, MachineTraitRoller.maxUpgradeTier() + 1, random),
                    strainAfter);
            case GRAFT -> {
                MachineModifier donor = donorAffix();
                MachineTraits grafted = RefinementEngine.phaseGraft(profile, traits, donor, selection.emptySlot(),
                        partialKind == PhaseOutcome.Partial.DONOR_TIER_DOWN, random);
                if (grafted == null) {
                    yield new Applied(null, false, null, strainAfter);
                }
                if (overholdKind == PhaseOutcome.Overhold.KEEP_DONOR) {
                    MachineTraits stripped = RefinementEngine.phaseRemove(
                            RefinementTargets.eligibilityProfile(donorStack()),
                            RefinementTargets.storedTraits(donorStack()), donorAffixIndex, random);
                    yield new Applied(grafted, stripped == null, stripped, strainAfter);
                }
                yield new Applied(grafted, true, null, strainAfter);
            }
        };
    }

    private static Applied changed(MachineTraits traits, int strain) {
        return new Applied(traits, false, null, strain);
    }

    private static MachineTraits withPotential(MachineTraits traits, int potential) {
        return new MachineTraits(traits.rarity(), potential, traits.modifiers(), traits.behaviors(), traits.corruption(), traits.retired());
    }

    private void playOutcome(PhasePulse.Outcome outcome) {
        if (level == null) {
            return;
        }
        switch (outcome) {
            case SUCCESS, OVERHOLD -> level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 1.2F);
            case PARTIAL -> level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.6F, 0.8F);
            case MISS -> level.playSound(null, worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.8F, 0.9F);
            case FRACTURE -> level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
    }

    // -- Inventories ---------------------------------------------------------------------------------------------------

    public ItemStackHandler getItems() {
        return items;
    }

    public ItemStackHandler getCells() {
        return cells;
    }

    public ItemStack targetStack() {
        return items.getStackInSlot(SLOT_TARGET);
    }

    public ItemStack glyphStack() {
        return items.getStackInSlot(SLOT_GLYPH);
    }

    public ItemStack donorStack() {
        return items.getStackInSlot(SLOT_DONOR);
    }

    public ItemStack outputStack() {
        return items.getStackInSlot(SLOT_OUTPUT);
    }

    public PhaseArrayAction action() {
        return action;
    }

    public RefinementSelection selection() {
        return selection;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public void dropInventory(Level level) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            dropSlot(level, items, slot);
        }
        for (int slot = 0; slot < cells.getSlots(); slot++) {
            dropSlot(level, cells, slot);
        }
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    // -- Menu and Jade -------------------------------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.exotic_affix_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PhaseCradleMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        PhaseArrayRecipe recipe = recipe();
        int status = status(recipe);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        int demand = status == STATUS_CHARGING ? lineStats.chargeRate() : 0;
        boolean anyCell = false;
        for (int slot = 0; slot < CELL_SLOTS; slot++) {
            anyCell |= BatteryCellItem.isBatteryCell(cells.getStackInSlot(slot));
        }
        return MachineInfoSnapshot.builder("phase_cradle")
                .state(
                        MachineInfoSnapshot.workState(status == STATUS_READY || status == STATUS_CHARGING, charging,
                                status == STATUS_NO_TARGET || status == STATUS_NO_GLYPH),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .energy(charge, cost(recipe), -demand)
                .energyTelemetry(energyFlow.lastInput(), energyFlow.lastOutput(), connector.transferRate(),
                        AdjacentEnergyConnector.inputBottleneck(connector, demand))
                .gear(anyCell ? MachineInfoSnapshot.GearSummary.BATTERY_CELL_INSTALLED : MachineInfoSnapshot.GearSummary.NONE)
                .build();
    }

    public static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_NO_LINE -> "no_line";
            case STATUS_NO_RECIPE -> "no_recipe";
            case STATUS_NO_TARGET -> "no_target";
            case STATUS_LOCKED -> "locked";
            case STATUS_NO_COILS -> "no_coils";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_NO_SELECTION -> "no_selection";
            case STATUS_ILLEGAL -> "illegal";
            case STATUS_NO_GLYPH -> "no_glyph";
            case STATUS_NO_RP -> "no_rp";
            case STATUS_CHARGING -> "charging";
            case STATUS_READY -> "ready";
            default -> "idle";
        };
        return "rngtech.phase_cradle.status." + name;
    }

    // -- Persistence ---------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.put("Cells", cells.serializeNBT(registries));
        tag.putLong("Charge", charge);
        tag.putBoolean("Charging", charging);
        tag.putString("Action", action.getSerializedName());
        tag.putString("SelectionKind", selection.kind().name());
        tag.putInt("SelectionAffix", selection.affixIndex());
        tag.putString("SelectionSlot", selection.emptySlot().name());
        tag.putInt("DefinitionIndex", definitionIndex);
        tag.putInt("DonorAffixIndex", donorAffixIndex);
        tag.putInt("LastOutcome", lastOutcome == null ? NO_OUTCOME : lastOutcome.ordinal());
        tag.putInt("LastHeld", lastHeld);
        tag.putInt("LastSlipped", lastSlipped);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lineRescanRequested = true;
        if (tag.contains("Items")) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
        }
        if (tag.contains("Cells")) {
            cells.deserializeNBT(registries, tag.getCompound("Cells"));
        }
        charge = Math.max(0L, tag.getLong("Charge"));
        charging = tag.getBoolean("Charging");
        action = actionByName(tag.getString("Action"));
        selection = selectionFromTag(tag);
        definitionIndex = Math.max(0, tag.getInt("DefinitionIndex"));
        donorAffixIndex = Math.max(0, tag.getInt("DonorAffixIndex"));
        int outcome = tag.contains("LastOutcome") ? tag.getInt("LastOutcome") : NO_OUTCOME;
        lastOutcome = outcome >= 0 && outcome < PhasePulse.Outcome.values().length ? PhasePulse.Outcome.values()[outcome] : null;
        lastHeld = tag.getInt("LastHeld");
        lastSlipped = tag.getInt("LastSlipped");
    }

    private static PhaseArrayAction actionByName(String name) {
        for (PhaseArrayAction candidate : PhaseArrayAction.values()) {
            if (candidate.getSerializedName().equals(name)) {
                return candidate;
            }
        }
        return PhaseArrayAction.REFORGE;
    }

    private static RefinementSelection selectionFromTag(CompoundTag tag) {
        String kind = tag.getString("SelectionKind");
        if (RefinementSelection.Kind.EXISTING_MODIFIER.name().equals(kind)) {
            return RefinementSelection.existingModifier(tag.getInt("SelectionAffix"));
        }
        if (RefinementSelection.Kind.EMPTY_SLOT.name().equals(kind)) {
            ModifierSlot slot = ModifierSlot.SUFFIX.name().equals(tag.getString("SelectionSlot")) ? ModifierSlot.SUFFIX : ModifierSlot.PREFIX;
            return RefinementSelection.emptySlot(slot);
        }
        return RefinementSelection.none();
    }
}
