package com.pinggusoft.bthud;

import android.graphics.Bitmap;
import android.graphics.Color;

public class FloydSteinbergDither {

    private static byte plus_truncate_uchar(byte a, int b) {
        if ((a & 0xff) + b < 0)
            return 0;
        else if ((a & 0xff) + b > 255)
            return (byte) 255;
        else
            return (byte) (a + b);
    }

    private static int findNearestColor(int color, int[] palette) {
        int minDistanceSquared = Integer.MAX_VALUE;
        byte bestIndex = 0;

        for (byte i = 0; i < palette.length; i++) {
            int Rdiff = (Color.red(color) & 0xff) - (Color.red(palette[i]) & 0xff);
            int Gdiff = (Color.green(color) & 0xff) - (Color.green(palette[i]) & 0xff);
            int Bdiff = (Color.blue(color) & 0xff) - (Color.blue(palette[i]) & 0xff);
            int distanceSquared = Rdiff * Rdiff + Gdiff * Gdiff + Bdiff * Bdiff;
            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared;
                bestIndex = i;
            }
        }
        return (bestIndex == 1) ? Color.WHITE : Color.BLACK;
    }

    private static int findNearestColor(int color) {
        if (Color.alpha(color) < 20)
            return Color.WHITE;
        
        int luminance = (int) ((0.2126 * Color.red(color)) + (0.7152 * Color.green(color)) + (0.0722 * Color
                .blue(color)));
        
        return (luminance >= 160) ? Color.WHITE : Color.BLACK;
    }

    public static Bitmap dither(Bitmap bmSrc) {
        Bitmap bmOut = bmSrc.copy(bmSrc.getConfig(), true);

        for (int y = 0; y < bmSrc.getHeight(); y++) {
            for (int x = 0; x < bmSrc.getWidth(); x++) {
                int pixel = bmSrc.getPixel(x, y);
                int newPix = findNearestColor(pixel);
                bmOut.setPixel(x, y, newPix);

                int errR = Color.red(pixel) - Color.red(newPix);
                int errG = Color.green(pixel) - Color.green(newPix);
                int errB = Color.blue(pixel) - Color.blue(newPix);
                int R, G, B, nePix;

                if (x + 1 < bmSrc.getWidth()) {
                    nePix = bmSrc.getPixel(x + 1, y + 0);
                    R = plus_truncate_uchar((byte) Color.red(nePix), (errR * 7) >> 4);
                    G = plus_truncate_uchar((byte) Color.green(nePix), (errG * 7) >> 4);
                    B = plus_truncate_uchar((byte) Color.blue(nePix), (errB * 7) >> 4);
                    bmOut.setPixel(x + 1, y + 0, Color.argb(0xff, R, G, B));
                }
                if (y + 1 < bmSrc.getHeight()) {
                    if (x - 1 > 0) {
                        nePix = bmSrc.getPixel(x - 1, y + 1);
                        R = plus_truncate_uchar((byte) Color.red(nePix), (errR * 3) >> 4);
                        G = plus_truncate_uchar((byte) Color.green(nePix), (errG * 3) >> 4);
                        B = plus_truncate_uchar((byte) Color.blue(nePix), (errB * 3) >> 4);
                        bmOut.setPixel(x - 1, y + 1, Color.argb(0xff, R, G, B));
                    }
                    nePix = bmSrc.getPixel(x + 0, y + 1);
                    R = plus_truncate_uchar((byte) Color.red(nePix), (errR * 5) >> 4);
                    G = plus_truncate_uchar((byte) Color.green(nePix), (errG * 5) >> 4);
                    B = plus_truncate_uchar((byte) Color.blue(nePix), (errB * 5) >> 4);
                    bmOut.setPixel(x + 0, y + 1, Color.argb(0xff, R, G, B));

                    if (x + 1 < bmSrc.getWidth()) {
                        nePix = bmSrc.getPixel(x + 1, y + 1);
                        R = plus_truncate_uchar((byte) Color.red(nePix), (errR * 1) >> 4);
                        G = plus_truncate_uchar((byte) Color.green(nePix), (errG * 1) >> 4);
                        B = plus_truncate_uchar((byte) Color.blue(nePix), (errB * 1) >> 4);
                        bmOut.setPixel(x + 1, y + 1, Color.argb(0xff, R, G, B));
                    }
                }
            }
        }

        return bmOut;
    }
}
