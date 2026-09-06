package com.zosh.utils;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

public class OtpUtil {

    private static final Random random = new Random();

    public static String generateOtp() {

        int otp = 100000 + random.nextInt(900000);

        return String.valueOf(otp);
    }
    
    public static String formatCategoryName(String categoryId) {
        return Arrays.stream(categoryId.split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}