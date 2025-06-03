package com.wearableintelligencesystem.androidsmartphone.hybrid;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * IntelligentRouter - Routes messages to appropriate processing strategies
 * 
 * Analyzes incoming messages and determines the optimal processing approach
 * based on message type, urgency, complexity, and system availability.
 */
public class IntelligentRouter {
    private static final String TAG = "IntelligentRouter";
    
    /**
     * Determine the optimal processing strategy for a message
     */
    public ProcessingStrategy determineStrategy(JSONObject message) {
        try {
            String messageType = message.getString("message_type");
            String urgency = message.optString("urgency", "normal");
            int complexity = analyzeComplexity(message);
            
            Log.d(TAG, "Routing message: " + messageType + ", urgency: " + urgency + ", complexity: " + complexity);
            
            // Emergency or high urgency - fast processing only
            if ("emergency".equals(urgency) || "urgent".equals(urgency)) {
                return ProcessingStrategy.WIS_ONLY;
            }
            
            // Simple queries that don't benefit from LLM
            if (isSimpleQuery(messageType, message)) {
                return ProcessingStrategy.WIS_ONLY;
            }
            
            // Complex analysis that benefits from LLM
            if (requiresDeepAnalysis(messageType, message)) {
                return ProcessingStrategy.PARALLEL;
            }
            
            // LLM-specific requests
            if (isLlmSpecificRequest(messageType, message)) {
                return ProcessingStrategy.EDGE_GALLERY_ONLY;
            }
            
            // Default to parallel processing for best experience
            return ProcessingStrategy.PARALLEL;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error determining strategy", e);
            return ProcessingStrategy.WIS_ONLY; // Fallback to safe option
        }
    }
    
    /**
     * Analyze message complexity
     */
    private int analyzeComplexity(JSONObject message) {
        int complexity = 1; // Base complexity
        
        try {
            // Image queries are more complex
            if (message.has("image_data")) {
                complexity += 2;
            }
            
            // Audio queries with context
            if (message.has("transcript") && message.has("audio_context")) {
                complexity += 1;
            }
            
            // User queries indicating analysis need
            String query = message.optString("user_query", "").toLowerCase();
            if (query.contains("analyze") || query.contains("explain") || 
                query.contains("what is") || query.contains("describe")) {
                complexity += 2;
            }
            
            // Context availability increases complexity value
            if (message.has("context")) {
                complexity += 1;
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error analyzing complexity", e);
        }
        
        return Math.min(complexity, 5); // Cap at 5
    }
    
    /**
     * Check if message is a simple query
     */
    private boolean isSimpleQuery(String messageType, JSONObject message) {
        // Simple face recognition requests
        if ("face_recognition_request".equals(messageType)) {
            return true;
        }
        
        // Simple voice commands
        if ("voice_command".equals(messageType)) {
            String command = message.optString("command", "").toLowerCase();
            return command.equals("take picture") || command.equals("start recording") || 
                   command.equals("stop recording") || command.equals("navigate");
        }
        
        // Basic object detection
        if ("object_detection_request".equals(messageType)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if message requires deep analysis
     */
    private boolean requiresDeepAnalysis(String messageType, JSONObject message) {
        // Image queries with user questions
        if ("pov_image_query".equals(messageType) && message.has("user_query")) {
            return true;
        }
        
        // Audio transcripts that need enhancement
        if ("audio_chunk_decrypted".equals(messageType)) {
            String transcript = message.optString("transcript", "");
            return transcript.length() > 20; // Longer transcripts benefit from LLM
        }
        
        // Any message explicitly requesting analysis
        String query = message.optString("user_query", "").toLowerCase();
        return query.contains("analyze") || query.contains("explain") || 
               query.contains("summarize") || query.contains("understand");
    }
    
    /**
     * Check if message is LLM-specific
     */
    private boolean isLlmSpecificRequest(String messageType, JSONObject message) {
        // Natural language queries
        if ("natural_language_query".equals(messageType)) {
            return true;
        }
        
        // Explicit LLM requests
        if ("llm_analysis_request".equals(messageType)) {
            return true;
        }
        
        // Complex reasoning requests
        String query = message.optString("user_query", "").toLowerCase();
        return query.contains("tell me about") || query.contains("help me understand") || 
               query.contains("what should i") || query.contains("recommend");
    }
}
