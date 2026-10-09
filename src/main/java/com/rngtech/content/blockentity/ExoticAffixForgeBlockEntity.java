package com.rngtech.content.blockentity;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.menu.ExoticAffixForgeMenu;
import com.rngtech.content.recipe.ExoticAffixForgeRecipe;
import com.rngtech.content.recipe.ExoticAffixForgeRecipeInput;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.ExoticAffixForgeAction;
import com.rngtech.rpg.refinement.ExoticAffixForgeHistory;
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

public class ExoticAffixForgeBlockEntity extends BlockEntity implements MenuProvider, MachineInfoProvider {
    public static final int SLOT_TARGET = 0;
    public static final int SLOT_CATALYST = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int PROCESS_SLOT_COUNT = 3;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int GEAR_SLOT_COUNT = 1;

    public static final int STATUS_READY = 0;
    public static final int STATUS_NO_TARGET = 1;
    public static final int STATUS_INVALID_TARGET = 2;
    public static final int STATUS_UNIQUE_TARGET = 3;
    public static final int STATUS_MISSING_CATALYST = 4;
    public static final int STATUS_INVALID_RECIPE = 5;
    public static final int STATUS_INSUFFICIENT_CATALYST = 6;
    public static final int STATUS_MISSING_SELECTION = 7;
    public static final int STATUS_INSUFFICIENT_POTENTIAL = 8;
    public static final int STATUS_ILLEGAL_OPERATION = 9;
    public static final int STATUS_NO_POWER = 10;
    public static final int STATUS_WORKING = 11;
    public static final int STATUS_OUTPUT_FULL = 12;
    public static final int STATUS_FAILED = 13;

    private static final int INTERNAL_ENERGY_CAPACITY = 2_000_000;
    private static final int POWER_FAILURE_THRESHOLD = 100;
    private static final int POWER_FAILURE_PER_TICK = 1;
    private static final int POWER_FAILURE_RECOVERY_PER_TICK = 1;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_STATUS = 4;
    private static final int DATA_EFFECTIVE_ENERGY_LOW = 5;
    private static final int DATA_EFFECTIVE_ENERGY_HIGH = 6;
    private static final int DATA_REQUIRED_CATALYSTS = 7;
    private static final int DATA_TOTAL_USES = 8;
    private static final int DATA_ACTION_USES = 9;
    private static final int DATA_REFINEMENT_POTENTIAL_COST = 10;
    private static final int DATA_POWER_FAILURE = 11;
    private static final int DATA_CRAFT_ACTIVE = 12;
    private static final int DATA_CRAFT_FAILED = 13;
    private static final int DATA_SELECTED_ACTION = 14;
    private static final int DATA_SELECTION = 15;
    private static final int DATA_COUNT = 16;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_TARGET -> RefinementTargets.canRefine(stack);
                case SLOT_CATALYST -> true;
                case SLOT_OUTPUT -> false;
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_TARGET || slot == SLOT_OUTPUT ? 1 : super.getSlotLimit(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_TARGET) {
                resetCycle();
            }
            setChanged();
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_BATTERY_CELL && BatteryCellItem.isBatteryCell(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IEnergyStorage energyStorage = new ForgeEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            ExoticAffixForgeRecipe recipe = nextRecipe();
            long energy = recipe == null ? 0L : effectiveEnergyCost(recipe);
            ExoticAffixForgeHistory history = ExoticAffixForgeHistory.get(targetStack());
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> recipe == null ? 0 : processingTicks(recipe);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_STATUS -> statusCode(recipe);
                case DATA_EFFECTIVE_ENERGY_LOW -> (int) (energy & 0xFFFFFFFFL);
                case DATA_EFFECTIVE_ENERGY_HIGH -> (int) (energy >>> 32);
                case DATA_REQUIRED_CATALYSTS -> recipe == null ? 0 : effectiveCatalystCount(recipe);
                case DATA_TOTAL_USES -> history.totalUses();
                case DATA_ACTION_USES -> history.uses(selectedAction);
                case DATA_REFINEMENT_POTENTIAL_COST -> recipe == null ? 0 : recipe.refinementPotentialCost();
                case DATA_POWER_FAILURE -> powerFailure;
                case DATA_CRAFT_ACTIVE -> craftActive ? 1 : 0;
                case DATA_CRAFT_FAILED -> craftFailed ? 1 : 0;
                case DATA_SELECTED_ACTION -> selectedAction.ordinal();
                case DATA_SELECTION -> ExoticAffixForgeMenu.selectionCode(selectedRefinement);
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

    private int progress;
    private int internalEnergy;
    private int powerFailure;
    private boolean craftActive;
    private boolean craftFailed;
    private ExoticAffixForgeAction selectedAction = ExoticAffixForgeAction.REFINE_ALL;
    private RefinementSelection selectedRefinement = RefinementSelection.none();

    public ExoticAffixForgeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.EXOTIC_AFFIX_FORGE.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ExoticAffixForgeBlockEntity forge) {
        if (!forge.craftActive) {
            return;
        }

        ExoticAffixForgeRecipe recipe = forge.nextRecipe();
        if (!forge.canProcess(recipe)) {
            forge.resetCycle();
            return;
        }

        int energyCost = forge.energyCostPerTick(recipe);
        if (forge.consumeWorkingEnergy(energyCost, true) < energyCost) {
            forge.registerPowerFailure();
            return;
        }

        forge.consumeWorkingEnergy(energyCost, false);
        forge.recoverPowerFailure();
        forge.progress++;
        if (forge.progress >= forge.processingTicks(recipe)) {
            forge.process(recipe);
        }
        forge.setChanged();
    }

