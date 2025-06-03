package com.wearableintelligencesystem.androidsmartphone.hybrid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity; // Using AppCompatActivity for modern lifecycle if needed

import com.wearableintelligencesystem.androidsmartphone.edgegallery.EdgeGalleryAppClient;
import com.wearableintelligencesystem.androidsmartphone.MainActivity; // Assuming MainActivity might hold the coordinator or use a singleton

import org.json.JSONObject;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class LlmResultForwarderActivity extends AppCompatActivity { // Changed to AppCompatActivity
    private static final String TAG = "LlmResultForwarder";
    public static final String EXTRA_REQUEST_JSON = "com.wearableintelligencesystem.REQUEST_JSON";
    public static final String EXTRA_IMAGE_URI = "com.wearableintelligencesystem.IMAGE_URI"; // URI as String
    public static final String EXTRA_REQUEST_CODE = "com.wearableintelligencesystem.REQUEST_CODE";

    private HybridAiCoordinator hybridAiCoordinator; // Will get this from a central point
    private int actualRequestCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        // Make this activity invisible
        // Can be done with a translucent theme in Manifest or programmatically:
        // supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        // getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Retrieve the HybridAiCoordinator instance
        // This is a simplification. In a real app, use a proper singleton, service locator, or DI.
        hybridAiCoordinator = MainActivity.getHybridAiCoordinatorInstance(); // ASSUMPTION: MainActivity provides a static getter

        if (hybridAiCoordinator == null) {
            Log.e(TAG, "HybridAiCoordinator instance is null. Cannot proceed.");
            finish();
            return;
        }

        Intent intent = getIntent();
        String requestJsonString = intent.getStringExtra(EXTRA_REQUEST_JSON);
        String imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI);
        actualRequestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0); // Get the original request code

        if (requestJsonString == null) {
            Log.e(TAG, "Request JSON string is null.");
            // It's possible that the coordinator might expect an original message even on error,
            // so we might call handleLlmError here if appropriate.
            // For now, just finishing if essential data is missing.
            if (hybridAiCoordinator != null && actualRequestCode != 0) {
                 hybridAiCoordinator.handleLlmError(actualRequestCode, "Request JSON was null in Forwarder.", null);
            }
            finish();
            return;
        }

        if (actualRequestCode == 0) {
            Log.e(TAG, "Actual request code is 0, this is not expected.");
            // Potentially handle error back through coordinator if possible, though context is limited.
            finish();
            return;
        }


        try {
            JSONObject requestJson = new JSONObject(requestJsonString);
            Bitmap imageBitmap = null;
            if (imageUriString != null) {
                try {
                    Uri imageUri = Uri.parse(imageUriString);
                    // Check if the URI is a content URI, which is expected from FileProvider
                    if ("content".equals(imageUri.getScheme())) {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        imageBitmap = BitmapFactory.decodeStream(inputStream);
                        if (inputStream != null) inputStream.close();
                        Log.d(TAG, "Successfully loaded image bitmap from content URI: " + imageUriString);
                    } else {
                        Log.w(TAG, "Image URI is not a content URI, attempting to load as file path (less ideal): " + imageUriString);
                        // This path might fail due to direct file path access restrictions (use FileProvider)
                        File imgFile = new File(imageUri.getPath());
                        if(imgFile.exists()){
                            imageBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                            Log.d(TAG, "Loaded image bitmap from file path (use with caution): " + imageUriString);
                        } else {
                             Log.e(TAG, "Image file does not exist at path: " + imageUriString);
                        }
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Image file not found for URI: " + imageUriString, e);
                    hybridAiCoordinator.handleLlmError(actualRequestCode, "Image file not found: " + imageUriString, requestJson);
                    finish(); // Finish as image processing failed
                    return;
                } catch (IOException e) {
                    Log.e(TAG, "Error loading image bitmap: " + imageUriString, e);
                    hybridAiCoordinator.handleLlmError(actualRequestCode, "Error loading image: " + imageUriString, requestJson);
                    finish(); // Finish as image processing failed
                    return;
                } catch (SecurityException e) {
                    Log.e(TAG, "Security exception loading image bitmap: " + imageUriString + ". Check URI permissions.", e);
                    hybridAiCoordinator.handleLlmError(actualRequestCode, "Permission error loading image: " + imageUriString, requestJson);
                    finish();
                    return;
                }
            }

            // The HybridAiCoordinator's processIncomingMessage expects an Activity to call startActivityForResult.
            // This LlmResultForwarderActivity IS that activity.
            hybridAiCoordinator.processIncomingMessage(this, requestJson, imageBitmap, actualRequestCode);
            // processIncomingMessage will call startActivityForResult on 'this' (LlmResultForwarderActivity)
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing request JSON", e);
            if (hybridAiCoordinator != null && actualRequestCode != 0) {
                hybridAiCoordinator.handleLlmError(actualRequestCode, "Error parsing request JSON in Forwarder.", null);
            }
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (hybridAiCoordinator == null) { // Should ideally not happen if onCreate succeeded
             Log.e(TAG, "HybridAiCoordinator is null in onActivityResult");
             finish();
             return;
        }

        // Ensure we are handling the specific request code we used for EdgeGallery
        if (requestCode == actualRequestCode) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String llmResponse = data.getStringExtra(EdgeGalleryAppClient.RESULT_EXTRA_LLM_RESPONSE);
                Log.d(TAG, "Received LLM response: " + (llmResponse != null ? llmResponse.substring(0, Math.min(llmResponse.length(), 100)) : "null"));
                hybridAiCoordinator.handleLlmResponse(requestCode, llmResponse != null ? llmResponse : "");
            } else {
                String errorMsg = "EdgeGallery request failed or was cancelled. ResultCode: " + resultCode;
                if (data != null && data.hasExtra(EdgeGalleryAppClient.RESULT_EXTRA_LLM_RESPONSE)) {
                    errorMsg += " Details: " + data.getStringExtra(EdgeGalleryAppClient.RESULT_EXTRA_LLM_RESPONSE);
                }
                Log.e(TAG, errorMsg);
                // The original message for this request code is already stored in HybridAiCoordinator's pendingLlmRequests map
                hybridAiCoordinator.handleLlmError(requestCode, errorMsg, null);
            }
        } else {
            Log.w(TAG, "Received onActivityResult for an unknown requestCode: " + requestCode + " (expected " + actualRequestCode + ")");
        }
        // Finish this headless activity after forwarding the result or error
        if (!isFinishing()) {
            finish();
        }
    }
}
