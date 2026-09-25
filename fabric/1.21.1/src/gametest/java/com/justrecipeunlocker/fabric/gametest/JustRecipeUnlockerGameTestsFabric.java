package com.justrecipeunlocker.fabric.gametest;

import com.justrecipeunlocker.gametest.RecipeUnlockServiceGameTestLogic;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric-side registration of the shared {@link RecipeUnlockServiceGameTestLogic} test bodies.
 */
public final class JustRecipeUnlockerGameTestsFabric implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testResolveExcludesSpecialRecipes(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_SPECIAL_RECIPES.accept(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testResolveIncludesOrdinaryRecipes(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_INCLUDES_ORDINARY_RECIPES.accept(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testResolveExcludesRecipeId(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_RECIPE_ID.accept(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testResolveExcludesNamespace(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_NAMESPACE.accept(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testUnlockVisibleAddsToRecipeBook(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_UNLOCK_VISIBLE_ADDS_TO_RECIPE_BOOK.accept(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void testUnlockSilentAddsToRecipeBookAndIsIdempotent(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_UNLOCK_SILENT_ADDS_TO_RECIPE_BOOK_AND_IS_IDEMPOTENT.accept(helper);
    }
}
