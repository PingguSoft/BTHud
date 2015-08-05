package com.pinggusoft.bthud;

import com.pinggusoft.device.CNKHud;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class BTLedSignApp extends Application {
    public final static String  KEY_BTDEVICE              = "KEY_BTDEVICE";
    
   
    private SharedPreferences  m_spBTHud;
    private Editor             m_editorBTHud;

    public static final int    MESSAGE_STATE_CHANGE = 1;
    public static final int    MESSAGE_READ         = 2;
    public static final int    MESSAGE_WRITE        = 3;
    public static final int    MESSAGE_DEVICE_NAME  = 4;
    public static final int    MESSAGE_TOAST        = 5;

    private CNKHud             mDevice           = null;
    private String             mStrBTMac = null;
    private LedSignBitmap      mLedSign             = new LedSignBitmap(16 * 5, 16, 16 * 20, 16, 2);

    public void connectDev(Handler handler, String strBTMac) {
        mDevice = new CNKHud(getBaseContext(), handler);
        if (strBTMac != null) {
            mStrBTMac = strBTMac;
            mDevice.connect(mStrBTMac);
        }
        save();
    }

    public void connectDev(Handler handler) {
        mDevice = new CNKHud(getBaseContext(), handler);
        if (mStrBTMac != null) {
            mDevice.connect(mStrBTMac);
        }
    }    
    
    public LedSignBitmap getLedSignBitmap() {
        return mLedSign;
    }

    public void disconnectDev() {
        if (mDevice != null)
            mDevice.stop();
        mDevice = null;
    }
    
    public CNKHud getDev() {
        return mDevice;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        m_spBTHud = PreferenceManager.getDefaultSharedPreferences(this);
        m_editorBTHud = m_spBTHud.edit();
        mStrBTMac = m_spBTHud.getString(KEY_BTDEVICE, null);
    }
    
    public void save() {
        m_editorBTHud.putString(KEY_BTDEVICE, mStrBTMac);
        m_editorBTHud.commit();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        disconnectDev();
    }

}
