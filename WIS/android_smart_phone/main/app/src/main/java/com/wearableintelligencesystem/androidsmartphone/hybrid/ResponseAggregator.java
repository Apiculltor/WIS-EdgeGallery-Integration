package com.wearableintelligencesystem.androidsmartphone.hybrid;

import androidx.annotation.Nullable;
import org.json.JSONObject;
import org.json.JSONException;

public class ResponseAggregator {

    // Represents the final aggregated response to be sent to Vuzix
    public static class AggregatedResponse {
        private final String wisText; // Placeholder for text derived from WIS processing
        private final String llmText; // Text from LLM
        private final String combinedText; // Simple combination for now
        private final JSONObject originalWisResponseJson; // If WIS produces JSON
        private final JSONObject llmResponseJson; // If LLM interaction provides more structured data

        public AggregatedResponse(@Nullable String wisText, @Nullable String llmText, @Nullable JSONObject originalWisResponseJson) {
            this.wisText = wisText;
            this.llmText = llmText;
            this.originalWisResponseJson = originalWisResponseJson;

            JSONObject tempLlmJson = null;
            if (this.llmText != null) {
                try {
                    tempLlmJson = new JSONObject();
                    tempLlmJson.put("llm_output", this.llmText);
                } catch (JSONException e) {
                    // ignore
                }
            }
            this.llmResponseJson = tempLlmJson;

            StringBuilder sb = new StringBuilder();
            if (wisText != null && !wisText.isEmpty()) {
                sb.append("WIS: ").append(wisText);
            }
            if (llmText != null && !llmText.isEmpty()) {
                if (sb.length() > 0) sb.append("\nLLM: ");
                else sb.append("LLM: ");
                sb.append(llmText);
            }
            this.combinedText = sb.toString();
        }

        public String getWisText() { return wisText; }
        public String getLlmText() { return llmText; }
        public String getCombinedText() { return combinedText; }

        // Method to convert this AggregatedResponse to a JSONObject for sending
        public JSONObject toJSONObject() {
            JSONObject json = new JSONObject();
            try {
                if (originalWisResponseJson != null) {
                     json.put("wis_original_response", originalWisResponseJson);
                }
                if (wisText != null) {
                    json.put("wis_text", wisText);
                }
                if (llmText != null) {
                    json.put("llm_text", llmText);
                }
                if (!combinedText.isEmpty()) {
                    json.put("display_text", combinedText); // Key for Vuzix to display
                }
                // Add more structured fields as needed
            } catch (JSONException e) {
                // Handle exception
            }
            return json;
        }
    }

    // wisResult might be a simple String from traditional WIS, or a more complex object/JSON
    public AggregatedResponse combine(@Nullable Object wisResult, @Nullable String llmResultText) {
        String wisText = null;
        JSONObject wisJson = null;

        if (wisResult instanceof String) {
            wisText = (String) wisResult;
        } else if (wisResult instanceof JSONObject) {
            wisJson = (JSONObject) wisResult;
            wisText = wisJson.optString("summary", wisJson.toString()); // Example
        }
        // Add more types of wisResult handling as needed

        return new AggregatedResponse(wisText, llmResultText, wisJson);
    }
}