    public ItemStackHandler getProcessInventory() {
        return processInventory;
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return trackedEnergyStorage;
    }

    public ExoticAffixForgeAction selectedAction() {
        return selectedAction;
    }

    public RefinementSelection selectedRefinement() {
        return selectedRefinement;
    }

    public boolean requestCraft() {
        if (craftActive) {
            return false;
        }

        ExoticAffixForgeRecipe recipe = nextRecipe();
        if (!canProcess(recipe)) {
            return false;
        }

        craftFailed = false;
        craftActive = true;
        progress = 0;
        powerFailure = 0;
        setChanged();
        return true;
    }

    public void setSelectedAction(ExoticAffixForgeAction action) {
        if (selectedAction != action) {
            selectedAction = action;
            resetCycle();
            setChanged();
        }
    }

    public void setSelectedRefinement(RefinementSelection selection) {
        if (!selectedRefinement.equals(selection)) {
            selectedRefinement = selection;
            resetCycle();
            setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.exotic_affix_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ExoticAffixForgeMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }

    public void dropInventory(Level level) {
        for (int slot = 0; slot < processInventory.getSlots(); slot++) {
            dropSlot(level, processInventory, slot);
        }
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            dropSlot(level, gearInventory, slot);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("Energy", internalEnergyStored());
        tag.putInt("PowerFailure", powerFailure);
        tag.putBoolean("CraftActive", craftActive);
        tag.putBoolean("CraftFailed", craftFailed);
        tag.putString("SelectedAction", selectedAction.getSerializedName());
        tag.putString("SelectedSelectionKind", selectedRefinement.kind().name());
        tag.putInt("SelectedAffixIndex", selectedRefinement.affixIndex());
        tag.putString("SelectedEmptySlot", selectedRefinement.emptySlot().name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ProcessInventory")) {
            CompoundTag inventoryTag = tag.getCompound("ProcessInventory");
            if (inventoryTag.getInt("Size") < PROCESS_SLOT_COUNT) {
                inventoryTag = inventoryTag.copy();
                inventoryTag.putInt("Size", PROCESS_SLOT_COUNT);
            }
            processInventory.deserializeNBT(registries, inventoryTag);
        }
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        progress = tag.getInt("Progress");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        powerFailure = Math.max(0, Math.min(POWER_FAILURE_THRESHOLD, tag.getInt("PowerFailure")));
        craftActive = tag.getBoolean("CraftActive");
        craftFailed = tag.getBoolean("CraftFailed");
        selectedAction = actionByName(tag.getString("SelectedAction"));
        selectedRefinement = selectionFromTag(tag);
        clampInternalEnergy();
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        ExoticAffixForgeRecipe recipe = nextRecipe();
        int status = statusCode(recipe);
        int energyDemand = status == STATUS_WORKING || status == STATUS_NO_POWER ? energyCostPerTick(recipe) : 0;
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("exotic_affix_forge")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_WORKING || status == STATUS_READY,
                                craftActive,
                                status == STATUS_NO_TARGET || status == STATUS_MISSING_CATALYST
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .progress(progress, recipe == null ? 0 : processingTicks(recipe))
                .energy(energyStored(), energyCapacity(), -energyDemand)
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        AdjacentEnergyConnector.inputBottleneck(connector, energyDemand)
                )
                .gear(BatteryCellItem.isBatteryCell(batteryCellStack())
                        ? MachineInfoSnapshot.GearSummary.BATTERY_CELL_INSTALLED
                        : MachineInfoSnapshot.GearSummary.NONE)
                .output(status == STATUS_OUTPUT_FULL
                        ? MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .build();
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_READY -> "ready";
            case STATUS_NO_TARGET -> "no_target";
            case STATUS_INVALID_TARGET -> "invalid_target";
            case STATUS_UNIQUE_TARGET -> "unique";
            case STATUS_MISSING_CATALYST -> "missing_catalyst";
            case STATUS_INVALID_RECIPE -> "invalid_recipe";
            case STATUS_INSUFFICIENT_CATALYST -> "insufficient_catalyst";
            case STATUS_MISSING_SELECTION -> "missing_selection";
            case STATUS_INSUFFICIENT_POTENTIAL -> "insufficient_potential";
            case STATUS_ILLEGAL_OPERATION -> "illegal_operation";
            case STATUS_NO_POWER -> "no_power";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_WORKING -> "working";
            case STATUS_FAILED -> "failed";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.exotic_affix_forge.status." + name;
    }

    private ExoticAffixForgeRecipe nextRecipe() {
        return level == null
                ? null
                : level.getRecipeManager()
                        .getRecipeFor(
                                ModRecipes.EXOTIC_AFFIX_FORGE_TYPE.get(),
                                new ExoticAffixForgeRecipeInput(targetStack(), catalystStack(), selectedAction),
                                level
                        )
                        .map(holder -> holder.value())
                        .orElse(null);
    }

    private boolean canProcess(ExoticAffixForgeRecipe recipe) {
        return blockingStatus(recipe) == STATUS_READY;
    }

    private int statusCode(ExoticAffixForgeRecipe recipe) {
        if (craftFailed) {
            return STATUS_FAILED;
        }
        int status = blockingStatus(recipe);
        if (status != STATUS_READY) {
            return status;
        }
        if (craftActive) {
            int energyCost = energyCostPerTick(recipe);
            if (consumeWorkingEnergy(energyCost, true) >= energyCost) {
                return STATUS_WORKING;
            }
            return STATUS_NO_POWER;
        }
        return STATUS_READY;
    }

    private int blockingStatus(ExoticAffixForgeRecipe recipe) {
        ItemStack target = targetStack();
        if (target.isEmpty()) {
            return STATUS_NO_TARGET;
        }
        if (!RefinementTargets.canRefine(target)) {
            return STATUS_INVALID_TARGET;
        }
        MachineTraits traits = RefinementTargets.storedTraits(target);
        if (traits.rarity() == Rarity.UNIQUE) {
            return STATUS_UNIQUE_TARGET;
        }
        if (catalystStack().isEmpty()) {
            return STATUS_MISSING_CATALYST;
        }
        if (recipe == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (catalystStack().getCount() < effectiveCatalystCount(recipe)) {
            return STATUS_INSUFFICIENT_CATALYST;
        }
        if (!outputStack().isEmpty()) {
            return STATUS_OUTPUT_FULL;
        }
        return operationStatus(recipe, traits);
    }

    private int operationStatus(ExoticAffixForgeRecipe recipe, MachineTraits traits) {
        ModifierEligibilityProfile profile = RefinementTargets.eligibilityProfile(targetStack());
        if (traits.refinementPotential() < recipe.refinementPotentialCost()) {
            return STATUS_INSUFFICIENT_POTENTIAL;
        }
        return switch (recipe.action()) {
            case REFORGE -> STATUS_READY;
            case REFINE_ALL -> RefinementEngine.hasRetunableAffix(profile, traits)
                    ? STATUS_READY
                    : STATUS_ILLEGAL_OPERATION;
            case UPGRADE_RANDOM_MODIFIER -> upgradeRandomStatus(profile, traits);
            case REFINE_SELECTED_MODIFIER -> selectedRefinement.kind() != RefinementSelection.Kind.EXISTING_MODIFIER
                    ? STATUS_MISSING_SELECTION
                    : RefinementEngine.hasSelectedRetunableAffix(profile, traits, selectedRefinement)
                            ? STATUS_READY
                            : STATUS_ILLEGAL_OPERATION;
            case ADD_MODIFIER -> selectedRefinement.kind() != RefinementSelection.Kind.EMPTY_SLOT
                    ? STATUS_MISSING_SELECTION
                    : addModifierStatus(profile, traits);
            case REMOVE_SELECTED_MODIFIER -> selectedRefinement.kind() != RefinementSelection.Kind.EXISTING_MODIFIER
                    ? STATUS_MISSING_SELECTION
                    : RefinementEngine.hasSelectedRemovableAffix(profile, traits, selectedRefinement)
                            ? STATUS_READY
                            : STATUS_ILLEGAL_OPERATION;
        };
    }

    private int upgradeRandomStatus(ModifierEligibilityProfile profile, MachineTraits traits) {
        if (RefinementEngine.hasUpgradeableAffix(profile, traits, true)) {
            return STATUS_READY;
        }
        return RefinementEngine.hasUpgradeableAffix(profile, traits, false)
                ? STATUS_INSUFFICIENT_POTENTIAL
                : STATUS_ILLEGAL_OPERATION;
    }

    private int addModifierStatus(ModifierEligibilityProfile profile, MachineTraits traits) {
        if (RefinementEngine.hasOpenSelectedAffixSlot(
                profile,
                traits,
                selectedRefinement.emptySlot(),
                RefinementTargets.modifierRollComponentStage(targetStack())
        )) {
            return STATUS_READY;
        }
        return traits.refinementPotential() <= 0 ? STATUS_INSUFFICIENT_POTENTIAL : STATUS_ILLEGAL_OPERATION;
    }

    private boolean process(ExoticAffixForgeRecipe recipe) {
        if (!canProcess(recipe)) {
            return false;
        }
        RefinementResult result = operationResult(recipe);
        if (!result.success()) {
            failCycle(false);
            return false;
        }

        ItemStack refinedTarget = targetStack().copy();
        RefinementTargets.setTraits(refinedTarget, result.traits());
        if (refinedTarget.getItem() instanceof BatteryCellItem) {
            BatteryCellItem.clampEnergy(refinedTarget, BatteryCellItem.energyCapacity(refinedTarget));
        }
        refinedTarget.set(
                ModDataComponents.EXOTIC_AFFIX_FORGE_HISTORY.get(),
                ExoticAffixForgeHistory.get(refinedTarget).increment(recipe.action())
        );

        ItemStack catalyst = catalystStack().copy();
        catalyst.shrink(effectiveCatalystCount(recipe));
        processInventory.setStackInSlot(SLOT_OUTPUT, refinedTarget);
        processInventory.setStackInSlot(SLOT_TARGET, ItemStack.EMPTY);
        processInventory.setStackInSlot(SLOT_CATALYST, catalyst);
        resetCycle();
        setChanged();
        return true;
    }

    private RefinementResult operationResult(ExoticAffixForgeRecipe recipe) {
        ItemStack target = targetStack();
        ModifierEligibilityProfile profile = RefinementTargets.eligibilityProfile(target);
        MachineTraits traits = RefinementTargets.storedTraits(target);
        return switch (recipe.action()) {
            case REFORGE -> applyExtraCost(
                    RefinementEngine.reforgeTarget(target, level.random),
                    recipe.refinementPotentialCost()
            );
            case REFINE_ALL -> RefinementEngine.refineAllSameTier(
                    profile,
                    traits,
                    recipe.refinementPotentialCost(),
                    level.random
            );
            case UPGRADE_RANDOM_MODIFIER -> applyExtraCost(
                    RefinementEngine.upgradeRandomModifier(
                            profile,
                            traits,
                            RefinementTargets.modifierRollComponentStage(target),
                            level.random
                    ),
                    recipe.refinementPotentialCost()
            );
            case REFINE_SELECTED_MODIFIER -> RefinementEngine.refineSelectedSameTier(
                    profile,
                    traits,
                    selectedRefinement,
                    recipe.refinementPotentialCost(),
                    level.random
            );
            case ADD_MODIFIER -> applyExtraCost(
                    RefinementEngine.addSelectedModifier(
                            profile,
                            traits,
                            RefinementTargets.modifierRollComponentStage(target),
                            selectedRefinement,
                            level.random
                    ),
                    recipe.refinementPotentialCost()
            );
            case REMOVE_SELECTED_MODIFIER -> RefinementEngine.removeSelectedModifier(
                    profile,
                    traits,
                    selectedRefinement,
                    recipe.refinementPotentialCost()
            );
        };
    }

    private RefinementResult applyExtraCost(RefinementResult result, int refinementPotentialCost) {
        if (!result.success() || refinementPotentialCost <= 0) {
            return result;
        }
        MachineTraits traits = result.traits();
        MachineTraits adjusted = new MachineTraits(
                traits.rarity(),
                Math.max(0, traits.refinementPotential() - refinementPotentialCost),
                traits.modifiers(),
                traits.behaviors()
        );
        return RefinementResult.success(
                adjusted,
                result.consumedPotential() + refinementPotentialCost,
                result.messageKey(),
                false,
                false
        );
    }

    private long effectiveEnergyCost(ExoticAffixForgeRecipe recipe) {
        ExoticAffixForgeHistory history = ExoticAffixForgeHistory.get(targetStack());
        double scale = costScale(recipe, history);
        double configured = recipe.energy() * scale * RNGTechConfig.EXOTIC_AFFIX_FORGE_ENERGY_MULTIPLIER.get();
        if (!Double.isFinite(configured) || configured >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (long) Math.ceil(configured));
    }

    private int effectiveCatalystCount(ExoticAffixForgeRecipe recipe) {
        ExoticAffixForgeHistory history = ExoticAffixForgeHistory.get(targetStack());
        int scaled = (int) Math.ceil(recipe.catalystCount() * costScale(recipe, history));
        return Math.max(1, Math.min(recipe.maxCatalystCount(), scaled));
    }

    private double costScale(ExoticAffixForgeRecipe recipe, ExoticAffixForgeHistory history) {
        return 1.0D
                + history.totalUses() * recipe.totalUseEnergyMultiplier()
                + history.uses(recipe.action()) * recipe.actionUseEnergyMultiplier();
    }

    private int processingTicks(ExoticAffixForgeRecipe recipe) {
        return Math.max(
                1,
                (int) Math.ceil(recipe.processingTicks() * RNGTechConfig.EXOTIC_AFFIX_FORGE_TIME_MULTIPLIER.get())
        );
    }

    private int energyCostPerTick(ExoticAffixForgeRecipe recipe) {
        long energy = effectiveEnergyCost(recipe);
        int ticks = processingTicks(recipe);
        long perTick = Math.max(1L, (long) Math.ceil(energy / (double) ticks));
        return perTick >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) perTick;
    }

    private int energyStored() {
        return internalEnergyStored() + BatteryCellItem.energyStored(batteryCellStack());
    }

    private int energyCapacity() {
        return internalEnergyCapacity() + BatteryCellItem.energyCapacity(batteryCellStack());
    }

    private int internalEnergyCapacity() {
        return INTERNAL_ENERGY_CAPACITY;
    }

    private int internalEnergyStored() {
        return Math.max(0, Math.min(internalEnergy, internalEnergyCapacity()));
    }

    private int receiveInternalEnergy(int toReceive, boolean simulate) {
        if (toReceive <= 0) {
            return 0;
        }
        int received = Math.min(toReceive, internalEnergyCapacity() - internalEnergyStored());
        if (!simulate && received > 0) {
            internalEnergy = internalEnergyStored() + received;
            setChanged();
        }
        return received;
    }

    private int consumeWorkingEnergy(int toConsume, boolean simulate) {
        if (toConsume <= 0) {
            return 0;
        }

        int internal = Math.min(toConsume, internalEnergyStored());
        int remaining = toConsume - internal;
        IEnergyStorage cell = batteryCellEnergyStorage();
        int cellExtracted = 0;
        if (remaining > 0 && cell != null && cell.canExtract()) {
            cellExtracted = cell.extractEnergy(remaining, true);
        }

        int consumed = internal + cellExtracted;
        if (simulate || consumed < toConsume) {
            return consumed;
        }

        if (internal > 0) {
            internalEnergy = internalEnergyStored() - internal;
        }
        if (cellExtracted > 0) {
            cell.extractEnergy(cellExtracted, false);
        }
        if (consumed > 0) {
            setChanged();
        }
        return consumed;
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        ItemStack cell = batteryCellStack();
        return cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private ItemStack targetStack() {
        return processInventory.getStackInSlot(SLOT_TARGET);
    }

    private ItemStack catalystStack() {
        return processInventory.getStackInSlot(SLOT_CATALYST);
    }

    private ItemStack outputStack() {
        return processInventory.getStackInSlot(SLOT_OUTPUT);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private void registerPowerFailure() {
        powerFailure = Math.min(POWER_FAILURE_THRESHOLD, powerFailure + POWER_FAILURE_PER_TICK);
        if (powerFailure >= POWER_FAILURE_THRESHOLD) {
            failCycle(true);
            return;
        }
        setChanged();
    }

    private void recoverPowerFailure() {
        powerFailure = Math.max(0, powerFailure - POWER_FAILURE_RECOVERY_PER_TICK);
    }

    private void resetCycle() {
        progress = 0;
        powerFailure = 0;
        craftActive = false;
        craftFailed = false;
    }

    private void failCycle(boolean audible) {
        progress = 0;
        powerFailure = POWER_FAILURE_THRESHOLD;
        craftActive = false;
        craftFailed = true;
        if (audible && level != null) {
            level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 0.45F);
        }
        setChanged();
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private static ExoticAffixForgeAction actionByName(String name) {
        for (ExoticAffixForgeAction action : ExoticAffixForgeAction.values()) {
            if (action.getSerializedName().equals(name)) {
                return action;
            }
        }
        return ExoticAffixForgeAction.REFINE_ALL;
    }

    private static RefinementSelection selectionFromTag(CompoundTag tag) {
        String kind = tag.getString("SelectedSelectionKind");
        if (RefinementSelection.Kind.EXISTING_MODIFIER.name().equals(kind)) {
            return RefinementSelection.existingModifier(tag.getInt("SelectedAffixIndex"));
        }
        if (RefinementSelection.Kind.EMPTY_SLOT.name().equals(kind)) {
            try {
                return RefinementSelection.emptySlot(ModifierSlot.valueOf(tag.getString("SelectedEmptySlot")));
            } catch (IllegalArgumentException ignored) {
                return RefinementSelection.none();
            }
        }
        return RefinementSelection.none();
    }

    private final class ForgeEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (!canReceive() || toReceive <= 0) {
                return 0;
            }
            int remaining = toReceive;
            int received = receiveInternalEnergy(remaining, simulate);
            remaining -= received;
            IEnergyStorage cell = batteryCellEnergyStorage();
            if (cell != null && cell.canReceive() && remaining > 0) {
                int cellReceived = cell.receiveEnergy(remaining, simulate);
                received += cellReceived;
                if (!simulate && cellReceived > 0) {
                    setChanged();
                }
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            if (internalEnergyStored() < internalEnergyCapacity()) {
                return true;
            }
            IEnergyStorage cell = batteryCellEnergyStorage();
            return cell != null && cell.canReceive() && cell.getEnergyStored() < cell.getMaxEnergyStored();
        }
    }
}
