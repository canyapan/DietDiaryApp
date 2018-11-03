package com.canyapan.dietdiaryapp.utils;

import org.apache.commons.lang3.ArrayUtils;

import java.math.BigInteger;

public class Base62 {
    private static final char[] alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static String encode(BigInteger val) {
        StringBuilder sb = new StringBuilder();
        while (val.compareTo(BigInteger.valueOf(0)) > 0) {
            sb.append(alphabet[val.mod(BigInteger.valueOf(62)).intValue()]);
            val = val.divide(BigInteger.valueOf(62));
        }

        return sb.reverse().toString();
    }

    public static BigInteger decode(final String val) {
        BigInteger bi = BigInteger.ZERO;
        long mult = 1;
        for (int i = val.length() - 1; i >= 0; i--) {
            bi = bi.add(BigInteger.valueOf(ArrayUtils.indexOf(alphabet, val.charAt(i)) * mult));
            mult *= 62;
        }

        return bi;
    }
}
