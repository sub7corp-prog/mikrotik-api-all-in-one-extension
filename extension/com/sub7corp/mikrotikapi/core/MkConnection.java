package com.sub7corp.mikrotikapi.core;

import com.sub7corp.mikrotikapi.util.HexUtils;
import com.sub7corp.mikrotikapi.util.Md5Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;

public class MkConnection {

    private Socket socket;
    private BufferedInputStream input;
    private BufferedOutputStream output;

    private String host;
    private int port;
    private int timeout = 5000;

    public MkConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    // =========================
    // CONNECT
    // =========================
    public void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeout);
        socket.setSoTimeout(timeout);

        input = new BufferedInputStream(socket.getInputStream());
        output = new BufferedOutputStream(socket.getOutputStream());
    }

    // =========================
    // LOGIN (AUTO ROS6 / ROS7)
    // =========================
    public void login(String user, String password) throws Exception {

        // Intento ROS7 (login directo)
        writeSentence("/login");
        writeSentence("=name=" + user);
        writeSentence("=password=" + password);
        writeSentence("");

        MkResponse response = readResponse();
        if (!response.hasTrap()) {
            return; // ROS7 OK
        }

        // Si falló, intento ROS6 (challenge)
        writeSentence("/login");
        writeSentence("");

        response = readResponse();
        String challenge = response.getAttribute("ret");
        if (challenge == null) {
            throw new Exception("No challenge received (login failed)");
        }

        String hashed = md5Challenge(password, challenge);

        writeSentence("/login");
        writeSentence("=name=" + user);
        writeSentence("=response=00" + hashed);
        writeSentence("");

        response = readResponse();
        if (response.hasTrap()) {
            throw new Exception("Login failed (invalid credentials)");
        }
    }

    // =========================
    // SEND / READ
    // =========================
    public void writeSentence(String sentence) throws IOException {
        byte[] data = HexUtils.encodeLength(sentence.length());
        output.write(data);
        output.write(sentence.getBytes("UTF-8"));
        output.flush();
    }

    public MkResponse readResponse() throws IOException {
        MkResponse response = new MkResponse();

        while (true) {
            int len = HexUtils.decodeLength(input);
            if (len == 0) break;

            byte[] data = new byte[len];
            input.read(data);

            String line = new String(data, "UTF-8");
            response.parseLine(line);
        }
        return response;
    }

    // =========================
    // DISCONNECT
    // =========================
    public void disconnect() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }

    // =========================
    // MD5 CHALLENGE (ROS6)
    // =========================
    private String md5Challenge(String password, String challenge) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update((byte) 0x00);
        md.update(password.getBytes("UTF-8"));
        md.update(HexUtils.hexStringToByteArray(challenge));
        return Md5Utils.toHex(md.digest());
    }
}
