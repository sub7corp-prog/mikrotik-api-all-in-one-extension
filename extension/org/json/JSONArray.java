package org.json;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JSONArray {
    private final List<Object> values = new ArrayList<>();

    public JSONArray put(Object value) {
        values.add(value);
        return this;
    }

    @Override
    public String toString() {
        String body = values.stream()
                .map(JSONObject::serializeValue)
                .collect(Collectors.joining(","));
        return "[" + body + "]";
    }
}
