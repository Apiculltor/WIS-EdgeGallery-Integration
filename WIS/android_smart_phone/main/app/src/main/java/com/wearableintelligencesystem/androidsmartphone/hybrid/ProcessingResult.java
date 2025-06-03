package com.wearableintelligencesystem.androidsmartphone.hybrid;

// Placeholder for actual WIS response structure
class WisResponse {
    String data;
    public WisResponse(String data) { this.data = data; }
    public String getData() { return data; }
}
// Placeholder for actual LLM Analysis Result (though we get String from EdgeGallery now)
class LlmAnalysisResult {
    String text;
    public LlmAnalysisResult(String text) { this.text = text; }
    public String getText() { return text; }
}


public class ProcessingResult {
    private String source; // e.g., "WIS", "EdgeGallery"
    private WisResponse wisResponse;
    private String llmResultText; // Changed from LlmAnalysisResult to String
    private long processingTime;
    private boolean success;
    private Exception error;
    private String correlationId;

    private ProcessingResult(Builder builder) {
        this.source = builder.source;
        this.wisResponse = builder.wisResponse;
        this.llmResultText = builder.llmResultText;
        this.processingTime = builder.processingTime;
        this.success = builder.success;
        this.error = builder.error;
        this.correlationId = builder.correlationId;
    }

    // Getters
    public String getSource() { return source; }
    public WisResponse getWisResponse() { return wisResponse; }
    public String getLlmResultText() { return llmResultText; }
    public long getProcessingTime() { return processingTime; }
    public boolean isSuccess() { return success; }
    public Exception getError() { return error; }
    public String getCorrelationId() { return correlationId; }

    public static class Builder {
        private String source;
        private WisResponse wisResponse;
        private String llmResultText;
        private long processingTime;
        private boolean success;
        private Exception error;
        private String correlationId;

        public Builder setSource(String source) { this.source = source; return this; }
        public Builder setWisResponse(WisResponse response) { this.wisResponse = response; return this; }
        public Builder setLlmResultText(String resultText) { this.llmResultText = resultText; return this; }
        public Builder setProcessingTime(long time) { this.processingTime = time; return this; }
        public Builder setSuccess(boolean success) { this.success = success; return this; }
        public Builder setError(Exception error) { this.error = error; return this; }
        public Builder setCorrelationId(String id) { this.correlationId = id; return this; }
        public ProcessingResult build() { return new ProcessingResult(this); }
    }
}
