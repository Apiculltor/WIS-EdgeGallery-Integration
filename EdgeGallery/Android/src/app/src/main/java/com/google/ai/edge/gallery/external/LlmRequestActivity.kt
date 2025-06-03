package com.google.ai.edge.gallery.external

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
// import androidx.lifecycle.ViewModelProvider // Not using directly in this simplified version
// import com.google.ai.edge.gallery.ui.llmchat.LlmAskImageViewModel // Not using directly
import com.google.ai.edge.gallery.data.DefaultModels
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper // Assuming this will have the new method
// import com.google.ai.edge.gallery.data.Model // Not directly used, but LlmChatModelHelper would use it
// import com.google.ai.edge.gallery.data.TASK_LLM_ASK_IMAGE // Not directly used
// import com.google.ai.edge.gallery.data.ConfigKey // Not directly used
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class LlmRequestActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PROMPT = "com.google.ai.edge.gallery.external.EXTRA_PROMPT"
        const val EXTRA_IMAGE_URI = "com.google.ai.edge.gallery.external.EXTRA_IMAGE_URI"
        const val RESULT_EXTRA_LLM_RESPONSE = "com.google.ai.edge.gallery.external.RESULT_EXTRA_LLM_RESPONSE"
        private const val TAG = "LlmRequestActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        val prompt = intent.getStringExtra(EXTRA_PROMPT)
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        var imageUri: Uri? = null
        if (imageUriString != null) {
            try {
                imageUri = Uri.parse(imageUriString)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing image URI string: $imageUriString", e)
                setErrorResultAndFinish("Invalid image URI format.")
                return
            }
        }

        Log.d(TAG, "Received prompt: $prompt")
        Log.d(TAG, "Received image URI: $imageUriString")

        // Simplified Model access for this subtask
        // In a full implementation, proper ViewModel/Model management is key
        // Also, model selection could be dynamic (e.g., from Intent extras or a config)
        val selectedModel = DefaultModels.GEMMA_2B_ITI_CPU // Example model

        // This is a conceptual simplification. Proper model initialization is complex
        // and typically handled by ModelManagerViewModel or similar, ensuring the model
        // is downloaded, unzipped, and loaded into memory.
        // Here, we assume LlmChatModelHelper.runInferenceForExternalRequest
        // will internally handle or ensure model initialization.
        // If direct ViewModel usage were feasible here, it would look like:
        // val viewModel = ViewModelProvider(this).get(LlmAskImageViewModel::class.java)
        // viewModel.ensureModelInitialized(selectedModel) // Conceptual method in ViewModel

        val bitmap = imageUri?.let {
            val loadedBitmap = loadBitmapFromUri(it)
            if (loadedBitmap == null) {
                setErrorResultAndFinish("Failed to load image from URI.")
                return // Exit onCreate if bitmap loading failed
            }
            loadedBitmap
        }

        // Use a CoroutineScope to launch the LLM call on a background thread
        CoroutineScope(Dispatchers.Default).launch { // Use Default for CPU-bound work like LLM
            try {
                // Conceptual: LlmChatModelHelper is adapted or a new helper exists
                // that takes a completion callback for the full string.
                // This bypasses the ViewModel's UI-centric streaming for this Activity.
                // The 'model' parameter here would be the Model object, which needs to be
                // initialized (instance != null). We're assuming the helper handles this.
                LlmChatModelHelper.runInferenceForExternalRequest( // Conceptual new/adapted method
                    context = applicationContext,
                    model = selectedModel, // Model object, assumed to be handled by the helper
                    input = prompt ?: "",
                    image = bitmap,
                    onCompleted = { fullResponse ->
                        // Switch back to Main thread for UI operations (setResult, finish)
                        CoroutineScope(Dispatchers.Main).launch {
                            val resultIntent = Intent()
                            resultIntent.putExtra(RESULT_EXTRA_LLM_RESPONSE, fullResponse)
                            setResult(Activity.RESULT_OK, resultIntent)
                            Log.d(TAG, "Setting OK result with LLM response.")
                            finish()
                        }
                    },
                    onError = { errorMsg ->
                        // Switch back to Main thread for UI operations (setErrorResultAndFinish)
                        CoroutineScope(Dispatchers.Main).launch {
                            setErrorResultAndFinish(errorMsg)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception during LLM request execution", e)
                // Switch back to Main thread for UI operations
                CoroutineScope(Dispatchers.Main).launch {
                    setErrorResultAndFinish("Exception during LLM request: ${e.message}")
                }
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(this.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true // Make mutable if needed downstream, though not strictly here
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load bitmap from URI: $uri", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception loading bitmap from URI: $uri. Check URI permissions.", e)
            null
        }
    }

    private fun setErrorResultAndFinish(errorMessage: String = "LLM processing failed") {
        Log.e(TAG, errorMessage)
        val resultIntent = Intent()
        // Optionally include the error message for the caller to inspect
        resultIntent.putExtra(RESULT_EXTRA_LLM_RESPONSE, "Error: $errorMessage")
        setResult(Activity.RESULT_CANCELED, resultIntent)
        if (!isFinishing) { // Ensure finish() is called only once
            finish()
            Log.d(TAG, "Finishing LlmRequestActivity due to error.")
        }
    }
}
