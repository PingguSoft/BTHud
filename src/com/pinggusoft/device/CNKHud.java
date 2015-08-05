package com.pinggusoft.device;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import com.pinggusoft.bthud.FloydSteinbergDither;
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

public class CNKHud {
    public static final int     CNK_MSG_TIMEOUT    = 1;
    public static final int     CNK_MSG_RX         = 2;
    public static final int     CNK_BT_STATUS      = 3;
    public static final int     CNK_MSG_LAST       = 4;

    private static final String MSG_TEXT_HEADER    = "$COP,A000000000070";

    private SerialPort          m_Serial           = null;
    private boolean             m_boolConnected    = false;
    private Handler             m_hCallback;
    private BTHandler           mBTHandler         = new BTHandler(this);
    private String              m_strConDeviceName = null;
    private Context             m_ctx              = null;

    public CNKHud(Context ctx, Handler callback) {
        m_ctx = ctx;
        m_hCallback = callback;
        m_Serial = new BTSerialPort(m_ctx, mBTHandler);
    }

    public void connect(String addr) {
        BluetoothDevice devBT;

        if (m_Serial.getState() == SerialPort.STATE_CONNECTED)
            return;

        devBT = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(addr);
        m_Serial.connect(devBT);
    }

    public void stop() {
        m_Serial.stop();
    }

    public int getBTState() {
        return m_Serial.getState();
    }

    static class BTHandler extends Handler {
        private WeakReference<CNKHud> mParent;

        BTHandler(CNKHud parent) {
            mParent = new WeakReference<CNKHud>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            final CNKHud parent = mParent.get();

            if (parent == null)
                return;

            switch (msg.what) {
                case SerialPort.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case SerialPort.STATE_CONNECTED:
                            parent.m_boolConnected = true;
                            break;

                        case SerialPort.STATE_CONNECTING:
                            break;

                        case SerialPort.STATE_LISTEN:
                        case SerialPort.STATE_NONE:
                            parent.m_boolConnected = false;
                            break;
                    }
                    if (parent.m_hCallback != null)
                        parent.m_hCallback.obtainMessage(CNK_BT_STATUS, msg.arg1, 0, null)
                                .sendToTarget();
                    break;

                case SerialPort.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    parent.m_strConDeviceName = msg.getData().getString(BTSerialPort.DEVICE_NAME);
                    break;

                case SerialPort.MESSAGE_TOAST:
                    LogUtil.alert(parent.m_ctx, msg.getData().getString(BTSerialPort.TOAST));
                    break;

                case SerialPort.MESSAGE_READ:
                    break;
            }
        }
    }

    public void write(byte[] out) {
        if (!m_boolConnected)
            return;

        // LogUtil.d("[TJ] TX:" + BTSerialPort.byteArrayToHex(out, out.length));
        m_Serial.write(out);
    }

    public void write(byte[] out, int pos, int len) {
        if (!m_boolConnected)
            return;

        // LogUtil.d("[TJ] TX:"+BTSerialService.byteArrayToHex(out,
        // out.length));
        m_Serial.write(out, pos, len);
    }

    public void showDist(int dist) {
        write(String.format("$EVR,%d;", dist).getBytes());
    }

    public void showLimit(int limit) {
        write(String.format("$DSP,%d;", limit).getBytes());
    }

    public void showSpeed(int speed) {
        write(String.format("$SPS,%d;", speed).getBytes());
    }

    public void showText(Bitmap icon, String strText, String strFont) {
        final int height = 16;
        final int bpp = 1;

        Typeface face = null;
        if (strFont != null)
            face = Typeface.createFromFile(strFont);

        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        paint.setTextSize(14f);
        paint.setTextScaleX(1.0f);
        paint.setAlpha(0);
        paint.setAntiAlias(false);
        paint.setDither(false);
        paint.setFilterBitmap(false);
        if (face != null)
            paint.setTypeface(face);

        int width = (int) paint.measureText(strText, 0, strText.length()) + 16;
        LogUtil.d("Width:%d", width);
        width = (width + 7) / 8 * 8;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        if (icon != null) {
            paint.setColor(Color.BLACK);
            Bitmap bm = Bitmap.createScaledBitmap(icon, 16, 16, false);
            Bitmap newBm = FloydSteinbergDither.dither(bm);
            Rect srcRect = new Rect(0, 0, newBm.getWidth(), newBm.getHeight());
            Rect dstRect = new Rect(0, 0, 16, 16);
            canvas.drawBitmap(newBm, srcRect, dstRect, paint);
        }

        paint.setColor(Color.BLACK);
        canvas.drawText(strText, 16f, 13f, paint);

        byte[] bufBitmap = new byte[width * height / (8 / bpp)];
        Arrays.fill(bufBitmap, (byte) 0x00);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pixel = bitmap.getPixel(i, j);
                if (pixel == Color.BLACK) {
                    putPixel2Buf(bufBitmap, i, j, 1, bpp, height);
                }
            }
        }

        StringBuilder buf = new StringBuilder();
        buf.append(MSG_TEXT_HEADER);
        for (int i = 0; i < bufBitmap.length; i++)
            buf.append(String.format("%02X", bufBitmap[i]));
        buf.append(";");
        write(buf.toString().getBytes());
    }

    private void putPixel2Buf(byte[] buf, int x, int y, int color, int bpp, int height) {
        int nBit;
        byte ucRead;
        int dwOff;
        int nBPPMask = (1 << bpp) - 1;
        int nPPB = 8 / bpp;

        dwOff = x * height + y;
        nBit = (((nPPB - 1) - (dwOff % nPPB)) * bpp);
        ucRead = buf[dwOff / nPPB];
        ucRead = (byte) ((byte) (ucRead & ~(nBPPMask << nBit)) | (color << nBit));
        buf[dwOff / nPPB] = ucRead;
    }
}
