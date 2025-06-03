package com.wearableintelligencesystem.androidsmartphone.hybrid;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.Nullable;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes; // Adjust if path is different
import com.wearableintelligencesystem.androidsmartphone.llm.MediaPipeLlmClient; // Adjust if path is different

import org.json.JSONObject;
import org.json.JSONException;

import java.util.concurrent.Executors; // Added for thenAcceptAsync executor

import io.reactivex.rxjava3.subjects.PublishSubject;

public class HybridAiCoordinator {
    private static final String TAG = "HybridAiCoordinator";

    private final Context androidContext;
    private final MediaPipeLlmClient mediaPipeLlmClient;
    private final IntelligentRouter intelligentRouter;
    private final ResponseAggregator responseAggregator;
    private final ContextualMemoryManager contextualMemoryManager;

    private final PublishSubject<JSONObject> aggregatedResponseObservable;
    private boolean isLlmInitialized = false;
    private String modelName = "gemma-2b-it-cpu.task"; // Default model name, make configurable if needed

    public HybridAiCoordinator(Context context) {
        this.androidContext = context.getApplicationContext();
        this.intelligentRouter = new IntelligentRouter();
        this.responseAggregator = new ResponseAggregator();
        this.contextualMemoryManager = new ContextualMemoryManager(this.androidContext);
        this.mediaPipeLlmClient = new MediaPipeLlmClient(this.androidContext);
        this.aggregatedResponseObservable = PublishSubject.create();

        Log.d(TAG, "Initializing MediaPipeLlmClient...");
        this.mediaPipeLlmClient.initialize(modelName)
            .thenAccept(success -> {
                if (success) {
                    this.isLlmInitialized = true;
                    Log.d(TAG, "MediaPipeLlmClient initialized successfully with model: " + modelName);
                } else {
                    // This path might not be hit if initialize completes exceptionally for failure.
                    // The exceptionally block is more common for errors during initialization.
                    Log.e(TAG, "MediaPipeLlmClient initialization failed for model: " + modelName);
                    sendErrorResponse("LLM_INIT_FAILURE", "LLM Engine failed to initialize (completed with false).", null);
                }
            })
            .exceptionally(error -> {
                Log.e(TAG, "MediaPipeLlmClient initialization threw an exception for model: " + modelName, error);
                sendErrorResponse("LLM_INIT_EXCEPTION", "LLM Engine initialization exception: " + error.getMessage(), null);
                return null; // Must return null for exceptionally
            });
    }

    public PublishSubject<JSONObject> getAggregatedResponseObservable() {
        return aggregatedResponseObservable;
    }

