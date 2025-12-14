package com.sub7corp.mikrotikapi.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * High-level MikroTik API client
 * Sends commands and parses replies into structured data
 */
public class MkClient {

    private final MkConnection connection;

    public MkClient(MkConnection connection) {
        this.connection = connection;
    }

    /* =========================
       ===== EXECUTE ===========
       ========================= */

    public MkResult execute(String path, String... params) throws IOException {
        if (!connection.isConnected()) {
            throw new IOException("Not connected to MikroTik");
        }

        List<String> words = new ArrayList<>();
        words.add(path);

        if (params != null) {
            for (String p : params) {
                if (p == null) continue;
                p = p.trim();
                if (p.isEmpty()) continue;

                // MkConnection expects each property like "=key=value"
                if (!p.startsWith("=") && !p.startsWith(".")) {
                    words.add("=" + p);
                } else if (p.startsWith(".")) {
                    // allow ".id=*X" (we will convert to "=.id=*X" style)
                    words.add("=" + p);
                } else {
                    words.add(p);
                }
            }
        }

        connection.writeSentence(words.toArray(new String[0]));
        return readResult();
    }

    /* =========================
       ===== READ RESULT =======
       ========================= */

    private MkResult readResult() throws IOException {
        MkResult result = new MkResult();

        String sentence;
        while ((sentence = connection.readSentence()) != null) {

            if (sentence.contains("!trap")) {
                result.setError(true);
                result.setMessage(extractMessage(sentence));
                break;
            }

            if (sentence.contains("!re")) {
                result.addRecord(parseRecord(sentence));
            }

            if (sentence.contains("!done")) {
                result.setSuccess(true);
                break;
            }
        }

        return result;
    }

    /* =========================
       ===== PARSING ===========
       ========================= */

    private HashMap<String, String> parseRecord(String sentence) {
        HashMap<String, String> map = new HashMap<>();
        String[] lines = sentence.split("\n");

        for (String line : lines) {
            // records look like: "=key=value" OR "=.id=*A"
            if (line.startsWith("=")) {
                int idx = line.indexOf("=", 1);
                if (idx > 1) {
                    String key = line.substring(1, idx);
                    String value = line.substring(idx + 1);
                    map.put(key, value);
                }
            }
        }

        return map;
    }

    private String extractMessage(String sentence) {
        String[] lines = sentence.split("\n");
        for (String line : lines) {
            if (line.startsWith("=message=")) {
                return line.substring(9);
            }
        }
        return "Unknown MikroTik API error";
    }

    /* =========================
       ===== RESULT MODEL ======
       ========================= */

    public static class MkResult {

        private boolean success = false;
        private boolean error = false;
        private String message = "";
        private final List<HashMap<String, String>> records = new ArrayList<>();

        public boolean isSuccess() {
            return success;
        }

        private void setSuccess(boolean success) {
            this.success = success;
        }

        public boolean isError() {
            return error;
        }

        private void setError(boolean error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        private void setMessage(String message) {
            this.message = message;
        }

        public List<HashMap<String, String>> getRecords() {
            return records;
        }

        private void addRecord(HashMap<String, String> record) {
            records.add(record);
        }
    }
}
