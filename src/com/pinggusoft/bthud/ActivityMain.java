package com.pinggusoft.bthud;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import com.pinggusoft.bthud.ColorPickerDialog.OnColorChangedListener;
import com.pinggusoft.device.BTSerialPort;
import com.pinggusoft.device.CNKHud;
import com.pinggusoft.device.DisplayLED;
import com.pinggusoft.device.SerialPort;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;

public class ActivityMain extends Activity implements OnColorChangedListener {
    private final static String     TAG           = "BTLedSign";

    // Definition of the one requestCode we use for receiving results.
    static final private int        GET_CODE      = 0;
    static final private int        GET_OPEN      = 1;
    static final private int        GET_SAVE      = 2;

    private HashMap<String, String> mHashFonts;
    private ArrayAdapter<String>    mArrayAdapterFont;
    private Spinner                 mSpinnerFont;
    private LedSignView             mViewBanner;
    private int                     mFontSelIdx   = 0;
    private boolean                 mBoolPlay     = false;

    private int                     mIntPlaySpeed = 8;
    private boolean                 mBTConnected  = false;
    private BTLedSignApp            mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApp = (BTLedSignApp) getApplication();
        setContentView(R.layout.btledsign_main);
        mViewBanner = (LedSignView) findViewById(R.id.ViewBanner);
        mViewBanner.setDisplay(mApp.getDisplay());
        
        //Intent i = new Intent(this, TMAPLinkage.class);
        //startService(i);
        
