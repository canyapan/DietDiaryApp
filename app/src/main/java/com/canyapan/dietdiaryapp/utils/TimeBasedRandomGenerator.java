package com.canyapan.dietdiaryapp.utils;


import org.joda.time.DateTime;

import java.math.BigInteger;
import java.util.Random;

public class TimeBasedRandomGenerator {
    private static Random sRandom = null;

    public static BigInteger generate() {
        return BigInteger.valueOf(DateTime.now().getMillis())           // Time
                .multiply(BigInteger.valueOf(10000))                    // Push 4 digit left
                .add(BigInteger.valueOf(getRandom().nextInt(9999)));    // Rand 0000:9999
    }

    public static long generateLong() {
        return generate().longValue(); // LONG MAX 9,223,372,036,854,775,807
        // Should be OK until, Sun Sep 14 02:48:05 UTC 31197
    }

    public static long generateLong(DateTime dateTime) {
        return BigInteger.valueOf(dateTime.getMillis())
                .multiply(BigInteger.valueOf(10000))
                .add(BigInteger.valueOf(getRandom().nextInt(9999)))
                .longValue();
    }

    private static Random getRandom() {
        if (null == sRandom) {
            synchronized (TimeBasedRandomGenerator.class) {
                if (null == sRandom) {
                    sRandom = new Random();
                }
            }
        }

        return sRandom;
    }
}
