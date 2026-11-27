package com.scanmyfood.backend.services.nutrition;

import com.scanmyfood.backend.mapper.UserIntakeMapper;
import com.scanmyfood.backend.models.FoodItemRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DatabaseNutritionProvider implements NutritionDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseNutritionProvider.class);

    @Autowired(required = false)
    private UserIntakeMapper userIntakeMapper;

    @Override
    public Optional<FoodNutrientProfile> getNutrientProfile(String canonicalName, String preparationState) {
        if (userIntakeMapper == null || canonicalName == null || canonicalName.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            FoodItemRecord record = userIntakeMapper.findFoodItemByCanonical(canonicalName);
            if (record == null) {
                return Optional.empty();
            }

            // Only prioritize records that have valid calories/macros
            if (record.getCaloriesValuePer100g() == null && record.getProteinValuePer100g() == null) {
                return Optional.empty();
            }

            return Optional.of(FoodNutrientProfile.builder()
                    .canonicalName(record.getCanonicalName())
                    .displayName(record.getItemName() != null ? record.getItemName() : record.getCanonicalName())
                    .preparationState(preparationState != null ? preparationState : "COOKED")
                    .caloriesPer100g(safe(record.getCaloriesValuePer100g()))
                    .proteinPer100g(safe(record.getProteinValuePer100g()))
                    .carbohydratePer100g(safe(record.getTotalCarbohydrateValuePer100g()))
                    .fatPer100g(safe(record.getTotalFatValuePer100g()))
                    .fiberPer100g(safe(record.getDietaryFiberValuePer100g()))
                    .sugarPer100g(safe(record.getTotalSugarsValuePer100g()))
                    .sodiumPer100g(safe(record.getSodiumValuePer100g()))
                    .ironPer100g(safe(record.getIronValuePer100g()))
                    .dataSource("DATABASE_STORE")
                    .verified(Boolean.TRUE.equals(record.getVerifiedInd()))
                    .build());
        } catch (Exception e) {
            logger.warn("Error querying database nutrition profile for {}: {}", canonicalName, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean hasProfile(String canonicalName) {
        return getNutrientProfile(canonicalName, null).isPresent();
    }

    private double safe(Double val) {
        return val != null ? val : 0.0;
    }
}