        SeekBar speedBar = (SeekBar) findViewById(R.id.seekBarSpeed);
        if (speedBar != null) {
            speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mBoolPlay) {
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            getWindow().getDecorView().setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mIntPlaySpeed = progress;
                    mViewBanner.setPlaySpeed(mIntPlaySpeed);
                    Log.d(TAG, "H3 " + mIntPlaySpeed);

                    if (mBTConnected) {
                        byte[] bufSpeed = new byte[1];
                        bufSpeed[0] = (byte) (9 - mIntPlaySpeed);
                        String str = String.format("VAL : %d", bufSpeed[0]);
                        Log.d(TAG, str);
                        //mApp.sendData(BTLedSignApp.IOCTL_LED_PLAY, bufSpeed, 20);
                    }
                }
            });
            speedBar.setProgress(0);
        }

        // Font List
        mHashFonts = FontManager.enumerateFonts();

        if (mHashFonts != null) {
            Iterator<String> s = mHashFonts.keySet().iterator();
            mSpinnerFont = (Spinner) findViewById(R.id.SpinnerFont);
            if (mSpinnerFont != null) {
                //mFontSelIdx = mFontSpinner.getSelectedItemPosition();
                mArrayAdapterFont = new ArrayAdapter<String>(this, R.layout.font_list);
                if (mArrayAdapterFont != null) {
                    mSpinnerFont.setAdapter(mArrayAdapterFont);
                    String strFontFile;
                    while (s.hasNext()) {
                        strFontFile = s.next();
                        //Log.d("TEST", ">>>" + strFontFile + " : " + mFontList.get(strFontFile));
                        mArrayAdapterFont.add(strFontFile);
                    }
                    mArrayAdapterFont.sort(null);
                    mSpinnerFont.setOnItemSelectedListener(new OnItemSelectedListener() {
                        public void onItemSelected(AdapterView<?> parent, View view, int position,
                                long id) {
                            if (mFontSelIdx != position) {
                                String strFontName;
                                strFontName = (String) mSpinnerFont.getItemAtPosition(position);
                                mFontSelIdx = position;
                                LogUtil.d("SEL %s : %s", strFontName, mApp.getDisplay().getText());
                                mApp.getDisplay().setFontName(strFontName);
                                mViewBanner.invalidate();
                            }
                        }

                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        LogUtil.e("-----------");
        savedInstanceState.putInt("font_idx", mFontSelIdx);
        savedInstanceState.putInt("play_speed", mIntPlaySpeed);
        savedInstanceState.putBoolean("bt_connected", mBTConnected);
        mApp.getDisplay().saveInstanceState(savedInstanceState);
        mViewBanner.saveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        LogUtil.e("-----------");
        mFontSelIdx = savedInstanceState.getInt("font_idx");
        mIntPlaySpeed = savedInstanceState.getInt("play_speed");
        mBTConnected = savedInstanceState.getBoolean("bt_connected");
        mApp.getDisplay().restoreInstanceState(savedInstanceState);
        mViewBanner.restoreInstanceState(savedInstanceState);
    }

    public void updateLedSignInfo(boolean updated) {
        TextView textInfo = (TextView) findViewById(R.id.TextViewPhyRes);
        if (textInfo != null) {
            String strInfo = String.format(Locale.getDefault(), "%d x %d", mApp.getDisplay()
                    .getWidth(), mApp.getDisplay().getHeight());
            textInfo.setText(strInfo);
        }

        textInfo = (TextView) findViewById(R.id.TextViewVirRes);
        if (textInfo != null) {
            String strInfo = String.format(Locale.getDefault(), "%d x %d", mApp.getDisplay()
                    .getVirtualWidth(), mApp.getDisplay().getVirtualHeight());
            textInfo.setText(strInfo);
        }

        textInfo = (TextView) findViewById(R.id.TextViewBPP);
        if (textInfo != null) {
            String strInfo = String.format(Locale.getDefault(), "%d (%d)", mApp.getDisplay()
                    .getColorCount(), mApp.getDisplay().getBPP());
            textInfo.setText(strInfo);
        }

        if (updated)
            mViewBanner.invalidate();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        OnButtonDone(null);
        String str = mApp.getDisplay().getText();
        EditText editBanner = (EditText) findViewById(R.id.editTextBanner);
        if (editBanner != null && str != null) {
            editBanner.setText(str.toCharArray(), 0, str.length());
        }

        if (mSpinnerFont != null) {
            mSpinnerFont.setSelection(mFontSelIdx);
        }

        updateLedSignInfo(false);

        CheckBox checkVScrollLock = (CheckBox) findViewById(R.id.checkBoxVScroll);
        if (checkVScrollLock != null) {
            boolean check = mViewBanner.getVScroll();
            checkVScrollLock.setChecked(check);
        }

        int nColor = mApp.getDisplay().getDefaultColor();
        mViewBanner.setSelColor(mApp.getDisplay().getColorCount() - 1, nColor);

        int pos = mApp.getDisplay().getColorCount() - 1;
        ImageButton btn = (ImageButton) findViewById(R.id.buttonColor);
        if (btn != null) {
            Bitmap bm = ColorPickerAdapter.getColorBitmap(pos, 80, 0, nColor, mApp
                    .getDisplay().getColorCount(), false);
            btn.setImageBitmap(Bitmap.createScaledBitmap(bm, 64, 64, true));
        }

        SeekBar speedBar = (SeekBar) findViewById(R.id.seekBarSpeed);
        if (speedBar != null) {
            speedBar.setProgress(mIntPlaySpeed);
        }

        changeSyncButtonState(mBTConnected);

        mViewBanner.invalidate();
        
        if (mApp.getDisplay() == null || mApp.getDisplay().getBTState() != SerialPort.STATE_CONNECTED)
            mApp.connectDev(new CNKHandler(this));
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        Log.d(TAG, "onPause !!!");
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
        if (mBoolPlay)
            mViewBanner.stopPlay();
        Log.d(TAG, "onStop !!!");

        Intent i = new Intent(this, TMAPLinkage.class);
        stopService(i);
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        mApp.disconnectDev();
        Log.d(TAG, "onDestroy !!!");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String str;

        str = String.format("sel : %d", item.getItemId());
        Log.d(TAG, str);
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, ActivitySettings.class);
                startActivityForResult(intent, GET_CODE);
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void OnButtonDone(View v) {
        EditText editBanner = (EditText) findViewById(R.id.editTextBanner);
        String str = null;
        
        if (editBanner != null)
            str = editBanner.getText().toString();
        
        if (str == null || str.length() == 0)
            str = "æ»≥Á«œººø‰ Hello!!";
        
        mApp.getDisplay().setText(str);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (editBanner != null)
            imm.hideSoftInputFromWindow(editBanner.getWindowToken(), 0);
        mViewBanner.invalidate();
    }

    @Override
    public void colorChanged(int pos, int color) {
        ImageButton btn = (ImageButton) findViewById(R.id.buttonColor);

        int btnColor = 0;
        if (pos >= mApp.getDisplay().getColorCount())
            btnColor = mViewBanner.getSelColor();

        Bitmap bm = ColorPickerAdapter.getColorBitmap(pos, 80, btnColor, color, mApp
                .getDisplay().getColorCount(), false);
        btn.setImageBitmap(Bitmap.createScaledBitmap(bm, 64, 64, true));

        Log.d(TAG, "color " + pos);
        if (mViewBanner != null)
            mViewBanner.setSelColor(pos, color);
    }

    public void onClickColorPickerDialog(View v) {
        ColorPickerDialog dialog;
        dialog = new ColorPickerDialog(this, this, mApp.getDisplay().getColorTable(), mApp
                .getDisplay().getColorCount(), mViewBanner.getSelColor());
        dialog.show();
    }

    public void onClickVScrollLock(View v) {
        CheckBox checkVScrollLock = (CheckBox) findViewById(R.id.checkBoxVScroll);
        if (checkVScrollLock != null) {
            mViewBanner.lockVScroll(checkVScrollLock.isChecked());
        }
    }

    public void onClickPlay(View v) {
        Button btn = (Button) findViewById(R.id.buttonPlay);
        if (!mBoolPlay) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
            mViewBanner.startPlay();
            btn.setText(R.string.strStop);
            mBoolPlay = true;
        } else {
            mViewBanner.stopPlay();
            btn.setText(R.string.strPlay);
            mBoolPlay = false;
        }
    }

    public void onClickZoomPlus(View v) {
        int nFontSize = mApp.getDisplay().getFontSize();
        
        if (nFontSize < 50)
            nFontSize++;
        if (mViewBanner != null) {
            mApp.getDisplay().setFontSize(nFontSize);
            mViewBanner.invalidate();
        }
    }

    public void onClickZoomMinus(View v) {
        int nFontSize = mApp.getDisplay().getFontSize();
        
        if (nFontSize > 4)
            nFontSize--;
        if (mViewBanner != null) {
            mApp.getDisplay().setFontSize(nFontSize);
            mViewBanner.invalidate();
        }
    }

    public void changeSyncButtonState(boolean enable) {
        ImageButton btn = (ImageButton) findViewById(R.id.buttonSync);
        if (btn != null)
            btn.setEnabled(enable);
    }

    public void onClickFile(View v) {
        Intent intent = new Intent(this, ActivityLoadSave.class);

        Bundle b = new Bundle();
        b.putInt("key", 0);
        intent.putExtras(b);
        startActivityForResult(intent, GET_OPEN);
    }

    public void onClickSave(View v) {
        Intent intent = new Intent(this, ActivityLoadSave.class);

        Bundle b = new Bundle();
        b.putInt("key", 1);
        intent.putExtras(b);
        startActivityForResult(intent, GET_SAVE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "Activity result" + requestCode + ", result code" + resultCode);
        switch (requestCode) {
            case GET_CODE:
                if (data != null) {
                    String strBTMac = data.getAction();
                    Log.e(TAG, "RESULT " + strBTMac);
                    mApp.connectDev(new CNKHandler(this), strBTMac);
                }
                break;

            case GET_OPEN:
                CheckBox checkVScrollLock = (CheckBox) findViewById(R.id.checkBoxVScroll);
                if (checkVScrollLock != null) {
                    checkVScrollLock.setChecked(true);
                }
                mViewBanner.lockVScroll(true);
                break;

            case GET_SAVE:
                break;
        }
    }

    public void onClickBT(View v) {
        Intent intent = new Intent(this, ActivitySettings.class);
        startActivityForResult(intent, GET_CODE);
    }

    public void onClickSync(View v) {
        if (!mBTConnected)
            return;

        mApp.getDisplay().show();
    }
    
    static class CNKHandler extends Handler {
        private WeakReference<ActivityMain> mParent;

        CNKHandler(ActivityMain parent) {
            mParent = new WeakReference<ActivityMain>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            final ActivityMain parent = mParent.get();

            if (parent == null)
                return;

            switch (msg.what) {
                case DisplayLED.MSG_STATUS:
                    if (msg.arg1 == SerialPort.STATE_CONNECTED) {
                        parent.mBTConnected = true;
//                        LogUtil.d("SENDING....");
//
//                        Options options = new BitmapFactory.Options();
//                        options.inScaled = false;
//                        Bitmap bm = BitmapFactory.decodeResource(parent.getResources(), R.drawable.gps_receiving, options);
//                        CNKHud hud = (CNKHud)parent.mApp.getDisplay();
//                        hud.showDist(999);
//                        parent.mApp.getDisplay().setText("æ»≥Á«œººø‰!! Hello");
//                        parent.mApp.getDisplay().show();
                    } else {
                        parent.mBTConnected = false;
                    }
                    parent.changeSyncButtonState(parent.mBTConnected);
                    break;
            }
        }
    }
}
