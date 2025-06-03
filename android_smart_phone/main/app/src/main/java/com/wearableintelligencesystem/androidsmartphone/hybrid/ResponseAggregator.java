 priority
     */
    private String determinePriority(JSONObject wisResponse, JSONObject llmResponse) {
        try {
            // Check for urgent indicators in WIS response
            if (wisResponse != null) {
                if (wisResponse.has("emergency") && wisResponse.getBoolean("emergency")) {
                    return "urgent";
                }
                
                if (wisResponse.has("face_recognition") && 
                    !wisResponse.getString("face_recognition").isEmpty()) {
                    return "high";
                }
            }
            
            // Check LLM confidence and analysis type
            if (llmResponse != null) {
                float confidence = (float) llmResponse.optDouble("confidence", 0.0);
                if (confidence > 0.9) {
                    return "high";
                } else if (confidence > 0.7) {
                    return "medium";
                }
            }
            
            return "normal";
            
        } catch (JSONException e) {
            Log.w(TAG, "Error determining priority", e);
            return "normal";
        }
    }
    
    /**
     * Create error response
     */
    private JSONObject createErrorResponse(String error) {
        try {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message_type", "aggregation_error");
            errorResponse.put("error", error);
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            // Create minimal display layers
            JSONObject displayLayers = new JSONObject();
            displayLayers.put("immediate", "Processing error");
            displayLayers.put("contextual", error);
            errorResponse.put("display_layers", displayLayers);
            
            return errorResponse;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating error response", e);
            return new JSONObject();
        }
    }
    
    /**
     * Create single-source response (when only one processor responds)
     */
    public JSONObject createSingleSourceResponse(JSONObject response, String source) {
        try {
            JSONObject singleResponse = new JSONObject();
            singleResponse.put("message_type", "single_source_response");
            singleResponse.put("source", source);
            singleResponse.put("timestamp", System.currentTimeMillis());
            
            // Add the response data
            singleResponse.put("results", response);
            
            // Create display layers
            JSONObject displayLayers = createDisplayLayers(
                "WIS".equals(source) ? response : null,
                "EdgeGallery".equals(source) ? response : null
            );
            singleResponse.put("display_layers", displayLayers);
            
            return singleResponse;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating single source response", e);
            return createErrorResponse("Failed to create response");
        }
    }
}
