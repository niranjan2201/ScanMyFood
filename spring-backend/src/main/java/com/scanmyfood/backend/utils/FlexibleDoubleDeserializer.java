package com.scanmyfood.backend.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Deserializer that handles Gemini returning nutrient values as either:
 * - A proper JSON number: {"value": 25.5, "unit": "g"}
 * - A string-encoded number: {"value": "25.5", "unit": "g"}
 * - A descriptive string placeholder: {"value": "Quantity of nutrient...", "unit": "g"} → null
 * - null or missing → null
 */
public class FlexibleDoubleDeserializer extends StdDeserializer<Double> {

    public FlexibleDoubleDeserializer() {
        super(Double.class);
    }

    @Override
    public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NUMBER_FLOAT || p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return p.getDoubleValue();
        }
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
            if (text.isEmpty()) return null;
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                // Gemini returned a descriptive string like "Quantity of nutrient in per serving..."
                // Treat as 0.0 (not null) so it doesn't break downstream arithmetic
                return 0.0;
            }
        }
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        return null;
    }
}
