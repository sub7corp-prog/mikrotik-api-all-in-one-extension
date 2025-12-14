package com.sub7corp.mikrotikapi.core;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Low-level connection handler for MikroTik API
 * Supports plain and SSL connections.
 *
 * NOTE: allowInsecureSSL=true will trust all certificates (use only in LAN/dev).
 */
public class MkConnection {

    private static final String TAG = "MikrotikAPI";
    private static final boolean DEBUG = true;

    private Socket socket;
    private BufferedInputStream in;
    private BufferedOutputStream out;

    private boolean connected = false;

    // Timeouts
    private int soTimeoutMs = 5000;
    private int connectTimeoutMs = 5000; // (reserved if you later use SocketAddress connect)

    // SSL options
    private boolean allowInsecureSSL = false;

    public void setSoTimeoutMs(int value) {
        this.soTimeoutMs = Math.max(1000, value);
    }

    public void setConnectTimeoutMs(int value) {
        this.connectTimeoutMs = Math.max(1000, value);
    }

    public void setAllowInsecureSSL(boolean value) {
        this.allowInsecureSSL = value;
    }

    public boolean isConnected() {
        return connected;
    }

    /* =========================
       ===== CONNECT ===========
       ========================= */

    public void connect(String host, int port, boolean useSSL) throws IOException {
        // Prevent leaked sockets if Connect() is called multiple times.
        if (socket != null && !socket.isClosed()) {
            disconnect();
        }

        if (DEBUG) Log.d(TAG, "Connecting to " + host + ":" + port + " ssl=" + useSSL);

        if (useSSL) {
            SSLSocketFactory factory = buildSslSocketFactory();
            socket = factory.createSocket(host, port);

            // Handshake explicitly to surface SSL errors early.
            ((SSLSocket) socket).startHandshake();

        } else {
            socket = new Socket(host, port);
        }

        // Read timeout for API operations
        socket.setSoTimeout(soTimeoutMs);

        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());

        connected = true;
    }

    private SSLSocketFactory buildSslSocketFactory() throws IOException {
        if (!allowInsecureSSL) {
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }

        // Trust-all SSL context (ONLY for dev/LAN)
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            throw new IOException("Failed to init insecure SSL: " + e.getMessage(), e);
        }
    }

    /* =========================
       ===== LOGIN ==============
       ========================= */

    public boolean login(String username, String password) throws IOException {
        if (!connected) return false;

        if (DEBUG) Log.d(TAG, "Login user=" + username);

        writeSentence(
                "/login",
                "=name=" + username,
                "=password=" + password
        );

        String sentence;
        while ((sentence = readSentence()) != null) {
            if (sentence.contains("!done")) {
                return true;
            }
            if (sentence.contains("!trap")) {
                return false;
            }
        }
        return false;
    }

    /* =========================
       ===== WRITE ==============
       ========================= */

    public synchronized void writeSentence(String... words) throws IOException {
        ensureConnected();

        for (String word : words) {
            writeWord(word);
        }
        writeWord(""); // end sentence
        out.flush();
    }

    private void writeWord(String word) throws IOException {
        byte[] data = word.getBytes("UTF-8");
        writeLength(data.length);
        out.write(data);
    }

    private void writeLength(int length) throws IOException {
        if (length < 0x80) {
            out.write(length);
        } else if (length < 0x4000) {
            out.write((length >> 8) | 0x80);
            out.write(length & 0xFF);
        } else if (length < 0x200000) {
            out.write((length >> 16) | 0xC0);
            out.write((length >> 8) & 0xFF);
            out.write(length & 0xFF);
        } else {
            out.write((length >> 24) | 0xE0);
            out.write((length >> 16) & 0xFF);
            out.write((length >> 8) & 0xFF);
            out.write(length & 0xFF);
        }
    }

    /* =========================
       ===== READ ===============
       ========================= */

    public synchronized String readSentence() throws IOException {
        ensureConnected();

        StringBuilder sb = new StringBuilder();
        String word;

        while (!(word = readWord()).isEmpty()) {
            sb.append(word).append("\n");
        }
        return sb.toString();
    }

    private String readWord() throws IOException {
        int length = readLength();
        if (length == 0) return "";

        byte[] data = new byte[length];
        readFully(data, 0, length);

        return new String(data, "UTF-8");
    }

    private void readFully(byte[] buffer, int off, int len) throws IOException {
        int total = 0;
        while (total < len) {
            int r = in.read(buffer, off + total, len - total);
            if (r < 0) throw new EOFException("Connection closed while reading");
            total += r;
        }
    }

    private int readLength() throws IOException {
        int c = in.read();
        if (c < 0) throw new EOFException("Connection closed");

        if ((c & 0x80) == 0x00) {
            return c;
        } else if ((c & 0xC0) == 0x80) {
            int b = in.read();
            if (b < 0) throw new EOFException("Connection closed");
            return ((c & 0x3F) << 8) + b;
        } else if ((c & 0xE0) == 0xC0) {
            int b1 = in.read();
            int b2 = in.read();
            if (b1 < 0 || b2 < 0) throw new EOFException("Connection closed");
            return ((c & 0x1F) << 16) + (b1 << 8) + b2;
        } else {
            int b1 = in.read();
            int b2 = in.read();
            int b3 = in.read();
            if (b1 < 0 || b2 < 0 || b3 < 0) throw new EOFException("Connection closed");
            return ((c & 0x0F) << 24) + (b1 << 16) + (b2 << 8) + b3;
        }
    }

    private void ensureConnected() throws IOException {
        if (!connected || socket == null || socket.isClosed()) {
            throw new IOException("Not connected");
        }
    }

    /* =========================
       ===== DISCONNECT =========
       ========================= */

    public synchronized void disconnect() {
        if (DEBUG) Log.d(TAG, "Disconnecting...");

        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}

        in = null;
        out = null;
        socket = null;
        connected = false;
    }
}
