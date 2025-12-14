package com.sub7corp.mikrotikapi.util;

import java.io.IOException;
import java.io.InputStream;

public class HexUtils {

    // =========================
    // ENCODE LENGTH (SEND)
    // =========================
    public static byte[] encodeLength(int length) {

        if (length < 0x80) {
            return new byte[]{(byte) length};
        } else if (length < 0x4000) {
            length |= 0x8000;
            return new byte[]{
                    (byte) (length >> 8),
                    (byte) length
            };
        } else if (length < 0x200000) {
            length |= 0xC00000;
            return new byte[]{
                    (byte) (length >> 16),
                    (byte) (length >> 8),
                    (byte) length
            };
        } else if (length < 0x10000000) {
            length |= 0xE0000000;
            return new byte[]{
                    (byte) (length >> 24),
                    (byte) (length >> 16),
                    (byte) (length >> 8),
                    (byte) length
            };
        } else {
            return new byte[]{
                    (byte) 0xF0,
                    (byte) (length >> 24),
                    (byte) (length >> 16),
                    (byte) (length >> 8),
                    (byte) length
            };
        }
    }

    // =========================
    // DECODE LENGTH (READ)
    // =========================
    public static int decodeLength(InputStream in) throws IOException {

        int c = in.read();
        if (c < 0x80) return c;

        if ((c & 0xC0) == 0x80) {
            return ((c & 0x3F) << 8) + in.read();
        }

        if ((c & 0xE0) == 0xC0) {
            return ((c & 0x1F) << 16)
                    + (in.read() << 8)
                    + in.read();
        }

        if ((c & 0xF0) == 0xE0) {
            return ((c & 0x0F) << 24)
                    + (in.read() << 16)
                    + (in.read() << 8)
                    + in.read();
        }

        return (in.read() << 24)
                + (in.read() << 16)
                + (in.read() << 8)
                + in.read();
    }

    // =========================
    // HEX STRING → BYTE[]
    // =========================
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)
                    ((Character.digit(s.charAt(i), 16) << 4)
                            + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
