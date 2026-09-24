package com.justrecipeunlocker.recipe;

import com.justrecipeunlocker.config.JustRecipeUnlockerConfig;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

public final class RecipeUnlockService {

    private final RecipeManager recipeManager;

    public RecipeUnlockService(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    public List<ResourceLocation> resolveUnlockableRecipeIds(JustRecipeUnlockerConfig config) {
        RecipeExclusionFilter filter = new RecipeExclusionFilter(config.excludedNamespaces(), config.excludedRecipeIds());
        List<ResourceLocation> recipeIds = new ArrayList<>();
        for (var holder : recipeManager.getRecipes()) {
            ResourceLocation id = holder.id();
            // Special recipes (armor dye, firework star, map cloning, etc.) are never shown in
            // the recipe book and vanilla's ServerRecipeBook#addRecipes skips them too; excluding
            // them here keeps the silent and non-silent unlock paths consistent.
            if (!holder.value().isSpecial() && !filter.isExcluded(id.toString())) {
                recipeIds.add(id);
            }
        }
        return recipeIds;
    }

    public void unlock(ServerPlayer player, List<ResourceLocation> recipeIds, boolean suppressToast) {
        if (suppressToast) {
            unlockSilently(player, recipeIds);
        } else {
            player.awardRecipesByKey(recipeIds);
        }
    }

    // Mirrors vanilla ServerRecipeBook#addRecipes (which awardRecipesByKey ultimately calls) but
    // without sending the "New recipes unlocked!" toast: only newly-known recipes are added and
    // trigger CRITERIA_TRIGGERS.RECIPE_UNLOCKED, and the recipe book sync packet is only sent if
    // something actually changed.
    private void unlockSilently(ServerPlayer player, List<ResourceLocation> recipeIds) {
        var recipeBook = player.getRecipeBook();
        boolean addedAny = false;
        for (ResourceLocation id : recipeIds) {
            if (recipeBook.contains(id)) {
                continue;
            }
            var holder = recipeManager.byKey(id);
            if (holder.isEmpty()) {
                continue;
            }
            recipeBook.add(holder.get());
            CriteriaTriggers.RECIPE_UNLOCKED.trigger(player, holder.get());
            addedAny = true;
        }
        if (addedAny) {
            recipeBook.sendInitialRecipeBook(player);
        }
    }
}
