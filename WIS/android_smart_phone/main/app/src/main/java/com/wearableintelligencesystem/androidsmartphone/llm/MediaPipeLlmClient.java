package com.wearableintelligencesystem.androidsmartphone.llm;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.Nullable;

import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.genai.llminference.LlmInference;
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaPipeLlmClient {
    private static final String TAG = "MediaPipeLlmClient";

    private LlmInference llmInference;
    private LlmInferenceSession llmSession;
    private final Context context;
    private final ExecutorService backgroundExecutor;

    public MediaPipeLlmClient(Context context) {
        this.context = context.getApplicationContext();
        this.backgroundExecutor = Executors.newSingleThreadExecutor();
    }

    public CompletableFuture<Boolean> initialize(String modelNameInAssets) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        backgroundExecutor.submit(() -> {
            try {
                Log.d(TAG, "Initializing LlmInference with model: " + modelNameInAssets);
                LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath(modelNameInAssets).build())
                    .setMaxTokens(1024) // Default, can be configured
                    .setTopK(40)        // Default
                    .setTemperature(0.7f) // Default
                    // Random seed can be set if needed: .setRandomSeed(1234)
                    .build();

                llmInference = LlmInference.createFromOptions(context, options);
                // Create a session from the LlmInference instance.
                // Session specific options can be set here if needed,
                // e.g., .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build()) if model supports vision
                llmSession = llmInference.createSession();

                Log.d(TAG, "LlmInference and session initialized successfully.");
                future.complete(true);
            } catch (Exception e) { // Catch generic Exception as MediaPipe tasks can throw various runtime exceptions
                Log.e(TAG, "Failed to initialize LlmInference", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<String> generateResponseAsync(String prompt, @Nullable Bitmap image) {
        CompletableFuture<String> future = new CompletableFuture<>();
        backgroundExecutor.submit(() -> {
            if (llmSession == null) {
                Log.e(TAG, "LlmInference session not initialized. Call initialize() first.");
                future.completeExceptionally(new IllegalStateException("LlmInference session not initialized."));
                return;
            }

            final StringBuilder responseBuilder = new StringBuilder();
            try {
                // For a new sequence of interactions, you might need to reset the session
                // or ensure it's in a clean state if it's being reused.
                // The EdgeGallery LlmChatModelHelper did this: LlmChatModelHelper.resetSession(model)
                // This client currently doesn't expose a direct resetSession method, so it assumes
                // either single-turn use per session, or the caller manages session state/reset.
                // For this implementation, we'll assume the session is ready for a new query.

                llmSession.addQueryChunk(prompt); // Add text part first
                if (image != null) {
                    try {
                        // Ensure the model and session are configured for vision modality if using images.
                        // This is typically done during LlmInference.LlmInferenceOptions
                        // or LlmInferenceSession.LlmInferenceSessionOptions if applicable.
                        // For example, .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
                        // might be needed on the session options if not on the main inference engine options.
                        // The current LlmInferenceOptions doesn't show a direct vision modality toggle,
                        // it might be inferred from the model capabilities or set in session options if the API allows.
                        MPImage mpImage = BitmapImageBuilder.newInstance(image).build();
                        llmSession.addImage(mpImage); // Add image part if available
                        Log.d(TAG, "Image added to LLM session.");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to create or add MPImage to session", e);
                        future.completeExceptionally(new RuntimeException("Failed to process image for LLM.", e));
                        return;
                    }
                }

                // The generateResponseAsync method on the session does not take the prompt again.
                // It uses the state built up by addQueryChunk and addImage.
                llmSession.generateResponseAsync(new LlmInference.LlmResultListener() {
                    @Override
                    public void onResult(LlmInference.LlmResult result) {
                        responseBuilder.append(result.getText());
                        if (result.isDone()) {
                            Log.d(TAG, "LLM async response finished. Full response length: " + responseBuilder.length());
                            future.complete(responseBuilder.toString());
                        }
                    }

                    @Override
                    public void onError(RuntimeException error) {
                        Log.e(TAG, "LLM async response error", error);
                        future.completeExceptionally(error);
                    }
                });

            } catch (Exception e) { // Catching generic exception from MediaPipe
                Log.e(TAG, "Error during LLM generateResponseAsync setup or call", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public void close() {
        backgroundExecutor.submit(() -> {
            try {
                if (llmSession != null) {
                    llmSession.close();
                    llmSession = null;
                    Log.d(TAG, "LlmInferenceSession closed.");
                }
                if (llmInference != null) {
                    llmInference.close();
                    llmInference = null;
                    Log.d(TAG, "LlmInference closed.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing LlmInference resources", e);
            } finally {
                if (!backgroundExecutor.isShutdown()) {
                    backgroundExecutor.shutdown();
                    Log.d(TAG, "Background executor shutdown.");
                }
            }
        });
    }
}
