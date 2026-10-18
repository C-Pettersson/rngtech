package com.rngtech.content.blockentity;

import com.rngtech.content.registry.ModFluids;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.BooleanSupplier;

/**
 * A heated machine's atmosphere tank. With Blast Draft, a tank holding Nitrogen gives 15% increased Overheat Tolerance,
 * and a lane uses Nitrogen only while it works above the overheat temperature it would have without it. A Metal Press
 * with an annealing ascendancy node may fill it with Hydrogen instead, one gas at a time.
 */
public final class InertAtmosphere {
    public static final int CAPACITY = 4000;
    public static final int USE_PER_LANE_TICK = 1;
    /** Added to Overheat Tolerance, so the overheat temperature rises by this share of the safe maximum. */
    public static final double OVERHEAT_BONUS = 0.15;
    /** Hydrogen one annealed item costs. */
    public static final int HYDROGEN_PER_ANNEAL = 250;
    private static final String TAG = "NitrogenTank";

    private final FluidTank tank;
    private final BooleanSupplier accepts;
    private final BooleanSupplier acceptsHydrogen;
    private final IFluidHandler inlet;
    private int usedThisTick;
    private int lastUse;

    /** {@code accepts} turns the inlet off on machines that cannot overheat, such as the Stage 0 Furnace. */
    public InertAtmosphere(Runnable onChanged, BooleanSupplier accepts) {
        this(onChanged, accepts, () -> false);
    }

    /** {@code acceptsHydrogen} lets the inlet take Hydrogen, for a Metal Press with an annealing node. */
    public InertAtmosphere(Runnable onChanged, BooleanSupplier accepts, BooleanSupplier acceptsHydrogen) {
        this.accepts = accepts;
        this.acceptsHydrogen = acceptsHydrogen;
        this.tank = new FluidTank(CAPACITY) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.is(ModFluids.NITROGEN_SOURCE.get())
                        || stack.is(ModFluids.HYDROGEN_SOURCE.get()) && InertAtmosphere.this.acceptsHydrogen.getAsBoolean();
            }

            @Override
            protected void onContentsChanged() {
                onChanged.run();
            }
        };
        this.inlet = new Inlet();
    }

    public IFluidHandler inlet() {
        return inlet;
    }

    public int amount() {
        return tank.getFluidAmount();
    }

    /** Whether the tank holds enough Nitrogen to feed a lane this tick. */
    public boolean available() {
        return tank.getFluid().is(ModFluids.NITROGEN_SOURCE.get()) && tank.getFluidAmount() >= USE_PER_LANE_TICK;
    }

    public boolean holdsHydrogen() {
        return tank.getFluid().is(ModFluids.HYDROGEN_SOURCE.get());
    }

    /** Whether the tank can anneal {@code items} items now. */
    public boolean canAnneal(int items) {
        return holdsHydrogen() && tank.getFluidAmount() >= HYDROGEN_PER_ANNEAL * Math.max(1, items);
    }

    /** Spends the Hydrogen for {@code items} annealed items. */
    public void anneal(int items) {
        usedThisTick += tank.drain(HYDROGEN_PER_ANNEAL * Math.max(1, items), IFluidHandler.FluidAction.EXECUTE).getAmount();
    }

    /** The Overheat Tolerance the atmosphere adds: only with Blast Draft and Nitrogen in the tank. */
    public double overheatBonus(boolean blastDraft) {
        return blastDraft && available() ? OVERHEAT_BONUS : 0.0;
    }

    /** Spends one lane's Nitrogen for this tick when it works hotter than its overheat temperature without Nitrogen. */
    public void use(double workTemperature, int overheatWithout, double bonus) {
        if (bonus > 0.0 && workTemperature > overheatWithout) {
            usedThisTick += tank.drain(USE_PER_LANE_TICK, IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
    }

    /** Closes the previous tick's use count. Call once at the start of every machine tick. */
    public void beginTick() {
        lastUse = usedThisTick;
        usedThisTick = 0;
    }

    /** Gas the machine used on the last full tick, for display. */
    public int lastUse() {
        return lastUse;
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tank.isEmpty()) {
            tag.put(TAG, tank.writeToNBT(registries, new CompoundTag()));
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        tank.setFluid(FluidStack.EMPTY);
        if (tag.contains(TAG)) {
            tank.readFromNBT(registries, tag.getCompound(TAG));
        }
    }

    /** Gas goes in from any side; pumping it out empties the tank, for example to switch gases. */
    private final class Inlet implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return accepts.getAsBoolean() && tank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return accepts.getAsBoolean() ? tank.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, tank.getFluid()) ? FluidStack.EMPTY : tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.drain(maxDrain, action);
        }
    }
}
