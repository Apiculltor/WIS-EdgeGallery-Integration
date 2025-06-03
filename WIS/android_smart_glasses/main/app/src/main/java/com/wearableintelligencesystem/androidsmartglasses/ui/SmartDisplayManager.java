package com.wearableintelligencesystem.androidsmartglasses.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import com.wearableintelligencesystem.androidsmartglasses.comms.MessageTypes; // Ensure this path is correct
import org.json.JSONObject;

public class SmartDisplayManager {
    private static final String TAG = "SmartDisplayManager_ASG";
    private Context mContext;
    private Handler uiHandler;

    public SmartDisplayManager(Context context) {
        this.mContext = context;
        this.uiHandler = new Handler(Looper.getMainLooper());
    }

    public void displayHybridResponse(JSONObject hybridResponseJson) {
        if (hybridResponseJson == null) {
            Log.w(TAG, "displayHybridResponse called with null JSON.");
            return;
        }
        Log.i(TAG, "Displaying Hybrid Response: " + hybridResponseJson.toString());

        final String displayText = hybridResponseJson.optString(MessageTypes.DISPLAY_TEXT, "Received hybrid data.");

        // Ensure Toast runs on UI thread
        uiHandler.post(() -> Toast.makeText(mContext, "SmartDisplay: " + displayText, Toast.LENGTH_LONG).show());

        // Placeholder for future multi-layer display logic
        // displayImmediate(hybridResponseJson.optString("immediate_layer"));
        // displayContextual(hybridResponseJson.optString("contextual_layer"));
    }

    // Placeholder methods
    public void displayImmediate(String text) {
        if (text == null || text.isEmpty()) return;
        Log.d(TAG, "displayImmediate: " + text);
        uiHandler.post(() -> Toast.makeText(mContext, "Immediate: " + text, Toast.LENGTH_SHORT).show());
    }

    public void displayContextual(String text) {
        if (text == null || text.isEmpty()) return;
        Log.d(TAG, "displayContextual: " + text);
        uiHandler.post(() -> Toast.makeText(mContext, "Contextual: " + text, Toast.LENGTH_SHORT).show());
    }
}
