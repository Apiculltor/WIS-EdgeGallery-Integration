package com.wearableintelligencesystem.androidsmartphone.hybrid;

import android.content.Context;
import android.util.Log;

import com.wearableintelligencesystem.androidsmartphone.edgegallery.EdgeGalleryLlmProcessor;
import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * HybridAiCoordinator - Central coordinator for hybrid WIS + EdgeGallery processing
 * 
 * This class orchestrates the parallel processing of requests using both traditional
 * WIS modules and EdgeGallery LLM capabilities, then aggregates the results for
 * enhanced user experience.
 * 
 * @author WIS + EdgeGallery Integration Team
 * @version 1.0
 */
public class HybridAiCoordinator {
    private static final String TAG = "HybridAiCoordinator";
    
    // Core components
    private Context context;
    private EdgeGalleryLlmProcessor edgeGalleryProcessor;
    private IntelligentRouter intelligentRouter;
    private ResponseAggregator responseAggregator;
    
    // Threading and execution
    private ExecutorService processingExecutor;
    private CompositeDisposable disposables;
    
    // Response observables
    private PublishSubject<JSONObject> responseObservable;
    
    // State management
    private boolean isInitialized = false;
    
    public HybridAiCoordinator(Context context) {
        this.context = context;
        this.disposables = new CompositeDisposable();
        this.responseObservable = PublishSubject.create();
        this.processingExecutor = Executors.newFixedThreadPool(4);
        
        initializeComponents();
    }
    
    /**
     * Initialize all hybrid processing components
     */
    private void initializeComponents() {
        try {
            Log.d(TAG, "Initializing Hybrid AI Coordinator...");
            
            // Initialize EdgeGallery processor
            edgeGalleryProcessor = new EdgeGalleryLlmProcessor(context);
            
            // Initialize routing and aggregation
            intelligentRouter = new IntelligentRouter();
            responseAggregator = new ResponseAggregator();
            
            // Subscribe to EdgeGallery responses
            disposables.add(
                edgeGalleryProcessor.getResponseObservable()
                    .subscribe(
                        this::handleLlmResponse,
                        this::handleLlmError
                    )
            );
            
            isInitialized = true;
            Log.d(TAG, "Hybrid AI Coordinator initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Hybrid AI Coordinator", e);
            isInitialized = false;
        }
    }
    
