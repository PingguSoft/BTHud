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

public class DotMatrix2BPP extends DisplayLED {
    private static final int DISP_WIDTH       = 16 * 5;
    private static final int DISP_HEIGHT      = 16;
    private static final int DISP_VIRT_WIDTH  = 16 * 20;
    private static final int DISP_VIRT_HEIGHT = 16;
    private static final int DISP_BPP         = 2;

    public DotMatrix2BPP(Context ctx, Handler callback) {
        super(ctx, callback, DISP_WIDTH, DISP_HEIGHT, DISP_VIRT_WIDTH, DISP_VIRT_HEIGHT,
                DISP_BPP);
    }

    public void show() {
        byte[] bufDisp = getDispBuf();
        byte[] bufAttr = getAttrBuf();
        byte[] buf     = new byte[bufDisp.length + bufAttr.length];

        System.arraycopy(bufDisp, 0, buf, 0, bufDisp.length);
        System.arraycopy(bufAttr, 0, buf, bufDisp.length, bufAttr.length);
        //mApp.sendData(BTLedSignApp.IOCTL_LED_SET_DATA, buf, 300);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte[] bufSpeed = new byte[1];
        //bufSpeed[0] = (byte) (9 - mIntPlaySpeed);
        //mApp.sendData(BTLedSignApp.IOCTL_LED_PLAY, bufSpeed, 0);
    }
}
