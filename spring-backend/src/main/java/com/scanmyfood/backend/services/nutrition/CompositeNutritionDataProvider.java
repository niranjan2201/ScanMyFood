package com.scanmyfood.backend.services.nutrition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Primary
public class CompositeNutritionDataProvider implements NutritionDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(CompositeNutritionDataProvider.class);

    private final VerifiedReferenceNutritionProvider referenceProvider;
    private final DatabaseNutritionProvider databaseProvider;

    @Autowired
    public CompositeNutritionDataProvider(VerifiedReferenceNutritionProvider referenceProvider,
                                         DatabaseNutritionProvider databaseProvider) {
        this.referenceProvider = referenceProvider;
        this.databaseProvider = databaseProvider;
    }

    @Override
    public Optional<FoodNutrientProfile> getNutrientProfile(String canonicalName, String preparationState) {
        if (canonicalName == null || canonicalName.trim().isEmpty()) {
            return Optional.empty();
        }

        // 1. Try verified reference tables (IFCT 2017 / USDA FDC)
        Optional<FoodNutrientProfile> refProfile = referenceProvider.getNutrientProfile(canonicalName, preparationState);
        if (refProfile.isPresent()) {
            return refProfile;
        }

        // 2. Try database provider (verified database records)
        Optional<FoodNutrientProfile> dbProfile = databaseProvider.getNutrientProfile(canonicalName, preparationState);
        if (dbProfile.isPresent()) {
            return dbProfile;
        }

        logger.debug("No verified nutrient record found for: {}", canonicalName);
        return Optional.empty();
    }

    @Override
    public boolean hasProfile(String canonicalName) {
        return referenceProvider.hasProfile(canonicalName) || databaseProvider.hasProfile(canonicalName);
    }
}
