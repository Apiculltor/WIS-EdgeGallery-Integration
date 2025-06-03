package com.wearableintelligencesystem.androidsmartphone.hybrid;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import org.json.JSONObject;
import org.json.JSONException;
import android.util.Log;

public class IntelligentRouter {
    private static final String TAG = "IntelligentRouter";

    public enum ProcessingStrategy {
        WIS_ONLY("WIS processing only"),
        LLM_ONLY("LLM processing only via EdgeGallery"),
        SEQUENTIAL_WIS_THEN_LLM("WIS first, then LLM enhancement");
        // PARALLEL_HYBRID ("Parallel processing - not applicable with startActivityForResult for LLM");


        private final String description;
        ProcessingStrategy(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    public ProcessingStrategy determineStrategy(JSONObject message) {
        if (message == null) return ProcessingStrategy.WIS_ONLY; // Default or error case

        try {
            String messageType = message.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            // Example logic (to be expanded based on actual message types from Vuzix)
            // These message types would need to be defined in WIS's MessageTypes
            // or sent from Vuzix with a clear indicator for LLM processing.
            if (MessageTypes.LLM_VOICE_COMMAND.equals(messageType) ||
                MessageTypes.NATURAL_LANGUAGE_QUERY.equals(messageType) ||
                "CONTEXTUAL_IMAGE_QUERY".equals(messageType)) { // Assuming CONTEXTUAL_IMAGE_QUERY is a new type for this
                return ProcessingStrategy.LLM_ONLY; // Or SEQUENTIAL if WIS preprocessing is needed
            } else if (MessageTypes.POV_IMAGE.equals(messageType) && !message.has("llm_prompt")){ // A raw image without a specific LLM prompt might be WIS_ONLY
                 return ProcessingStrategy.WIS_ONLY;
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error determining strategy from message: " + message.toString(), e);
            return ProcessingStrategy.WIS_ONLY; // Fallback on error
        }

        // Default to WIS_ONLY if no specific LLM strategy matches
        return ProcessingStrategy.WIS_ONLY;
    }
}
