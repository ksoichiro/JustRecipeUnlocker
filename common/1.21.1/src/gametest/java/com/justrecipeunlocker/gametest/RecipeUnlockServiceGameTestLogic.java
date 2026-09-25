package com.justrecipeunlocker.gametest;

import com.justrecipeunlocker.config.JustRecipeUnlockerConfig;
import com.justrecipeunlocker.recipe.RecipeUnlockService;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;
import java.util.function.Consumer;

/**
 * Platform-agnostic GameTest bodies for {@link RecipeUnlockService}, exercised with real
 * {@link RecipeManager}/{@link ServerPlayer} objects obtained from a running GameTest server.
 * Only vanilla Minecraft classes are referenced here so this class can be shared verbatim by
 * both the Fabric and NeoForge GameTest registrations.
 */
public final class RecipeUnlockServiceGameTestLogic {

    private static final ResourceLocation OAK_PLANKS = ResourceLocation.withDefaultNamespace("oak_planks");
    private static final ResourceLocation STICK = ResourceLocation.withDefaultNamespace("stick");

    private RecipeUnlockServiceGameTestLogic() {
    }

    private static JustRecipeUnlockerConfig config(List<String> excludedNamespaces, List<String> excludedRecipeIds) {
        return new JustRecipeUnlockerConfig(true, excludedNamespaces, excludedRecipeIds, false, false);
    }

    public static final Consumer<GameTestHelper> TEST_RESOLVE_EXCLUDES_SPECIAL_RECIPES = helper -> {
        RecipeManager recipeManager = helper.getLevel().getServer().getRecipeManager();
        RecipeUnlockService service = new RecipeUnlockService(recipeManager);
        List<ResourceLocation> resolved = service.resolveUnlockableRecipeIds(config(List.of(), List.of()));

        boolean anySpecialRecipeExists = recipeManager.getRecipes().stream().anyMatch(holder -> holder.value().isSpecial());
        if (!anySpecialRecipeExists) {
            helper.fail("Expected at least one special recipe in the registry to validate exclusion");
            return;
        }
        boolean specialRecipeLeaked = recipeManager.getRecipes().stream()
                .filter(holder -> holder.value().isSpecial())
                .anyMatch(holder -> resolved.contains(holder.id()));
        if (specialRecipeLeaked) {
            helper.fail("resolveUnlockableRecipeIds must not include special recipes");
            return;
        }
        helper.succeed();
    };

    public static final Consumer<GameTestHelper> TEST_RESOLVE_INCLUDES_ORDINARY_RECIPES = helper -> {
        RecipeManager recipeManager = helper.getLevel().getServer().getRecipeManager();
        RecipeUnlockService service = new RecipeUnlockService(recipeManager);
        List<ResourceLocation> resolved = service.resolveUnlockableRecipeIds(config(List.of(), List.of()));

        if (!resolved.contains(OAK_PLANKS) || !resolved.contains(STICK)) {
            helper.fail("Expected ordinary recipes " + OAK_PLANKS + " and " + STICK + " to be resolved, got " + resolved.size() + " recipes");
            return;
        }
        helper.succeed();
    };

    public static final Consumer<GameTestHelper> TEST_RESOLVE_EXCLUDES_RECIPE_ID = helper -> {
        RecipeManager recipeManager = helper.getLevel().getServer().getRecipeManager();
        RecipeUnlockService service = new RecipeUnlockService(recipeManager);
        List<ResourceLocation> resolved = service.resolveUnlockableRecipeIds(config(List.of(), List.of(STICK.toString())));

        if (resolved.contains(STICK)) {
            helper.fail("Expected " + STICK + " to be excluded via excludedRecipeIds");
            return;
        }
        if (!resolved.contains(OAK_PLANKS)) {
            helper.fail("Expected " + OAK_PLANKS + " to remain resolved when only " + STICK + " is excluded");
            return;
        }
        helper.succeed();
    };

    public static final Consumer<GameTestHelper> TEST_RESOLVE_EXCLUDES_NAMESPACE = helper -> {
        RecipeManager recipeManager = helper.getLevel().getServer().getRecipeManager();
        RecipeUnlockService service = new RecipeUnlockService(recipeManager);
        List<ResourceLocation> resolved = service.resolveUnlockableRecipeIds(config(List.of("minecraft"), List.of()));

        // In a vanilla-only GameTest environment every recipe lives under the "minecraft"
        // namespace, so excluding that namespace should drop the resolved list to empty.
        if (!resolved.isEmpty()) {
            helper.fail("Expected excludedNamespaces=[minecraft] to exclude all recipes, but " + resolved.size() + " remained");
            return;
        }
        helper.succeed();
    };

    public static final Consumer<GameTestHelper> TEST_UNLOCK_VISIBLE_ADDS_TO_RECIPE_BOOK = helper -> {
        RecipeManager recipeManager = helper.getLevel().getServer().getRecipeManager();
        RecipeUnlockService service = new RecipeUnlockService(recipeManager);
        // Deprecated-for-removal in vanilla but still the supported way to obtain a real,
        // fully-placed ServerPlayer (with a real ServerRecipeBook) inside a GameTest.
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<ResourceLocation> ids = List.of(OAK_PLANKS, STICK);

        service.unlock(player, ids, false);

        for (ResourceLocation id : ids) {
            if (!player.getRecipeBook().contains(id)) {
                helper.fail("Expected " + id + " to be present in the recipe book after unlock(suppressToast=false)");
                return;
            }
        }
        helper.succeed();
    };

    public static final Consumer<GameTestHelper> TEST_UNLOCK_SILENT_ADDS_TO_RECIPE_BOOK_AND_IS_IDEMPOTENT = helper -> {
        RecipeManager recipeManager = helper.getLevel().getServer().getRecipeManager();
        RecipeUnlockService service = new RecipeUnlockService(recipeManager);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<ResourceLocation> ids = List.of(OAK_PLANKS, STICK);

        service.unlock(player, ids, true);

        for (ResourceLocation id : ids) {
            if (!player.getRecipeBook().contains(id)) {
                helper.fail("Expected " + id + " to be present in the recipe book after unlock(suppressToast=true)");
                return;
            }
        }

        // Re-unlocking the same IDs must be a safe no-op: no exception (including from the
        // now-real CriteriaTriggers.RECIPE_UNLOCKED.trigger call) and no duplicate entries.
        try {
            service.unlock(player, ids, true);
        } catch (RuntimeException e) {
            helper.fail("Second unlock(suppressToast=true) call threw: " + e);
            return;
        }

        for (ResourceLocation id : ids) {
            if (!player.getRecipeBook().contains(id)) {
                helper.fail("Expected " + id + " to remain present after repeated unlock(suppressToast=true)");
                return;
            }
        }
        helper.succeed();
    };
}
