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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
        config = ConfigLoader.load(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!config.unlockOnJoin()) {
                return;
            }
            ServerPlayer player = handler.getPlayer();
            RecipeUnlockService service = new RecipeUnlockService(server.getRecipeManager());
            List<ResourceLocation> recipeIds = service.resolveUnlockableRecipeIds(config);
            service.unlock(player, recipeIds, config.suppressRecipeToast());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = Commands.literal("justrecipeunlocker")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("unlockAll").executes(ctx -> {
                        var server = ctx.getSource().getServer();
                        RecipeUnlockService service = new RecipeUnlockService(server.getRecipeManager());
                        List<ResourceLocation> recipeIds = service.resolveUnlockableRecipeIds(config);
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
                                List<ResourceLocation> recipeIds = service.resolveUnlockableRecipeIds(config);
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
}
