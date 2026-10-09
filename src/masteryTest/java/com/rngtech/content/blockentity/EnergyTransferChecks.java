package com.rngtech.content.blockentity;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.util.TickTransferCounter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Executable checks for per-tick FE caps, connector bottleneck readouts and dead Energy Transfer rolls. */
public final class EnergyTransferChecks {
    private static int checks;

    private EnergyTransferChecks() {
    }

    public static void run() {
        capIsSharedWithinTick();
        capResetsNextTick();
        lastTickIsOnlyThePreviousTick();
        bottlenecksNameTheConnector();
        uncappedMachinesDoNotRollEnergyTransfer();
        System.out.println("Energy transfer: " + checks + " checks passed");
    }

    private static void capIsSharedWithinTick() {
        TickTransferCounter counter = new TickTransferCounter();
        require(counter.remaining(10, 64) == 64, "a fresh tick has the full cap");
        counter.add(10, 40);
        require(counter.remaining(10, 64) == 24, "a second source in the same tick sees what is left");
        counter.add(10, 40);
        require(counter.remaining(10, 64) == 0, "an over-delivered tick has nothing left, never a negative budget");
    }

    private static void capResetsNextTick() {
        TickTransferCounter counter = new TickTransferCounter();
        counter.add(10, 64);
        require(counter.remaining(11, 64) == 64, "the next tick starts with the full cap");
    }

    private static void lastTickIsOnlyThePreviousTick() {
        TickTransferCounter counter = new TickTransferCounter();
        counter.add(10, 30);
        counter.add(10, 12);
        require(counter.lastTick(11) == 42, "the readout is the previous tick's total");
        require(counter.lastTick(13) == 0, "an idle gap reads as zero, not a stale total");
    }

    private static void bottlenecksNameTheConnector() {
        AdjacentEnergyConnector.Info crude = new AdjacentEnergyConnector.Info(64, 0, 0);
        require(
                AdjacentEnergyConnector.inputBottleneck(crude, 65) == MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_INPUT,
                "demand above the sink connector reports the connector"
        );
        require(
                AdjacentEnergyConnector.inputBottleneck(crude, 64) == MachineInfoSnapshot.EnergyBottleneck.NONE,
                "demand the connector carries is not a bottleneck"
        );
        require(
                AdjacentEnergyConnector.outputBottleneck(crude, 200) == MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_OUTPUT,
                "generation above the source connector reports the connector"
        );
        require(
                AdjacentEnergyConnector.inputBottleneck(AdjacentEnergyConnector.Info.NONE, 1_000) == MachineInfoSnapshot.EnergyBottleneck.NONE,
                "no connector is never a connector bottleneck"
        );
    }

    /** These machines no longer read ENERGY_TRANSFER, so rolling it would be a dead affix. */
    private static void uncappedMachinesDoNotRollEnergyTransfer() {
        Set<String> energyTransferIds = ModifierEligibilityProfiles.CAVITATION_GENERATOR.definitions().stream()
                .filter(definition -> definition.stat() == MachineStat.ENERGY_TRANSFER)
                .map(ModifierDefinition::id)
                .collect(Collectors.toSet());
        require(!energyTransferIds.isEmpty(), "connector-controlled generators still roll Energy Transfer");
        List<ModifierEligibilityProfile> uncapped = List.of(
                ModifierEligibilityProfiles.BIO_GENERATOR,
                ModifierEligibilityProfiles.SOLAR_PANEL,
                ModifierEligibilityProfiles.POTENTIAL_REACTOR,
                ModifierEligibilityProfiles.CORROSION_CELL,
                ModifierEligibilityProfiles.SYNGAS_COMBUSTOR,
                ModifierEligibilityProfiles.AMMONIA_FUEL_CELL,
                ModifierEligibilityProfiles.FUEL_CELL_MEMBRANE,
                ModifierEligibilityProfiles.COMPONENT_RECYCLER,
                ModifierEligibilityProfiles.METAL_PRESS,
                ModifierEligibilityProfiles.MELTER,
                ModifierEligibilityProfiles.BATTERY_ASSEMBLER,
                ModifierEligibilityProfiles.COAL_GASIFIER,
                ModifierEligibilityProfiles.STEAM_METHANE_REFORMER,
                ModifierEligibilityProfiles.AMMONIA_SYNTHESIZER,
                ModifierEligibilityProfiles.COMPRESSOR_TANK,
                ModifierEligibilityProfiles.RESONANCE_CALIBRATOR,
                ModifierEligibilityProfiles.RESONANCE_COIL
        );
        for (ModifierEligibilityProfile profile : uncapped) {
            boolean rollsEnergyTransfer = profile.definitions().stream()
                    .anyMatch(definition -> energyTransferIds.contains(definition.id()));
            require(!rollsEnergyTransfer, profile.id() + " does not roll Energy Transfer");
        }
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