    /**
     * Process message with hybrid strategy (parallel WIS + EdgeGallery)
     */
    public void processParallel(JSONObject message) {
        if (!isInitialized) {
            Log.w(TAG, "Coordinator not initialized, skipping hybrid processing");
            sendErrorResponse(message, "Hybrid coordinator not available");
            return;
        }
        
        processingExecutor.execute(() -> {
            try {
                String messageType = message.getString("message_type");
                long startTime = System.currentTimeMillis();
                
                Log.d(TAG, "Starting parallel processing for: " + messageType);
                
                // Determine processing strategy
                ProcessingStrategy strategy = intelligentRouter.determineStrategy(message);
                
                switch (strategy) {
                    case PARALLEL:
                        executeParallelProcessing(message, startTime);
                        break;
                        
                    case WIS_ONLY:
                        executeWisOnlyProcessing(message, startTime);
                        break;
                        
                    case EDGE_GALLERY_ONLY:
                        executeEdgeGalleryOnlyProcessing(message, startTime);
                        break;
                        
                    case SEQUENTIAL_WIS_FIRST:
                        executeSequentialProcessing(message, startTime, true);
                        break;
                        
                    case SEQUENTIAL_LLM_FIRST:
                        executeSequentialProcessing(message, startTime, false);
                        break;
                        
                    default:
                        executeParallelProcessing(message, startTime);
                        break;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in parallel processing", e);
                sendErrorResponse(message, e.getMessage());
            }
        });
    }
    
    /**
     * Execute parallel processing with both WIS and EdgeGallery
     */
    private void executeParallelProcessing(JSONObject message, long startTime) {
        try {
            // Submit WIS processing (simulated for now)
            Future<JSONObject> wisResult = processingExecutor.submit(() -> {
                return simulateWisProcessing(message);
            });
            
            // Submit EdgeGallery processing
            String messageType = message.getString("message_type");
            if (MessageTypes.POV_IMAGE_QUERY.equals(messageType)) {
                edgeGalleryProcessor.processImageQuery(message);
            } else if (MessageTypes.AUDIO_CHUNK_DECRYPTED.equals(messageType)) {
                edgeGalleryProcessor.processAudioTranscript(message);
            } else {
                edgeGalleryProcessor.processGenericQuery(message);
            }
            
            // Get WIS result (quick processing)
            JSONObject wisResponse = wisResult.get();
            
            // Create immediate response with WIS data
            JSONObject immediateResponse = createImmediateResponse(message, wisResponse, startTime);
            responseObservable.onNext(immediateResponse);
            
            Log.d(TAG, "Immediate response sent, waiting for LLM enhancement...");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in parallel processing execution", e);
            sendErrorResponse(message, e.getMessage());
        }
    }
    
    /**
     * Execute WIS-only processing
     */
    private void executeWisOnlyProcessing(JSONObject message, long startTime) {
        try {
            JSONObject wisResponse = simulateWisProcessing(message);
            JSONObject response = createWisOnlyResponse(message, wisResponse, startTime);
            responseObservable.onNext(response);
            
            Log.d(TAG, "WIS-only processing completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in WIS-only processing", e);
            sendErrorResponse(message, e.getMessage());
        }
    }
    
    /**
     * Execute EdgeGallery-only processing
     */
    private void executeEdgeGalleryOnlyProcessing(JSONObject message, long startTime) {
        try {
            String messageType = message.getString("message_type");
            
            if (MessageTypes.POV_IMAGE_QUERY.equals(messageType)) {
                edgeGalleryProcessor.processImageQuery(message);
            } else if (MessageTypes.AUDIO_CHUNK_DECRYPTED.equals(messageType)) {
                edgeGalleryProcessor.processAudioTranscript(message);
            } else {
                edgeGalleryProcessor.processGenericQuery(message);
            }
            
            Log.d(TAG, "EdgeGallery-only processing initiated");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in EdgeGallery-only processing", e);
            sendErrorResponse(message, e.getMessage());
        }
    }
    
    /**
     * Execute sequential processing (WIS first or LLM first)
     */
    private void executeSequentialProcessing(JSONObject message, long startTime, boolean wisFirst) {
        try {
            if (wisFirst) {
                // Process with WIS first, then enhance with LLM
                JSONObject wisResponse = simulateWisProcessing(message);
                JSONObject immediateResponse = createImmediateResponse(message, wisResponse, startTime);
                responseObservable.onNext(immediateResponse);
                
                // Then process with LLM for enhancement
                String messageType = message.getString("message_type");
                if (MessageTypes.POV_IMAGE_QUERY.equals(messageType)) {
                    edgeGalleryProcessor.processImageQuery(message);
                } else if (MessageTypes.AUDIO_CHUNK_DECRYPTED.equals(messageType)) {
                    edgeGalleryProcessor.processAudioTranscript(message);
                }
            } else {
                // Process with LLM first, then add WIS data
                String messageType = message.getString("message_type");
                if (MessageTypes.POV_IMAGE_QUERY.equals(messageType)) {
                    edgeGalleryProcessor.processImageQuery(message);
                } else if (MessageTypes.AUDIO_CHUNK_DECRYPTED.equals(messageType)) {
                    edgeGalleryProcessor.processAudioTranscript(message);
                }
                
                // Store for later WIS enhancement
                // TODO: Implement delayed WIS processing
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in sequential processing", e);
            sendErrorResponse(message, e.getMessage());
        }
    }
    
    /**
     * Simulate WIS processing (placeholder for actual WIS integration)
     */
    private JSONObject simulateWisProcessing(JSONObject message) {
        try {
            Thread.sleep(150); // Simulate 150ms processing time
            
            JSONObject wisResponse = new JSONObject();
            wisResponse.put("source", "WIS");
            wisResponse.put("processing_time", 150);
            wisResponse.put("message_type", message.getString("message_type"));
            
            String messageType = message.getString("message_type");
            if (MessageTypes.POV_IMAGE_QUERY.equals(messageType)) {
                wisResponse.put("detection_results", "Object detected: person, confidence: 0.95");
                wisResponse.put("face_recognition", "Known person: John Doe");
            } else if (MessageTypes.AUDIO_CHUNK_DECRYPTED.equals(messageType)) {
                wisResponse.put("transcription", message.optString("transcript", "Audio transcribed"));
                wisResponse.put("voice_command", "No specific command detected");
            }
            
            return wisResponse;
            
        } catch (Exception e) {
            Log.e(TAG, "Error simulating WIS processing", e);
            return new JSONObject();
        }
    }
    
    /**
     * Create immediate response with WIS data
     */
    private JSONObject createImmediateResponse(JSONObject originalMessage, JSONObject wisResponse, long startTime) {
        try {
            JSONObject response = new JSONObject();
            response.put("message_type", "hybrid_immediate_response");
            response.put("original_timestamp", originalMessage.optLong("timestamp"));
            response.put("processing_start_time", startTime);
            response.put("immediate_processing_time", System.currentTimeMillis() - startTime);
            
            // Add WIS results
            response.put("wis_results", wisResponse);
            
            // Add display information
            JSONObject displayLayers = new JSONObject();
            
            String messageType = originalMessage.getString("message_type");
            if (MessageTypes.POV_IMAGE_QUERY.equals(messageType)) {
                displayLayers.put("immediate", "Object detected");
                displayLayers.put("contextual", wisResponse.optString("detection_results", ""));
            } else if (MessageTypes.AUDIO_CHUNK_DECRYPTED.equals(messageType)) {
                displayLayers.put("immediate", "Audio processed");
                displayLayers.put("contextual", wisResponse.optString("transcription", ""));
            }
            
            response.put("display_layers", displayLayers);
            response.put("enhancement_pending", true);
            
            return response;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating immediate response", e);
            return new JSONObject();
        }
    }
    
    /**
     * Create WIS-only response
     */
    private JSONObject createWisOnlyResponse(JSONObject originalMessage, JSONObject wisResponse, long startTime) {
        try {
            JSONObject response = new JSONObject();
            response.put("message_type", "wis_only_response");
            response.put("original_timestamp", originalMessage.optLong("timestamp"));
            response.put("processing_time", System.currentTimeMillis() - startTime);
            response.put("wis_results", wisResponse);
            
            return response;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating WIS-only response", e);
            return new JSONObject();
        }
    }
    
    /**
     * Handle LLM response and create enhanced hybrid response
     */
    private void handleLlmResponse(JSONObject llmResponse) {
        try {
            Log.d(TAG, "Received LLM response, creating enhanced hybrid response");
            
            // Create enhanced response combining all data
            JSONObject enhancedResponse = new JSONObject();
            enhancedResponse.put("message_type", "hybrid_enhanced_response");
            enhancedResponse.put("llm_results", llmResponse);
            enhancedResponse.put("timestamp", System.currentTimeMillis());
            
            // Extract and format display information
            JSONObject displayLayers = new JSONObject();
            
            String analysis = llmResponse.optString("analysis", "");
            if (!analysis.isEmpty()) {
                displayLayers.put("smart", analysis.substring(0, Math.min(analysis.length(), 100)));
            }
            
            if (llmResponse.has("suggested_actions")) {
                displayLayers.put("actionable", llmResponse.getJSONArray("suggested_actions"));
            }
            
            enhancedResponse.put("display_layers", displayLayers);
            
            // Send enhanced response
            responseObservable.onNext(enhancedResponse);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling LLM response", e);
        }
    }
    
    /**
     * Handle LLM processing errors
     */
    private void handleLlmError(Throwable error) {
        Log.e(TAG, "LLM processing error", error);
        
        try {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message_type", "hybrid_error_response");
            errorResponse.put("error", "LLM processing failed: " + error.getMessage());
            errorResponse.put("fallback_mode", "WIS_ONLY");
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            responseObservable.onNext(errorResponse);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating error response", e);
        }
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(JSONObject originalMessage, String error) {
        try {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message_type", "hybrid_processing_error");
            errorResponse.put("error", error);
            errorResponse.put("original_message_type", originalMessage.optString("message_type"));
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            responseObservable.onNext(errorResponse);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating error response", e);
        }
    }
    
    /**
     * Get response observable for integration with ASGRepresentative
     */
    public Observable<JSONObject> getResponseObservable() {
        return responseObservable.asObservable();
    }
    
    /**
     * Check if coordinator is available
     */
    public boolean isAvailable() {
        return isInitialized && (edgeGalleryProcessor == null || edgeGalleryProcessor.isAvailable());
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (disposables != null) {
            disposables.clear();
        }
        
        if (processingExecutor != null && !processingExecutor.isShutdown()) {
            processingExecutor.shutdown();
        }
        
        if (edgeGalleryProcessor != null) {
            edgeGalleryProcessor.cleanup();
        }
    }
}
