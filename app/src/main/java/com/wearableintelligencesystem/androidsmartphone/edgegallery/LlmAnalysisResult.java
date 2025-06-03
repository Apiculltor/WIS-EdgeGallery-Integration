package com.wearableintelligencesystem.androidsmartphone.edgegallery;

import java.util.List;

/**
 * LlmAnalysisResult - Data class for LLM analysis results
 * 
 * Encapsulates the results from EdgeGallery LLM processing including
 * analysis text, confidence scores, suggested actions, and timing information.
 */
public class LlmAnalysisResult {
    private final String analysis;
    private final long processingTime;
    private final float confidence;
    private final List<String> suggestedActions;
    private final List<String> proactiveInsights;
    private final String intent;
    private final List<String> actionItems;
    private final String sentiment;
    private final String enhancedTranscript;
    private final boolean success;
    
    private LlmAnalysisResult(Builder builder) {
        this.analysis = builder.analysis;
        this.processingTime = builder.processingTime;
        this.confidence = builder.confidence;
        this.suggestedActions = builder.suggestedActions;
        this.proactiveInsights = builder.proactiveInsights;
        this.intent = builder.intent;
        this.actionItems = builder.actionItems;
        this.sentiment = builder.sentiment;
        this.enhancedTranscript = builder.enhancedTranscript;
        this.success = builder.success;
    }
    
    public static class Builder {
        private String analysis = "";
        private long processingTime = 0;
        private float confidence = 0.0f;
        private List<String> suggestedActions = null;
        private List<String> proactiveInsights = null;
        private String intent = "";
        private List<String> actionItems = null;
        private String sentiment = "";
        private String enhancedTranscript = "";
        private boolean success = false;
        
        public Builder setAnalysis(String analysis) {
            this.analysis = analysis;
            return this;
        }
        
        public Builder setProcessingTime(long time) {
            this.processingTime = time;
            return this;
        }
        
        public Builder setConfidence(float confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder setSuggestedActions(List<String> actions) {
            this.suggestedActions = actions;
            return this;
        }
        
        public Builder setProactiveInsights(List<String> insights) {
            this.proactiveInsights = insights;
            return this;
        }
        
        public Builder setIntent(String intent) {
            this.intent = intent;
            return this;
        }
        
        public Builder setActionItems(List<String> items) {
            this.actionItems = items;
            return this;
        }
        
        public Builder setSentiment(String sentiment) {
            this.sentiment = sentiment;
            return this;
        }
        
        public Builder setEnhancedTranscript(String transcript) {
            this.enhancedTranscript = transcript;
            return this;
        }
        
        public Builder setSuccess(boolean success) {
            this.success = success;
            return this;
        }
        
        public LlmAnalysisResult build() {
            return new LlmAnalysisResult(this);
        }
    }
    
    // Getters
    public String getAnalysis() { return analysis; }
    public long getProcessingTime() { return processingTime; }
    public float getConfidence() { return confidence; }
    public List<String> getSuggestedActions() { return suggestedActions; }
    public List<String> getProactiveInsights() { return proactiveInsights; }
    public String getIntent() { return intent; }
    public List<String> getActionItems() { return actionItems; }
    public String getSentiment() { return sentiment; }
    public String getEnhancedTranscript() { return enhancedTranscript; }
    public boolean isSuccess() { return success; }
}
