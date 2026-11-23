package com.scanmyfood.backend.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.scanmyfood.backend.configurations.VertexAIConfig;
import com.scanmyfood.backend.constants.NutrientConstants;
import com.scanmyfood.backend.dto.HealthConditionDto;
import com.scanmyfood.backend.mapper.UserIntakeMapper;
import com.scanmyfood.backend.models.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class VertexAiServiceImpl implements AiService {

  private static final Logger logger = LoggerFactory.getLogger(VertexAiServiceImpl.class);

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private HttpClient geminiHttpClient;

  @Autowired
  private VertexAIConfig.GeminiConfig geminiConfig;

  @Autowired
  private UserService userService;

  @Autowired
  private HealthConditionService healthConditionService;

  @Autowired
  private UserIntakeMapper userIntakeMapper;

  // -------------------------------------------------------------------------
  // Core REST helper: call Gemini Developer API
  // -------------------------------------------------------------------------

  /**
   * Call Gemini REST API with text-only prompt with automatic model fallback. Returns the text response.
   */
  private String callGeminiText(String prompt, boolean useImageModel) throws IOException, InterruptedException {
    List<String> modelsToTry = List.of(
        useImageModel ? geminiConfig.getImageModelName() : geminiConfig.getModelName(),
        "gemini-3.5-flash-lite",
        "gemini-flash-latest",
        "gemini-3.1-flash-lite"
    );

    Map<String, Object> part = Map.of("text", prompt);
    Map<String, Object> content = Map.of("parts", List.of(part));
    Map<String, Object> body = Map.of("contents", List.of(content));
    String bodyJson = objectMapper.writeValueAsString(body);

    IOException lastException = null;
    Set<String> triedModels = new HashSet<>();
    for (String model : modelsToTry) {
      if (model == null || model.isBlank() || !triedModels.add(model)) {
        continue;
      }
      String url = geminiConfig.getUrlForModel(model);
      try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
            .build();

        HttpResponse<String> response = geminiHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
          logger.info("Gemini text success with model: {}", model);
          return extractTextFromGeminiResponse(response.body());
        } else {
          logger.warn("Gemini text error with model {}: {} - {}", model, response.statusCode(), response.body());
          lastException = new IOException("Gemini API error " + response.statusCode() + " (" + model + "): " + response.body());
        }
      } catch (IOException e) {
        logger.warn("Gemini text exception with model {}: {}", model, e.getMessage());
        lastException = e;
      }
    }
    throw lastException != null ? lastException : new IOException("All Gemini models failed for text generation");
  }

  /**
   * Call Gemini REST API with text + one or more image parts with automatic model fallback. Returns text response.
   */
  private String callGeminiMultimodal(String prompt, List<MultipartFile> images, boolean useImageModel)
      throws IOException, InterruptedException {
    List<String> modelsToTry = List.of(
        useImageModel ? geminiConfig.getImageModelName() : geminiConfig.getModelName(),
        "gemini-3.5-flash-lite",
        "gemini-flash-latest",
        "gemini-3.1-flash-lite"
    );

    List<Map<String, Object>> parts = new ArrayList<>();
    parts.add(Map.of("text", prompt));
    for (MultipartFile image : images) {
      String mimeType = determineMimeType(image);
      String base64Data = Base64.getEncoder().encodeToString(image.getBytes());
      parts.add(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64Data)));
    }
    Map<String, Object> content = Map.of("parts", parts);
    Map<String, Object> body = Map.of("contents", List.of(content));
    String bodyJson = objectMapper.writeValueAsString(body);

    IOException lastException = null;
    Set<String> triedModels = new HashSet<>();
    for (String model : modelsToTry) {
      if (model == null || model.isBlank() || !triedModels.add(model)) {
        continue;
      }
      String url = geminiConfig.getUrlForModel(model);
      try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
            .build();

        HttpResponse<String> response = geminiHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
          logger.info("Gemini multimodal success with model: {}", model);
          return extractTextFromGeminiResponse(response.body());
        } else {
          logger.warn("Gemini multimodal error with model {}: {} - {}", model, response.statusCode(), response.body());
          lastException = new IOException("Gemini API error " + response.statusCode() + " (" + model + "): " + response.body());
        }
      } catch (IOException e) {
        logger.warn("Gemini multimodal exception with model {}: {}", model, e.getMessage());
        lastException = e;
      }
    }
    throw lastException != null ? lastException : new IOException("All Gemini models failed for multimodal generation");
  }

  /**
   * Extract text from Gemini REST API JSON response.
   */
  private String extractTextFromGeminiResponse(String responseBody) throws IOException {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode candidates = root.path("candidates");
    if (candidates.isMissingNode() || candidates.isEmpty()) {
      throw new IOException("No candidates in Gemini response: " + responseBody);
    }
    JsonNode parts = candidates.get(0).path("content").path("parts");
    if (parts.isMissingNode() || parts.isEmpty()) {
      throw new IOException("No parts in Gemini response: " + responseBody);
    }
    // Look for text parts and concatenate all of them
    StringBuilder sb = new StringBuilder();
    for (JsonNode part : parts) {
      if (part.has("text")) {
        sb.append(part.get("text").asText());
      }
    }
    if (sb.length() > 0) {
      return sb.toString();
    }
    throw new IOException("No text part in Gemini response: " + responseBody);
  }

  /**
   * Extract image bytes from Gemini REST response (inlineData).
   */
  private byte[] extractImageFromGeminiResponse(String responseBody) throws IOException {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode candidates = root.path("candidates");
    if (candidates.isMissingNode() || candidates.isEmpty()) {
      throw new IOException("No candidates in Gemini image response");
    }
    JsonNode parts = candidates.get(0).path("content").path("parts");
    for (JsonNode part : parts) {
      if (part.has("inlineData")) {
        String b64 = part.path("inlineData").path("data").asText();
        return Base64.getDecoder().decode(b64);
      }
      if (part.has("inline_data")) {
        String b64 = part.path("inline_data").path("data").asText();
        return Base64.getDecoder().decode(b64);
      }
    }
    throw new IOException("No image data in Gemini response");
  }

  // -------------------------------------------------------------------------
  // AiService implementations
  // -------------------------------------------------------------------------

  @Override
  public ProductAnalysisResponse analyzeProductImages(MultipartFile frontImage, MultipartFile labelImage,
      String userId) {

    try {
      User user = null;
      List<HealthConditionDto> conditions = null;
      Double bmi = null;
      String bmiCategory = "Unknown";

      try {
        if (userId != null && !userId.trim().isEmpty()) {
          user = userService.getUserByUserId(userId);
          conditions = healthConditionService.getUserConditions(userId);

          if (user.getWeightKg() != null && user.getHeightFeet() != null && user.getHeightInches() != null) {
            int totalInches = (user.getHeightFeet() * 12) + user.getHeightInches();
            double heightMeters = totalInches * 0.0254;
            if (heightMeters > 0) {
              bmi = user.getWeightKg() / (heightMeters * heightMeters);
              bmi = Math.round(bmi * 10.0) / 10.0;
              if (bmi < 18.5) bmiCategory = "Underweight";
              else if (bmi < 25) bmiCategory = "Normal weight";
              else if (bmi < 30) bmiCategory = "Overweight";
              else bmiCategory = "Obese";
            }
          }
        }
      } catch (Exception e) {
        logger.warn("Could not retrieve user profile context for AI personalization: {}", e.getMessage());
      }

      StringBuilder contextBuilder = new StringBuilder();
      contextBuilder.append("USER HEALTH PROFILE & CONTEXT:\n");
      if (user != null) {
        contextBuilder.append(String.format("- Dietary Preference: %s\n",
            user.getDietaryPreference() != null ? user.getDietaryPreference().name() : "None"));
        contextBuilder.append(String.format("- Health Goal: %s\n", user.getGoal() != null ? user.getGoal().name() : "None"));
        contextBuilder.append(String.format("- BMI: %s (%s)\n", bmi != null ? bmi : "Unknown", bmiCategory));
        contextBuilder.append(String.format("- Region/Country: %s\n", user.getCountry() != null ? user.getCountry() : "Global"));
        if (conditions != null && !conditions.isEmpty()) {
          contextBuilder.append("- Pre-existing Health Conditions:\n");
          for (HealthConditionDto cond : conditions) {
            contextBuilder.append(String.format("  * %s: %s\n", cond.getName(), cond.getDescription()));
            if (cond.getNutrientsToLimit() != null && !cond.getNutrientsToLimit().isEmpty()) {
              contextBuilder.append(String.format("    - Nutrients to Limit: %s\n", String.join(", ", cond.getNutrientsToLimit())));
            }
            if (cond.getNutrientsToIncrease() != null && !cond.getNutrientsToIncrease().isEmpty()) {
              contextBuilder.append(String.format("    - Nutrients to Increase: %s\n", String.join(", ", cond.getNutrientsToIncrease())));
            }
            if (cond.getIngredientsToLimit() != null && !cond.getIngredientsToLimit().isEmpty()) {
              contextBuilder.append(String.format("    - Ingredients to Limit: %s\n", String.join(", ", cond.getIngredientsToLimit())));
            }
          }
        } else {
          contextBuilder.append("- Pre-existing Health Conditions: None declared\n");
        }
      } else {
        contextBuilder.append("- No user health profile available. Perform general scientific analysis.\n");
      }

      String userContext = contextBuilder.toString();
      String prompt = """
          %s

          Analyze the food product, product name and its nutrition label. Provide response in this strict JSON format:
          {
            "product": {
              "name": "Product name from front image",
              "category": "Food category (e.g., snack, beverage, etc.)"
            },
            "nutrition_analysis": {
              "total_quantity": {"value": 0, "unit": "unit in packet"},
              "serving_size": {"value": 0, "unit": "unit in packet"},
              "nutrients": [
                {
                  "name": "nutrient name in snake_case from the list below",
                  "quantity": {"value": "Quantity of nutrient in per serving of food product", "unit": "unit in packet"},
                  "daily_value": 0,
                  "dv_status": "High/Moderate/Low based on calculated DV%%",
                  "goal": "Goal of consumption of a nutrient can be - 'At least' or 'Less than' based on recommended DV%%",
                  "health_impact": "Good/Moderate/Bad"
                }
              ],
              "primary_concerns": [
                {
                  "issue": "Primary nutritional concern",
                  "explanation": "Brief explanation of health impact",
                  "recommendations": [
                    {
                      "food": "Complementary food to add",
                      "quantity": "Recommended quantity to add",
                      "reasoning": "How this helps balance nutrition"
                    }
                  ]
                }
              ]
            }
          }

          Strictly follow these rules:
          1. MUST include ALL nutrients from this list: %s
          2. If a nutrient is not found on the label, set its value to 0.0 and use standard unit
          3. Extract total nutrient (e.g., Total Sugars, Total Fat) and its sub-nutrients if mentioned
          4. Calculate DV%% using these reference values: %s
             Formula: (nutrient_quantity_per_serving / daily_value_reference) × 100
          5. Return calculated DV%% in double, not the ones found in label
          6. Do not include any extra characters or formatting outside of the JSON object
          7. Use accurate escape sequences for any special characters
          8. For primary_concerns, focus on major nutritional imbalances
          9. For recommendations suggest complementary foods
          10. Use %%DV guidelines: 5%% DV or less is Low, 20%% DV or more is High, between is Moderate
          11. For health_impact: 'At least' nutrients - High=Good, Moderate=Moderate, Low=Bad; 'Less than' nutrients - Low=Good, Moderate=Moderate, High=Bad
          12. PERSONALIZATION: Tailor primary_concerns to user's health conditions, goal, and BMI. Suggest regionally appropriate foods.
          """
          .formatted(userContext, NutrientConstants.ALL_NUTRIENT_NAMES, NutrientConstants.DAILY_VALUES_REFERENCE);

      String responseText = callGeminiMultimodal(prompt, List.of(frontImage, labelImage), false);
      String jsonString = extractJsonString(responseText);
      return objectMapper.readValue(jsonString, ProductAnalysisResponse.class);

    } catch (Exception e) {
      logger.error("Error analyzing product images", e);
      throw new RuntimeException("Failed to analyze product images: " + e.getMessage());
    }
  }

  @Override
  public FoodAnalysisResponse analyzeFoodImage(MultipartFile imageFile, String description, String userId) {
    try {
      User user = null;
      try {
        if (userId != null && !userId.trim().isEmpty()) {
          user = userService.getUserByUserId(userId);
        }
      } catch (Exception e) {
        logger.warn("Could not retrieve user context for food analysis localization: {}", e.getMessage());
      }

      String country = (user != null && user.getCountry() != null) ? user.getCountry() : "Global";

      StringBuilder promptBuilder = new StringBuilder();
      promptBuilder.append(String.format("USER REGION/COUNTRY: %s\n\n", country));
      promptBuilder.append("Analyze this food image and break down each visible food item.\n");
      if (description != null && !description.trim().isEmpty()) {
        promptBuilder.append(String.format(
            "User's description/context: \"%s\". Use this to guide identification and portion estimation.\n",
            description));
      }
      promptBuilder.append(String.format(
          """
              Provide response in this strict JSON format:
              {
                "meal_name": "Name of the meal",
                "analyzed_food_items": [
                  {
                    "name": "Regional Name of the food item",
                    "canonical_name": "Unique lowercase snake_case English name, e.g., 'roti', 'idli', 'basmati_rice'",
                    "quantity": {"value": 0, "unit": "g"},
                    "nutrients": [
                      {"name": "calories", "quantity": {"value": 0, "unit": "kcal"}},
                      {"name": "protein", "quantity": {"value": 0, "unit": "g"}},
                      {"name": "total_carbohydrate", "quantity": {"value": 0, "unit": "g"}},
                      {"name": "total_fat", "quantity": {"value": 0, "unit": "g"}},
                      {"name": "dietary_fiber", "quantity": {"value": 0, "unit": "g"}},
                      {"name": "total_sugars", "quantity": {"value": 0, "unit": "g"}},
                      {"name": "sodium", "quantity": {"value": 0, "unit": "mg"}}
                    ]
                  }
                ],
                "total_plate_nutrients": [
                  {"name": "calories", "quantity": {"value": 0, "unit": "kcal"}},
                  {"name": "protein", "quantity": {"value": 0, "unit": "g"}},
                  {"name": "total_carbohydrate", "quantity": {"value": 0, "unit": "g"}},
                  {"name": "total_fat", "quantity": {"value": 0, "unit": "g"}},
                  {"name": "dietary_fiber", "quantity": {"value": 0, "unit": "g"}},
                  {"name": "total_sugars", "quantity": {"value": 0, "unit": "g"}},
                  {"name": "sodium", "quantity": {"value": 0, "unit": "mg"}}
                ]
              }

              Rules:
              1. Use visual cues to estimate portions
              2. Provide nutrients for the estimated total quantity of each food item
              3. Use USDA FoodData Central values where possible
              4. LOCALIZATION: Return meal_name and food item names in the most common localized names for user's region (%s).
              """,
          country));
      String prompt = promptBuilder.toString();

      String responseText = callGeminiMultimodal(prompt, List.of(imageFile), false);
      String jsonString = extractJsonString(responseText);
      FoodAnalysisResponse responseObj = objectMapper.readValue(jsonString, FoodAnalysisResponse.class);
      enrichFoodAnalysisResponse(responseObj, userId, "SM");
      return responseObj;

    } catch (Exception e) {
      logger.error("Error analyzing food image", e);
      throw new RuntimeException("Failed to analyze food image: " + e.getMessage());
    }
  }

  @Override
  public FoodAnalysisResponse analyzeFoodDescription(String description, String userId) {
    try {
      User user = null;
      try {
        if (userId != null && !userId.trim().isEmpty()) {
          user = userService.getUserByUserId(userId);
        }
      } catch (Exception e) {
        logger.warn("Could not retrieve user context for food analysis localization: {}", e.getMessage());
      }

      String country = (user != null && user.getCountry() != null) ? user.getCountry() : "Global";

      String prompt = """
          USER REGION/COUNTRY: %s

          You are a highly qualified nutritionist. Analyze these food items (consider cooked form where applicable) and their quantities:

          %s

          Generate nutritional info for each food item and respond in this JSON schema:
          {
            "meal_name": "Name of the food item/meal",
            "analyzed_food_items": [
              {
                "name": "Regional Name of the food item",
                "canonical_name": "Unique lowercase snake_case English name, e.g., 'roti', 'idli', 'basmati_rice'",
                "quantity": {"value": 0, "unit": "g"},
                "nutrients": [
                  {"name": "calories", "quantity": {"value": 0, "unit": "kcal"}},
                  {"name": "protein", "quantity": {"value": 0, "unit": "g"}},
                  {"name": "total_carbohydrate", "quantity": {"value": 0, "unit": "g"}},
                  {"name": "total_fat", "quantity": {"value": 0, "unit": "g"}},
                  {"name": "dietary_fiber", "quantity": {"value": 0, "unit": "g"}},
                  {"name": "total_sugars", "quantity": {"value": 0, "unit": "g"}},
                  {"name": "sodium", "quantity": {"value": 0, "unit": "mg"}}
                ]
              }
            ],
            "total_plate_nutrients": [
              {"name": "calories", "quantity": {"value": 0, "unit": "kcal"}},
              {"name": "protein", "quantity": {"value": 0, "unit": "g"}},
              {"name": "total_carbohydrate", "quantity": {"value": 0, "unit": "g"}},
              {"name": "total_fat", "quantity": {"value": 0, "unit": "g"}},
              {"name": "dietary_fiber", "quantity": {"value": 0, "unit": "g"}},
              {"name": "total_sugars", "quantity": {"value": 0, "unit": "g"}},
              {"name": "sodium", "quantity": {"value": 0, "unit": "mg"}}
            ]
          }

          Rules:
          1. Use USDA FoodData Central values, or EFSA if unavailable.
          2. Convert all measurements to g or ml. If unit not specified, assume grams.
          3. Round all nutritional values to one decimal place.
          4. LOCALIZATION: Return meal_name and food item names using the most common localized names for user's region (%s).
          """
          .formatted(country, description, country);

      String responseText = callGeminiText(prompt, false);
      String jsonString = extractJsonString(responseText);
      FoodAnalysisResponse responseObj = objectMapper.readValue(jsonString, FoodAnalysisResponse.class);
      enrichFoodAnalysisResponse(responseObj, userId, "SD");
      return responseObj;

    } catch (Exception e) {
      logger.error("Error analyzing food description", e);
      throw new RuntimeException("Failed to analyze food description: " + e.getMessage());
    }
  }

  @Override
  public byte[] generateFoodImage(String foodDescription) {
    try {
      String prompt = String.format(
          "Generate a professional food photography image of %s. " +
              "Requirements: " +
              "- Position the primary food item in the top 50%% of the frame, leaving the lower 50%% as a clean table surface for UI overlays " +
              "- Serve on appropriate cutlery and dinnerware based on the food type " +
              "- Proper plating with accurate portion sizes " +
              "- Natural soft daylight lighting from the side " +
              "- Shot from a 45-degree angle in landscape format (16:9 aspect ratio) " +
              "- Shallow depth of field, Canon EOS R5 photography style " +
              "- 8K resolution, photorealistic, restaurant quality presentation " +
              "- Professional food styling with vibrant colors and fresh ingredients " +
              "- Slightly blurred background with warm neutral tones " +
              "- No hands, people, or text visible - focus entirely on the food " +
              "- Add steam if it's a hot dish for realism",
          foodDescription);

      logger.info("Generating food image with Gemini for: {}", foodDescription);

      String url = geminiConfig.getGenerateContentUrl(true);
      List<Map<String, Object>> parts = List.of(Map.of("text", prompt));
      Map<String, Object> content = Map.of("parts", parts);
      Map<String, Object> generationConfig = Map.of("responseModalities", List.of("IMAGE", "TEXT"));
      Map<String, Object> body = Map.of("contents", List.of(content), "generationConfig", generationConfig);
      String bodyJson = objectMapper.writeValueAsString(body);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
          .build();

      HttpResponse<String> response = geminiHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IOException("Gemini API error " + response.statusCode() + ": " + response.body());
      }

      byte[] imageBytes = extractImageFromGeminiResponse(response.body());
      logger.info("Successfully generated food image, size: {} bytes", imageBytes.length);
      if (imageBytes.length == 0) {
        throw new RuntimeException("Image data is empty");
      }
      return imageBytes;

    } catch (Exception e) {
      logger.error("Error generating food image", e);
      throw new RuntimeException("Failed to generate food image: " + e.getMessage(), e);
    }
  }

  // -------------------------------------------------------------------------
  // Utility methods
  // -------------------------------------------------------------------------

  private String determineMimeType(MultipartFile file) {
    String mimeType = file.getContentType();
    if (mimeType == null || mimeType.equals("application/octet-stream")) {
      String filename = file.getOriginalFilename();
      if (filename != null) {
        if (filename.toLowerCase().endsWith(".png")) return "image/png";
        if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) return "image/jpeg";
      }
      return "image/jpeg";
    }
    return mimeType;
  }

  private String extractJsonString(String responseText) throws IOException {
    Pattern pattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(responseText);
    if (matcher.find()) {
      return matcher.group(0);
    } else {
      throw new IOException("No valid JSON found in the response");
    }
  }

  private void enrichFoodAnalysisResponse(FoodAnalysisResponse response, String userId, String sourceOfIntake) {
    if (userId == null || userId.trim().isEmpty() || response == null) return;
    try {
      User user = userService.getUserByUserId(userId);
      String countryCode = CountryCode.fromCountryName(user.getCountry()).getCode();
      String canonicalMealName = response.getMealName().toLowerCase().trim().replaceAll("\\s+", "_");

      if ("SD".equalsIgnoreCase(sourceOfIntake)) {
        MealDictionaryRecord dbMeal = userIntakeMapper.findMealInDictionary(canonicalMealName);
        if (dbMeal != null) {
          logger.info("SD scan: Found standardized recipe in meal_dictionary for {}", dbMeal.getDisplayMealName());
          response.setMealName(dbMeal.getDisplayMealName());

          List<FoodNutrient> mealNutrients = new ArrayList<>();
          if (dbMeal.getCaloriesValue() != null)
            mealNutrients.add(new FoodNutrient(NutrientConstants.CALORIES, new Quantity(dbMeal.getCaloriesValue(), dbMeal.getCaloriesUnit())));
          if (dbMeal.getProteinValue() != null)
            mealNutrients.add(new FoodNutrient(NutrientConstants.PROTEIN, new Quantity(dbMeal.getProteinValue(), dbMeal.getProteinUnit())));
          if (dbMeal.getTotalCarbohydrateValue() != null)
            mealNutrients.add(new FoodNutrient(NutrientConstants.TOTAL_CARBOHYDRATE, new Quantity(dbMeal.getTotalCarbohydrateValue(), dbMeal.getTotalCarbohydrateUnit())));
          if (dbMeal.getTotalFatValue() != null)
            mealNutrients.add(new FoodNutrient(NutrientConstants.TOTAL_FAT, new Quantity(dbMeal.getTotalFatValue(), dbMeal.getTotalFatUnit())));
          if (dbMeal.getDietaryFiberValue() != null)
            mealNutrients.add(new FoodNutrient(NutrientConstants.DIETARY_FIBER, new Quantity(dbMeal.getDietaryFiberValue(), dbMeal.getDietaryFiberUnit())));
          if (dbMeal.getTotalSugarsValue() != null)
            mealNutrients.add(new FoodNutrient(NutrientConstants.TOTAL_SUGARS, new Quantity(dbMeal.getTotalSugarsValue(), dbMeal.getTotalSugarsUnit())));
          if (dbMeal.getSodiumValue() != null)
            mealNutrients.add(new FoodNutrient(NutrientConstants.SODIUM, new Quantity(dbMeal.getSodiumValue(), dbMeal.getSodiumUnit())));
          response.setTotalPlateNutrients(mealNutrients);

          if (dbMeal.getRecipeItems() != null && !dbMeal.getRecipeItems().isEmpty()) {
            List<FoodItem> foodItems = new ArrayList<>();
            for (RecipeItem recipeItem : dbMeal.getRecipeItems()) {
              FoodItem foodItem = new FoodItem();
              foodItem.setCanonicalName(recipeItem.getCanonicalName());
              foodItem.setName(recipeItem.getName());
              foodItem.setQuantity(new Quantity(recipeItem.getValue(), recipeItem.getUnit()));

              FoodItemRecord dbFoodItem = userIntakeMapper.findFoodItemByCanonical(recipeItem.getCanonicalName());
              if (dbFoodItem != null) {
                double portionFactor = recipeItem.getValue() / 100.0;
                List<FoodNutrient> itemNutrients = new ArrayList<>();
                itemNutrients.add(new FoodNutrient(NutrientConstants.CALORIES, new Quantity(round(safeDouble(dbFoodItem.getCaloriesValuePer100g()) * portionFactor), dbFoodItem.getCaloriesUnit())));
                itemNutrients.add(new FoodNutrient(NutrientConstants.PROTEIN, new Quantity(round(safeDouble(dbFoodItem.getProteinValuePer100g()) * portionFactor), dbFoodItem.getProteinUnit())));
                itemNutrients.add(new FoodNutrient(NutrientConstants.TOTAL_CARBOHYDRATE, new Quantity(round(safeDouble(dbFoodItem.getTotalCarbohydrateValuePer100g()) * portionFactor), dbFoodItem.getTotalCarbohydrateUnit())));
                itemNutrients.add(new FoodNutrient(NutrientConstants.TOTAL_FAT, new Quantity(round(safeDouble(dbFoodItem.getTotalFatValuePer100g()) * portionFactor), dbFoodItem.getTotalFatUnit())));
                itemNutrients.add(new FoodNutrient(NutrientConstants.DIETARY_FIBER, new Quantity(round(safeDouble(dbFoodItem.getDietaryFiberValuePer100g()) * portionFactor), dbFoodItem.getDietaryFiberUnit())));
                itemNutrients.add(new FoodNutrient(NutrientConstants.TOTAL_SUGARS, new Quantity(round(safeDouble(dbFoodItem.getTotalSugarsValuePer100g()) * portionFactor), dbFoodItem.getTotalSugarsUnit())));
                itemNutrients.add(new FoodNutrient(NutrientConstants.SODIUM, new Quantity(round(safeDouble(dbFoodItem.getSodiumValuePer100g()) * portionFactor), dbFoodItem.getSodiumUnit())));
                foodItem.setNutrients(itemNutrients);
              }
              foodItems.add(foodItem);
            }
            response.setAnalyzedFoodItems(foodItems);
          }
          return;
        }
      }

      List<RecipeItem> recipeItemsForCatalog = new ArrayList<>();
      if (response.getAnalyzedFoodItems() != null) {
        for (FoodItem foodItem : response.getAnalyzedFoodItems()) {
          FoodItemRecord dbFoodItem = userIntakeMapper.findFoodItemByAlias(foodItem.getName(), countryCode);
          boolean foundByAlias = (dbFoodItem != null);
          if (dbFoodItem == null) dbFoodItem = userIntakeMapper.findFoodItemByCanonical(foodItem.getCanonicalName());

          double actualQuantityValue = foodItem.getQuantity() != null ? foodItem.getQuantity().getValue() : 100.0;
          if (actualQuantityValue <= 0) actualQuantityValue = 100.0;

          if (dbFoodItem != null) {
            foodItem.setCanonicalName(dbFoodItem.getCanonicalName());
            if (foundByAlias) foodItem.setName(dbFoodItem.getItemName());
            double portionFactor = actualQuantityValue / 100.0;
            List<FoodNutrient> overriddenNutrients = new ArrayList<>();
            overriddenNutrients.add(new FoodNutrient(NutrientConstants.CALORIES, new Quantity(round(safeDouble(dbFoodItem.getCaloriesValuePer100g()) * portionFactor), dbFoodItem.getCaloriesUnit())));
            overriddenNutrients.add(new FoodNutrient(NutrientConstants.PROTEIN, new Quantity(round(safeDouble(dbFoodItem.getProteinValuePer100g()) * portionFactor), dbFoodItem.getProteinUnit())));
            overriddenNutrients.add(new FoodNutrient(NutrientConstants.TOTAL_CARBOHYDRATE, new Quantity(round(safeDouble(dbFoodItem.getTotalCarbohydrateValuePer100g()) * portionFactor), dbFoodItem.getTotalCarbohydrateUnit())));
            overriddenNutrients.add(new FoodNutrient(NutrientConstants.TOTAL_FAT, new Quantity(round(safeDouble(dbFoodItem.getTotalFatValuePer100g()) * portionFactor), dbFoodItem.getTotalFatUnit())));
            overriddenNutrients.add(new FoodNutrient(NutrientConstants.DIETARY_FIBER, new Quantity(round(safeDouble(dbFoodItem.getDietaryFiberValuePer100g()) * portionFactor), dbFoodItem.getDietaryFiberUnit())));
            overriddenNutrients.add(new FoodNutrient(NutrientConstants.TOTAL_SUGARS, new Quantity(round(safeDouble(dbFoodItem.getTotalSugarsValuePer100g()) * portionFactor), dbFoodItem.getTotalSugarsUnit())));
            overriddenNutrients.add(new FoodNutrient(NutrientConstants.SODIUM, new Quantity(round(safeDouble(dbFoodItem.getSodiumValuePer100g()) * portionFactor), dbFoodItem.getSodiumUnit())));
            foodItem.setNutrients(overriddenNutrients);
          } else {
            double factor = 100.0 / actualQuantityValue;
            Map<String, Quantity> itemNutrientMap = foodItem.getNutrients() != null
                ? foodItem.getNutrients().stream().collect(Collectors.toMap(FoodNutrient::getName, FoodNutrient::getQuantity, (v1, v2) -> v1))
                : Collections.emptyMap();
            userIntakeMapper.insertOrUpdateFoodItemDictionary(
                foodItem.getCanonicalName(),
                round(safeGetNutrientVal(itemNutrientMap, NutrientConstants.CALORIES) * factor), safeGetNutrientUnit(itemNutrientMap, NutrientConstants.CALORIES, "kcal"),
                round(safeGetNutrientVal(itemNutrientMap, NutrientConstants.PROTEIN) * factor), safeGetNutrientUnit(itemNutrientMap, NutrientConstants.PROTEIN, "g"),
                round(safeGetNutrientVal(itemNutrientMap, NutrientConstants.TOTAL_CARBOHYDRATE) * factor), safeGetNutrientUnit(itemNutrientMap, NutrientConstants.TOTAL_CARBOHYDRATE, "g"),
                round(safeGetNutrientVal(itemNutrientMap, NutrientConstants.TOTAL_FAT) * factor), safeGetNutrientUnit(itemNutrientMap, NutrientConstants.TOTAL_FAT, "g"),
                round(safeGetNutrientVal(itemNutrientMap, NutrientConstants.DIETARY_FIBER) * factor), safeGetNutrientUnit(itemNutrientMap, NutrientConstants.DIETARY_FIBER, "g"),
                round(safeGetNutrientVal(itemNutrientMap, NutrientConstants.TOTAL_SUGARS) * factor), safeGetNutrientUnit(itemNutrientMap, NutrientConstants.TOTAL_SUGARS, "g"),
                round(safeGetNutrientVal(itemNutrientMap, NutrientConstants.SODIUM) * factor), safeGetNutrientUnit(itemNutrientMap, NutrientConstants.SODIUM, "mg"),
                0.0, "mg",
                false
            );
            userIntakeMapper.insertFoodItemAlias(foodItem.getCanonicalName(), countryCode, foodItem.getName());
          }
          recipeItemsForCatalog.add(new RecipeItem(foodItem.getCanonicalName(), foodItem.getName(), actualQuantityValue, foodItem.getQuantity() != null ? foodItem.getQuantity().getUnit() : "g"));
        }
        recalculateTotalPlateNutrients(response);
      }

      Map<String, Quantity> plateNutrientMap = response.getTotalPlateNutrients() != null
          ? response.getTotalPlateNutrients().stream().collect(Collectors.toMap(FoodNutrient::getName, FoodNutrient::getQuantity, (v1, v2) -> v1))
          : Collections.emptyMap();

      userIntakeMapper.insertOrUpdateMealDictionary(
          canonicalMealName, response.getMealName(), recipeItemsForCatalog, 1.0, "serving",
          safeGetNutrientVal(plateNutrientMap, NutrientConstants.CALORIES), safeGetNutrientUnit(plateNutrientMap, NutrientConstants.CALORIES, "kcal"),
          safeGetNutrientVal(plateNutrientMap, NutrientConstants.PROTEIN), safeGetNutrientUnit(plateNutrientMap, NutrientConstants.PROTEIN, "g"),
          safeGetNutrientVal(plateNutrientMap, NutrientConstants.TOTAL_CARBOHYDRATE), safeGetNutrientUnit(plateNutrientMap, NutrientConstants.TOTAL_CARBOHYDRATE, "g"),
          safeGetNutrientVal(plateNutrientMap, NutrientConstants.TOTAL_FAT), safeGetNutrientUnit(plateNutrientMap, NutrientConstants.TOTAL_FAT, "g"),
          safeGetNutrientVal(plateNutrientMap, NutrientConstants.DIETARY_FIBER), safeGetNutrientUnit(plateNutrientMap, NutrientConstants.DIETARY_FIBER, "g"),
          safeGetNutrientVal(plateNutrientMap, NutrientConstants.TOTAL_SUGARS), safeGetNutrientUnit(plateNutrientMap, NutrientConstants.TOTAL_SUGARS, "g"),
          safeGetNutrientVal(plateNutrientMap, NutrientConstants.SODIUM), safeGetNutrientUnit(plateNutrientMap, NutrientConstants.SODIUM, "mg"),
          false, userId
      );

    } catch (Exception e) {
      logger.warn("Failed to enrich food analysis response with DB values: {}", e.getMessage(), e);
    }
  }

  private double safeGetNutrientVal(Map<String, Quantity> map, String key) {
    Quantity q = map.get(key);
    return q != null ? q.getValue() : 0.0;
  }

  private String safeGetNutrientUnit(Map<String, Quantity> map, String key, String defaultUnit) {
    Quantity q = map.get(key);
    return q != null ? q.getUnit() : defaultUnit;
  }

  private double safeDouble(Double val) {
    return val == null ? 0.0 : val;
  }

  private double round(double val) {
    return Math.round(val * 100.0) / 100.0;
  }

  private void recalculateTotalPlateNutrients(FoodAnalysisResponse response) {
    PlateNutrients plateNutrients = new PlateNutrients();
    for (FoodItem item : response.getAnalyzedFoodItems()) {
      plateNutrients.addFoodItem(item);
    }
    response.setTotalPlateNutrients(plateNutrients.toFoodNutrientList());
  }
}
