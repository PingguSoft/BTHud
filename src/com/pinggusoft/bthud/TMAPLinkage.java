package com.pinggusoft.bthud;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import com.skt.tmap.standard.interlock.IRemoteTmapInterlockCallback;
import com.skt.tmap.standard.interlock.IRemoteTmapInterlockParcelMsgService;
import com.skt.tmap.standard.interlock.TmapAudioData;
import com.skt.tmap.standard.interlock.TmapAuthorization;
import com.skt.tmap.standard.interlock.TmapEDCApi;
import com.skt.tmap.standard.interlock.TmapEDCApi.AuthorizationData;
import com.skt.tmap.standard.network.Requester;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

public class TMAPLinkage  extends Service {
    private ServiceConnection mConnection = new TMAPConnection();
    private IRemoteTmapInterlockCallback mInterlockCallback = new TMAPInterlockCallback();    
    private IRemoteTmapInterlockParcelMsgService mService;
    private Timer mPollTimer = new Timer();
    
    public TMAPLinkage () {
        LogUtil.e("!!");
        bindTMAP();
    }
    
    public void bindTMAP() {
        final Context ctx = this;
        
        mPollTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (TMAPLinkage.isRunningProcess(ctx, "com.skt.skaf.l001mtm091")) {
                    LogUtil.e("tmap is running!!!");
                    Intent intent = new Intent("tmap.interlock.intent.action.init");
                    intent.setClassName("com.skt.skaf.l001mtm091", "com.skt.tmap.standard.interlock.EDCService");
                    bindService(intent, mConnection, 1);
                    mPollTimer.cancel();
                } else {
                    LogUtil.e("tmap is not running!!!");
                }
            }
        }, 0, 5000);
    }
    
    public void unbindTMAP() {
        try {
            mService.unregisterTmapCallback(mInterlockCallback);
            unbindService(mConnection);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        mService = null;
    }

    private class TMAPConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.e("!!");
            mService = com.skt.tmap.standard.interlock.IRemoteTmapInterlockParcelMsgService.Stub.asInterface(service);
            AuthorizationData componentname;
            componentname = new com.skt.tmap.standard.interlock.TmapEDCApi.AuthorizationData();
            componentname.serviceType      = 1;
            componentname.manufacturerType = 12;
            componentname.modelName = "HLBHUD3";
            componentname.serialNo  = "SAA99999";
            componentname.isForceLogin = true;

            TmapEDCApi.RequestAuthorization(TMAPLinkage.this, componentname, new com.skt.tmap.standard.interlock.TmapEDCApi.AuthorizationCallback() {
                @Override
                public void onCancel() {
                    LogUtil.e("!!");
                }

                @Override
                public void onComplete(TmapAuthorization arg0) {
                    LogUtil.e("!!");
                }

                @Override
                public void onFail(int arg0, String arg1) {
                    LogUtil.e("%d - %s", arg0, arg1);
                }

                @Override
                public void onProgress(float arg0) {
                    LogUtil.e("%f", arg0);
                }

                @Override
                public void onStart(Requester arg0) {
                    LogUtil.e("!!");
                }
            });

            try {
                String ver = mService.getTmapVersion();
                LogUtil.d("TMAP Ver:%s", ver);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                String str = mService.getTmapInfo(0);
                LogUtil.d("TMAP info0:%s", str);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.e("!!");
        }
    }
    
    private class TMAPInterlockCallback implements IRemoteTmapInterlockCallback {
        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public void onTmapAudioCallback(TmapAudioData arg0) throws RemoteException {
            LogUtil.e("!!");
        }

        @Override
        public void onTmapRGCallback(String arg0) throws RemoteException {
            LogUtil.e("!!");
        }
    }
    
    public static boolean isRunningProcess(Context ctx, String s)
    {
        Iterator<RunningAppProcessInfo> info;
        
        info = ((ActivityManager)ctx.getSystemService("activity")).getRunningAppProcesses().iterator();
        do {
            if (info.next().processName.equals(s))
                return true;
        } while (info.hasNext());
        return false;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_STICKY;
        
        return START_STICKY;
    }
}
