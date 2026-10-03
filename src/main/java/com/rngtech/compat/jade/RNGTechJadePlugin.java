package com.rngtech.compat.jade;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.MachineInfoProvider;
import com.rngtech.content.blockentity.MachineInfoSnapshot;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.menu.UniversalConnectorAccess;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.progression.AscendancyCatalog;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import java.util.Locale;

@WailaPlugin(RNGTech.MOD_ID)
public final class RNGTechJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineStateProvider.MACHINE, BaseMachineBlockEntity.class);
        registration.registerBlockDataProvider(MachineStateProvider.UNIVERSAL_CONNECTOR, UniversalConnectorBlockEntity.class);
        registration.registerBlockDataProvider(MachineStateProvider.CABLE_CONNECTOR, CableBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(MachineStateProvider.MACHINE.getUid(), true);
        registration.addConfig(MachineStateProvider.UNIVERSAL_CONNECTOR.getUid(), true);
        registration.addConfig(MachineStateProvider.CABLE_CONNECTOR.getUid(), true);
        registration.registerBlockComponent(MachineStateProvider.MACHINE, BaseMachineBlock.class);
        registration.registerBlockComponent(MachineStateProvider.UNIVERSAL_CONNECTOR, UniversalConnectorBlock.class);
        registration.registerBlockComponent(MachineStateProvider.CABLE_CONNECTOR, CableBlock.class);
        registration.registerBlockComponent(CableConnectorProvider.INSTANCE, CableBlock.class);
        registration.registerBlockIcon(CableConnectorProvider.INSTANCE, CableBlock.class);
        registration.usePickedResult(ModBlocks.CABLE.get());
    }

    private enum MachineStateProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        MACHINE("machine_state"),
        UNIVERSAL_CONNECTOR("universal_connector_state"),
        CABLE_CONNECTOR("cable_connector_state");

        private static final String DATA_KEY = "RNGTechMachineInfo";
        private static final String CONNECTOR_DATA_KEY = "RNGTechConnectorInfo";
        private static final String ASCENDANCY_DATA_KEY = "RNGTechAscendancy";
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
            CompoundTag connectorInfo = connectorInfo(accessor);
            if (connectorInfo != null) {
                tag.put(CONNECTOR_DATA_KEY, connectorInfo);
            }
        }

        @Override
        public boolean shouldRequestData(BlockAccessor accessor) {
            return machineInfoProvider(accessor) != null || connectorAccess(accessor) != null;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!config.get(uid)) {
                return;
            }

            if (accessor.getServerData().contains(DATA_KEY)) {
                MachineInfoSnapshot info = MachineInfoSnapshot.load(accessor.getServerData().getCompound(DATA_KEY));
                boolean details = accessor.showDetails();
                appendState(tooltip, info);
                appendProgress(tooltip, info);
                appendEnergy(tooltip, info);
                appendFuel(tooltip, info);
                appendGateLines(tooltip, info, details);
                appendGear(tooltip, info, details);
                appendOutput(tooltip, info);
                appendSlots(tooltip, info, details);
                appendRefinement(tooltip, info, details);
                appendAscendancy(tooltip, accessor, details);
            }
            appendConnectorEnergy(tooltip, accessor);
            appendConnectorBridge(tooltip, accessor);
        }

        private static void appendState(ITooltip tooltip, MachineInfoSnapshot info) {
            Component value = info.blockedReason() == MachineInfoSnapshot.BlockedReason.NONE
                    ? Component.translatable("rngtech.jade.machine_state.state." + info.state().serializedName())
                    : Component.translatable("rngtech.jade.machine_state.reason." + info.blockedReason().serializedName());
            tooltip.add(line("state", value));
        }

        private static void appendProgress(ITooltip tooltip, MachineInfoSnapshot info) {
            if (info.progressMax() <= 0) {
                return;
            }
            int percent = Math.min(100, Math.round(info.progress() * 100.0f / info.progressMax()));
            tooltip.add(Component.translatable(
                    "rngtech.jade.machine_state.line.progress",
                    value(percent),
                    value(info.progress()),
                    value(info.progressMax())
            ));
        }

        private static void appendEnergy(ITooltip tooltip, MachineInfoSnapshot info) {
            if (info.energyCapacity() <= 0) {
                return;
            }
            if (info.energyRate() == 0) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.energy",
                        value(info.energy()),
                        value(info.energyCapacity())
                ));
                appendEnergyTelemetry(tooltip, info);
                return;
            }
            tooltip.add(Component.translatable(
                    "rngtech.jade.machine_state.line.energy_rate",
                    value(info.energy()),
                    value(info.energyCapacity()),
                    value(signed(info.energyRate()))
            ));
            appendEnergyTelemetry(tooltip, info);
        }

        private static void appendEnergyTelemetry(ITooltip tooltip, MachineInfoSnapshot info) {
            if (info.lastEnergyInput() > 0 || info.lastEnergyOutput() > 0 || info.connectorCap() > 0) {
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
            if (info.requiredProcessingLevel() != MachineInfoSnapshot.UNSET) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.level_required",
                        value(info.processingLevel()),
                        value(info.requiredProcessingLevel())
                ));
            } else if (details && info.processingLevel() != MachineInfoSnapshot.UNSET) {
                tooltip.add(Component.translatable("rngtech.jade.machine_state.line.level", value(info.processingLevel())));
            }

            if (info.requiredHeat() > 0) {
                tooltip.add(Component.translatable(
                        "rngtech.jade.machine_state.line.heat_required",
                        value(info.heat()),
                        value(info.requiredHeat())
                ));
            } else if (details && info.heat() != MachineInfoSnapshot.UNSET) {
                tooltip.add(Component.translatable("rngtech.jade.machine_state.line.heat", value(info.heat())));
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

        private static void appendOutput(ITooltip tooltip, MachineInfoSnapshot info) {
            if (info.outputSummary() == MachineInfoSnapshot.OutputSummary.NONE) {
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
            boolean hasEnergy = data.get(UniversalConnectorBlockEntity.dataHasConnectorIndex()) != 0;
            int bridgeType = data.get(UniversalConnectorBlockEntity.dataBridgeTypeIndex());
            if (!hasEnergy && bridgeType == 0) {
                return null;
            }
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("has_energy", hasEnergy);
            tag.putInt("cap", data.get(UniversalConnectorBlockEntity.dataTransferRateIndex()));
            tag.putInt("last_input", data.get(UniversalConnectorBlockEntity.dataLastEnergyInputIndex()));
            tag.putInt("last_output", data.get(UniversalConnectorBlockEntity.dataLastEnergyOutputIndex()));
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
