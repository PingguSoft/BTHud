package com.pinggusoft.device;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import com.pinggusoft.bthud.FloydSteinbergDither;
import com.pinggusoft.bthud.LedSignBitmap;
import com.pinggusoft.bthud.LogUtil;
import com.pinggusoft.bthud.R;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class CNKHud extends DisplayLED {
    private static final int    DISP_WIDTH       = 16 + 36;
    private static final int    DISP_HEIGHT      = 16;
    private static final int    DISP_VIRT_WIDTH  = 256;
    private static final int    DISP_VIRT_HEIGHT = 16;
    private static final int    DISP_BPP         = 1;
    private static final String MSG_TEXT_HEADER  = "$COP,A000000000070";

    public CNKHud(Context ctx, Handler callback) {
        super(ctx, callback, DISP_WIDTH, DISP_HEIGHT, DISP_VIRT_WIDTH, DISP_VIRT_HEIGHT,
                DISP_BPP) ;
        LogUtil.e("-------");
    }

    public void showDist(int dist) {
        String str = String.format("$EVR,%d;", dist);
        LogUtil.d("%s", str);
        write(str.getBytes());
    }

    public void showLimit(int limit) {
        write(String.format("$DSP,%d;", limit).getBytes());
    }

    public void showSpeed(int speed) {
        write(String.format("$SPS,%d;", speed).getBytes());
    }

    public void show() {
        LogUtil.e("-------");
        genBitmap();
        byte[] bufBitmap = getDispBuf();

        StringBuilder buf = new StringBuilder();
        buf.append(MSG_TEXT_HEADER);
        for (int i = 0; i < bufBitmap.length; i++)
            buf.append(String.format("%02X", bufBitmap[i]));
        buf.append(";");
        write(buf.toString().getBytes());
    }
}