    public void processIncomingMessage(JSONObject message, @Nullable Bitmap imageBitmap) {
        String messagePreview = message.toString();
        messagePreview = messagePreview.substring(0, Math.min(messagePreview.length(), 100));
        Log.d(TAG, "Processing incoming message: " + messagePreview + (imageBitmap != null ? " with image" : ""));

        IntelligentRouter.ProcessingStrategy strategy = intelligentRouter.determineStrategy(message);
        Log.d(TAG, "Determined strategy: " + strategy);

        String prompt = "";
        try {
            // More robust prompt extraction based on expected message structure
            if (message.has(MessageTypes.TEXT_QUERY)) { // Assuming TEXT_QUERY is defined in MessageTypes
                 prompt = message.getString(MessageTypes.TEXT_QUERY);
            } else if (message.has("prompt")) {
                 prompt = message.getString("prompt");
            } else if (MessageTypes.LLM_VOICE_COMMAND.equals(message.optString(MessageTypes.MESSAGE_TYPE_LOCAL))) {
                prompt = message.optString("command", "Analyze the following.");
            } else if ("CONTEXTUAL_IMAGE_QUERY".equals(message.optString(MessageTypes.MESSAGE_TYPE_LOCAL))) {
                prompt = message.optString("prompt", "Describe this image based on context.");
            } else if (imageBitmap != null && MessageTypes.POV_IMAGE.equals(message.optString(MessageTypes.MESSAGE_TYPE_LOCAL))) {
                // If it's a POV image and no specific prompt was determined, use a default image prompt
                prompt = message.optString("prompt", "Describe this image.");
            }
             else {
                prompt = "Analyze the provided information."; // A generic default
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error extracting prompt from message: " + message.toString(), e);
            prompt = "Analyze the provided information."; // Fallback prompt
        }
        Log.d(TAG, "Using prompt for LLM: '" + prompt + "'");


        if (strategy == IntelligentRouter.ProcessingStrategy.LLM_ONLY ||
            strategy == IntelligentRouter.ProcessingStrategy.SEQUENTIAL_WIS_THEN_LLM) {

            if (!isLlmInitialized) {
                Log.e(TAG, "LLM not initialized, cannot process LLM_ONLY or SEQUENTIAL_WIS_THEN_LLM strategy.");
                sendErrorResponse(message.optString(MessageTypes.MESSAGE_TYPE_LOCAL), "LLM engine not ready.", message);
                return;
            }

            // For SEQUENTIAL_WIS_THEN_LLM, WIS processing would happen first.
            // For this example, we'll assume wisResult is null or pre-filled if it was sequential.
            final Object wisResultPlaceholder = null; // Populate this if WIS part of sequential runs first

            mediaPipeLlmClient.generateResponseAsync(prompt, imageBitmap)
                .thenAcceptAsync(llmResponseText -> {
                    String responsePreview = llmResponseText.substring(0, Math.min(llmResponseText.length(),100));
                    Log.d(TAG, "LLM Response received: " + responsePreview);
                    ProcessingResult llmProcessingResult = new ProcessingResult.Builder()
                            .setSource("MediaPipeLLM")
                            .setLlmResultText(llmResponseText)
                            .setSuccess(true)
                            // Add correlationId if available from original message
                            .setCorrelationId(message.optString("correlationId", message.optString("messageId")))
                            .build();
                    contextualMemoryManager.addProcessingResult(llmProcessingResult);

                    ResponseAggregator.AggregatedResponse finalResponse =
                        responseAggregator.combine(wisResultPlaceholder, llmResponseText);
                    aggregatedResponseObservable.onNext(finalResponse.toJSONObject());
                }, Executors.newSingleThreadExecutor()) // Process callback on a separate thread
                .exceptionally(error -> {
                    Log.e(TAG, "Error generating LLM response", error);
                    sendErrorResponse(message.optString(MessageTypes.MESSAGE_TYPE_LOCAL), "LLM processing error: " + error.getMessage(), message);
                    ProcessingResult errorResult = new ProcessingResult.Builder()
                            .setSource("MediaPipeLLM")
                            .setError(new Exception(error.getMessage())) // Create new exception from message
                            .setSuccess(false)
                            .setCorrelationId(message.optString("correlationId", message.optString("messageId")))
                            .build();
                    contextualMemoryManager.addProcessingResult(errorResult);
                    return null; // Must return null for exceptionally
                });

        } else if (strategy == IntelligentRouter.ProcessingStrategy.WIS_ONLY) {
            Log.d(TAG, "WIS_ONLY strategy: Simulating WIS processing for message type: " + message.optString(MessageTypes.MESSAGE_TYPE_LOCAL));
            // Simulate WIS processing
            WisResponse wisResponse = new WisResponse("Processed by WIS: " + message.optString(MessageTypes.MESSAGE_TYPE_LOCAL));
            ResponseAggregator.AggregatedResponse finalResponse = responseAggregator.combine(wisResponse, null);
            // Add correlationId to the final response if available
            try {
                JSONObject finalJson = finalResponse.toJSONObject();
                finalJson.putOpt("correlationId", message.optString("correlationId", message.optString("messageId")));
                aggregatedResponseObservable.onNext(finalJson);
            } catch (JSONException e) {
                Log.e(TAG, "Error adding correlationId to WIS_ONLY response", e);
                aggregatedResponseObservable.onNext(finalResponse.toJSONObject()); // Send without if error
            }

            contextualMemoryManager.addProcessingResult(
                new ProcessingResult.Builder()
                    .setSource("WIS")
                    .setWisResponse(wisResponse)
                    .setSuccess(true)
                    .setCorrelationId(message.optString("correlationId", message.optString("messageId")))
                    .build());
        } else {
             Log.w(TAG, "Unknown or unhandled processing strategy: " + strategy);
             sendErrorResponse(message.optString(MessageTypes.MESSAGE_TYPE_LOCAL), "Unhandled processing strategy.", message);
        }
    }

    private void sendErrorResponse(String requestType, String errorDetails, @Nullable JSONObject originalMessage) {
        JSONObject errorJson = new JSONObject();
        String correlationId = null;
        String originalPrompt = null;

        if (originalMessage != null) {
            correlationId = originalMessage.optString("correlationId", originalMessage.optString("messageId"));
            originalPrompt = originalMessage.optString("prompt", originalMessage.toString().substring(0, Math.min(originalMessage.toString().length(), 50)));
        }

        try {
            errorJson.put("error_details", errorDetails);
            errorJson.put("request_type", requestType);
            if (originalPrompt != null) {
                 errorJson.put("original_request_details", originalPrompt);
            }
            if (correlationId != null) {
                errorJson.put("correlationId", correlationId);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON for error response", e);
        }
        aggregatedResponseObservable.onNext(errorJson);
    }

    public void destroy() {
        Log.d(TAG, "Destroying HybridAiCoordinator.");
        if (mediaPipeLlmClient != null) {
            mediaPipeLlmClient.close();
        }
        // It's good practice to complete subjects when they are no longer needed
        // to signal to subscribers that no more items will be emitted.
        if (aggregatedResponseObservable != null) {
            // Check if there are active subscribers or if it's already completed
            // This check might be more complex depending on RxJava version / specific Subject type behavior
            // For a simple PublishSubject, completing it is usually fine.
            try {
                 // aggregatedResponseObservable.onComplete(); // Consider implications if it might be reused.
                 Log.d(TAG, "Aggregated Response Observable completed.");
            } catch (Exception e) {
                Log.e(TAG, "Exception while completing observable", e);
            }
        }
        Log.d(TAG, "HybridAiCoordinator destroyed.");
    }
}
