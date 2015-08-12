package com.pinggusoft.bthud;

import com.pinggusoft.device.CNKHud;
import com.pinggusoft.device.DisplayLED;

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
    public final static String KEY_BTDEVICE         = "KEY_BTDEVICE";

    private SharedPreferences  m_spBTHud;
    private Editor             m_editorBTHud;

    public static final int    MESSAGE_STATE_CHANGE = 1;
    public static final int    MESSAGE_READ         = 2;
    public static final int    MESSAGE_WRITE        = 3;
    public static final int    MESSAGE_DEVICE_NAME  = 4;
    public static final int    MESSAGE_TOAST        = 5;

    private DisplayLED         mDisplay             = null;
    private String             mStrBTMac            = null;
    
    public void connectDev(Handler handler, String strBTMac) {
        mDisplay.setCallback(handler);
        if (strBTMac != null) {
            mStrBTMac = strBTMac;
            mDisplay.connect(mStrBTMac);
        }
        save();
    }

    public void connectDev(Handler handler) {
        mDisplay.setCallback(handler);
        if (mStrBTMac != null) {
            mDisplay.connect(mStrBTMac);
        }
    }

    public void disconnectDev() {
        if (mDisplay != null)
            mDisplay.stop();
        mDisplay = null;
    }

    public DisplayLED getDisplay() {
        if (mDisplay == null)
            mDisplay = new CNKHud(getBaseContext(), null);
        return mDisplay;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.e("-----------");
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
