package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.tool.FieldToolAssembly;
import com.rngtech.content.tool.FieldToolEnchantments;
import com.rngtech.content.tool.FieldToolTags;
import com.rngtech.content.tool.OreHardness;
import com.rngtech.content.tool.ToolBaseStatCatalog;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.content.tool.ToolHeadMaterial;
import com.rngtech.rpg.MachineTraits;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public class ModularToolItem extends Item {
    private static final ThreadLocal<Boolean> EXTRA_BREAKING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Integer> EXTRA_BLOCK_FE_SURCHARGE = ThreadLocal.withInitial(() -> 0);
    private static final Map<UUID, AreaHit> AREA_HITS = new HashMap<>();
    private static final Map<UUID, ActiveUse> ACTIVE_USES = new HashMap<>();
    private static final int SELF_REPAIR_INTERVAL_TICKS = 150;
    private static final int ACTIVE_USE_GRACE_TICKS = 2;
    private static final int STORED_WEAR_SCALE = 10_000;
    private static final int DAMAGE_REMAINDER_SCALE = 10_000;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;

    private final ToolHeadFamily family;

    public ModularToolItem(ToolHeadFamily family, Properties properties) {
        super(properties);
        this.family = family;
    }

    public static boolean isModularTool(ItemStack stack) {
        return stack.getItem() instanceof ModularToolItem;
    }

    public static FieldToolAssembly assembly(ItemStack stack) {
        FieldToolAssembly assembly = stack.get(ModDataComponents.FIELD_TOOL_ASSEMBLY.get());
        return assembly == null ? FieldToolAssembly.EMPTY : assembly;
    }

    public static MachineTraits storedTraits(ItemStack stack) {
        return MachineTraits.EMPTY;
    }

    public static MachineTraits traits(ItemStack stack) {
        return storedTraits(stack);
    }

    public static boolean hasValidAssembly(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularToolItem tool)) {
            return false;
        }
        return assembly(stack).isValidFor(tool.family());
    }

    public static void refreshMaxDamage(ItemStack stack) {
        if (!isModularTool(stack)) {
            return;
        }
        int maxDamage = ToolBaseStatCatalog.maxDurability(stack);
        stack.set(DataComponents.MAX_DAMAGE, maxDamage);
        if (stack.getDamageValue() > maxDamage) {
            stack.setDamageValue(maxDamage);
        }
        if (stack.getDamageValue() >= maxDamage) {
            setDamageRemainder(stack, 0);
        }
    }

    public static boolean isBroken(ItemStack stack) {
        if (!isModularTool(stack)) {
            return false;
        }
        refreshMaxDamage(stack);
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    public static int storedWear(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.FIELD_TOOL_WEAR.get(), 0), 0, STORED_WEAR_SCALE);
    }

    public static boolean hasStoredWear(ItemStack stack) {
        return storedWear(stack) > 0;
    }

    public static int storedWearPercent(ItemStack stack) {
        int wear = storedWear(stack);
        return wear <= 0 ? 0 : Math.max(1, Math.round(wear * 100.0F / STORED_WEAR_SCALE));
    }

    public static void setStoredWear(ItemStack stack, int wear) {
        int clampedWear = Mth.clamp(wear, 0, STORED_WEAR_SCALE);
        if (clampedWear <= 0) {
            stack.remove(ModDataComponents.FIELD_TOOL_WEAR.get());
        } else {
            stack.set(ModDataComponents.FIELD_TOOL_WEAR.get(), clampedWear);
        }
    }

    public static void clearStoredWear(ItemStack stack) {
        stack.remove(ModDataComponents.FIELD_TOOL_WEAR.get());
    }

    public static int damageWear(ItemStack stack) {
        if (!isModularTool(stack)) {
            return 0;
        }
        refreshMaxDamage(stack);
        int maxDamage = Math.max(1, stack.getMaxDamage());
        return Mth.clamp((int) Math.round(exactDamageValue(stack) * STORED_WEAR_SCALE / maxDamage), 0, STORED_WEAR_SCALE);
    }

    public static void applyDamageWear(ItemStack stack, int wear) {
        if (!isModularTool(stack)) {
            return;
        }
        refreshMaxDamage(stack);
        int clampedWear = Mth.clamp(wear, 0, STORED_WEAR_SCALE);
        int maxDamage = Math.max(1, stack.getMaxDamage());
        applyExactDamage(stack, maxDamage * (double) clampedWear / STORED_WEAR_SCALE);
    }

    public static void damageWithoutBreaking(ItemStack stack, int amount) {
        if (!isModularTool(stack) || amount <= 0) {
            return;
        }
        damageByAmount(stack, amount);
    }

    public static void repairWithoutBreaking(ItemStack stack, int amount) {
        if (!isModularTool(stack) || amount <= 0) {
            return;
        }
        applyExactDamage(stack, exactDamageValue(stack) - amount);
    }

    private static void damageByAmount(ItemStack stack, double amount) {
        if (!isModularTool(stack) || amount <= 0.0) {
            return;
        }
        applyExactDamage(stack, exactDamageValue(stack) + amount);
    }

    private static double exactDamageValue(ItemStack stack) {
        if (!isModularTool(stack)) {
            return 0.0;
        }
        refreshMaxDamage(stack);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return stack.getMaxDamage();
        }
        return Math.min(
                stack.getMaxDamage(),
                stack.getDamageValue() + damageRemainder(stack) / (double) DAMAGE_REMAINDER_SCALE
        );
    }

    private static void applyExactDamage(ItemStack stack, double exactDamage) {
        if (!isModularTool(stack)) {
            return;
        }
        refreshMaxDamage(stack);
        double clampedDamage = Mth.clamp(exactDamage, 0.0, stack.getMaxDamage());
        int wholeDamage = Mth.clamp((int) Math.floor(clampedDamage), 0, stack.getMaxDamage());
        int remainder = wholeDamage >= stack.getMaxDamage()
                ? 0
                : (int) Math.round((clampedDamage - wholeDamage) * DAMAGE_REMAINDER_SCALE);
        if (remainder >= DAMAGE_REMAINDER_SCALE) {
            wholeDamage = Math.min(stack.getMaxDamage(), wholeDamage + 1);
            remainder = 0;
        }
        stack.set(DataComponents.DAMAGE, wholeDamage);
        setDamageRemainder(stack, wholeDamage >= stack.getMaxDamage() ? 0 : remainder);
    }

    private static int damageRemainder(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.FIELD_TOOL_DAMAGE_REMAINDER.get(), 0), 0, DAMAGE_REMAINDER_SCALE - 1);
    }

    private static void setDamageRemainder(ItemStack stack, int remainder) {
        int clampedRemainder = Mth.clamp(remainder, 0, DAMAGE_REMAINDER_SCALE - 1);
        if (clampedRemainder <= 0) {
            stack.remove(ModDataComponents.FIELD_TOOL_DAMAGE_REMAINDER.get());
        } else {
            stack.set(ModDataComponents.FIELD_TOOL_DAMAGE_REMAINDER.get(), clampedRemainder);
        }
    }

    public static IEnergyStorage energyStorage(ItemStack stack) {
        if (!isModularTool(stack)) {
            return null;
        }
        FieldToolAssembly assembly = assembly(stack);
        if (!hasValidAssembly(stack) || assembly.batteryCell().isEmpty() || !isBatteryAccepted(stack, assembly.batteryCell())) {
            return null;
        }
        return new InstalledCellEnergyStorage(stack);
    }

    public static boolean isBatteryAccepted(ItemStack toolStack, ItemStack cellStack) {
        if (!hasValidAssembly(toolStack)) {
            return false;
        }
        if (!(cellStack.getItem() instanceof BatteryCellItem cell)) {
            return false;
        }
        return cell.material().stage() <= ToolBaseStatCatalog.batterySupport(toolStack);
    }

    public static void rememberAreaHit(Player player, BlockPos pos, Direction face) {
        if (face == null) {
            return;
        }
        AREA_HITS.put(player.getUUID(), new AreaHit(pos.immutable(), face));
    }

    public static void forgetAreaHit(Player player, BlockPos pos) {
        AreaHit hit = AREA_HITS.get(player.getUUID());
        if (hit != null && hit.pos().equals(pos)) {
            AREA_HITS.remove(player.getUUID());
        }
    }

    public static boolean canSpendEnergy(ItemStack stack, int amount) {
        if (amount <= 0) {
            return true;
        }
        IEnergyStorage storage = energyStorage(stack);
        return storage != null && storage.extractEnergy(amount, true) >= amount;
    }

    public static void markActiveUse(Player player, ItemStack stack) {
        if (player == null || !isModularTool(stack)) {
            return;
        }
        InteractionHand hand = player.getMainHandItem() == stack
                ? InteractionHand.MAIN_HAND
                : player.getOffhandItem() == stack ? InteractionHand.OFF_HAND : null;
        if (hand != null) {
            ACTIVE_USES.put(player.getUUID(), new ActiveUse(hand, player.tickCount));
        }
    }

    public static void clearActiveUse(Player player, ItemStack stack) {
        if (player == null || !isModularTool(stack)) {
            return;
        }
        ActiveUse activeUse = ACTIVE_USES.get(player.getUUID());
        if (activeUse != null && activeUse.handMatches(player, stack, true)) {
            ACTIVE_USES.remove(player.getUUID());
        }
    }

    public static boolean runWithoutExtraBreaks(BooleanSupplier action) {
        boolean previous = EXTRA_BREAKING.get();
        EXTRA_BREAKING.set(true);
        try {
            return action.getAsBoolean();
        } finally {
            EXTRA_BREAKING.set(previous);
        }
    }

    public static int spendEnergy(ItemStack stack, int amount) {
        if (amount <= 0) {
            return 0;
        }
        IEnergyStorage storage = energyStorage(stack);
        return storage == null ? 0 : storage.extractEnergy(amount, false);
    }

    public static boolean isMatchingInstalledHeadRepairItem(ItemStack toolStack, ItemStack repairCandidate) {
        if (!hasValidAssembly(toolStack)) {
            return false;
        }
        FieldToolAssembly assembly = assembly(toolStack);
        if (!(assembly.head().getItem() instanceof ToolHeadItem installedHead)) {
            return false;
        }
        ToolHeadMaterial material = installedHead.material();
        if (material == ToolHeadMaterial.FLINT) {
            return repairCandidate.is(Items.FLINT);
        }
        return repairCandidate.is(ingotTag(material));
    }

    public static boolean isKnownRepairMaterial(ItemStack repairCandidate) {
        if (repairCandidate.is(Items.FLINT)) {
            return true;
        }
        for (ToolHeadMaterial material : ToolHeadMaterial.values()) {
            if (material != ToolHeadMaterial.FLINT && repairCandidate.is(ingotTag(material))) {
                return true;
            }
        }
        return false;
    }

    private static TagKey<Item> ingotTag(ToolHeadMaterial material) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/" + material.materialId()));
    }

    private static String repairTooltipKey(ToolHeadMaterial material) {
        return material == ToolHeadMaterial.FLINT
                ? "rngtech.tooltip.modular_tool.repair.flint"
                : "rngtech.tooltip.modular_tool.repair.ingot";
    }

    public ToolHeadFamily family() {
        return family;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return hasValidAssembly(stack) && FieldToolEnchantments.headEnchantments(stack).isEmpty();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        FieldToolAssembly assembly = assembly(stack);
        return hasValidAssembly(stack)
                ? FieldToolEnchantments.enchantmentValue(assembly.headMaterial())
                : 0;
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        FieldToolAssembly assembly = assembly(stack);
        return hasValidAssembly(stack) && assembly.head().isPrimaryItemFor(enchantment);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        FieldToolAssembly assembly = assembly(stack);
        return hasValidAssembly(stack) && assembly.head().supportsEnchantment(enchantment);
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return FieldToolEnchantments.enchantmentLevel(stack, enchantment);
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
        return FieldToolEnchantments.allEnchantments(stack);
    }

    @Override
    public ItemStack applyEnchantments(ItemStack stack, List<EnchantmentInstance> enchantments) {
        return FieldToolEnchantments.applyEnchantmentsToHead(stack, enchantments);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return FieldToolEnchantments.hasAnyEnchantments(stack) || super.isFoil(stack);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (!hasValidAssembly(stack)) {
            return ItemAttributeModifiers.EMPTY;
        }
        double attackSpeedModifier = ToolBaseStatCatalog.attackSpeed(stack) - PLAYER_BASE_ATTACK_SPEED;
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, attackSpeedModifier, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!hasUsableAssembly(stack) || !family.validTarget(state)) {
            return 1.0F;
        }
        if (!OreHardness.canHarvest(stack, state)) {
            return 1.0F;
        }
        return ToolBaseStatCatalog.miningSpeed(stack);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!hasUsableAssembly(stack) || !family.validTarget(state)) {
            return false;
        }
        Tier tier = tierForMiningLevel(ToolBaseStatCatalog.miningLevel(stack));
        return !state.is(tier.getIncorrectBlocksForDrops()) && OreHardness.canHarvest(stack, state);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (!hasUsableAssembly(stack)) {
            return false;
        }
        return switch (family) {
            case PICK, HAMMER -> itemAbility == ItemAbilities.PICKAXE_DIG;
            case SHOVEL, DIGGER -> ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility);
            case AXE, TREEFELLER -> ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility);
        };
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return isMatchingInstalledHeadRepairItem(stack, repairCandidate);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!family.shovelTool() || !hasUsableAssembly(stack) || context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        BlockState modified = null;
        BlockState flattened = state.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
        if (flattened != null && level.getBlockState(pos.above()).isAir()) {
            level.playSound(player, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
            modified = flattened;
        } else {
            modified = state.getToolModifiedState(context, ItemAbilities.SHOVEL_DOUSE, false);
            if (modified != null && !level.isClientSide()) {
                level.levelEvent(null, 1009, pos, 0);
            }
        }

        if (modified == null) {
            return InteractionResult.PASS;
        }

        markActiveUse(player, stack);
        if (!level.isClientSide) {
            level.setBlock(pos, modified, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, modified));
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                chargeToolUse(
                        stack,
                        serverLevel,
                        serverPlayer,
                        ToolBaseStatCatalog.feUsage(stack)
                );
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (!(level instanceof ServerLevel serverLevel) || !(miningEntity instanceof ServerPlayer player)) {
            return true;
        }
        markActiveUse(player, stack);
        refreshMaxDamage(stack);
        if (isBroken(stack)) {
            return true;
        }
        if (state.getDestroySpeed(level, pos) > 0.0F) {
            chargeForBrokenBlock(stack, serverLevel, player, state, pos);
        }
        if (!EXTRA_BREAKING.get()) {
            breakExtraBlocks(stack, serverLevel, player, state, pos);
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        refreshMaxDamage(stack);
        FieldToolEnchantments.migrateOuterEnchantments(stack);
        selfRepair(stack, player, slotId, isSelected);
        transferEnergyFromInventory(stack, player);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        refreshMaxDamage(stack);
        return stack.isDamaged() || damageRemainder(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        refreshMaxDamage(stack);
        int maxDamage = Math.max(1, stack.getMaxDamage());
        return Math.round(13.0F - 13.0F * (float) exactDamageValue(stack) / maxDamage);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x6FC46F;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        refreshMaxDamage(stack);
        FieldToolAssembly assembly = assembly(stack);
        boolean hasDetails = TooltipKeyState.hasShiftDown();
        if (hasDetails) {
            appendDetailedPartsTooltip(stack, assembly, tooltipComponents);
        } else {
            appendCompactPartsTooltip(stack, assembly, tooltipComponents);
        }
        FieldToolEnchantments.appendHeadEnchantments(stack, context, tooltipComponents, tooltipFlag);
        appendDurabilityTooltip(stack, tooltipComponents);
        appendOreSummaryTooltip(stack, tooltipComponents);
        if (hasDetails) {
            appendMechanicsTooltip(stack, assembly, tooltipComponents);
            FieldToolTooltip.appendBaseStats(stack, ToolBaseStatCatalog.baseStats(stack), tooltipComponents);
        } else {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.modular_tool.hint.shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private void appendCompactPartsTooltip(
            ItemStack stack,
            FieldToolAssembly assembly,
            List<Component> tooltipComponents
    ) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.modular_tool.parts.compact",
                compactHeadName(assembly.head()),
                compactRodName(assembly.rod())
        ).withStyle(ChatFormatting.GRAY));
        appendCompactBatteryTooltip(stack, assembly.batteryCell(), tooltipComponents);
    }

    private void appendDetailedPartsTooltip(
            ItemStack stack,
            FieldToolAssembly assembly,
            List<Component> tooltipComponents
    ) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.modular_tool.head",
                assembly.head().isEmpty() ? Component.translatable("rngtech.tooltip.empty") : assembly.head().getHoverName()
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.modular_tool.rod",
                assembly.rod().isEmpty() ? Component.translatable("rngtech.tooltip.empty") : assembly.rod().getHoverName()
        ).withStyle(ChatFormatting.GRAY));
        if (ToolHeadItem.isDiamondTipped(assembly.head())) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.tool_head.diamond_tipped").withStyle(ChatFormatting.AQUA));
        }
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.tool_rod.battery_support",
                ToolBaseStatCatalog.batterySupport(stack)
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.tool_rod.accepts_cells",
                ToolBaseStatCatalog.batterySupport(stack)
        ).withStyle(ChatFormatting.GRAY));
        appendDetailedBatteryTooltip(assembly.batteryCell(), tooltipComponents);
    }

    private Component compactHeadName(ItemStack head) {
        if (head.getItem() instanceof ToolHeadItem headItem) {
            return Component.translatable(
                    "rngtech.tooltip.modular_tool.head.compact",
                    Component.translatable(headItem.material().translationKey()),
                    Component.translatable(headItem.family().translationKey())
            );
        }
        return Component.translatable("rngtech.tooltip.empty");
    }

    private Component compactRodName(ItemStack rod) {
        if (rod.getItem() instanceof ToolRodItem rodItem) {
            return Component.translatable(
                    "rngtech.tooltip.modular_tool.rod.compact",
                    Component.translatable(rodItem.material().translationKey())
            );
        }
        return Component.translatable("rngtech.tooltip.empty");
    }

    private void appendCompactBatteryTooltip(
            ItemStack stack,
            ItemStack batteryCell,
            List<Component> tooltipComponents
    ) {
        int batterySupport = ToolBaseStatCatalog.batterySupport(stack);
        if (batteryCell.isEmpty()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.modular_tool.power.empty",
                    batterySupport
            ).withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.modular_tool.power",
                BatteryCellItem.energyStored(batteryCell),
                BatteryCellItem.energyCapacity(batteryCell),
                batterySupport
        ).withStyle(ChatFormatting.GRAY));
    }

    private void appendDetailedBatteryTooltip(ItemStack batteryCell, List<Component> tooltipComponents) {
        if (batteryCell.isEmpty()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.modular_tool.battery.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.modular_tool.battery",
                batteryCell.getHoverName(),
                BatteryCellItem.energyStored(batteryCell),
                BatteryCellItem.energyCapacity(batteryCell)
        ).withStyle(ChatFormatting.GRAY));
    }

    private void appendDurabilityTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                isBroken(stack) ? "rngtech.tooltip.modular_tool.durability.broken" : "rngtech.tooltip.modular_tool.durability",
                Math.max(0, (int) Math.floor(stack.getMaxDamage() - exactDamageValue(stack))),
                stack.getMaxDamage()
        ).withStyle(isBroken(stack) ? ChatFormatting.RED : ChatFormatting.GRAY));
    }

    private void appendOreSummaryTooltip(ItemStack stack, List<Component> tooltipComponents) {
        if (family != ToolHeadFamily.PICK && family != ToolHeadFamily.HAMMER) {
            return;
        }
        boolean addedVeinMiner = false;
        if (family == ToolHeadFamily.PICK && ToolBaseStatCatalog.veinMineLimit(stack) > 1) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.modular_tool.ore_summary.vein_miner",
                    OreHardness.oreReach(stack),
                    ToolBaseStatCatalog.veinMineLimit(stack),
                    ToolBaseStatCatalog.veinMineFeUsage(stack)
            ).withStyle(ChatFormatting.GRAY));
            addedVeinMiner = true;
        }
        if (ToolBaseStatCatalog.hasOreBurstMode(stack)) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.modular_tool.ore_summary.burst",
                    OreHardness.oreReach(stack),
                    ToolBaseStatCatalog.oreBurstFeUsage(stack)
            ).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (addedVeinMiner) {
            return;
        }
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.modular_tool.ore_summary",
                OreHardness.oreReach(stack)
        ).withStyle(ChatFormatting.GRAY));
    }

    private void appendMechanicsTooltip(
            ItemStack stack,
            FieldToolAssembly assembly,
            List<Component> tooltipComponents
    ) {
        if (hasValidAssembly(stack)) {
            tooltipComponents.add(Component.translatable(repairTooltipKey(assembly.headMaterial())).withStyle(ChatFormatting.GRAY));
        }
        double controlShieldShare = ToolBaseStatCatalog.controlDurabilityProtectionShare(stack);
        int controlShieldCost = ToolBaseStatCatalog.controlDurabilityProtectionFeCost(stack);
        if (controlShieldShare > 0.0 && controlShieldCost > 0) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.modular_tool.control_shield",
                    percent(controlShieldShare),
                    controlShieldCost
            ).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("rngtech.tooltip.modular_tool.fallback").withStyle(ChatFormatting.GRAY));
    }

    private void chargeForBrokenBlock(ItemStack stack, ServerLevel level, ServerPlayer player, BlockState state, BlockPos pos) {
        int feCost = ToolBaseStatCatalog.feUsage(stack);
        int requiredExtraEnergy = EXTRA_BLOCK_FE_SURCHARGE.get();
        feCost += requiredExtraEnergy;
        if (state.is(FieldToolTags.ORE_BURST_TARGETS) && ToolBaseStatCatalog.hasOreBurstMode(stack)) {
            int burstCost = ToolBaseStatCatalog.oreBurstFeUsage(stack);
            if (canSpendEnergy(stack, requiredExtraEnergy + burstCost)) {
                feCost += burstCost;
            }
        }
        chargeToolUse(stack, level, player, feCost);
    }

    private void chargeToolUse(ItemStack stack, ServerLevel level, ServerPlayer player, int feCost) {
        if (player.getAbilities().instabuild) {
            return;
        }
        refreshMaxDamage(stack);
        if (isBroken(stack)) {
            return;
        }
        if (avoidsToolCost(stack, level)) {
            return;
        }
        spendEnergy(stack, feCost);
        double durabilityCost = durabilityCostAfterControlProtection(stack);
        int wholeCost = (int) Math.ceil(durabilityCost);
        if (wholeCost <= 0) {
            return;
        }
        int processedCost = EnchantmentHelper.processDurabilityChange(level, stack, wholeCost);
        if (processedCost <= 0) {
            return;
        }
        damageByAmount(stack, durabilityCost * processedCost / wholeCost);
    }

    private boolean avoidsToolCost(ItemStack stack, ServerLevel level) {
        double chance = ToolBaseStatCatalog.stabilityCostAvoidanceChance(stack);
        return chance > 0.0 && level.random.nextDouble() < chance;
    }

    private double durabilityCostAfterControlProtection(ItemStack stack) {
        double protectionShare = ToolBaseStatCatalog.controlDurabilityProtectionShare(stack);
        if (protectionShare <= 0.0) {
            return 1.0;
        }
        int feCost = ToolBaseStatCatalog.controlDurabilityProtectionFeCost(stack);
        if (feCost > 0 && canSpendEnergy(stack, feCost) && spendEnergy(stack, feCost) >= feCost) {
            return Math.max(0.0, 1.0 - protectionShare);
        }
        return 1.0;
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0);
    }

    private void selfRepair(ItemStack stack, Player player, int slotId, boolean isSelected) {
        if (!isSelfRepairActive(player, stack, slotId, isSelected)
                || isToolInUse(player, stack, isSelected)
                || player.tickCount % SELF_REPAIR_INTERVAL_TICKS != 0
                || !stack.isDamaged()) {
            return;
        }
        int repairAmount = ToolBaseStatCatalog.selfRepairAmount(stack);
        if (repairAmount <= 0) {
            return;
        }
        repairWithoutBreaking(stack, repairAmount);
    }

    private boolean isSelfRepairActive(Player player, ItemStack stack, int slotId, boolean isSelected) {
        return isSelected || (slotId >= 0 && slotId < 9) || player.getOffhandItem() == stack;
    }

    private boolean isToolInUse(Player player, ItemStack stack, boolean isSelected) {
        if (player.isUsingItem() && player.getUseItem() == stack) {
            return true;
        }
        ActiveUse activeUse = ACTIVE_USES.get(player.getUUID());
        if (activeUse == null) {
            return false;
        }
        if (player.tickCount - activeUse.tick() > ACTIVE_USE_GRACE_TICKS) {
            ACTIVE_USES.remove(player.getUUID());
            return false;
        }
        return activeUse.handMatches(player, stack, isSelected);
    }

    private void breakExtraBlocks(ItemStack stack, ServerLevel level, ServerPlayer player, BlockState originalState, BlockPos origin) {
        if (!hasUsableAssembly(stack)) {
            return;
        }
        switch (family) {
            case PICK -> breakConnectedVein(stack, level, player, originalState, origin);
            case HAMMER, DIGGER -> breakArea(stack, level, player, originalState, origin);
            case TREEFELLER -> breakConnectedLogs(stack, level, player, originalState, origin);
            default -> {
            }
        }
    }

    private void breakArea(ItemStack stack, ServerLevel level, ServerPlayer player, BlockState originalState, BlockPos origin) {
        if (!family.validTarget(originalState)) {
            return;
        }
        int width = ToolBaseStatCatalog.areaWidth(stack);
        int height = ToolBaseStatCatalog.areaHeight(stack);
        Direction hitFace = consumeAreaHitFace(player, origin);
        List<BlockPos> targets = areaTargets(player, origin, hitFace, width, height);
        breakTargets(stack, level, player, targets);
    }

    private void breakConnectedVein(ItemStack stack, ServerLevel level, ServerPlayer player, BlockState originalState, BlockPos origin) {
        int limit = ToolBaseStatCatalog.veinMineLimit(stack);
        if (limit <= 1 || player.isShiftKeyDown() || !originalState.is(FieldToolTags.ORE_BURST_TARGETS)) {
            return;
        }
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> targets = new ArrayList<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && targets.size() < limit - 1) {
            BlockPos current = queue.remove();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!seen.add(next)) {
                    continue;
                }
                BlockState nextState = level.getBlockState(next);
                if (!sameVeinBlock(originalState, nextState)
                        || !canBreakTarget(stack, level, player, nextState, next, true)) {
                    continue;
                }
                targets.add(next);
                if (targets.size() >= limit - 1) {
                    break;
                }
                queue.add(next);
            }
        }
        breakTargets(stack, level, player, targets, ToolBaseStatCatalog.veinMineFeUsage(stack));
    }

    private void breakConnectedLogs(
            ItemStack stack,
            ServerLevel level,
            ServerPlayer player,
            BlockState originalState,
            BlockPos origin
    ) {
        if (!originalState.is(BlockTags.LOGS)) {
            return;
        }
        int limit = ToolBaseStatCatalog.treeFellLimit(stack);
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> targets = new ArrayList<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && targets.size() < limit - 1) {
            BlockPos current = queue.remove();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = current.offset(dx, dy, dz);
                        if (!seen.add(next) || !level.getBlockState(next).is(BlockTags.LOGS)) {
                            continue;
                        }
                        targets.add(next);
                        if (targets.size() >= limit - 1) {
                            break;
                        }
                        queue.add(next);
                    }
                    if (targets.size() >= limit - 1) {
                        break;
                    }
                }
                if (targets.size() >= limit - 1) {
                    break;
                }
            }
        }
        breakTargets(stack, level, player, targets, 0, false);
    }

    private void breakTargets(ItemStack stack, ServerLevel level, ServerPlayer player, List<BlockPos> targets) {
        breakTargets(stack, level, player, targets, 0, true);
    }

    private void breakTargets(ItemStack stack, ServerLevel level, ServerPlayer player, List<BlockPos> targets, int extraFeCost) {
        breakTargets(stack, level, player, targets, extraFeCost, true);
    }

    private void breakTargets(
            ItemStack stack,
            ServerLevel level,
            ServerPlayer player,
            List<BlockPos> targets,
            int extraFeCost,
            boolean enforceInteractionRange
    ) {
        EXTRA_BREAKING.set(true);
        try {
            for (BlockPos target : targets) {
                if (stack.isEmpty() || isBroken(stack)) {
                    break;
                }
                BlockState targetState = level.getBlockState(target);
                if (canBreakTarget(stack, level, player, targetState, target, enforceInteractionRange)) {
                    if (extraFeCost > 0 && !canSpendEnergy(stack, extraFeCost)) {
                        break;
                    }
                    EXTRA_BLOCK_FE_SURCHARGE.set(extraFeCost);
                    try {
                        player.gameMode.destroyBlock(target);
                    } finally {
                        EXTRA_BLOCK_FE_SURCHARGE.set(0);
                    }
                }
            }
        } finally {
            EXTRA_BREAKING.set(false);
            EXTRA_BLOCK_FE_SURCHARGE.set(0);
        }
    }

    private boolean canBreakTarget(
            ItemStack stack,
            ServerLevel level,
            ServerPlayer player,
            BlockState state,
            BlockPos pos,
            boolean enforceInteractionRange
    ) {
        return !state.isAir()
                && state.getDestroySpeed(level, pos) >= 0.0F
                && family.validTarget(state)
                && isCorrectToolForDrops(stack, state)
                && player.mayBuild()
                && (!enforceInteractionRange || player.canInteractWithBlock(pos, player.blockInteractionRange()));
    }

    private boolean sameVeinBlock(BlockState originalState, BlockState candidateState) {
        return candidateState.is(FieldToolTags.ORE_BURST_TARGETS) && candidateState.is(originalState.getBlock());
    }

    private Direction consumeAreaHitFace(ServerPlayer player, BlockPos origin) {
        AreaHit hit = AREA_HITS.remove(player.getUUID());
        return hit != null && hit.pos().equals(origin) ? hit.face() : null;
    }

    private List<BlockPos> areaTargets(ServerPlayer player, BlockPos origin, Direction hitFace, int width, int height) {
        Direction.Axis hitAxis = hitFace == null ? null : hitFace.getAxis();
        Direction facing = player.getDirection();
        boolean horizontalPlane = hitAxis == Direction.Axis.Y || (hitAxis == null && Math.abs(player.getXRot()) > 60.0F);
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        List<BlockPos> targets = new ArrayList<>(width * height - 1);
        for (int a = -halfWidth; a <= halfWidth; a++) {
            for (int b = -halfHeight; b <= halfHeight; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                BlockPos target;
                if (horizontalPlane) {
                    target = origin.offset(a, 0, b);
                } else if (hitAxis == Direction.Axis.Z || (hitAxis == null && (facing == Direction.NORTH || facing == Direction.SOUTH))) {
                    target = origin.offset(a, b, 0);
                } else {
                    target = origin.offset(0, b, a);
                }
                targets.add(target);
            }
        }
        return targets;
    }

    private static boolean hasUsableAssembly(ItemStack stack) {
        return hasValidAssembly(stack) && !isBroken(stack);
    }

    private record AreaHit(BlockPos pos, Direction face) {
    }

    private record ActiveUse(InteractionHand hand, int tick) {
        private boolean handMatches(Player player, ItemStack stack, boolean isSelected) {
            return switch (hand) {
                case MAIN_HAND -> isSelected && player.getMainHandItem() == stack;
                case OFF_HAND -> player.getOffhandItem() == stack;
            };
        }
    }

    private void transferEnergyFromInventory(ItemStack stack, Player player) {
        IEnergyStorage toolStorage = energyStorage(stack);
        if (toolStorage == null || !toolStorage.canReceive()) {
            return;
        }
        int support = ToolBaseStatCatalog.batterySupport(stack);
        int transferLimit = Math.max(0, ToolBaseStatCatalog.feTransfer(stack));
        if (transferLimit <= 0) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            int orderedSlot = slot < 9 ? slot : slot;
            ItemStack source = player.getInventory().items.get(orderedSlot);
            if (!(source.getItem() instanceof BatteryCellItem cell) || cell.material().stage() > support) {
                continue;
            }
            int remaining = toolStorage.getMaxEnergyStored() - toolStorage.getEnergyStored();
            if (remaining <= 0) {
                return;
            }
            IEnergyStorage sourceStorage = BatteryCellItem.energyStorage(source);
            if (sourceStorage == null || !sourceStorage.canExtract()) {
                continue;
            }
            int requested = Math.min(remaining, transferLimit);
            int extracted = sourceStorage.extractEnergy(requested, true);
            if (extracted <= 0) {
                continue;
            }
            int received = toolStorage.receiveEnergy(extracted, false);
            if (received > 0) {
                sourceStorage.extractEnergy(received, false);
            }
        }
    }

    private static Tier tierForMiningLevel(int level) {
        if (level <= 0) {
            return Tiers.WOOD;
        }
        if (level == 1) {
            return Tiers.STONE;
        }
        if (level == 2) {
            return Tiers.IRON;
        }
        if (level == 3) {
            return Tiers.DIAMOND;
        }
        return Tiers.NETHERITE;
    }

    private static final class InstalledCellEnergyStorage implements IEnergyStorage {
        private final ItemStack toolStack;

        private InstalledCellEnergyStorage(ItemStack toolStack) {
            this.toolStack = toolStack;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            IEnergyStorage cellStorage = cellStorage();
            if (cellStorage == null || !cellStorage.canReceive() || toReceive <= 0) {
                return 0;
            }
            int limit = Math.min(toReceive, ToolBaseStatCatalog.feTransfer(toolStack));
            int received = cellStorage.receiveEnergy(limit, simulate);
            if (!simulate && received > 0) {
                saveCell();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            IEnergyStorage cellStorage = cellStorage();
            if (cellStorage == null || !cellStorage.canExtract() || toExtract <= 0) {
                return 0;
            }
            int limit = Math.min(toExtract, ToolBaseStatCatalog.feTransfer(toolStack));
            int extracted = cellStorage.extractEnergy(limit, simulate);
            if (!simulate && extracted > 0) {
                saveCell();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return BatteryCellItem.energyStored(cell());
        }

        @Override
        public int getMaxEnergyStored() {
            return BatteryCellItem.energyCapacity(cell());
        }

        @Override
        public boolean canExtract() {
            return BatteryCellItem.maxOutput(cell()) > 0 && ToolBaseStatCatalog.feTransfer(toolStack) > 0;
        }

        @Override
        public boolean canReceive() {
            return BatteryCellItem.maxInput(cell()) > 0 && ToolBaseStatCatalog.feTransfer(toolStack) > 0;
        }

        private IEnergyStorage cellStorage() {
            ItemStack cell = cell();
            return cell.isEmpty() ? null : BatteryCellItem.energyStorage(cell);
        }

        private ItemStack cell() {
            return assembly(toolStack).batteryCell();
        }

        private void saveCell() {
            FieldToolAssembly assembly = assembly(toolStack);
            toolStack.set(ModDataComponents.FIELD_TOOL_ASSEMBLY.get(), assembly.withBatteryCell(cell()));
        }
    }
}
