package com.scanmyfood.backend.services.nutrition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNutrientProfile {
    private String canonicalName;
    private String displayName;
    private String preparationState; // COOKED, RAW, PREPARED, etc.
    
    // Per 100g nutrient values
    private double caloriesPer100g;
    private double proteinPer100g;
    private double carbohydratePer100g;
    private double fatPer100g;
    private double saturatedFatPer100g;
    private double fiberPer100g;
    private double sugarPer100g;
    private double sodiumPer100g;
    private double ironPer100g;
    
    private String dataSource; // "IFCT_2017", "USDA_FDC", "PACKAGED_LABEL", "ESTIMATE"
    private boolean verified;
}
