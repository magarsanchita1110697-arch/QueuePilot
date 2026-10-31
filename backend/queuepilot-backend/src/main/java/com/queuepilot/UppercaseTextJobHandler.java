package com.queuepilot;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class UppercaseTextJobHandler implements JobHandler {

    @Override
    public String getJobType() {
        return "UPPERCASE_TEXT";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {

        Object textValue = payload.get("text");

        if (!(textValue instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Payload must contain a non-empty 'text' field."
            );
        }

        Map<String, Object> result = new HashMap<>();

        result.put("originalText", text);
        result.put("transformedText", text.toUpperCase(java.util.Locale.ROOT));

        return result;
    }
}
