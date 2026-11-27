package com.scanmyfood.backend.services.nutrition;

import com.scanmyfood.backend.models.FoodAnalysisResponse;
import com.scanmyfood.backend.models.FoodItem;
import com.scanmyfood.backend.models.FoodNutrient;

import java.util.List;

public interface NutritionCalculationService {

    /**
     * Converts a given quantity and unit to estimated weight in grams,
     * taking into account food-specific densities and portion standards.
     */
    double convertToGrams(String canonicalName, double quantityValue, String unit);

    /**
     * Resolves food nutrient profile and calculates all nutrient values deterministically
     * using the formula: nutrientAmount = (weightInGrams / 100.0) * nutrientValuePer100g.
     */
    FoodItem calculateFoodItemNutrients(String name, String canonicalName, double quantityValue, String unit,
                                        String preparationState, Double userProvidedPortion);

    /**
     * Calculates plate totals and validates energy consistency across all analyzed food items.
     */
    FoodAnalysisResponse calculatePlateNutrients(FoodAnalysisResponse response);

    /**
     * Calculates food nutrient list from a known nutrient profile and weight in grams.
     */
    List<FoodNutrient> calculateFromProfile(FoodNutrientProfile profile, double weightInGrams);

    /**
     * Checks Atwater energy consistency: Calories ~= 4*protein + 4*carbs + 9*fat (+ 2*fiber).
     */
    boolean validateAtwaterEnergyConsistency(double calories, double protein, double carbs, double fat, double fiber);

    /**
     * Detects and merges duplicate food items (by canonical name) within the meal.
     */
    void mergeDuplicateFoodItems(FoodAnalysisResponse response);
}
