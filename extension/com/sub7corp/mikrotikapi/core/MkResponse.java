package com.sub7corp.mikrotikapi.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MkResponse {

    private boolean done = false;
    private boolean trap = false;

    private String trapMessage;
    private final Map<String, String> attributes = new HashMap<>();
    private final List<Map<String, String>> records = new ArrayList<>();

    private Map<String, String> currentRecord;

    // =========================
    // PARSE LINE
    // =========================
    public void parseLine(String line) {

        if (line.startsWith("!done")) {
            done = true;
            closeRecord();
            return;
        }

        if (line.startsWith("!trap")) {
            trap = true;
            closeRecord();
            return;
        }

        if (line.startsWith("!re")) {
            closeRecord();
            currentRecord = new HashMap<>();
            records.add(currentRecord);
            return;
        }

        if (line.startsWith("=")) {
            int idx = line.indexOf("=", 1);
            if (idx > 0) {
                String key = line.substring(1, idx);
                String value = line.substring(idx + 1);

                if (currentRecord != null) {
                    currentRecord.put(key, value);
                } else {
                    attributes.put(key, value);
                    if ("message".equals(key)) {
                        trapMessage = value;
                    }
                }
            }
        }
    }

    private void closeRecord() {
        currentRecord = null;
    }

    // =========================
    // GETTERS
    // =========================
    public boolean isDone() {
        return done;
    }

    public boolean hasTrap() {
        return trap;
    }

    public String getTrapMessage() {
        return trapMessage;
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<Map<String, String>> getRecords() {
        return records;
    }

    // =========================
    // DEBUG
    // =========================
    @Override
    public String toString() {
        return "MkResponse{" +
                "done=" + done +
                ", trap=" + trap +
                ", trapMessage='" + trapMessage + '\'' +
                ", attributes=" + attributes +
                ", records=" + records +
                '}';
    }
}
