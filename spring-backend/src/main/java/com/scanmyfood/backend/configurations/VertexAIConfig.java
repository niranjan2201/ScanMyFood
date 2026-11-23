package com.scanmyfood.backend.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class VertexAIConfig {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model.name:gemini-flash-latest}")
    private String geminiModelName;

    @Value("${gemini.image.model.name:gemini-flash-latest}")
    private String geminiImageModelName;

    @Bean
    public HttpClient geminiHttpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    public GeminiConfig geminiConfig() {
        return new GeminiConfig(geminiApiKey, geminiModelName, geminiImageModelName);
    }

    public static class GeminiConfig {
        private final String apiKey;
        private final String modelName;
        private final String imageModelName;

        public GeminiConfig(String apiKey, String modelName, String imageModelName) {
            this.apiKey = apiKey;
            this.modelName = modelName;
            this.imageModelName = imageModelName;
        }

        public String getApiKey() { return apiKey; }
        public String getModelName() { return modelName; }
        public String getImageModelName() { return imageModelName; }

        public String getGenerateContentUrl(boolean useImageModel) {
            String model = useImageModel ? imageModelName : modelName;
            return "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        }

        public String getUrlForModel(String model) {
            return "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        }
    }
}
