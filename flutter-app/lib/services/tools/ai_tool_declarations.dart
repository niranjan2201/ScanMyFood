class AiToolDeclarations {
  static List<Map<String, dynamic>> get restTools => [
        {
          'function_declarations': [
            {
              'name': 'getUserProfile',
              'description':
                  'Fetches the user\'s health profile including weight, height, BMI, dietary preferences, health conditions, and fitness goals. No PII is returned.',
              'parameters': {
                'type': 'OBJECT',
                'properties': <String, dynamic>{},
              },
            },
            {
              'name': 'getDailyIntake',
              'description':
                  'Fetches the user\'s logged meals and daily nutrient intake summary for a given date range.',
              'parameters': {
                'type': 'OBJECT',
                'properties': {
                  'from_date': {
                    'type': 'STRING',
                    'description':
                        'The start date for the query in YYYY-MM-DD format.',
                  },
                  'to_date': {
                    'type': 'STRING',
                    'description':
                        'The end date for the query in YYYY-MM-DD format.',
                  },
                },
              },
            },
            {
              'name': 'logMealViaDescription',
              'description':
                  'Analyzes a natural language meal description to extract nutritional data and saves it as a meal intake record. Returns the meal details.',
              'parameters': {
                'type': 'OBJECT',
                'properties': {
                  'description': {
                    'type': 'STRING',
                    'description':
                        'Natural language description of the meal (e.g. "2 eggs and toast with butter")',
                  },
                },
                'required': ['description'],
              },
            },
            {
              'name': 'saveScannedFoodIntake',
              'description':
                  'Saves the analyzed meal details into the user\'s daily intake log. Call this only after the user explicitly confirms they want to log the meal.',
              'parameters': {
                'type': 'OBJECT',
                'properties': {
                  'food_analysis_response': {
                    'type': 'OBJECT',
                    'description': 'The food analysis response details to save',
                  },
                  'source_of_intake': {
                    'type': 'STRING',
                    'description':
                        'The source of the intake, e.g., "SD" for scanned description.',
                  },
                  'created_at': {
                    'type': 'STRING',
                    'description':
                        'The ISO-8601 consumed date and time (e.g. "2026-07-02T12:00:00"). Optional, defaults to now.',
                  },
                },
                'required': ['food_analysis_response'],
              },
            },
          ],
        },
      ];

  static String humanLabel(String toolName, Map<String, dynamic> args) {
    switch (toolName) {
      case 'getUserProfile':
        return 'Fetching your profile...';
      case 'getDailyIntake':
        return 'Fetching your daily intake...';
      case 'logMealViaDescription':
        final desc = args['description'] ?? 'meal';
        return 'Logging "$desc"...';
      case 'saveScannedFoodIntake':
        final mealName = args['food_analysis_response']?['meal_name'] ?? 'meal';
        return 'Saving "$mealName" to your intake...';
      default:
        return 'Processing...';
    }
  }
}
