package com.rngtech.compat.jade;

import com.rngtech.RNGTech;
import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.MachineInfoProvider;
import com.rngtech.content.blockentity.MachineInfoSnapshot;
import com.rngtech.content.blockentity.PotentialReactorBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.UniversalConnectorAccess;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.progression.AscendancyCatalog;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkOrderHost;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@WailaPlugin(RNGTech.MOD_ID)
public final class RNGTechJadePlugin implements IWailaPlugin {
    private static final String WORK_ORDER_DATA_KEY = "WorkOrders";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineStateProvider.MACHINE, BlockEntity.class);
        registration.registerBlockDataProvider(MachineStateProvider.UNIVERSAL_CONNECTOR, UniversalConnectorBlockEntity.class);
        registration.registerBlockDataProvider(MachineStateProvider.CABLE_CONNECTOR, CableBlockEntity.class);
        registration.registerFluidStorage(MachineTankProvider.INSTANCE, BlockEntity.class);
        registration.registerEnergyStorage(MachineEnergyBarFilter.INSTANCE, BlockEntity.class);
        registration.registerItemStorage(MachineItemStorageProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineStateProvider.MACHINE, Block.class);
        registration.registerBlockComponent(MachineStateProvider.UNIVERSAL_CONNECTOR, UniversalConnectorBlock.class);
        registration.registerBlockComponent(MachineStateProvider.CABLE_CONNECTOR, CableBlock.class);
        registration.registerBlockComponent(CableConnectorProvider.INSTANCE, CableBlock.class);
        registration.registerBlockIcon(CableConnectorProvider.INSTANCE, CableBlock.class);
        registration.registerFluidStorageClient(MachineTankProvider.INSTANCE);
        registration.usePickedResult(ModBlocks.CABLE.get());
    }

    private enum MachineStateProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        MACHINE("machine_state"),
        UNIVERSAL_CONNECTOR("universal_connector_state"),
        CABLE_CONNECTOR("cable_connector_state");

        private static final String DATA_KEY = "RNGTechMachineInfo";
        private static final String CONNECTOR_DATA_KEY = "RNGTechConnectorInfo";
        private static final String ASCENDANCY_DATA_KEY = "RNGTechAscendancy";
        private static final String REPEAT_PAYOUT_DATA_KEY = "RNGTechRepeatPayout";
        private final ResourceLocation uid;

        MachineStateProvider(String uid) {
            this.uid = RNGTech.id(uid);
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!owns(accessor)) {
                return;
            }
            MachineInfoProvider provider = machineInfoProvider(accessor);
            if (provider != null) {
                tag.put(DATA_KEY, provider.machineInfo().save());
            }
            if (provider instanceof MachineMasteryHost host && host.masteryState().sealTiers() > 0) {
                MachineProgressionState state = host.masteryState();
                CompoundTag ascendancy = new CompoundTag();
                ascendancy.putString("id", state.ascendancy());
                ascendancy.putInt("spent", state.ascendancyNodes().size());
                ascendancy.putInt("points", state.ascendancyPoints());
                ascendancy.putInt("tiers", state.sealTiers());
                tag.put(ASCENDANCY_DATA_KEY, ascendancy);
            }
            if (provider instanceof PotentialReactorBlockEntity reactor && reactor.repeatPayoutPercent() >= 0) {
                CompoundTag payout = new CompoundTag();
                payout.putInt("percent", reactor.repeatPayoutPercent());
                payout.putInt("repeats", reactor.inputGearRepeats());
                tag.put(REPEAT_PAYOUT_DATA_KEY, payout);
            }
            if (accessor.getBlockEntity() instanceof WorkOrderHost host && !host.workOrders().isEmpty()) {
                tag.put(WORK_ORDER_DATA_KEY, workOrderData(host));
            }
            CompoundTag connectorInfo = connectorInfo(accessor);
            if (connectorInfo != null) {
                tag.put(CONNECTOR_DATA_KEY, connectorInfo);
            }
        }

        /** The machine's chips for Jade: the active job, its progress and Rush seconds left, and how many chips are loaded and done. */
        private static CompoundTag workOrderData(WorkOrderHost host) {
            CompoundTag data = new CompoundTag();
            MachineWorkOrders orders = host.workOrders();
            data.putInt("loaded", orders.orders().size());
            data.putInt("done", (int) orders.orders().stream().filter(LoadedWorkOrder::fulfilled).count());
            LoadedWorkOrder active = orders.activeOrder();
            if (active != null) {
                WorkObjective.CODEC.encodeStart(NbtOps.INSTANCE, active.order().objective()).result().ifPresent(job -> data.put("job", job));
                data.putLong("progress", active.totalProgress());
                if (active.rushed()) {
                    data.putLong("rush", Math.max(0, (active.rushDeadline() - host.workOrderGameTime()) / 20));
                }
            }
            return data;
        }

        @Override
        public boolean shouldRequestData(BlockAccessor accessor) {
            return owns(accessor) && machineInfoProvider(accessor) != null || connectorAccess(accessor) != null;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!config.get(uid) || !owns(accessor)) {
                return;
            }

            if (accessor.getServerData().contains(DATA_KEY)) {
                MachineInfoSnapshot info = MachineInfoSnapshot.load(accessor.getServerData().getCompound(DATA_KEY));
                boolean details = accessor.showDetails();
                appendState(tooltip, info);
                appendNote(tooltip, info);
                appendFluidProduct(tooltip, info);
                appendProgress(tooltip, info, details);
                appendEnergy(tooltip, info, details);
                appendFuel(tooltip, info);
                appendGateLines(tooltip, info, details);
                appendGear(tooltip, info, details);
                appendOutput(tooltip, info, details);
                appendRepeatPayout(tooltip, accessor, details);
                appendSlots(tooltip, info, details);
                appendRefinement(tooltip, info, details);
                appendAscendancy(tooltip, accessor, details);
            }
            appendWorkOrders(tooltip, accessor, accessor.showDetails());
            appendConnectorEnergy(tooltip, accessor);
            appendConnectorBridge(tooltip, accessor);
        }

        private static void appendWorkOrders(ITooltip tooltip, BlockAccessor accessor, boolean details) {
            if (!accessor.getServerData().contains(WORK_ORDER_DATA_KEY)) {
                return;
            }
            CompoundTag data = accessor.getServerData().getCompound(WORK_ORDER_DATA_KEY);
            if (data.contains("job")) {
                WorkObjective job = WorkObjective.CODEC.parse(NbtOps.INSTANCE, data.get("job")).result().orElse(null);
                if (job != null) {
                    tooltip.add(Component.translatable("rngtech.jade.work_order.active", JobChipItem.objectiveText(job)));
                    tooltip.add(Component.translatable("rngtech.jade.work_order.progress", JobChipItem.progressText(job, data.getLong("progress"))));
                }
                if (data.contains("rush")) {
                    tooltip.add(Component.translatable("rngtech.jade.work_order.rush", JobChipItem.clock(data.getLong("rush"))).withStyle(ChatFormatting.GOLD));
                }
            } else {
                tooltip.add(Component.translatable("rngtech.jade.work_order.idle"));
            }
            if (details) {
                tooltip.add(Component.translatable("rngtech.jade.work_order.loaded", data.getInt("loaded"), data.getInt("done")));
            }
        }

        private boolean owns(BlockAccessor accessor) {
            BlockEntity blockEntity = accessor.getBlockEntity();
            return switch (this) {
                case MACHINE -> blockEntity instanceof MachineInfoProvider;
                case UNIVERSAL_CONNECTOR -> blockEntity instanceof UniversalConnectorBlockEntity;
                case CABLE_CONNECTOR -> blockEntity instanceof CableBlockEntity;
            };
        }

        private static void appendState(ITooltip tooltip, MachineInfoSnapshot info) {
            tooltip.add(line("state", stateValue(info)));
        }

        private static void appendNote(ITooltip tooltip, MachineInfoSnapshot info) {
            if (info.noteKey().isEmpty()) {
                return;
            }
            Object[] args = info.noteArgs().stream()
                    .map(arg -> arg.startsWith("rngtech.") ? Component.translatable(arg).withStyle(ChatFormatting.WHITE) : value(arg))
                    .toArray();
            tooltip.add(Component.translatable(info.noteKey(), args));
        }

        private static Component stateValue(MachineInfoSnapshot info) {
            if (info.blockedReason() != MachineInfoSnapshot.BlockedReason.NONE) {
                return Component.translatable("rngtech.jade.machine_state.reason." + info.blockedReason().serializedName());
            }
            if (info.state() != MachineInfoSnapshot.WorkState.RUNNING && !info.statusKey().isEmpty()) {
                return Component.translatable(info.statusKey(), info.statusArgs().toArray());
            }
            return Component.translatable("rngtech.jade.machine_state.state." + info.state().serializedName());
        }

        private static void appendProgress(ITooltip tooltip, MachineInfoSnapshot info, boolean details) {
            if (info.progressMax() <= 0) {
                return;
            }
            if (!details && info.progress() <= 0 && info.state() != MachineInfoSnapshot.WorkState.RUNNING) {
                return;
            }
            int percent = Math.min(100, Math.round(info.progress() * 100.0f / info.progressMax()));
            if (!details) {
                tooltip.add(Component.translatable("rngtech.jade.machine_state.line.progress_compact", value(percent)));
                return;
            }
            tooltip.add(Component.translatable(
                    "rngtech.jade.machine_state.line.progress",
                    value(percent),
                    value(info.progress()),
                    value(info.progressMax())
            ));
            if (info.state() == MachineInfoSnapshot.WorkState.RUNNING) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.time_remaining",
                        value(String.format(Locale.ROOT, "%.1f", Math.max(0, info.progressMax() - info.progress()) / 20.0D))
                ));
                if (info.outputPerMinuteTenths() > 0 && info.productFluid().isEmpty()) {
                    tooltip.add(Component.translatable(
                            info.fluidOutputRate()
                                    ? "rngtech.jade.machine_state.line.output_rate_fluid"
                                    : "rngtech.jade.machine_state.line.output_rate",
                            value(String.format(
                                    Locale.ROOT,
                                    info.fluidOutputRate() ? "%.0f" : "%.1f",
                                    info.outputPerMinuteTenths() / 10.0D
                            ))
                    ));
                }
            }
        }

        private static void appendEnergy(ITooltip tooltip, MachineInfoSnapshot info, boolean details) {
            // A fluid product's rate stands in for the energy lines, which only the detailed view keeps.
            if (info.energyCapacity() <= 0 || !details && !info.productFluid().isEmpty()) {
                return;
            }
            if (info.energyRate() == 0) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.energy",
                        value(info.energy()),
                        value(info.energyCapacity())
                ));
                appendEnergyTelemetry(tooltip, info, details);
                return;
            }
            tooltip.add(Component.translatable(
                    "rngtech.jade.machine_state.line.energy_rate",
                    value(info.energy()),
                    value(info.energyCapacity()),
                    value(signed(info.energyRate()))
            ));
            appendEnergyTelemetry(tooltip, info, details);
        }

        private static void appendFluidProduct(ITooltip tooltip, MachineInfoSnapshot info) {
            if (info.productFluid().isEmpty()
                    || info.outputPerMinuteTenths() <= 0
                    || info.state() != MachineInfoSnapshot.WorkState.RUNNING) {
                return;
            }
            tooltip.add(Component.translatable(
                    "rngtech.jade.machine_state.line.fluid_product",
                    Component.translatable(info.productFluid()),
                    value(fluidRate(info.outputPerMinuteTenths() / 10.0D))
            ));
        }

        /** mB per minute, switching to buckets from 1,000 mB. */
        private static String fluidRate(double perMinute) {
            if (perMinute < 1000.0D) {
                return String.format(Locale.ROOT, "%.0f mB/min", perMinute);
            }
            String buckets = String.format(Locale.ROOT, "%.2f", perMinute / 1000.0D).replaceAll("\\.?0+$", "");
            return buckets + " B/min";
        }

        private static void appendEnergyTelemetry(ITooltip tooltip, MachineInfoSnapshot info, boolean details) {
            if (details && info.lastEnergyInput() > 0 || info.lastEnergyOutput() > 0 || info.connectorCap() > 0) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.energy_live",
                        value(info.lastEnergyInput()),
                        value(info.lastEnergyOutput()),
                        value(info.connectorCap())
                ));
            }
            if (info.energyBottleneck() != MachineInfoSnapshot.EnergyBottleneck.NONE) {
                tooltip.add(line("energy_bottleneck", Component.translatable(
                        "rngtech.jade.machine_state.energy_bottleneck." + info.energyBottleneck().serializedName()
                )));
            }
        }

        private static void appendFuel(ITooltip tooltip, MachineInfoSnapshot info) {
            if (info.fuelMax() <= 0) {
                return;
            }
            tooltip.add(Component.translatable(
                    "rngtech.jade.machine_state.line.fuel",
                    value(info.fuel()),
                    value(info.fuelMax())
            ));
        }

        private static void appendGateLines(ITooltip tooltip, MachineInfoSnapshot info, boolean details) {
            boolean levelShort = info.processingLevel() < info.requiredProcessingLevel();
            if (info.requiredProcessingLevel() != MachineInfoSnapshot.UNSET && (details || levelShort)) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.level_required",
                        value(info.processingLevel()),
                        value(info.requiredProcessingLevel())
                ));
            } else if (details && info.processingLevel() != MachineInfoSnapshot.UNSET) {
                tooltip.add(Component.translatable("rngtech.jade.machine_state.line.level", value(info.processingLevel())));
            }

            boolean heatShort = info.heat() < info.requiredHeat();
            if (info.requiredHeat() > 0 && (details || heatShort)) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.heat_required",
                        value(info.heat()),
                        value(info.requiredHeat())
                ));
            } else if (details && info.heat() != MachineInfoSnapshot.UNSET) {
                tooltip.add(Component.translatable("rngtech.jade.machine_state.line.heat", value(info.heat())));
            }
            if (details && info.requiredHeat() > 0 && info.setpoint() > info.requiredHeat()) {
                tooltip.add(Component.translatable("rngtech.jade.machine_state.line.setpoint", value(info.setpoint())));
            }
            if (info.batch() > 1) {
                tooltip.add(Component.translatable("rngtech.jade.machine_state.line.batch", value(info.batch())));
            }
        }

        private static void appendGear(ITooltip tooltip, MachineInfoSnapshot info, boolean details) {
            if (info.gearSummary() == MachineInfoSnapshot.GearSummary.NONE) {
                return;
            }
            boolean missing = info.gearSummary().serializedName().startsWith("missing_")
                    || info.gearSummary() == MachineInfoSnapshot.GearSummary.NO_CELLS;
            if (!missing && !details && info.blockedReason() == MachineInfoSnapshot.BlockedReason.NONE) {
                return;
            }
            tooltip.add(line("gear", Component.translatable(
                    "rngtech.jade.machine_state.gear." + info.gearSummary().serializedName()
            )));
        }

        private static void appendOutput(ITooltip tooltip, MachineInfoSnapshot info, boolean details) {
            if (info.outputSummary() == MachineInfoSnapshot.OutputSummary.NONE) {
                return;
            }
            boolean repeatsState = info.outputSummary() == MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                    || info.outputSummary() == MachineInfoSnapshot.OutputSummary.ENERGY_FULL;
            if (repeatsState && !details) {
                return;
            }
            tooltip.add(line("output", Component.translatable(
                    "rngtech.jade.machine_state.output." + info.outputSummary().serializedName()
            )));
        }

        private static void appendSlots(ITooltip tooltip, MachineInfoSnapshot info, boolean details) {
            if (info.activeSlots() == MachineInfoSnapshot.UNSET || info.maxSlots() == MachineInfoSnapshot.UNSET) {
                return;
            }
            if ("battery_chassis".equals(info.family())) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.cells",
                        value(info.activeSlots()),
                        value(info.maxSlots())
                ));
            } else if (details) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.slots",
                        value(info.activeSlots()),
                        value(info.maxSlots())
                ));
            }
        }

        private static void appendRefinement(ITooltip tooltip, MachineInfoSnapshot info, boolean details) {
            if (details && info.corrupted()) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.refinement_corrupted",
                        Component.translatable("rngtech.rarity." + info.refinementRarity()),
                        value(info.affixCount())
                ));
                tooltip.add(Component.translatable("rngtech.tooltip.corrupted").withStyle(ChatFormatting.DARK_RED));
                return;
            }
            if (!details
                    || info.refinementPotential() <= 0 && info.affixCount() <= 0 && "normal".equals(info.refinementRarity())) {
                return;
            }
            tooltip.add(Component.translatable(
                    "rngtech.jade.machine_state.line.refinement",
                    Component.translatable("rngtech.rarity." + info.refinementRarity()),
                    value(info.refinementPotential()),
                    value(info.affixCount())
            ));
        }

        /** A Potential Reactor's payout for the gear in its input; the compact view shows it only when repeats cut it. */
        private static void appendRepeatPayout(ITooltip tooltip, BlockAccessor accessor, boolean details) {
            if (!accessor.getServerData().contains(REPEAT_PAYOUT_DATA_KEY)) {
                return;
            }
            CompoundTag tag = accessor.getServerData().getCompound(REPEAT_PAYOUT_DATA_KEY);
            int percent = tag.getInt("percent");
            if (!details && percent >= 100) {
                return;
            }
            tooltip.add(Component.translatable(
                    "rngtech.jade.machine_state.line.repeat_payout",
                    value(percent + "%"),
                    value(tag.getInt("repeats"))
            ));
        }

        private static void appendAscendancy(ITooltip tooltip, BlockAccessor accessor, boolean details) {
            if (!details || !accessor.getServerData().contains(ASCENDANCY_DATA_KEY)) {
                return;
            }
            CompoundTag tag = accessor.getServerData().getCompound(ASCENDANCY_DATA_KEY);
            var ascendancy = AscendancyCatalog.get(tag.getString("id"));
            if (ascendancy == null) {
                tooltip.add(Component.translatable("rngtech.jade.machine_state.line.ascendancy_unchosen", value(tag.getInt("tiers"))));
                return;
            }
            tooltip.add(Component.translatable(
                    "rngtech.jade.machine_state.line.ascendancy",
                    Component.translatable(ascendancy.translationKey()).withStyle(ChatFormatting.GOLD),
                    value(tag.getInt("spent")),
                    value(tag.getInt("points"))
            ));
        }

        private static void appendConnectorEnergy(ITooltip tooltip, BlockAccessor accessor) {
            if (!accessor.getServerData().contains(CONNECTOR_DATA_KEY)) {
                return;
            }
            CompoundTag tag = accessor.getServerData().getCompound(CONNECTOR_DATA_KEY);
            if (!tag.getBoolean("has_energy")) {
                return;
            }
            tooltip.add(Component.translatable(
                    "rngtech.jade.connector_energy.line",
                    value(tag.getInt("last_input")),
                    value(tag.getInt("last_output")),
                    value(tag.getInt("cap"))
            ));
        }

        private static void appendConnectorBridge(ITooltip tooltip, BlockAccessor accessor) {
            if (!accessor.getServerData().contains(CONNECTOR_DATA_KEY)) {
                return;
            }
            CompoundTag tag = accessor.getServerData().getCompound(CONNECTOR_DATA_KEY);
            NetworkBridgeType bridgeType = NetworkBridgeType.byDataId(tag.getInt("bridge_type"));
            if (bridgeType == NetworkBridgeType.NONE) {
                return;
            }
            tooltip.add(Component.translatable(
                    "rngtech.jade.connector_bridge.line",
                    Component.translatable(bridgeType.translationKey()),
                    value(tag.getInt("bridge_channel"))
            ));
        }

        private static Component line(String key, Component value) {
            return Component.translatable("rngtech.jade.machine_state.line." + key, value);
        }

        private static Component value(long value) {
            return Component.literal(format(value)).withStyle(ChatFormatting.WHITE);
        }

        private static Component value(String value) {
            return Component.literal(value).withStyle(ChatFormatting.WHITE);
        }

        private static String signed(long value) {
            return value > 0 ? "+" + format(value) : format(value);
        }

        private static String format(long value) {
            return String.format(Locale.ROOT, "%,d", value);
        }

        private static MachineInfoProvider machineInfoProvider(BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof MachineInfoProvider provider) {
                return provider;
            }
            if (accessor.getBlockEntity() instanceof UniversalConnectorBlockEntity connector) {
                return adjacentMachine(accessor, connector.targetDirection());
            }
            if (accessor.getBlockState().getBlock() instanceof CableBlock
                    && accessor.getBlockEntity() instanceof CableBlockEntity) {
                Direction direction = CableBlock.resolveInstalledConnectorDirection(
                        accessor.getBlockState(),
                        accessor.getHitResult(),
                        false
                );
                return direction == null ? null : adjacentMachine(accessor, direction);
            }
            return null;
        }

        private static CompoundTag connectorInfo(BlockAccessor accessor) {
            UniversalConnectorAccess access = connectorAccess(accessor);
            if (access == null) {
                return null;
            }
            ContainerData data = access.menuData();
            int cap = 0;
            int lastInput = 0;
            int lastOutput = 0;
            for (int module = 0; module < UniversalConnectorBlockEntity.ENERGY_MODULE_SLOT_COUNT; module++) {
                cap += data.get(UniversalConnectorBlockEntity.dataEnergyTransferRateIndex(module));
                lastInput += data.get(UniversalConnectorBlockEntity.dataEnergyLastInputIndex(module));
                lastOutput += data.get(UniversalConnectorBlockEntity.dataEnergyLastOutputIndex(module));
            }
            boolean hasEnergy = cap > 0;
            int bridgeType = data.get(UniversalConnectorBlockEntity.dataBridgeTypeIndex());
            if (!hasEnergy && bridgeType == 0) {
                return null;
            }
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("has_energy", hasEnergy);
            tag.putInt("cap", cap);
            tag.putInt("last_input", lastInput);
            tag.putInt("last_output", lastOutput);
            tag.putInt("bridge_type", bridgeType);
            tag.putInt("bridge_channel", data.get(UniversalConnectorBlockEntity.dataBridgeChannelIndex()));
            return tag;
        }

        private static UniversalConnectorAccess connectorAccess(BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof UniversalConnectorBlockEntity connector) {
                return connector.universalConnectorAccess(connector.targetDirection());
            }
            if (accessor.getBlockState().getBlock() instanceof CableBlock
                    && accessor.getBlockEntity() instanceof CableBlockEntity cable) {
                Direction direction = CableBlock.resolveInstalledConnectorDirection(
                        accessor.getBlockState(),
                        accessor.getHitResult(),
                        false
                );
                return direction == null ? null : cable.universalConnectorAccess(direction);
            }
            return null;
        }

        private static MachineInfoProvider adjacentMachine(BlockAccessor accessor, Direction direction) {
            if (direction == null) {
                return null;
            }
            BlockPos pos = accessor.getPosition().relative(direction);
            BlockEntity blockEntity = accessor.getLevel().getBlockEntity(pos);
            return blockEntity instanceof MachineInfoProvider provider ? provider : null;
        }
    }

    private enum MachineTankProvider
            implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, FluidView> {
        INSTANCE;

        private static final ResourceLocation UID = RNGTech.id("machine_tanks");
        private static final String TITLE_KEY = "title";

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            if (!(accessor.getTarget() instanceof PurgeableFluidStorage storage)
                    || !(accessor.getTarget() instanceof MachineInfoProvider)) {
                return null;
            }
            List<FluidPurgeTarget> targets = storage.fluidPurgeTargets();
            if (targets.isEmpty()) {
                return null;
            }
            return targets.stream().map(MachineTankProvider::group).toList();
        }

        @Override
        public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
            return ClientViewGroup.map(groups, FluidView::readDefault, (group, clientGroup) -> {
                String title = group.getExtraData().getString(TITLE_KEY);
                if (!title.isEmpty()) {
                    clientGroup.title = Component.translatable(title);
                }
            });
        }

        private static ViewGroup<CompoundTag> group(FluidPurgeTarget target) {
            FluidStack fluid = target.fluid();
            JadeFluidObject jadeFluid = fluid.isEmpty()
                    ? JadeFluidObject.empty()
                    : JadeFluidObject.of(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch());
            long capacity = Math.max(1, Math.max(target.capacity(), fluid.getAmount()));
            ViewGroup<CompoundTag> group = new ViewGroup<>(List.of(FluidView.writeDefault(jadeFluid, capacity)));
            if (target.name().getContents() instanceof TranslatableContents translatable) {
                group.getExtraData().putString(TITLE_KEY, translatable.getKey());
            }
            return group;
        }
    }

    private enum MachineEnergyBarFilter implements IServerExtensionProvider<CompoundTag> {
        INSTANCE;

        private static final ResourceLocation UID = RNGTech.id("machine_energy_bar_filter");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            return accessor.getTarget() instanceof MachineInfoProvider ? List.of() : null;
        }
    }

    /** Lists a machine's installed Gear only in the detailed view; the compact view keeps its inputs and outputs. */
    private enum MachineItemStorageProvider implements IServerExtensionProvider<ItemStack> {
        INSTANCE;

        private static final ResourceLocation UID = RNGTech.id("machine_item_storage");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
            if (accessor.showDetails() || !(accessor.getTarget() instanceof BaseMachineBlockEntity machine)) {
                return null;
            }
            IItemHandler inventory = machine.getItemHandler(null);
            List<ItemStack> stacks = new ArrayList<>();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && !machine.isGearSlot(slot)) {
                    merge(stacks, stack);
                }
            }
            return stacks.isEmpty() ? List.of() : List.of(new ViewGroup<>(stacks));
        }

        private static void merge(List<ItemStack> stacks, ItemStack stack) {
            for (ItemStack listed : stacks) {
                if (ItemStack.isSameItemSameComponents(listed, stack)) {
                    listed.grow(stack.getCount());
                    return;
                }
            }
            stacks.add(stack.copy());
        }
    }

    private enum CableConnectorProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID = RNGTech.id("cable_connector_target");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
            ItemStack stack = connectorStack(accessor);
            return stack.isEmpty() ? currentIcon : IElementHelper.get().item(stack);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = connectorStack(accessor);
            if (!stack.isEmpty()) {
                tooltip.replace(JadeIds.CORE_OBJECT_NAME, stack.getHoverName());
            }
        }

        private static ItemStack connectorStack(BlockAccessor accessor) {
            if (!(accessor.getBlockState().getBlock() instanceof CableBlock)
                    || !(accessor.getBlockEntity() instanceof CableBlockEntity cable)) {
                return ItemStack.EMPTY;
            }

            Direction direction = CableBlock.resolveInstalledConnectorDirection(
                    accessor.getBlockState(),
                    accessor.getHitResult(),
                    false
            );
            if (direction == null) {
                return ItemStack.EMPTY;
            }
            if (cable.hasUniversalConnector(direction)) {
                return new ItemStack(ModItems.UNIVERSAL_CONNECTOR.get());
            }
            CableBlockEntity.ConnectorData connector = cable.connector(direction);
            if (connector == null) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(ModItems.energyConnector(connector.tier()).get());
        }
    }
}
