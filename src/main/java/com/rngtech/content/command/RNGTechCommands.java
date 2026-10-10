package com.rngtech.content.command;

import com.rngtech.RNGTech;
import com.rngtech.rpg.unique.UniqueCatalog;
import com.rngtech.rpg.unique.UniqueDefinition;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

/** {@code /rngtech unique give <players> <id>}: gives each player an unidentified copy, as a quest reward would. */
public final class RNGTechCommands {
    private static final DynamicCommandExceptionType UNKNOWN_UNIQUE =
            new DynamicCommandExceptionType(id -> Component.translatable("rngtech.command.unique.unknown", id));

    private RNGTechCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(RNGTech.MOD_ID)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("unique")
                        .then(Commands.literal("give")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("unique", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        UniqueCatalog.all().stream().map(unique -> RNGTech.id(unique.id())),
                                                        builder
                                                ))
                                                .executes(RNGTechCommands::giveUnique))))));
    }

    private static int giveUnique(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "unique");
        UniqueDefinition unique = RNGTech.MOD_ID.equals(id.getNamespace()) ? UniqueCatalog.get(id.getPath()) : null;
        if (unique == null) {
            throw UNKNOWN_UNIQUE.create(id.toString());
        }
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : targets) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        context.getSource().sendSuccess(() -> Component.translatable(
                "rngtech.command.unique.given",
                Component.translatable(unique.translationKey()),
                targets.size()
        ), true);
        return targets.size();
    }
}
