package com.wearableintelligencesystem.androidsmartphone.edgegallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * EdgeGalleryLlmProcessor - Integrates EdgeGallery LLM capabilities with WIS
 * 
 * This class processes image and audio data using EdgeGallery's Gemma 3n models
 * running on the Samsung S24 Ultra. It maintains compatibility with the existing
 * WIS messaging system while adding advanced LLM analysis capabilities.
 * 
 * @author WIS + EdgeGallery Integration Team
 * @version 1.0
 */
public class EdgeGalleryLlmProcessor {
    private static final String TAG = "EdgeGalleryLlmProcessor";
    
    // Context and core components
    private Context context;
    private EdgeGalleryApiClient apiClient;
    private PromptTemplateManager promptManager;
    
    // Response management
    private PublishSubject<JSONObject> responseObservable;
    private ExecutorService processingExecutor;
    
    // Configuration flags
    private boolean isInitialized = false;
    private boolean isModelAvailable = false;
    
    public EdgeGalleryLlmProcessor(Context context) {
        this.context = context;
        this.responseObservable = PublishSubject.create();
        this.processingExecutor = Executors.newFixedThreadPool(2);
        this.promptManager = new PromptTemplateManager();
        
        initializeAsync();
    }
    
    /**
     * Initialize EdgeGallery connection asynchronously
     */
    private void initializeAsync() {
        processingExecutor.execute(() -> {
            try {
                Log.d(TAG, "Initializing EdgeGallery LLM Processor...");
                
                // Initialize API client for communication with EdgeGallery on S24 Ultra
                apiClient = new EdgeGalleryApiClient();
                
                // Test connection and model availability
                isModelAvailable = testModelAvailability();
                isInitialized = true;
                
                Log.d(TAG, "EdgeGallery LLM Processor initialized. Model available: " + isModelAvailable);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize EdgeGallery LLM Processor", e);
                isInitialized = true; // Set to true even on failure for fallback behavior
                isModelAvailable = false;
            }
        });
    }
    
    /**
     * Test if the LLM model is available on the S24 Ultra
     */
    private boolean testModelAvailability() {
        try {
            // Simple ping to EdgeGallery service
            JSONObject testRequest = new JSONObject();
            testRequest.put("action", "ping");
            testRequest.put("timestamp", System.currentTimeMillis());
            
            // This would normally make a network call to the S24 Ultra
            // For now, we'll simulate availability
            Log.d(TAG, "Testing EdgeGallery model availability...");
            
            // TODO: Implement actual network call to S24 Ultra EdgeGallery service
            // For development, assume model is available
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Model availability test failed", e);
            return false;
        }
    }
    
