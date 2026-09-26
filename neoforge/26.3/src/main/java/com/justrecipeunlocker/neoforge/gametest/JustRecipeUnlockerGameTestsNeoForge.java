package com.justrecipeunlocker.neoforge.gametest;

import com.justrecipeunlocker.gametest.RecipeUnlockServiceGameTestLogic;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * NeoForge-side registration of the shared {@link RecipeUnlockServiceGameTestLogic} test bodies.
 * All test methods resolve to the same "justrecipeunlocker:empty" structure template (see
 * common/26.3/src/gametest/resources/data/justrecipeunlocker/structure/empty.nbt).
 *
 * The GameTest framework became registry-based starting with Minecraft 1.21.5: test functions
 * are registered via a {@link DeferredRegister} to {@code BuiltInRegistries.TEST_FUNCTION}, and
 * test instances are then registered via {@link RegisterGameTestsEvent}, replacing the old
 * {@code @GameTest}/{@code @GameTestHolder} annotation-based discovery.
 */
public final class JustRecipeUnlockerGameTestsNeoForge {

    private static final String MOD_ID = "justrecipeunlocker";
    private static final Identifier EMPTY_STRUCTURE = Identifier.fromNamespaceAndPath(MOD_ID, "empty");

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, MOD_ID);

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> TESTS =
            new LinkedHashMap<>();

    private static boolean testInstancesRegistered = false;

    static {
        register("test_resolve_excludes_special_recipes",
                RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_SPECIAL_RECIPES);
        register("test_resolve_includes_ordinary_recipes",
                RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_INCLUDES_ORDINARY_RECIPES);
        register("test_resolve_excludes_recipe_id",
                RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_RECIPE_ID);
        register("test_resolve_excludes_namespace",
                RecipeUnlockServiceGameTestLogic.TEST_RESOLVE_EXCLUDES_NAMESPACE);
        register("test_unlock_visible_adds_to_recipe_book",
                RecipeUnlockServiceGameTestLogic.TEST_UNLOCK_VISIBLE_ADDS_TO_RECIPE_BOOK);
        register("test_unlock_silent_adds_to_recipe_book_and_is_idempotent",
                RecipeUnlockServiceGameTestLogic.TEST_UNLOCK_SILENT_ADDS_TO_RECIPE_BOOK_AND_IS_IDEMPOTENT);
    }

    private JustRecipeUnlockerGameTestsNeoForge() {
    }

    private static void register(String name, Consumer<GameTestHelper> test) {
        TESTS.put(name, TEST_FUNCTIONS.register(name, () -> test));
    }

    /** Called from the mod constructor to wire the test-function registry and event listener. */
    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(JustRecipeUnlockerGameTestsNeoForge::onRegisterGameTests);
    }

    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        // RegisterGameTestsEvent may fire more than once (data loading + server startup); only
        // register test instances on the first invocation while registries are still writable.
        if (testInstancesRegistered) {
            return;
        }
        testInstancesRegistered = true;

        Holder<TestEnvironmentDefinition<?>> defaultEnv =
                event.registerEnvironment(Identifier.fromNamespaceAndPath(MOD_ID, "default"));

        for (var entry : TESTS.entrySet()) {
            var holder = entry.getValue();
            if (!holder.isBound()) {
                continue;
            }
            var testData = new TestData<>(defaultEnv, EMPTY_STRUCTURE, 100, 0, true);
            var instance = new FunctionGameTestInstance(holder.getKey(), testData);
            event.registerTest(holder.getId(), instance);
        }
    }
}
