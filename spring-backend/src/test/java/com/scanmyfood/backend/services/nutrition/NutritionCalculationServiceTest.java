package com.scanmyfood.backend.services.nutrition;

import com.scanmyfood.backend.constants.NutrientConstants;
import com.scanmyfood.backend.models.FoodAnalysisResponse;
import com.scanmyfood.backend.models.FoodItem;
import com.scanmyfood.backend.models.FoodNutrient;
import com.scanmyfood.backend.models.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class NutritionCalculationServiceTest {

    private NutritionCalculationService nutritionService;
    private VerifiedReferenceNutritionProvider referenceProvider;

    @BeforeEach
    void setUp() {
        referenceProvider = new VerifiedReferenceNutritionProvider();
        DatabaseNutritionProvider dbProvider = new DatabaseNutritionProvider(); // no mapper attached for unit tests
        CompositeNutritionDataProvider compositeProvider = new CompositeNutritionDataProvider(referenceProvider, dbProvider);
        nutritionService = new NutritionCalculationServiceImpl(compositeProvider);
    }

    @Test
    @DisplayName("Scenario 1: Rajma curry and cooked white rice analysed as separate components")
    void testRajmaCurryAndCookedRiceDecomposition() {
        FoodAnalysisResponse response = new FoodAnalysisResponse();
        response.setMealName("Rajma Chawal");

        List<FoodItem> items = new ArrayList<>();
        FoodItem rice = new FoodItem();
        rice.setName("Steamed Rice");
        rice.setCanonicalName("cooked_white_rice");
        rice.setQuantity(new Quantity(150.0, "g"));
        items.add(rice);

        FoodItem rajma = new FoodItem();
        rajma.setName("Rajma Masala");
        rajma.setCanonicalName("rajma_curry");
        rajma.setQuantity(new Quantity(200.0, "g"));
        items.add(rajma);

        response.setAnalyzedFoodItems(items);

        FoodAnalysisResponse result = nutritionService.calculatePlateNutrients(response);

        assertNotNull(result);
        assertEquals(2, result.getAnalyzedFoodItems().size(), "Components must remain separate");

        // Rice: 150g * (130 kcal / 100g) = 195 kcal
        FoodItem analyzedRice = result.getAnalyzedFoodItems().get(0);
        Map<String, Double> riceNutrients = extractNutrientMap(analyzedRice.getNutrients());
        assertEquals(195.0, riceNutrients.get(NutrientConstants.CALORIES), 1.0);
        assertEquals(4.05, riceNutrients.get(NutrientConstants.PROTEIN), 0.5);

        // Rajma: 200g * (143 kcal / 100g) = 286 kcal
        FoodItem analyzedRajma = result.getAnalyzedFoodItems().get(1);
        Map<String, Double> rajmaNutrients = extractNutrientMap(analyzedRajma.getNutrients());
        assertEquals(286.0, rajmaNutrients.get(NutrientConstants.CALORIES), 1.0);
        assertEquals(11.4, rajmaNutrients.get(NutrientConstants.PROTEIN), 0.5);

        // Total plate calories = 195 + 286 = 481 kcal
        Map<String, Double> plateNutrients = extractNutrientMap(result.getTotalPlateNutrients());
        assertEquals(481.0, plateNutrients.get(NutrientConstants.CALORIES), 1.0);
        assertEquals(15.45, plateNutrients.get(NutrientConstants.PROTEIN), 0.5);
    }

    @Test
    @DisplayName("Scenario 2: Different portion sizes of the same food scale strictly deterministically")
    void testDifferentPortionSizesScaling() {
        FoodItem smallRoti = nutritionService.calculateFoodItemNutrients("Roti", "roti", 35.0, "g", "COOKED", 1.0);
        FoodItem doubleRoti = nutritionService.calculateFoodItemNutrients("Roti", "roti", 70.0, "g", "COOKED", 1.0);

        Map<String, Double> smallMap = extractNutrientMap(smallRoti.getNutrients());
        Map<String, Double> doubleMap = extractNutrientMap(doubleRoti.getNutrients());

        double smallCalories = smallMap.get(NutrientConstants.CALORIES);
        double doubleCalories = doubleMap.get(NutrientConstants.CALORIES);

        assertEquals(smallCalories * 2.0, doubleCalories, 1.0, "Double portion must have exactly double calories");
        assertEquals(smallMap.get(NutrientConstants.PROTEIN) * 2.0, doubleMap.get(NutrientConstants.PROTEIN), 0.2);
    }

    @Test
    @DisplayName("Scenario 3: Raw versus cooked rice density and nutrition distinction")
    void testRawVersusCookedRiceDistinction() {
        // 100g of raw rice vs 100g of cooked rice
        FoodItem cookedRice = nutritionService.calculateFoodItemNutrients("Cooked Rice", "cooked_white_rice", 100.0, "g", "COOKED", 1.0);
        FoodItem rawRice = nutritionService.calculateFoodItemNutrients("Raw Dry Rice", "raw_white_rice", 100.0, "g", "RAW", 1.0);

        Map<String, Double> cookedMap = extractNutrientMap(cookedRice.getNutrients());
        Map<String, Double> rawMap = extractNutrientMap(rawRice.getNutrients());

        // Cooked rice has absorbed ~2.5x water, so 100g cooked is ~130 kcal whereas 100g raw is ~345 kcal
        assertTrue(rawMap.get(NutrientConstants.CALORIES) > (cookedMap.get(NutrientConstants.CALORIES) * 2.0),
                "Raw dry rice must have significantly higher caloric density than cooked hydrated rice");
        assertEquals(130.0, cookedMap.get(NutrientConstants.CALORIES), 1.0);
        assertEquals(345.0, rawMap.get(NutrientConstants.CALORIES), 1.0);
    }

    @Test
    @DisplayName("Scenario 8: Grams versus milligrams and kilograms unit conversions")
    void testUnitConversions() {
        // 1 kg = 1000 g
        double kgToGrams = nutritionService.convertToGrams("cooked_white_rice", 1.0, "kg");
        assertEquals(1000.0, kgToGrams);

        // 500 mg = 0.5 g
        double mgToGrams = nutritionService.convertToGrams("salt", 500.0, "mg");
        assertEquals(0.5, mgToGrams);

        // 2 tbsp oil = 30 g
        double tbspToGrams = nutritionService.convertToGrams("cooking_oil", 2.0, "tbsp");
        assertEquals(30.0, tbspToGrams);

        // 2 pieces of roti = 70 g
        double rotiGrams = nutritionService.convertToGrams("roti", 2.0, "pieces");
        assertEquals(70.0, rotiGrams);
    }

    @Test
    @DisplayName("Scenario 10: Oil or ghee included in recipe adds correct energetic contribution")
    void testOilOrGheeInclusion() {
        FoodAnalysisResponse response = new FoodAnalysisResponse();
        response.setMealName("Dal with Ghee");

        List<FoodItem> items = new ArrayList<>();
        FoodItem dal = new FoodItem();
        dal.setName("Dal Tadka");
        dal.setCanonicalName("dal_tadka");
        dal.setQuantity(new Quantity(200.0, "g"));
        items.add(dal);

        FoodItem ghee = new FoodItem();
        ghee.setName("Desi Ghee");
        ghee.setCanonicalName("ghee");
        ghee.setQuantity(new Quantity(1.0, "tbsp")); // 1 tbsp = 15g ghee (~135 kcal)
        items.add(ghee);

        response.setAnalyzedFoodItems(items);

        FoodAnalysisResponse result = nutritionService.calculatePlateNutrients(response);
        Map<String, Double> plateMap = extractNutrientMap(result.getTotalPlateNutrients());

        // Dal: 200g * (115/100) = 230 kcal, fat: 7g
        // Ghee: 15g * (900/100) = 135 kcal, fat: 15g
        // Total kcal = 230 + 135 = 365 kcal, total fat = 7 + 15 = 22g
        assertEquals(365.0, plateMap.get(NutrientConstants.CALORIES), 2.0);
        assertEquals(22.0, plateMap.get(NutrientConstants.TOTAL_FAT), 1.0);
    }

    @Test
    @DisplayName("Scenario 11: Duplicate ingredient prevention merges duplicate entries")
    void testDuplicateIngredientMerging() {
        FoodAnalysisResponse response = new FoodAnalysisResponse();
        response.setMealName("Rice plate");

        List<FoodItem> items = new ArrayList<>();
        FoodItem rice1 = new FoodItem();
        rice1.setName("Steamed Rice Portion 1");
        rice1.setCanonicalName("cooked_white_rice");
        rice1.setQuantity(new Quantity(100.0, "g"));
        items.add(rice1);

        FoodItem rice2 = new FoodItem();
        rice2.setName("Steamed Rice Portion 2");
        rice2.setCanonicalName("cooked_white_rice");
        rice2.setQuantity(new Quantity(100.0, "g"));
        items.add(rice2);

        response.setAnalyzedFoodItems(items);

        nutritionService.mergeDuplicateFoodItems(response);

        assertEquals(1, response.getAnalyzedFoodItems().size(), "Duplicate items must be merged into 1 item");
        assertEquals(200.0, response.getAnalyzedFoodItems().get(0).getQuantity().getValue(), "Merged quantity must be 200g");
    }

    @Test
    @DisplayName("Atwater energy factor consistency validation")
    void testAtwaterConsistency() {
        // Consistent profile: 10g protein (40kcal) + 20g carb (80kcal) + 5g fat (45kcal) = 165 kcal
        assertTrue(nutritionService.validateAtwaterEnergyConsistency(165.0, 10.0, 20.0, 5.0, 2.0));

        // Heavily inconsistent profile: claims 500 kcal for 5g protein, 5g carbs, 1g fat (expected ~50 kcal)
        assertFalse(nutritionService.validateAtwaterEnergyConsistency(500.0, 5.0, 5.0, 1.0, 0.0));
    }

    private Map<String, Double> extractNutrientMap(List<FoodNutrient> nutrients) {
        if (nutrients == null) return Map.of();
        return nutrients.stream()
                .filter(n -> n != null && n.getQuantity() != null)
                .collect(Collectors.toMap(FoodNutrient::getName, n -> n.getQuantity().getValue(), (v1, v2) -> v1));
    }
}
