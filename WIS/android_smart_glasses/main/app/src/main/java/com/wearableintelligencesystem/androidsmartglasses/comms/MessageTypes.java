package com.wearableintelligencesystem.androidsmartglasses.comms;

public class MessageTypes {
    // Existing MessageTypes from WIS/android_smart_phone/main/app/src/main/java/com/wearableintelligencesystem/androidsmartphone/comms/MessageTypes.java
    // should ideally be kept in sync or shared. For this subtask, we'll define what's needed.

    // From Smartphone App (for reference, Vuzix receives these)
    public static final String MESSAGE_TYPE_LOCAL = "MESSAGE_TYPE_LOCAL"; // Key for the actual type
    public static final String UI_UPDATE_ACTION = "UI_UPDATE_ACTION"; // Existing example
    public static final String PHONE_CONNECTION_STATUS = "PHONE_CONNECTION_STATUS"; // Existing example

    // New types sent FROM Vuzix to Smartphone
    public static final String CONTEXTUAL_IMAGE_QUERY = "CONTEXTUAL_IMAGE_QUERY"; // For image with context/prompt
    public static final String LLM_TEXT_QUERY = "LLM_TEXT_QUERY"; // Generic text query for LLM
    public static final String LLM_VOICE_COMMAND = "LLM_VOICE_COMMAND"; // Specific for voice commands to LLM

    // Fields used in messages sent from Vuzix
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String CONTEXT_DATA = "CONTEXT_DATA"; // JSONObject containing device context
    public static final String PROMPT = "PROMPT";
    public static final String COMMAND_TEXT = "COMMAND_TEXT"; // Specific for voice commands
    public static final String QUERY_TYPE = "QUERY_TYPE"; // e.g., "Summarize", "Explain"

    // New type received BY Vuzix from Smartphone
    public static final String HYBRID_RESPONSE = "HYBRID_RESPONSE";
    // Fields expected in HYBRID_RESPONSE (mirroring ResponseAggregator.AggregatedResponse.toJSONObject())
    public static final String DISPLAY_TEXT = "display_text"; // Key Vuzix should look for to display
    public static final String LLM_TEXT = "llm_text";
    public static final String WIS_TEXT = "wis_text";
    public static final String ERROR_MESSAGE = "error_message"; // For errors from smartphone
}
