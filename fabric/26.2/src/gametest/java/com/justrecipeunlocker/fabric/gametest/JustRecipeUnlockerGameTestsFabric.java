package com.justrecipeunlocker.fabric.gametest;

import com.justrecipeunlocker.gametest.RecipeUnlockServiceGameTestLogic;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric-side registration of the shared {@link RecipeUnlockServiceGameTestLogic} test bodies.
 *
 * Minecraft 26.2 (1.21.5+) replaced the old {@code FabricGameTest} interface with Fabric API's
 * own {@code @GameTest} annotation ({@code net.fabricmc.fabric.api.gametest.v1.GameTest}), which
 * takes a {@code structure} id string and {@code maxTicks} instead of vanilla's
 * {@code template}/{@code timeoutTicks}.
 */
public final class JustRecipeUnlockerGameTestsFabric {

    private static final String STRUCTURE = "justrecipeunlocker:empty";

    @GameTest(structure = STRUCTURE, maxTicks = 100)
    public void testResolveExcludesSpecialRecipes(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_SPECIAL_RECIPES.accept(helper);
    }

    @GameTest(structure = STRUCTURE, maxTicks = 100)
    public void testResolveIncludesOrdinaryRecipes(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_INCLUDES_ORDINARY_RECIPES.accept(helper);
    }

    @GameTest(structure = STRUCTURE, maxTicks = 100)
    public void testResolveExcludesRecipeId(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_RECIPE_ID.accept(helper);
    }

    @GameTest(structure = STRUCTURE, maxTicks = 100)
    public void testResolveExcludesNamespace(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_NAMESPACE.accept(helper);
    }

    @GameTest(structure = STRUCTURE, maxTicks = 100)
    public void testUnlockVisibleAddsToRecipeBook(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_UNLOCK_VISIBLE_ADDS_TO_RECIPE_BOOK.accept(helper);
    }

    @GameTest(structure = STRUCTURE, maxTicks = 100)
    public void testUnlockSilentAddsToRecipeBookAndIsIdempotent(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_UNLOCK_SILENT_ADDS_TO_RECIPE_BOOK_AND_IS_IDEMPOTENT.accept(helper);
    }
}
