package com.scanmyfood.backend.services.nutrition;

import com.scanmyfood.backend.constants.NutrientConstants;
import com.scanmyfood.backend.models.FoodAnalysisResponse;
import com.scanmyfood.backend.models.FoodItem;
import com.scanmyfood.backend.models.FoodNutrient;
import com.scanmyfood.backend.models.PlateNutrients;
import com.scanmyfood.backend.models.Quantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NutritionCalculationServiceImpl implements NutritionCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(NutritionCalculationServiceImpl.class);

    private final NutritionDataProvider nutritionDataProvider;

    @Autowired
    public NutritionCalculationServiceImpl(NutritionDataProvider nutritionDataProvider) {
        this.nutritionDataProvider = nutritionDataProvider;
    }

    @Override
    public double convertToGrams(String canonicalName, double quantityValue, String unit) {
        if (quantityValue <= 0) {
            quantityValue = 1.0;
        }
        if (unit == null || unit.trim().isEmpty()) {
            return quantityValue;
        }

        String u = unit.trim().toLowerCase();
        String cName = canonicalName != null ? canonicalName.trim().toLowerCase() : "";

        switch (u) {
            case "g":
            case "gram":
            case "grams":
                return quantityValue;
            case "kg":
            case "kilogram":
            case "kilograms":
                return quantityValue * 1000.0;
            case "mg":
            case "milligram":
            case "milligrams":
                return quantityValue * 0.001;
            case "ml":
            case "milliliter":
            case "milliliters":
                return quantityValue * 1.0; // standard water density ~1g/ml
            case "l":
            case "liter":
            case "liters":
            case "litre":
            case "litres":
                return quantityValue * 1000.0;
            case "tbsp":
            case "tablespoon":
            case "tablespoons":
                return quantityValue * 15.0;
            case "tsp":
            case "teaspoon":
            case "teaspoons":
                return quantityValue * 5.0;
            case "piece":
            case "pieces":
            case "pcs":
            case "pc":
            case "slice":
            case "item":
                return quantityValue * getPieceWeightInGrams(cName);
            case "cup":
            case "cups":
            case "katori":
            case "bowl":
            case "bowls":
                return quantityValue * getCupWeightInGrams(cName);
            case "serving":
            case "servings":
            case "portion":
            case "portions":
                return quantityValue * getServingWeightInGrams(cName);
            case "plate":
            case "plates":
                return quantityValue * 250.0;
            default:
                logger.debug("Unrecognized unit '{}' for {}, assuming direct grams", unit, canonicalName);
                return quantityValue;
        }
    }

    private double getPieceWeightInGrams(String canonicalName) {
        if (canonicalName.contains("roti") || canonicalName.contains("chapati") || canonicalName.contains("phulka")) {
            return 35.0;
        } else if (canonicalName.contains("egg")) {
            return 50.0;
        } else if (canonicalName.contains("idli")) {
            return 40.0;
        } else if (canonicalName.contains("dosa")) {
            return 80.0;
        } else if (canonicalName.contains("banana")) {
            return 120.0;
        } else if (canonicalName.contains("apple")) {
            return 180.0;
        } else if (canonicalName.contains("bread") || canonicalName.contains("toast")) {
            return 30.0;
        } else if (canonicalName.contains("samosa")) {
            return 80.0;
        }
        return 50.0; // default piece weight
    }

    private double getCupWeightInGrams(String canonicalName) {
        if (canonicalName.contains("rice") || canonicalName.contains("grain") || canonicalName.contains("biryani")) {
            return 150.0; // 1 cup cooked rice ~150g
        } else if (canonicalName.contains("curry") || canonicalName.contains("dal") || canonicalName.contains("sambar")
                || canonicalName.contains("soup") || canonicalName.contains("rajma") || canonicalName.contains("chole")) {
            return 240.0; // 1 cup liquid / curry / dal ~240g
        } else if (canonicalName.contains("curd") || canonicalName.contains("yogurt") || canonicalName.contains("dahi")) {
            return 200.0;
        } else if (canonicalName.contains("salad") || canonicalName.contains("greens")) {
            return 75.0;
        } else if (canonicalName.contains("oats") || canonicalName.contains("oatmeal")) {
            return 234.0;
        }
        return 200.0; // standard default cup weight
    }

    private double getServingWeightInGrams(String canonicalName) {
        if (canonicalName.contains("curry") || canonicalName.contains("dal") || canonicalName.contains("rajma")) {
            return 150.0;
        } else if (canonicalName.contains("rice")) {
            return 150.0;
        }
        return 100.0;
    }

    @Override
    public FoodItem calculateFoodItemNutrients(String name, String canonicalName, double quantityValue, String unit,
                                               String preparationState, Double userProvidedPortion) {
        String safeName = (name != null && !name.trim().isEmpty()) ? name : "Food item";
        String safeCanonical = (canonicalName != null && !canonicalName.trim().isEmpty())
                ? canonicalName.trim().toLowerCase().replaceAll("[\\s-]+", "_")
                : safeName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");

        double weightInGrams = convertToGrams(safeCanonical, quantityValue, unit);
        double portionMultiplier = (userProvidedPortion != null && userProvidedPortion > 0) ? userProvidedPortion : 1.0;
        double effectiveWeightInGrams = weightInGrams * portionMultiplier;

        Optional<FoodNutrientProfile> profileOpt = nutritionDataProvider.getNutrientProfile(safeCanonical, preparationState);

        List<FoodNutrient> nutrients;
        if (profileOpt.isPresent()) {
            FoodNutrientProfile profile = profileOpt.get();
            nutrients = calculateFromProfile(profile, effectiveWeightInGrams);
        } else {
            // Default baseline profile if neither verified nor DB record found
            logger.warn("No nutrient profile found for canonical '{}', using safe estimation", safeCanonical);
            FoodNutrientProfile fallbackProfile = FoodNutrientProfile.builder()
                    .canonicalName(safeCanonical)
                    .displayName(safeName)
                    .preparationState(preparationState != null ? preparationState : "COOKED")
                    .caloriesPer100g(100.0)
                    .proteinPer100g(3.0)
                    .carbohydratePer100g(15.0)
                    .fatPer100g(3.0)
                    .fiberPer100g(1.5)
                    .sugarPer100g(1.0)
                    .sodiumPer100g(100.0)
                    .dataSource("ESTIMATED_BASELINE")
                    .verified(false)
                    .build();
            nutrients = calculateFromProfile(fallbackProfile, effectiveWeightInGrams);
        }

        FoodItem item = new FoodItem();
        item.setName(safeName);
        item.setCanonicalName(safeCanonical);
        item.setPortion(portionMultiplier);
        item.setQuantity(new Quantity(round(effectiveWeightInGrams), "g"));
        item.setNutrients(nutrients);
        return item;
    }

    @Override
    public List<FoodNutrient> calculateFromProfile(FoodNutrientProfile profile, double weightInGrams) {
        double factor = weightInGrams / 100.0;

        double calories = round(profile.getCaloriesPer100g() * factor);
        double protein = round(profile.getProteinPer100g() * factor);
        double carbs = round(profile.getCarbohydratePer100g() * factor);
        double fat = round(profile.getFatPer100g() * factor);
        double fiber = round(profile.getFiberPer100g() * factor);
        double sugar = round(profile.getSugarPer100g() * factor);
        double sodium = round(profile.getSodiumPer100g() * factor);

        validateAtwaterEnergyConsistency(calories, protein, carbs, fat, fiber);

        List<FoodNutrient> list = new ArrayList<>();
        list.add(new FoodNutrient(NutrientConstants.CALORIES, new Quantity(calories, "kcal")));
        list.add(new FoodNutrient(NutrientConstants.PROTEIN, new Quantity(protein, "g")));
        list.add(new FoodNutrient(NutrientConstants.TOTAL_CARBOHYDRATE, new Quantity(carbs, "g")));
        list.add(new FoodNutrient(NutrientConstants.TOTAL_FAT, new Quantity(fat, "g")));
        list.add(new FoodNutrient(NutrientConstants.DIETARY_FIBER, new Quantity(fiber, "g")));
        list.add(new FoodNutrient(NutrientConstants.TOTAL_SUGARS, new Quantity(sugar, "g")));
        list.add(new FoodNutrient(NutrientConstants.SODIUM, new Quantity(sodium, "mg")));
        return list;
    }

    @Override
    public FoodAnalysisResponse calculatePlateNutrients(FoodAnalysisResponse response) {
        if (response == null || response.getAnalyzedFoodItems() == null) {
            return response;
        }

        // 1. Merge duplicate ingredients
        mergeDuplicateFoodItems(response);

        // 2. Deterministically recalculate each item from verified data
        List<FoodItem> updatedItems = new ArrayList<>();
        for (FoodItem item : response.getAnalyzedFoodItems()) {
            double qty = (item.getQuantity() != null && item.getQuantity().getValue() != null)
                    ? item.getQuantity().getValue() : 100.0;
            String unit = (item.getQuantity() != null && item.getQuantity().getUnit() != null)
                    ? item.getQuantity().getUnit() : "g";
            double portion = item.getPortion() > 0 ? item.getPortion() : 1.0;

            FoodItem calculated = calculateFoodItemNutrients(
                    item.getName(),
                    item.getCanonicalName(),
                    qty,
                    unit,
                    "COOKED",
                    portion
            );
            updatedItems.add(calculated);
        }
        response.setAnalyzedFoodItems(updatedItems);

        // 3. Sum up plate totals deterministically
        PlateNutrients plateNutrients = new PlateNutrients();
        for (FoodItem item : updatedItems) {
            plateNutrients.addFoodItem(item);
        }
        response.setTotalPlateNutrients(plateNutrients.toFoodNutrientList());

        return response;
    }

    @Override
    public boolean validateAtwaterEnergyConsistency(double calories, double protein, double carbs, double fat, double fiber) {
        // Standard Atwater general factor system:
        // 4 kcal/g protein, 4 kcal/g available carb, 9 kcal/g fat, 2 kcal/g fiber
        double expectedKcal = (protein * 4.0) + (carbs * 4.0) + (fat * 9.0);
        double diff = Math.abs(calories - expectedKcal);

        // If difference is greater than 25% and more than 40 kcal, flag inconsistency
        if (expectedKcal > 20.0 && diff > (expectedKcal * 0.25) && diff > 40.0) {
            logger.debug("Atwater consistency alert: reported {} kcal vs expected {} kcal (P={}, C={}, F={}, Fiber={})",
                    calories, expectedKcal, protein, carbs, fat, fiber);
            return false;
        }
        return true;
    }

    @Override
    public void mergeDuplicateFoodItems(FoodAnalysisResponse response) {
        if (response == null || response.getAnalyzedFoodItems() == null || response.getAnalyzedFoodItems().size() <= 1) {
            return;
        }

        Map<String, FoodItem> mergedMap = new LinkedHashMap<>();
        for (FoodItem item : response.getAnalyzedFoodItems()) {
            String key = item.getCanonicalName();
            if (key == null || key.trim().isEmpty()) {
                key = item.getName() != null ? item.getName().toLowerCase().trim() : "unknown";
            }
            key = key.trim().toLowerCase().replaceAll("[\\s-]+", "_");

            if (!mergedMap.containsKey(key)) {
                mergedMap.put(key, item);
            } else {
                // Merge into existing item
                FoodItem existing = mergedMap.get(key);
                double existingGrams = convertToGrams(key,
                        existing.getQuantity() != null ? existing.getQuantity().getValue() : 100.0,
                        existing.getQuantity() != null ? existing.getQuantity().getUnit() : "g");
                double additionalGrams = convertToGrams(key,
                        item.getQuantity() != null ? item.getQuantity().getValue() : 100.0,
                        item.getQuantity() != null ? item.getQuantity().getUnit() : "g");

                double totalGrams = existingGrams + additionalGrams;
                existing.setQuantity(new Quantity(round(totalGrams), "g"));
                logger.info("Merged duplicate food item '{}': {}g + {}g = {}g", key, existingGrams, additionalGrams, totalGrams);
            }
        }
        response.setAnalyzedFoodItems(new ArrayList<>(mergedMap.values()));
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
