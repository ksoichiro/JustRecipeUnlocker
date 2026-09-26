package com.justrecipeunlocker.fabric;

import com.justrecipeunlocker.config.ConfigLoader;
import com.justrecipeunlocker.config.JustRecipeUnlockerConfig;
import com.justrecipeunlocker.recipe.RecipeUnlockService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

public final class JustRecipeUnlockerFabric implements ModInitializer {

    public static final String MOD_ID = "justrecipeunlocker";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private JustRecipeUnlockerConfig config;

    // Recipe unlocks pending a real (not same-stack-reentrant) tick where the joining player is
    // genuinely registered in the player list. See scheduleUnlockOnceJoined for why this can't
    // just be a recursive server.execute() call.
    private final Queue<PendingJoinUnlock> pendingJoinUnlocks = new ArrayDeque<>();

    private record PendingJoinUnlock(ServerPlayer player, int attempt) {
    }

    // On this version, Fabric's JOIN event fires strictly before PlayerList registers the
    // joining player, and that registration only completes on a later real server tick, not
    // synchronously within the JOIN callback's call stack. server.execute(), when called from
    // the server thread (which the JOIN callback runs on), executes its task inline instead of
    // deferring it, so a naive recursive server.execute() retry loop runs all of its attempts
    // in zero elapsed real time and can never observe the registration (confirmed via real
    // dedicated-server + client testing: debug logging showed 100 "attempts" all timestamped
    // within the same tick, playerList still empty throughout). Queuing the retry on
    // ServerTickEvents.END_SERVER_TICK instead guarantees each attempt runs on a genuinely
    // separate tick.
    private static final int MAX_JOIN_WAIT_TICKS = 100;

    @Override
    public void onInitialize() {
        config = ConfigLoader.load(FabricLoader.getInstance().getConfigDir());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!config.unlockOnJoin()) {
                return;
            }
            pendingJoinUnlocks.add(new PendingJoinUnlock(handler.getPlayer(), 0));
        });

        ServerTickEvents.END_SERVER_TICK.register(this::processPendingJoinUnlocks);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = Commands.literal("justrecipeunlocker")
                    .requires(source -> source.hasPermission(2))
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
                    .requires(source -> source.hasPermission(2))
                    .redirect(dispatcher.getRoot().getChild("justrecipeunlocker")));
        });

        LOGGER.info("Just Recipe Unlocker initialized");
    }

    private void processPendingJoinUnlocks(MinecraftServer server) {
        int pending = pendingJoinUnlocks.size();
        for (int i = 0; i < pending; i++) {
            PendingJoinUnlock entry = pendingJoinUnlocks.poll();
            if (entry == null) {
                break;
            }
            ServerPlayer player = entry.player();
            if (player.isRemoved()) {
                continue;
            }
            if (!server.getPlayerList().getPlayers().contains(player)) {
                if (entry.attempt() >= MAX_JOIN_WAIT_TICKS) {
                    LOGGER.warn("Gave up waiting for {} to finish joining after {} ticks; skipping unlock-on-join",
                            player.getGameProfile().name(), entry.attempt());
                    continue;
                }
                pendingJoinUnlocks.add(new PendingJoinUnlock(player, entry.attempt() + 1));
                continue;
            }
            RecipeUnlockService service = new RecipeUnlockService(server.getRecipeManager());
            List<ResourceKey<Recipe<?>>> recipeIds = service.resolveUnlockableRecipeIds(config);
            service.unlock(player, recipeIds, config.suppressRecipeToast());
        }
    }
}
