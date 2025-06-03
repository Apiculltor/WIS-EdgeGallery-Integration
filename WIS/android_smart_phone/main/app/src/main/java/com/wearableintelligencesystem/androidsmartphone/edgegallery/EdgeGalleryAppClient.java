package com.wearableintelligencesystem.androidsmartphone.edgegallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.Nullable;

public class EdgeGalleryAppClient {

    private static final String TAG = "EdgeGalleryAppClient";

    // Define constants for EdgeGallery's LlmRequestActivity
    // These must match the values expected by LlmRequestActivity in the EdgeGallery app
    public static final String EDGEGALLERY_PACKAGE_NAME = "com.google.ai.edge.gallery"; // Make sure this is EdgeGallery's applicationId
    public static final String LLM_REQUEST_ACTIVITY_CLASS_NAME = "com.google.ai.edge.gallery.external.LlmRequestActivity";

    // Intent Extras
    public static final String EXTRA_PROMPT = "com.google.ai.edge.gallery.external.EXTRA_PROMPT";
    public static final String EXTRA_IMAGE_URI = "com.google.ai.edge.gallery.external.EXTRA_IMAGE_URI";
    // For parsing the result from LlmRequestActivity
    public static final String RESULT_EXTRA_LLM_RESPONSE = "com.google.ai.edge.gallery.external.RESULT_EXTRA_LLM_RESPONSE";


    private EdgeGalleryAppClient() {
        // Private constructor for utility class
    }

    /**
     * Creates an Intent to launch EdgeGallery's LlmRequestActivity.
     * The calling component is responsible for launching this intent using
     * startActivityForResult or an ActivityResultLauncher and handling the result.
     *
     * @param context Context, used for logging and potentially other operations.
     * @param prompt The text prompt for the LLM.
     * @param imageUri Optional URI of an image to be analyzed. Must be accessible
     *                 to EdgeGallery (e.g., via FileProvider and URI permissions).
     *                 The URI should be a content URI for which read permission can be granted.
     * @return The configured Intent.
     */
    public static Intent createLlmRequestIntent(Context context, String prompt, @Nullable Uri imageUri) {
        Intent intent = new Intent();
        // Explicitly set the component name for the activity to be launched
        intent.setClassName(EDGEGALLERY_PACKAGE_NAME, LLM_REQUEST_ACTIVITY_CLASS_NAME);

        intent.putExtra(EXTRA_PROMPT, prompt);

        if (imageUri != null) {
            intent.putExtra(EXTRA_IMAGE_URI, imageUri.toString());
            // Grant temporary read permission to the receiving app (EdgeGallery) for this specific URI
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // For robust permission granting, especially if EdgeGallery targets Android S (API 31) or higher,
            // and if the URI is from a content provider owned by this app,
            // it's good practice to also grant permission to the package explicitly.
            // However, FLAG_GRANT_READ_URI_PERMISSION combined with Intent.setData or ClipData
            // is often sufficient. If setClassName is used without setData or ClipData,
            // the receiving app might need to be prepared to handle this.
            // For simplicity, we rely on FLAG_GRANT_READ_URI_PERMISSION.
            // If issues arise, context.grantUriPermission might be needed:
            // context.grantUriPermission(EDGEGALLERY_PACKAGE_NAME, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Log.d(TAG, "Image URI added to intent: " + imageUri.toString() + ". Read permission granted via flag.");
        }

        // It's generally a good idea to add FLAG_ACTIVITY_NEW_TASK if this client
        // is called from a non-Activity context (like a Service), though the calling
        // Activity/Service should make this decision. For a client library,
        // not adding it by default might be safer, allowing the caller to decide.
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Log.d(TAG, "Creating Intent for EdgeGallery: Component=" + intent.getComponent() + ", Prompt='" + prompt + "', ImageUri=" + (imageUri != null ? imageUri.toString() : "null"));
        return intent;
    }
}
