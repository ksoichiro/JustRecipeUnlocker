package com.justrecipeunlocker.neoforge;

import com.justrecipeunlocker.config.ConfigLoader;
import com.justrecipeunlocker.config.JustRecipeUnlockerConfig;
import com.justrecipeunlocker.neoforge.gametest.JustRecipeUnlockerGameTestsNeoForge;
import com.justrecipeunlocker.recipe.RecipeUnlockService;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

@Mod(JustRecipeUnlockerNeoForge.MOD_ID)
public final class JustRecipeUnlockerNeoForge {

    public static final String MOD_ID = "justrecipeunlocker";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final JustRecipeUnlockerConfig config;

    public JustRecipeUnlockerNeoForge(IEventBus modEventBus) {
        config = ConfigLoader.load(FMLPaths.CONFIGDIR.get());
        // GameTest became registry-based in 1.21.5+ and needs an explicit DeferredRegister
        // bound to the mod event bus (the old @GameTestHolder annotation scanning is gone).
        JustRecipeUnlockerGameTestsNeoForge.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        LOGGER.info("Just Recipe Unlocker initialized");
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!config.unlockOnJoin() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RecipeUnlockService service = new RecipeUnlockService(player.level().getServer().getRecipeManager());
        List<ResourceKey<Recipe<?>>> recipeIds = service.resolveUnlockableRecipeIds(config);
        service.unlock(player, recipeIds, config.suppressRecipeToast());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
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
    }
}
