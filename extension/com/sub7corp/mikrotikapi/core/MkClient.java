package com.sub7corp.mikrotikapi.core;

import java.io.IOException;

public class MkClient {

    private MkConnection connection;
    private boolean connected = false;

    public MkClient(String host, int port) {
        connection = new MkConnection(host, port);
    }

    public void setTimeout(int timeout) {
        connection.setTimeout(timeout);
    }

    // =========================
    // CONNECT / LOGIN
    // =========================
    public void connect(String user, String password) throws Exception {
        connection.connect();
        connection.login(user, password);
        connected = true;
    }

    public boolean isConnected() {
        return connected;
    }

    // =========================
    // EXECUTE COMMAND
    // =========================
    public MkResponse execute(String... words) throws IOException {

        if (!connected) {
            throw new IOException("Not connected to MikroTik");
        }

        for (String word : words) {
            connection.writeSentence(word);
        }
        connection.writeSentence("");

        return connection.readResponse();
    }

    // =========================
    // DISCONNECT
    // =========================
    public void disconnect() {
        connection.disconnect();
        connected = false;
    }
}
