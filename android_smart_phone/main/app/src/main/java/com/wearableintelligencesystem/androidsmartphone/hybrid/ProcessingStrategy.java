package com.wearableintelligencesystem.androidsmartphone.hybrid;

/**
 * ProcessingStrategy - Defines different processing approaches
 */
public enum ProcessingStrategy {
    WIS_ONLY("Traditional WIS processing only"),
    EDGE_GALLERY_ONLY("EdgeGallery LLM processing only"),
    PARALLEL("Simultaneous WIS and EdgeGallery processing"),
    SEQUENTIAL_WIS_FIRST("WIS first, then EdgeGallery enhancement"),
    SEQUENTIAL_LLM_FIRST("EdgeGallery first, then WIS enhancement");
    
    private final String description;
    
    ProcessingStrategy(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
