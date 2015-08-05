package com.pinggusoft.device;

import com.pinggusoft.bthud.LogUtil;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public abstract class SerialPort {
    public static final int    MESSAGE_STATE_CHANGE = 1;
    public static final int    MESSAGE_READ         = 2;
    public static final int    MESSAGE_WRITE        = 3;
    public static final int    MESSAGE_DEVICE_NAME  = 4;
    public static final int    MESSAGE_TOAST        = 5;

    public static final String DEVICE_NAME          = "device_name";
    public static final String TOAST                = "toast";

    // Constants that indicate the current connection state
    public static final int    STATE_NONE           = 0;            // we're doing nothing
    public static final int    STATE_LISTEN         = 1;            // now listening for incoming
                                                                     // connections
    public static final int    STATE_CONNECTING     = 2;            // now initiating an outgoing
                                                                     // connection
    public static final int    STATE_CONNECTED      = 3;            // now connected to a remote
                                                                     // device    

    protected Handler          mHandler;
    private int                mState;

    public SerialPort(Context context, Handler handler) {
        mHandler = handler;
        mState = STATE_NONE;
    }

    public void changeHandler(Handler handler) {
        mHandler = handler;
    }

    public Handler getHandler() {
        return mHandler;
    }

    protected synchronized void setState(int state, String strDev) {
        LogUtil.d("setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    protected void connectionLost(String strDev) {
        setState(STATE_LISTEN, strDev);
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "connection lost : " + strDev);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    protected void connectionFailed(String strDev) {
        setState(STATE_LISTEN, strDev);
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "unable connect : " + strDev);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    
    protected void connectionSuccess(String strDev) {
        setState(STATE_CONNECTED, strDev);
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "connected to : " + strDev);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    abstract public void connect(Object device);

    abstract public void stop();

    abstract public void write(byte[] out);

    abstract public void write(byte[] out, int pos, int len);

    public static String byteArrayToHex(byte[] a, int size) {
        if (a == null)
            return null;

        StringBuilder sb = new StringBuilder(a.length * 3);
        int c = 0;
        for (byte b : a) {
            if (++c > size)
                break;
            sb.append(String.format("%02x ", b & 0xff));
        }
        sb.append(" => ");
        c = 0;
        for (byte b : a) {
            if (++c > size)
                break;
            sb.append(String.format("%c", b & 0xff));
        }
        return sb.toString();
    }
}
