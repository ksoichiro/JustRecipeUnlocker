package com.justrecipeunlocker.recipe;

import com.justrecipeunlocker.config.JustRecipeUnlockerConfig;
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
            if (!filter.isExcluded(id.toString())) {
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

    private void unlockSilently(ServerPlayer player, List<ResourceLocation> recipeIds) {
        var recipeBook = player.getRecipeBook();
        for (ResourceLocation id : recipeIds) {
            recipeManager.byKey(id).ifPresent(recipeBook::add);
        }
        recipeBook.sendInitialRecipeBook(player);
    }
}
