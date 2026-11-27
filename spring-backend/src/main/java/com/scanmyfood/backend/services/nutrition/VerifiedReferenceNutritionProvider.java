package com.scanmyfood.backend.services.nutrition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Standard reference nutrition data provider backed by scientific nutritional tables:
 * - IFCT 2017 (Indian Food Composition Tables, National Institute of Nutrition, ICMR)
 * - USDA FoodData Central (Agricultural Research Service)
 */
@Component
public class VerifiedReferenceNutritionProvider implements NutritionDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(VerifiedReferenceNutritionProvider.class);

    private static final Map<String, FoodNutrientProfile> REFERENCE_DATABASE;

    static {
        Map<String, FoodNutrientProfile> map = new HashMap<>();

        // Helper to register standard items
        // Source: IFCT 2017 (Indian Food Composition Tables)
        register(map, FoodNutrientProfile.builder()
                .canonicalName("cooked_white_rice")
                .displayName("Cooked White Rice")
                .preparationState("COOKED")
                .caloriesPer100g(130.0)
                .proteinPer100g(2.7)
                .carbohydratePer100g(28.2)
                .fatPer100g(0.3)
                .saturatedFatPer100g(0.1)
                .fiberPer100g(0.4)
                .sugarPer100g(0.1)
                .sodiumPer100g(1.0)
                .ironPer100g(0.2)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("raw_white_rice")
                .displayName("Raw White Rice")
                .preparationState("RAW")
                .caloriesPer100g(345.0)
                .proteinPer100g(6.8)
                .carbohydratePer100g(78.2)
                .fatPer100g(0.5)
                .saturatedFatPer100g(0.1)
                .fiberPer100g(1.3)
                .sugarPer100g(0.2)
                .sodiumPer100g(5.0)
                .ironPer100g(0.7)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("cooked_brown_rice")
                .displayName("Cooked Brown Rice")
                .preparationState("COOKED")
                .caloriesPer100g(111.0)
                .proteinPer100g(2.6)
                .carbohydratePer100g(23.0)
                .fatPer100g(0.9)
                .saturatedFatPer100g(0.2)
                .fiberPer100g(1.8)
                .sugarPer100g(0.4)
                .sodiumPer100g(5.0)
                .ironPer100g(0.4)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("roti")
                .displayName("Roti / Chapati")
                .preparationState("COOKED")
                .caloriesPer100g(297.0)
                .proteinPer100g(9.6)
                .carbohydratePer100g(55.7)
                .fatPer100g(3.7)
                .saturatedFatPer100g(0.8)
                .fiberPer100g(9.6)
                .sugarPer100g(1.0)
                .sodiumPer100g(180.0)
                .ironPer100g(3.5)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("rajma_curry")
                .displayName("Rajma Masala / Curry")
                .preparationState("COOKED")
                .caloriesPer100g(143.0)
                .proteinPer100g(5.7)
                .carbohydratePer100g(17.8)
                .fatPer100g(4.2)
                .saturatedFatPer100g(0.6)
                .fiberPer100g(4.0)
                .sugarPer100g(1.5)
                .sodiumPer100g(320.0)
                .ironPer100g(1.8)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("dal_tadka")
                .displayName("Dal Tadka / Yellow Dal")
                .preparationState("COOKED")
                .caloriesPer100g(115.0)
                .proteinPer100g(6.5)
                .carbohydratePer100g(14.2)
                .fatPer100g(3.5)
                .saturatedFatPer100g(0.9)
                .fiberPer100g(3.2)
                .sugarPer100g(0.8)
                .sodiumPer100g(280.0)
                .ironPer100g(1.6)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("chana_masala")
                .displayName("Chana Masala / Chole")
                .preparationState("COOKED")
                .caloriesPer100g(166.0)
                .proteinPer100g(7.3)
                .carbohydratePer100g(21.0)
                .fatPer100g(5.2)
                .saturatedFatPer100g(0.7)
                .fiberPer100g(5.6)
                .sugarPer100g(2.0)
                .sodiumPer100g(350.0)
                .ironPer100g(2.2)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("paneer_curry")
                .displayName("Paneer Curry / Butter Masala")
                .preparationState("COOKED")
                .caloriesPer100g(195.0)
                .proteinPer100g(8.5)
                .carbohydratePer100g(7.2)
                .fatPer100g(15.0)
                .saturatedFatPer100g(7.8)
                .fiberPer100g(2.1)
                .sugarPer100g(2.8)
                .sodiumPer100g(410.0)
                .ironPer100g(1.1)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("paneer_raw")
                .displayName("Raw Paneer")
                .preparationState("RAW")
                .caloriesPer100g(265.0)
                .proteinPer100g(18.3)
                .carbohydratePer100g(1.2)
                .fatPer100g(20.8)
                .saturatedFatPer100g(13.2)
                .fiberPer100g(0.0)
                .sugarPer100g(1.2)
                .sodiumPer100g(22.0)
                .ironPer100g(0.2)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("idli")
                .displayName("Steamed Idli")
                .preparationState("COOKED")
                .caloriesPer100g(136.0)
                .proteinPer100g(4.6)
                .carbohydratePer100g(27.5)
                .fatPer100g(0.4)
                .saturatedFatPer100g(0.1)
                .fiberPer100g(1.4)
                .sugarPer100g(0.5)
                .sodiumPer100g(190.0)
                .ironPer100g(0.8)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("plain_dosa")
                .displayName("Plain Dosa")
                .preparationState("COOKED")
                .caloriesPer100g(206.0)
                .proteinPer100g(4.0)
                .carbohydratePer100g(29.0)
                .fatPer100g(8.0)
                .saturatedFatPer100g(1.5)
                .fiberPer100g(1.8)
                .sugarPer100g(0.6)
                .sodiumPer100g(210.0)
                .ironPer100g(1.2)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("sambar")
                .displayName("Sambar")
                .preparationState("COOKED")
                .caloriesPer100g(75.0)
                .proteinPer100g(2.8)
                .carbohydratePer100g(10.5)
                .fatPer100g(2.2)
                .saturatedFatPer100g(0.3)
                .fiberPer100g(2.5)
                .sugarPer100g(2.0)
                .sodiumPer100g(310.0)
                .ironPer100g(0.9)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("curd")
                .displayName("Plain Curd / Dahi / Yogurt")
                .preparationState("PREPARED")
                .caloriesPer100g(61.0)
                .proteinPer100g(3.5)
                .carbohydratePer100g(4.7)
                .fatPer100g(3.3)
                .saturatedFatPer100g(2.1)
                .fiberPer100g(0.0)
                .sugarPer100g(4.7)
                .sodiumPer100g(46.0)
                .ironPer100g(0.1)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("ghee")
                .displayName("Desi Ghee / Clarified Butter")
                .preparationState("PREPARED")
                .caloriesPer100g(900.0)
                .proteinPer100g(0.0)
                .carbohydratePer100g(0.0)
                .fatPer100g(100.0)
                .saturatedFatPer100g(65.0)
                .fiberPer100g(0.0)
                .sugarPer100g(0.0)
                .sodiumPer100g(0.0)
                .ironPer100g(0.0)
                .dataSource("IFCT_2017")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("cooking_oil")
                .displayName("Cooking Oil (Vegetable / Mustard / Sunflower)")
                .preparationState("PREPARED")
                .caloriesPer100g(884.0)
                .proteinPer100g(0.0)
                .carbohydratePer100g(0.0)
                .fatPer100g(100.0)
                .saturatedFatPer100g(14.0)
                .fiberPer100g(0.0)
                .sugarPer100g(0.0)
                .sodiumPer100g(0.0)
                .ironPer100g(0.0)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("butter")
                .displayName("Butter")
                .preparationState("PREPARED")
                .caloriesPer100g(717.0)
                .proteinPer100g(0.9)
                .carbohydratePer100g(0.1)
                .fatPer100g(81.1)
                .saturatedFatPer100g(51.4)
                .fiberPer100g(0.0)
                .sugarPer100g(0.1)
                .sodiumPer100g(576.0)
                .ironPer100g(0.0)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("boiled_egg")
                .displayName("Boiled Whole Egg")
                .preparationState("COOKED")
                .caloriesPer100g(155.0)
                .proteinPer100g(12.6)
                .carbohydratePer100g(1.1)
                .fatPer100g(10.6)
                .saturatedFatPer100g(3.3)
                .fiberPer100g(0.0)
                .sugarPer100g(1.1)
                .sodiumPer100g(124.0)
                .ironPer100g(1.8)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("cooked_chicken_breast")
                .displayName("Cooked Chicken Breast")
                .preparationState("COOKED")
                .caloriesPer100g(165.0)
                .proteinPer100g(31.0)
                .carbohydratePer100g(0.0)
                .fatPer100g(3.6)
                .saturatedFatPer100g(1.0)
                .fiberPer100g(0.0)
                .sugarPer100g(0.0)
                .sodiumPer100g(74.0)
                .ironPer100g(1.0)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("raw_chicken_breast")
                .displayName("Raw Chicken Breast")
                .preparationState("RAW")
                .caloriesPer100g(120.0)
                .proteinPer100g(22.5)
                .carbohydratePer100g(0.0)
                .fatPer100g(2.6)
                .saturatedFatPer100g(0.6)
                .fiberPer100g(0.0)
                .sugarPer100g(0.0)
                .sodiumPer100g(65.0)
                .ironPer100g(0.7)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("banana")
                .displayName("Banana")
                .preparationState("RAW")
                .caloriesPer100g(89.0)
                .proteinPer100g(1.1)
                .carbohydratePer100g(22.8)
                .fatPer100g(0.3)
                .saturatedFatPer100g(0.1)
                .fiberPer100g(2.6)
                .sugarPer100g(12.2)
                .sodiumPer100g(1.0)
                .ironPer100g(0.3)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("apple")
                .displayName("Apple")
                .preparationState("RAW")
                .caloriesPer100g(52.0)
                .proteinPer100g(0.3)
                .carbohydratePer100g(13.8)
                .fatPer100g(0.2)
                .saturatedFatPer100g(0.0)
                .fiberPer100g(2.4)
                .sugarPer100g(10.4)
                .sodiumPer100g(1.0)
                .ironPer100g(0.1)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("cow_milk")
                .displayName("Whole Cow Milk")
                .preparationState("RAW")
                .caloriesPer100g(61.0)
                .proteinPer100g(3.2)
                .carbohydratePer100g(4.8)
                .fatPer100g(3.3)
                .saturatedFatPer100g(1.9)
                .fiberPer100g(0.0)
                .sugarPer100g(5.1)
                .sodiumPer100g(43.0)
                .ironPer100g(0.0)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("cooked_oats")
                .displayName("Cooked Oatmeal / Oats")
                .preparationState("COOKED")
                .caloriesPer100g(71.0)
                .proteinPer100g(2.5)
                .carbohydratePer100g(12.0)
                .fatPer100g(1.5)
                .saturatedFatPer100g(0.2)
                .fiberPer100g(1.7)
                .sugarPer100g(0.3)
                .sodiumPer100g(49.0)
                .ironPer100g(0.9)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("boiled_potato")
                .displayName("Boiled Potato")
                .preparationState("COOKED")
                .caloriesPer100g(87.0)
                .proteinPer100g(1.9)
                .carbohydratePer100g(20.1)
                .fatPer100g(0.1)
                .saturatedFatPer100g(0.0)
                .fiberPer100g(1.8)
                .sugarPer100g(0.9)
                .sodiumPer100g(4.0)
                .ironPer100g(0.3)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        register(map, FoodNutrientProfile.builder()
                .canonicalName("salad_greens")
                .displayName("Mixed Salad (Cucumber, Tomato, Greens)")
                .preparationState("RAW")
                .caloriesPer100g(20.0)
                .proteinPer100g(0.8)
                .carbohydratePer100g(3.8)
                .fatPer100g(0.2)
                .saturatedFatPer100g(0.0)
                .fiberPer100g(1.2)
                .sugarPer100g(2.5)
                .sodiumPer100g(10.0)
                .ironPer100g(0.4)
                .dataSource("USDA_FDC")
                .verified(true)
                .build());

        REFERENCE_DATABASE = Collections.unmodifiableMap(map);
    }

    private static void register(Map<String, FoodNutrientProfile> map, FoodNutrientProfile profile) {
        map.put(profile.getCanonicalName().toLowerCase(), profile);
    }

    @Override
    public Optional<FoodNutrientProfile> getNutrientProfile(String canonicalName, String preparationState) {
        if (canonicalName == null || canonicalName.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalized = canonicalName.trim().toLowerCase().replaceAll("[\\s-]+", "_");

        // 1. Direct match
        FoodNutrientProfile direct = REFERENCE_DATABASE.get(normalized);
        if (direct != null) {
            return Optional.of(direct);
        }

        // 2. State-aware match (e.g. "rice" with preparationState "cooked" -> "cooked_white_rice")
        if (preparationState != null) {
            String stateNorm = preparationState.trim().toLowerCase();
            String prefix = stateNorm + "_" + normalized;
            if (REFERENCE_DATABASE.containsKey(prefix)) {
                return Optional.of(REFERENCE_DATABASE.get(prefix));
            }
        }

        // 3. Fallback alias mapping for common synonyms
        String synonymKey = mapSynonym(normalized);
        if (synonymKey != null && REFERENCE_DATABASE.containsKey(synonymKey)) {
            return Optional.of(REFERENCE_DATABASE.get(synonymKey));
        }

        return Optional.empty();
    }

    @Override
    public boolean hasProfile(String canonicalName) {
        if (canonicalName == null) return false;
        String normalized = canonicalName.trim().toLowerCase().replaceAll("[\\s-]+", "_");
        return REFERENCE_DATABASE.containsKey(normalized) || mapSynonym(normalized) != null;
    }

    private static String mapSynonym(String key) {
        switch (key) {
            case "white_rice":
            case "rice":
            case "basmati_rice":
            case "steamed_rice":
            case "boiled_rice":
                return "cooked_white_rice";
            case "brown_rice":
                return "cooked_brown_rice";
            case "chapati":
            case "phulka":
            case "flatbread":
            case "wheat_roti":
                return "roti";
            case "rajma":
            case "rajma_masala":
            case "kidney_beans_curry":
                return "rajma_curry";
            case "dal":
            case "yellow_dal":
            case "toor_dal":
            case "moong_dal":
                return "dal_tadka";
            case "chole":
            case "chickpea_curry":
                return "chana_masala";
            case "palak_paneer":
            case "paneer_butter_masala":
            case "paneer_makhani":
            case "paneer_tikka_masala":
                return "paneer_curry";
            case "dosa":
            case "masala_dosa":
                return "plain_dosa";
            case "egg":
            case "egg_boiled":
            case "whole_egg":
                return "boiled_egg";
            case "chicken_breast":
            case "grilled_chicken":
            case "boiled_chicken":
                return "cooked_chicken_breast";
            case "dahi":
            case "yogurt":
                return "curd";
            case "oil":
            case "vegetable_oil":
            case "sunflower_oil":
            case "mustard_oil":
                return "cooking_oil";
            case "clarified_butter":
            case "desi_ghee":
                return "ghee";
            case "milk":
            case "cow_milk_whole":
                return "cow_milk";
            case "oats":
            case "oatmeal":
                return "cooked_oats";
            case "potato":
            case "potatoes":
                return "boiled_potato";
            case "salad":
            case "green_salad":
            case "cucumber_salad":
                return "salad_greens";
            default:
                return null;
        }
    }
}
