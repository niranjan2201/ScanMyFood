package com.scanmyfood.backend.services.nutrition;

import java.util.Optional;

public interface NutritionDataProvider {
    /**
     * Look up verified or estimated nutrient profile per 100g for a given canonical name and preparation state.
     *
     * @param canonicalName canonical snake_case food name (e.g., cooked_white_rice, roti, rajma_curry)
     * @param preparationState optional state (e.g. cooked, raw)
     * @return Optional containing FoodNutrientProfile if found
     */
    Optional<FoodNutrientProfile> getNutrientProfile(String canonicalName, String preparationState);

    /**
     * Check if provider supports or has verified data for the food.
     */
    boolean hasProfile(String canonicalName);
}
