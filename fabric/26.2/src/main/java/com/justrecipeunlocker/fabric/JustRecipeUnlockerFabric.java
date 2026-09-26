package com.justrecipeunlocker.fabric;

import com.justrecipeunlocker.config.ConfigLoader;
import com.justrecipeunlocker.config.JustRecipeUnlockerConfig;
import com.justrecipeunlocker.recipe.RecipeUnlockService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

public final class JustRecipeUnlockerFabric implements ModInitializer {

    public static final String MOD_ID = "justrecipeunlocker";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private JustRecipeUnlockerConfig config;

    @Override
    public void onInitialize() {
        config = ConfigLoader.load(FabricLoader.getInstance().getConfigDir());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!config.unlockOnJoin()) {
                return;
            }
            ServerPlayer player = handler.getPlayer();
            scheduleUnlockOnceJoined(server, player, 0);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = Commands.literal("justrecipeunlocker")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("unlockAll").executes(ctx -> {
                        var server = ctx.getSource().getServer();
                        RecipeUnlockService service = new RecipeUnlockService(server.getRecipeManager());
                        List<ResourceKey<Recipe<?>>> recipeIds = service.resolveUnlockableRecipeIds(config);
                        Collection<ServerPlayer> targets = server.getPlayerList().getPlayers();
                        for (ServerPlayer player : targets) {
                            service.unlock(player, recipeIds, config.suppressRecipeToast());
                        }
                        ctx.getSource().sendSuccess(
                                () -> Component.literal("Unlocked recipes for " + targets.size() + " player(s)."), true);
                        return targets.size();
                    }))
                    .then(Commands.literal("unlock")
                            .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> {
                                Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                RecipeUnlockService service =
                                        new RecipeUnlockService(ctx.getSource().getServer().getRecipeManager());
                                List<ResourceKey<Recipe<?>>> recipeIds = service.resolveUnlockableRecipeIds(config);
                                for (ServerPlayer player : targets) {
                                    service.unlock(player, recipeIds, config.suppressRecipeToast());
                                }
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("Unlocked recipes for " + targets.size() + " player(s)."), true);
                                return targets.size();
                            })));

            dispatcher.register(root);
            dispatcher.register(Commands.literal("jru")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .redirect(dispatcher.getRoot().getChild("justrecipeunlocker")));
        });

        LOGGER.info("Just Recipe Unlocker initialized");
    }

    // Fabric's JOIN event fires before PlayerList has actually registered the joining player
    // on this Minecraft version (confirmed empirically: getPlayerList().getPlayers() is still
    // empty one server.execute() tick after JOIN fires). Poll each tick until the player is
    // genuinely registered (which also guarantees we run after vanilla's own initial
    // recipe-book sync, preserving packet order for the "new recipes unlocked" toast) or bail
    // out if they disconnect first.
    private static final int MAX_JOIN_WAIT_TICKS = 100;

    private void scheduleUnlockOnceJoined(MinecraftServer server, ServerPlayer player, int attempt) {
        server.execute(() -> {
            if (player.isRemoved()) {
                return;
            }
            if (!server.getPlayerList().getPlayers().contains(player)) {
                if (attempt >= MAX_JOIN_WAIT_TICKS) {
                    LOGGER.warn("Gave up waiting for {} to finish joining after {} ticks; skipping unlock-on-join",
                            player.getGameProfile().name(), attempt);
                    return;
                }
                scheduleUnlockOnceJoined(server, player, attempt + 1);
                return;
            }
            RecipeUnlockService service = new RecipeUnlockService(server.getRecipeManager());
            List<ResourceKey<Recipe<?>>> recipeIds = service.resolveUnlockableRecipeIds(config);
            service.unlock(player, recipeIds, config.suppressRecipeToast());
        });
    }
}
