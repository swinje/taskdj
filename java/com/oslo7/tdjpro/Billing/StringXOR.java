package com.oslo7.tdjpro.Billing;


import android.util.Base64;

public class StringXOR {

    public String encode(String s, String key) {
        return new String(Base64.encode(xorWithKey(s.getBytes(), key.getBytes()), Base64.NO_WRAP));
    }

    public String decode(String s, String key) {
        return new String(xorWithKey(Base64.decode(s.getBytes(), Base64.NO_WRAP), key.getBytes()));
    }

    private byte[] xorWithKey(byte[] a, byte[] key) {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ key[i%key.length]);
        }
        return out;
    }


}