package org.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JSONObject {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public JSONObject put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        String body = values.entrySet()
                .stream()
                .map(e -> quote(e.getKey()) + ":" + serializeValue(e.getValue()))
                .collect(Collectors.joining(","));
        return "{" + body + "}";
    }

    static String serializeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return quote((String) value);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof JSONObject || value instanceof JSONArray) {
            return value.toString();
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
