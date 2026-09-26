package com.justrecipeunlocker.neoforge.gametest;

import com.justrecipeunlocker.gametest.RecipeUnlockServiceGameTestLogic;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge-side registration of the shared {@link RecipeUnlockServiceGameTestLogic} test bodies.
 * All test methods resolve to the same "justrecipeunlocker:empty" structure template (see
 * common/1.21.3/src/gametest/resources/data/justrecipeunlocker/structure/empty.nbt).
 */
@GameTestHolder("justrecipeunlocker")
@PrefixGameTestTemplate(false)
public final class JustRecipeUnlockerGameTestsNeoForge {

    private JustRecipeUnlockerGameTestsNeoForge() {
    }

    // Registered explicitly on the mod event bus from the mod constructor instead of via
    // @EventBusSubscriber: that annotation's automatic bus detection for RegisterGameTestsEvent
    // (a mod-bus-only event) throws "argument not valid for this bus" on this NeoForge version,
    // even though the same @EventBusSubscriber-based registration works on 1.21.1.
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(JustRecipeUnlockerGameTestsNeoForge::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        event.register(JustRecipeUnlockerGameTestsNeoForge.class);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void testResolveExcludesSpecialRecipes(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_SPECIAL_RECIPES.accept(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void testResolveIncludesOrdinaryRecipes(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_INCLUDES_ORDINARY_RECIPES.accept(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void testResolveExcludesRecipeId(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_RECIPE_ID.accept(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void testResolveExcludesNamespace(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_NAMESPACE.accept(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void testUnlockVisibleAddsToRecipeBook(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_UNLOCK_VISIBLE_ADDS_TO_RECIPE_BOOK.accept(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void testUnlockSilentAddsToRecipeBookAndIsIdempotent(GameTestHelper helper) {
        RecipeUnlockServiceGameTestLogic.TEST_UNLOCK_SILENT_ADDS_TO_RECIPE_BOOK_AND_IS_IDEMPOTENT.accept(helper);
    }
}
