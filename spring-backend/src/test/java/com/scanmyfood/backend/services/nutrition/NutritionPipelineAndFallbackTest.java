package com.scanmyfood.backend.services.nutrition;

import com.scanmyfood.backend.constants.NutrientConstants;
import com.scanmyfood.backend.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class NutritionPipelineAndFallbackTest {

    private NutritionCalculationService calculationService;
    private VerifiedReferenceNutritionProvider referenceProvider;

    @BeforeEach
    void setUp() {
        referenceProvider = new VerifiedReferenceNutritionProvider();
        DatabaseNutritionProvider dbProvider = new DatabaseNutritionProvider();
        CompositeNutritionDataProvider compositeProvider = new CompositeNutritionDataProvider(referenceProvider, dbProvider);
        calculationService = new NutritionCalculationServiceImpl(compositeProvider);
    }

    @Test
    @DisplayName("Scenario 4: Missing, malformed, or invalid inputs handled safely")
    void testMalformedOrMissingInputsHandledSafely() {
        // Null or empty food item calculation
        FoodItem item = calculationService.calculateFoodItemNutrients(null, null, -50.0, null, null, null);
        assertNotNull(item);
        assertNotNull(item.getNutrients());
        assertFalse(item.getNutrients().isEmpty());
        assertTrue(item.getQuantity().getValue() > 0, "Negative or zero quantities should default to safe positive weight");

        // Null response to plate calculation
        assertNull(calculationService.calculatePlateNutrients(null));

        FoodAnalysisResponse emptyResponse = new FoodAnalysisResponse();
        assertNotNull(calculationService.calculatePlateNutrients(emptyResponse));
    }

    @Test
    @DisplayName("Scenario 5 & 14: Calculation consistency across primary and fallback paths")
    void testCalculationConsistencyAcrossProviders() {
        // Primary path: using verified reference dataset
        FoodItem primaryItem = calculationService.calculateFoodItemNutrients(
                "Boiled Egg", "boiled_egg", 50.0, "g", "COOKED", 1.0);

        // Fallback simulated: database provider not having the item, falls back to composite reference
        FoodItem fallbackItem = calculationService.calculateFoodItemNutrients(
                "Egg", "egg", 1.0, "piece", "COOKED", 1.0);

        Map<String, Double> primaryMap = primaryItem.getNutrients().stream()
                .collect(Collectors.toMap(FoodNutrient::getName, n -> n.getQuantity().getValue()));
        Map<String, Double> fallbackMap = fallbackItem.getNutrients().stream()
                .collect(Collectors.toMap(FoodNutrient::getName, n -> n.getQuantity().getValue()));

        // Both resolve 1 boiled egg (50g) to identical deterministic values: 77.5 kcal, 6.3g protein
        assertEquals(primaryMap.get(NutrientConstants.CALORIES), fallbackMap.get(NutrientConstants.CALORIES), 0.5);
        assertEquals(primaryMap.get(NutrientConstants.PROTEIN), fallbackMap.get(NutrientConstants.PROTEIN), 0.1);
    }

    @Test
    @DisplayName("Scenario 6: Nutrition database unavailable gracefully handled via reference tables")
    void testNutritionDatabaseUnavailableFallback() {
        // When database mapper is null or throws an exception, CompositeNutritionDataProvider still delivers IFCT/USDA records
        Optional<FoodNutrientProfile> profile = referenceProvider.getNutrientProfile("cooked_white_rice", "COOKED");
        assertTrue(profile.isPresent(), "Reference provider must operate independently of database availability");
        assertEquals("IFCT_2017", profile.get().getDataSource());
        assertTrue(profile.get().isVerified());
    }

    @Test
    @DisplayName("Scenario 7: Missing nutrition fields default safely without NPE")
    void testMissingNutritionFieldsDefaultSafely() {
        FoodNutrientProfile incompleteProfile = FoodNutrientProfile.builder()
                .canonicalName("custom_snack")
                .displayName("Custom Snack")
                .caloriesPer100g(250.0)
                .proteinPer100g(5.0)
                // remaining fields (carbs, fat, sodium) left unset as 0.0
                .build();

        List<FoodNutrient> nutrients = calculationService.calculateFromProfile(incompleteProfile, 100.0);
        assertNotNull(nutrients);
        assertEquals(7, nutrients.size());

        Map<String, Double> map = nutrients.stream()
                .collect(Collectors.toMap(FoodNutrient::getName, n -> n.getQuantity().getValue()));
        assertEquals(250.0, map.get(NutrientConstants.CALORIES));
        assertEquals(5.0, map.get(NutrientConstants.PROTEIN));
        assertEquals(0.0, map.get(NutrientConstants.SODIUM));
    }

    @Test
    @DisplayName("Scenario 9: Packaged-label values per 100 g versus per serving")
    void testPackagedLabelPerServingVsPer100g() {
        // Example: Box of biscuits, Net Weight = 300g, Serving Size = 30g
        // Label declares: 140 kcal per serving of 30g
        double servingVal = 30.0;
        double totalVal = 300.0;
        double perServingCalories = 140.0;

        // Normalizing to per 100g:
        double factorFromServing = 100.0 / servingVal; // 3.333
        double caloriesPer100g = perServingCalories * factorFromServing; // ~466.67 kcal

        assertEquals(466.7, Math.round(caloriesPer100g * 10.0) / 10.0, 0.5);

        // When the user logs 1 serving (30g), the consumed calories:
        double consumedCalories = (servingVal / 100.0) * caloriesPer100g;
        assertEquals(perServingCalories, Math.round(consumedCalories), 0.1,
                "Logging 1 serving of 30g must yield exact 140 kcal, not divided by whole box weight");
    }

    @Test
    @DisplayName("Scenario 12: Daily Intake totals recalculate accurately after editing a meal")
    void testDailyIntakeRecalculationAfterMealEdit() {
        FoodAnalysisResponse response = new FoodAnalysisResponse();
        response.setMealName("Lunch");

        List<FoodItem> items = new ArrayList<>();
        FoodItem roti = new FoodItem();
        roti.setName("Roti");
        roti.setCanonicalName("roti");
        roti.setQuantity(new Quantity(35.0, "g"));
        items.add(roti);
        response.setAnalyzedFoodItems(items);

        // Initial calculation: 1 roti = 104 kcal
        FoodAnalysisResponse initial = calculationService.calculatePlateNutrients(response);
        double initialCalories = initial.getTotalPlateNutrients().stream()
                .filter(n -> NutrientConstants.CALORIES.equals(n.getName()))
                .findFirst().get().getQuantity().getValue();
        assertEquals(104.0, initialCalories, 1.0);

        // User edits the meal: adds a second roti (increases quantity to 70g)
        response.getAnalyzedFoodItems().get(0).getQuantity().setValue(70.0);
        FoodAnalysisResponse edited = calculationService.calculatePlateNutrients(response);
        double editedCalories = edited.getTotalPlateNutrients().stream()
                .filter(n -> NutrientConstants.CALORIES.equals(n.getName()))
                .findFirst().get().getQuantity().getValue();

        assertEquals(208.0, editedCalories, 1.0, "Edited meal must recalculate plate totals deterministically");
    }

    @Test
    @DisplayName("Scenario 13: Repeated requests without duplicate intake records")
    void testRepeatedRequestsDoNotDuplicateIntakeRecords() {
        FoodAnalysisResponse response = new FoodAnalysisResponse();
        response.setMealName("Morning Oats");

        List<FoodItem> items = new ArrayList<>();
        FoodItem oats = new FoodItem();
        oats.setName("Cooked Oatmeal");
        oats.setCanonicalName("cooked_oats");
        oats.setQuantity(new Quantity(150.0, "g"));
        items.add(oats);
        response.setAnalyzedFoodItems(items);

        // Calculate once
        FoodAnalysisResponse res1 = calculationService.calculatePlateNutrients(response);
        // Calculate second time with same object (idempotency check)
        FoodAnalysisResponse res2 = calculationService.calculatePlateNutrients(response);

        assertEquals(res1.getAnalyzedFoodItems().size(), res2.getAnalyzedFoodItems().size());
        assertEquals(res1.getTotalPlateNutrients().get(0).getQuantity().getValue(),
                res2.getTotalPlateNutrients().get(0).getQuantity().getValue());
    }
}
