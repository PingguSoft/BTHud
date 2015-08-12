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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class DisplayLED {
    public static final int  MSG_TIMEOUT        = 1;
    public static final int  MSG_RX             = 2;
    public static final int  MSG_STATUS         = 3;
    public static final int  MSG_LAST           = 4;

    public static final int  COLOR_OFF_LED      = Color.argb(255, 100, 100, 0);
    private static final int COLOR_TBL_2BPP[][] = {
            { 0xff000000, 0xffff0000, 0xff00ff00, 0xffffc100 },
            { 0xfffffffe, 0xfffffffe, 0xfffffffe, 0x00000000 } };

    private SerialPort       m_Serial           = null;
    private boolean          m_boolConnected    = false;
    private Handler          m_hCallback;
    private BTHandler        m_hBT              = new BTHandler(this);
    private Context          m_ctx              = null;
    protected Bitmap         m_bmLED;
    private int              m_nWidth           = 16 * 5;
    private int              m_nHeight          = 16;
    private int              m_nVirWidth        = 16 * 20;
    private int              m_nVirHeight       = 16;
    private int              m_nBPP;
    private int              m_nColorCount;

    protected int            m_nFontSize        = 16;
    protected int            m_nYPos         = 14;
    private int[][]          m_arColorTbl       = COLOR_TBL_2BPP;
    protected String         m_strFontName;
    protected String         m_strText;
    private Bitmap           m_bmIcon;

    public DisplayLED(Context ctx, Handler callback, int width, int height, int vir_width,
            int vir_height, int bpp) {
        m_ctx = ctx;
        m_hCallback = callback;
        m_Serial = new BTSerialPort(m_ctx, m_hBT);
        m_nWidth = width;
        m_nHeight = height;
        m_nVirWidth = vir_width;
        m_nVirHeight = vir_height;
        m_nBPP = bpp;
        m_nColorCount = (int) Math.pow(2, bpp);
        LogUtil.e("11111");
    }

    public DisplayLED(Context ctx) {
        m_ctx = ctx;
        m_hCallback = null;
        m_Serial = new BTSerialPort(m_ctx, m_hBT);
        m_nWidth = 16 * 5;
        m_nHeight = 16;
        m_nVirWidth = 16 * 20;
        m_nVirHeight = 16;
        m_nBPP = 2;
        m_nColorCount = 4;
        LogUtil.e("22222");
    }

    public void setCallback(Handler callback) {
        m_hCallback = callback;
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
        private WeakReference<DisplayLED> mParent;

        BTHandler(DisplayLED parent) {
            mParent = new WeakReference<DisplayLED>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            final DisplayLED parent = mParent.get();

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
                        parent.m_hCallback.obtainMessage(MSG_STATUS, msg.arg1, 0, null)
                                .sendToTarget();
                    break;

                case SerialPort.MESSAGE_DEVICE_NAME:
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

    public Bitmap getBitmap() {
        return m_bmLED;
    }

    public void setBitmap(Bitmap bitmap) {
        m_bmLED = bitmap;
    }

    public void setFontName(String strFontName) {
        if (m_strFontName == null || !m_strFontName.equals(strFontName)) {
            m_strFontName = strFontName;
            genBitmap();
        }
    }

    public void setFontSize(int nFontSize) {
        if (m_nFontSize != nFontSize) {
            m_nFontSize = nFontSize;
            genBitmap();
        }
    }

    public void setText(String strText) {
        LogUtil.d("TEXT:%s", strText);
        if (m_strText == null || !m_strText.equals(strText)) {
            m_strText = strText;
            genBitmap();
        }
    }

    public String getText() {
        return m_strText;
    }

    public int getFontSize() {
        return m_nFontSize;
    }

    public void setFontYPos(int nYPos) {
        if (m_nYPos != nYPos) {
            m_nYPos = nYPos;
            genBitmap();
        }
    }

    public int getFontYPos() {
        return m_nYPos;
    }

    public String getFontName() {
        return m_strFontName;
    }

    public int getDefaultColor() {
        int nRow = m_nColorCount / (m_nColorCount + 1);
        int nCol = (m_nColorCount - 1) % m_nColorCount;

        return m_arColorTbl[nRow][nCol];
    }

    public int[][] getColorTable() {
        return m_arColorTbl;
    }

    public void genBitmap() {
        Typeface face = null;
        if (m_strFontName != null)
            face = Typeface.createFromFile(m_strFontName);

        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        paint.setTextSize(m_nFontSize);
        paint.setTextScaleX(1.0f);
        paint.setAlpha(0);
        paint.setAntiAlias(false);
        paint.setDither(false);
        paint.setFilterBitmap(false);
        if (face != null)
            paint.setTypeface(face);

        int width = (int) paint.measureText(m_strText, 0, m_strText.length()) + 16;
        LogUtil.d("Width:%d", width);
        width = (width + 7) / 8 * 8;
        if (width > m_nVirWidth)
            width = m_nVirWidth;

        m_bmLED = Bitmap.createBitmap(width, m_nVirHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(m_bmLED);
        canvas.drawColor(Color.BLACK);

        if (m_bmIcon != null) {
            paint.setColor(Color.WHITE);
            Bitmap bm = Bitmap.createScaledBitmap(m_bmIcon, 16, 16, false);
            Bitmap newBm = FloydSteinbergDither.dither(bm);
            Rect srcRect = new Rect(0, 0, newBm.getWidth(), newBm.getHeight());
            Rect dstRect = new Rect(0, 0, 16, 16);
            canvas.drawBitmap(newBm, srcRect, dstRect, paint);
        }

        paint.setColor(getDefaultColor());
        canvas.drawText(m_strText, 16f, m_nYPos, paint);
    }

    public void putPixel2Buf(byte[] buf, int x, int y, int color, int bpp, int height) {
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

    public byte[] getDispBuf() {
        byte[] bufBitmap = new byte[m_bmLED.getWidth() * m_bmLED.getHeight() / (8 / getBPP())];
        Arrays.fill(bufBitmap, (byte) 0x00);

        for (int i = 0; i < m_bmLED.getWidth(); i++) {
            for (int j = 0; j < m_bmLED.getHeight(); j++) {
                int pixel = m_bmLED.getPixel(i, j);

                if (pixel != Color.BLACK) {
                    int pix = 0;

                    for (int k = 0; k < m_nColorCount; k++) {
                        int nRow = k / 4;
                        int nCol = k % 4;

                        if (pixel == m_arColorTbl[nRow][nCol]) {
                            pix = k;
                            break;
                        }
                    }
                    putPixel2Buf(bufBitmap, i, j, pix, getBPP(), m_bmLED.getHeight());
                }
            }
        }

        return bufBitmap;
    }

    public byte[] getAttrBuf() {
        byte[] bufAttr = new byte[m_bmLED.getWidth() / 8];
        boolean bFlash;

        Arrays.fill(bufAttr, (byte) 0);

        for (int i = 0; i < m_bmLED.getWidth(); i++) {
            bFlash = false;
            for (int j = 0; j < m_bmLED.getHeight(); j++) {
                int pixel = m_bmLED.getPixel(i, j);
                if (Color.alpha(pixel) == 128) {
                    bFlash = true;
                }
            }
            if (bFlash)
                putPixel2Buf(bufAttr, i, 0, 1, 1, m_bmLED.getHeight());
        }

        return bufAttr;
    }

    public int getColorCount() {
        return m_nColorCount;
    }

    public int getInverseIndex() {
        return m_nColorCount;
    }

    public int getFillIndex() {
        return m_nColorCount + 1;
    }

    public int getFlashIndex() {
        return m_nColorCount + 2;
    }

    public int getWidth() {
        return m_nWidth;
    }

    public int getHeight() {
        return m_nHeight;
    }

    public int getVirtualWidth() {
        return m_nVirWidth;
    }

    public int getVirtualHeight() {
        return m_nVirHeight;
    }

    public int getBPP() {
        return m_nBPP;
    }

    private int    mLeftPixels[];
    private int    mRightPixels[];
    private Bitmap mSavedBitmap;

    public void shiftBitmap2Left() {
        m_bmLED.getPixels(mLeftPixels, 0, 1, 0, 0, 1, m_bmLED.getHeight());

        m_bmLED.getPixels(mRightPixels, 0, m_bmLED.getWidth() - 1, 1, 0, m_bmLED.getWidth() - 1,
                m_bmLED.getHeight());

        m_bmLED.setPixels(mRightPixels, 0, m_bmLED.getWidth() - 1, 0, 0, m_bmLED.getWidth() - 1,
                m_bmLED.getHeight());
        m_bmLED.setPixels(mLeftPixels, 0, 1, m_bmLED.getWidth() - 1, 0, 1, m_bmLED.getHeight());
    }

    public void saveBackup() {
        mLeftPixels = new int[m_bmLED.getHeight()];
        mRightPixels = new int[(m_bmLED.getWidth() - 1) * m_bmLED.getHeight()];
        mSavedBitmap = m_bmLED.copy(m_bmLED.getConfig(), true);
    }

    public void restoreBackup() {
        m_bmLED = mSavedBitmap.copy(mSavedBitmap.getConfig(), true);
    }

    public void show() {

    }

    public void saveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("font_name", m_strFontName);
        savedInstanceState.putString("text", m_strText);
        savedInstanceState.putInt("y_pos", m_nYPos);
        savedInstanceState.putInt("font_size", m_nFontSize);
        
        Bundle b = new Bundle();
        b.putParcelable("bitmap", m_bmLED);
        savedInstanceState.putBundle("bitmap_bundle", b);
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        m_strFontName = savedInstanceState.getString("font_name");
        m_strText = savedInstanceState.getString("text");
        m_nYPos   = savedInstanceState.getInt("y_pos");
        m_nFontSize = savedInstanceState.getInt("font_size");
        
        Bundle b = savedInstanceState.getBundle("bitmap_bundle");
        m_bmLED = b.getParcelable("bitmap");
    }
}
