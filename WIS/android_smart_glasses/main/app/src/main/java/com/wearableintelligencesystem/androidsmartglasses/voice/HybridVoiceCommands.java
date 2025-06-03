package com.wearableintelligencesystem.androidsmartglasses.voice;

import android.content.Context;
import android.util.Log;
import com.wearableintelligencesystem.androidsmartglasses.WearableAiService; // Assuming direct access or an interface

public class HybridVoiceCommands {
    private static final String TAG = "HybridVoiceCommands_ASG";
    private Context mContext;
    private WearableAiService wearableAiService; // Or an interface to it

    public HybridVoiceCommands(Context context, WearableAiService service) {
        this.mContext = context;
        this.wearableAiService = service;
    }

    public void processVoiceCommand(String command) {
        if (command == null || command.isEmpty()) {
            Log.w(TAG, "processVoiceCommand called with empty command.");
            return;
        }
        Log.i(TAG, "Processing voice command: " + command);

        String lowerCommand = command.toLowerCase();

        if (lowerCommand.contains("explain") || lowerCommand.contains("summarize") || lowerCommand.contains("describe")) {
            Log.d(TAG, "LLM-type command detected: " + command);
            if (wearableAiService != null) {
                wearableAiService.sendLlmTextQuery(command, "VOICE_COMMAND_LLM");
            } else {
                Log.e(TAG, "WearableAiService instance is null, cannot send LLM query.");
            }
        } else {
            Log.d(TAG, "Traditional WIS command (or not recognized for LLM): " + command);
            // Placeholder for existing WIS command processing
            // wearableAiService.processTraditionalWisCommand(command);
        }
    }
}
