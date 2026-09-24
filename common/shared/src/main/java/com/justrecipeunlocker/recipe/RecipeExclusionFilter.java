package com.justrecipeunlocker.recipe;

import java.util.Collection;
import java.util.Set;

public final class RecipeExclusionFilter {

    private final Set<String> excludedNamespaces;
    private final Set<String> excludedRecipeIds;

    public RecipeExclusionFilter(Collection<String> excludedNamespaces, Collection<String> excludedRecipeIds) {
        this.excludedNamespaces = Set.copyOf(excludedNamespaces);
        this.excludedRecipeIds = Set.copyOf(excludedRecipeIds);
    }

    public boolean isExcluded(String recipeId) {
        if (excludedRecipeIds.contains(recipeId)) {
            return true;
        }
        int colon = recipeId.indexOf(':');
        String namespace = colon >= 0 ? recipeId.substring(0, colon) : recipeId;
        return excludedNamespaces.contains(namespace);
    }
}
