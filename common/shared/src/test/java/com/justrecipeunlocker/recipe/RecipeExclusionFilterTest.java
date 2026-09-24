package com.justrecipeunlocker.recipe;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeExclusionFilterTest {

    @Test
    void excludesRecipeInExcludedNamespace() {
        var filter = new RecipeExclusionFilter(List.of("mymod"), List.of());
        assertTrue(filter.isExcluded("mymod:widget"));
    }

    @Test
    void excludesExplicitRecipeId() {
        var filter = new RecipeExclusionFilter(List.of(), List.of("minecraft:tnt"));
        assertTrue(filter.isExcluded("minecraft:tnt"));
    }

    @Test
    void allowsUnlistedRecipe() {
        var filter = new RecipeExclusionFilter(List.of("mymod"), List.of("minecraft:tnt"));
        assertFalse(filter.isExcluded("minecraft:torch"));
    }

    @Test
    void allowsAllWhenListsEmpty() {
        var filter = new RecipeExclusionFilter(List.of(), List.of());
        assertFalse(filter.isExcluded("minecraft:torch"));
    }

    @Test
    void namespaceMatchIsNotAPrefixMatch() {
        var filter = new RecipeExclusionFilter(List.of("mine"), List.of());
        assertFalse(filter.isExcluded("minecraft:torch"));
    }

    @Test
    void malformedExcludedRecipeIdWithoutNamespaceDoesNotMatchOtherRecipes() {
        var filter = new RecipeExclusionFilter(List.of(), List.of("nonamespace"));
        assertFalse(filter.isExcluded("minecraft:torch"));
        assertTrue(filter.isExcluded("nonamespace"));
    }
}
