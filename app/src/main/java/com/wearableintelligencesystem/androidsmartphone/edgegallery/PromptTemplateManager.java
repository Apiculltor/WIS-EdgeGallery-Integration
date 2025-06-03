        }
        
        prompt.append("\nProvide a clear, actionable response suitable for smart glasses display. ");
        prompt.append("Keep it concise but comprehensive.");
        
        return prompt.toString();
    }
    
    /**
     * Extract relevant context information from JSON
     */
    private String extractContextInfo(JSONObject context) {
        if (context == null) return "No additional context";
        
        StringBuilder contextStr = new StringBuilder();
        
        try {
            if (context.has("timestamp")) {
                contextStr.append("Time: ").append(context.getLong("timestamp")).append(", ");
            }
            if (context.has("location")) {
                contextStr.append("Location: ").append(context.getString("location")).append(", ");
            }
            if (context.has("timeOfDay")) {
                contextStr.append("Time of day: ").append(context.getString("timeOfDay")).append(", ");
            }
            if (context.has("recentActivity")) {
                contextStr.append("Recent activity: ").append(context.getString("recentActivity")).append(", ");
            }
            if (context.has("environmentType")) {
                contextStr.append("Environment: ").append(context.getString("environmentType"));
            }
        } catch (JSONException e) {
            return "Context parsing error";
        }
        
        String result = contextStr.toString();
        return result.endsWith(", ") ? result.substring(0, result.length() - 2) : result;
    }
}