    /**
     * Process image query with LLM enhancement
     */
    public void processImageQuery(JSONObject message) {
        if (!isInitialized || !isModelAvailable) {
            Log.w(TAG, "EdgeGallery not available, skipping LLM processing");
            sendErrorResponse(message, "EdgeGallery LLM not available");
            return;
        }
        
        processingExecutor.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Extract image data
                String imageBase64 = message.getString("image_data");
                String userQuery = message.optString("user_query", "Analyze this image in detail");
                long timestamp = message.optLong("timestamp", System.currentTimeMillis());
                
                // Get context information
                JSONObject context = message.optJSONObject("context");
                
                // Convert base64 to bitmap for validation
                Bitmap bitmap = base64ToBitmap(imageBase64);
                if (bitmap == null) {
                    sendErrorResponse(message, "Invalid image data");
                    return;
                }
                
                // Build enhanced prompt with context
                String enhancedPrompt = promptManager.buildImageAnalysisPrompt(
                    userQuery, 
                    context,
                    getCurrentContextualInfo()
                );
                
                // Send to EdgeGallery for processing
                LlmAnalysisResult result = processWithEdgeGallery(
                    imageBase64, 
                    enhancedPrompt, 
                    "image_analysis"
                );
                
                if (result != null) {
                    // Create enhanced response
                    JSONObject response = createImageAnalysisResponse(
                        message, 
                        result, 
                        System.currentTimeMillis() - startTime
                    );
                    
                    // Send via observable
                    responseObservable.onNext(response);
                    
                    Log.d(TAG, "Image analysis completed in " + (System.currentTimeMillis() - startTime) + "ms");
                } else {
                    sendErrorResponse(message, "EdgeGallery processing failed");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing image query", e);
                sendErrorResponse(message, e.getMessage());
            }
        });
    }
    
    /**
     * Process audio transcript with LLM enhancement
     */
    public void processAudioTranscript(JSONObject message) {
        if (!isInitialized || !isModelAvailable) {
            Log.w(TAG, "EdgeGallery not available, skipping LLM processing");
            sendErrorResponse(message, "EdgeGallery LLM not available");
            return;
        }
        
        processingExecutor.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Extract transcript data
                String transcript = message.getString("transcript");
                String audioContext = message.optString("audio_context", "");
                long timestamp = message.optLong("timestamp", System.currentTimeMillis());
                
                // Build enhanced prompt for audio analysis
                String enhancedPrompt = promptManager.buildAudioAnalysisPrompt(
                    transcript,
                    audioContext,
                    getCurrentContextualInfo()
                );
                
                // Process with EdgeGallery
                LlmAnalysisResult result = processWithEdgeGallery(
                    null, // No image data for audio
                    enhancedPrompt,
                    "audio_analysis"
                );
                
                if (result != null) {
                    // Create enhanced response
                    JSONObject response = createAudioAnalysisResponse(
                        message,
                        result,
                        System.currentTimeMillis() - startTime
                    );
                    
                    // Send via observable
                    responseObservable.onNext(response);
                    
                    Log.d(TAG, "Audio analysis completed in " + (System.currentTimeMillis() - startTime) + "ms");
                } else {
                    sendErrorResponse(message, "EdgeGallery processing failed");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing audio transcript", e);
                sendErrorResponse(message, e.getMessage());
            }
        });
    }
    
    /**
     * Process generic text query with LLM
     */
    public void processGenericQuery(JSONObject message) {
        if (!isInitialized || !isModelAvailable) {
            Log.w(TAG, "EdgeGallery not available, skipping LLM processing");
            sendErrorResponse(message, "EdgeGallery LLM not available");
            return;
        }
        
        processingExecutor.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                String query = message.getString("query");
                String messageType = message.optString("message_type", "generic_query");
                
                // Build contextual prompt
                String enhancedPrompt = promptManager.buildGenericQueryPrompt(
                    query,
                    messageType,
                    getCurrentContextualInfo()
                );
                
                // Process with EdgeGallery
                LlmAnalysisResult result = processWithEdgeGallery(
                    null,
                    enhancedPrompt,
                    "generic_query"
                );
                
                if (result != null) {
                    // Create response
                    JSONObject response = createGenericQueryResponse(
                        message,
                        result,
                        System.currentTimeMillis() - startTime
                    );
                    
                    responseObservable.onNext(response);
                    
                    Log.d(TAG, "Generic query completed in " + (System.currentTimeMillis() - startTime) + "ms");
                } else {
                    sendErrorResponse(message, "EdgeGallery processing failed");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing generic query", e);
                sendErrorResponse(message, e.getMessage());
            }
        });
    }
    
    /**
     * Core method to process requests with EdgeGallery
     */
    private LlmAnalysisResult processWithEdgeGallery(String imageBase64, String prompt, String analysisType) {
        try {
            // TODO: Implement actual network call to EdgeGallery on S24 Ultra
            // For now, simulate processing
            
            Log.d(TAG, "Processing with EdgeGallery - Type: " + analysisType);
            Log.d(TAG, "Prompt: " + prompt.substring(0, Math.min(prompt.length(), 100)) + "...");
            
            // Simulate processing time
            Thread.sleep(1500); // Simulate 1.5s processing time
            
            // Create mock result for development
            return createMockAnalysisResult(prompt, analysisType);
            
        } catch (Exception e) {
            Log.e(TAG, "EdgeGallery processing failed", e);
            return null;
        }
    }
    
    /**
     * Create mock analysis result for development/testing
     */
    private LlmAnalysisResult createMockAnalysisResult(String prompt, String analysisType) {
        LlmAnalysisResult.Builder builder = new LlmAnalysisResult.Builder();
        
        switch (analysisType) {
            case "image_analysis":
                builder.setAnalysis("I can see an image that appears to show a scene with various objects and possibly people. The lighting and composition suggest this might be taken in an indoor or outdoor environment.")
                       .setConfidence(0.85f)
                       .setSuggestedActions(Arrays.asList("Take notes", "Identify objects", "Analyze context"))
                       .setProactiveInsights(Arrays.asList("This appears to be a common scene type", "Similar images analyzed recently"));
                break;
                
            case "audio_analysis":
                builder.setAnalysis("The transcribed audio contains conversational content that suggests a discussion or meeting context.")
                       .setIntent("conversation")
                       .setSentiment("neutral")
                       .setActionItems(Arrays.asList("Follow up on topics discussed", "Schedule next meeting"))
                       .setEnhancedTranscript("Enhanced version of the original transcript with improved clarity");
                break;
                
            case "generic_query":
                builder.setAnalysis("This appears to be a general information request that can be addressed with available knowledge.")
                       .setConfidence(0.80f)
                       .setSuggestedActions(Arrays.asList("Provide detailed response", "Offer additional resources"));
                break;
        }
        
        return builder.setProcessingTime(1500)
                     .setSuccess(true)
                     .build();
    }
    
    /**
     * Get current contextual information for enhanced prompts
     */
    private JSONObject getCurrentContextualInfo() {
        try {
            JSONObject context = new JSONObject();
            context.put("timestamp", System.currentTimeMillis());
            context.put("device", "Samsung S24 Ultra");
            context.put("mode", "hybrid_processing");
            
            // TODO: Add more contextual information from WIS
            // - Current location
            // - Time of day
            // - Recent activities
            // - Recognized people
            
            return context;
        } catch (JSONException e) {
            Log.e(TAG, "Error creating contextual info", e);
            return new JSONObject();
        }
    }
    
    /**
     * Create image analysis response
     */
    private JSONObject createImageAnalysisResponse(JSONObject originalMessage, LlmAnalysisResult result, long processingTime) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("message_type", "llm_image_analysis_response");
        response.put("original_timestamp", originalMessage.optLong("timestamp"));
        response.put("processing_time_ms", processingTime);
        response.put("analysis", result.getAnalysis());
        response.put("confidence", result.getConfidence());
        
        if (result.getSuggestedActions() != null && !result.getSuggestedActions().isEmpty()) {
            response.put("suggested_actions", new JSONArray(result.getSuggestedActions()));
        }
        
        if (result.getProactiveInsights() != null && !result.getProactiveInsights().isEmpty()) {
            response.put("proactive_insights", new JSONArray(result.getProactiveInsights()));
        }
        
        return response;
    }
    
    /**
     * Create audio analysis response
     */
    private JSONObject createAudioAnalysisResponse(JSONObject originalMessage, LlmAnalysisResult result, long processingTime) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("message_type", "llm_audio_analysis_response");
        response.put("original_timestamp", originalMessage.optLong("timestamp"));
        response.put("processing_time_ms", processingTime);
        response.put("analysis", result.getAnalysis());
        response.put("intent", result.getIntent());
        response.put("sentiment", result.getSentiment());
        
        if (!result.getEnhancedTranscript().isEmpty()) {
            response.put("enhanced_transcript", result.getEnhancedTranscript());
        }
        
        if (result.getActionItems() != null && !result.getActionItems().isEmpty()) {
            response.put("action_items", new JSONArray(result.getActionItems()));
        }
        
        return response;
    }
    
    /**
     * Create generic query response
     */
    private JSONObject createGenericQueryResponse(JSONObject originalMessage, LlmAnalysisResult result, long processingTime) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("message_type", "llm_generic_response");
        response.put("original_timestamp", originalMessage.optLong("timestamp"));
        response.put("processing_time_ms", processingTime);
        response.put("analysis", result.getAnalysis());
        response.put("confidence", result.getConfidence());
        
        if (result.getSuggestedActions() != null && !result.getSuggestedActions().isEmpty()) {
            response.put("suggested_actions", new JSONArray(result.getSuggestedActions()));
        }
        
        return response;
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(JSONObject originalMessage, String error) {
        try {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message_type", "llm_error_response");
            errorResponse.put("error", error);
            errorResponse.put("original_message_type", originalMessage.optString("message_type"));
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            responseObservable.onNext(errorResponse);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating error response", e);
        }
    }
    
    /**
     * Convert base64 string to bitmap
     */
    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting base64 to bitmap", e);
            return null;
        }
    }
    
    /**
     * Get response observable for integration with WIS messaging system
     */
    public Observable<JSONObject> getResponseObservable() {
        return responseObservable.asObservable();
    }
    
    /**
     * Check if EdgeGallery is available
     */
    public boolean isAvailable() {
        return isInitialized && isModelAvailable;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (processingExecutor != null && !processingExecutor.isShutdown()) {
            processingExecutor.shutdown();
        }
        
        if (apiClient != null) {
            apiClient.cleanup();
        }
    }
}
