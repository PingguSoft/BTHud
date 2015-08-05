package com.pinggusoft.device;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import com.pinggusoft.bthud.LogUtil;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class BTSerialPort extends SerialPort {
    // Unique UUID for this application : L2CAP_PROTOCOL_UUID
    private static final UUID L2CAP_UUID      = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private BluetoothAdapter  mAdapter;
    private ConnectThread     mConnectThread;
    private ConnectedThread   mConnectedThread;
    private int               mMinRxSize      = 0;
    private Boolean           mBoolConnecting = false;
    private BluetoothDevice   mBTDev = null;

    public BTSerialPort(Context context, Handler handler) {
        super(context, handler);
    }

    public synchronized void start() {
        LogUtil.d("start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN, mBTDev.getName());
    }

    public synchronized void connect(Object device) {
        mBTDev = (BluetoothDevice) device;
        LogUtil.d("connect to: " + mBTDev.getName());

        synchronized (mBoolConnecting) {
            if (mBoolConnecting) {
                LogUtil.d("Still connecting....");
                return;
            }
        }

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // // Cancel any thread currently running a connection
        // if (mConnectedThread != null) {
        // mConnectedThread.cancel();
        // mConnectedThread = null;
        // }

        if (mConnectedThread == null) {
            // Start the thread to connect with the given device
            mConnectThread = new ConnectThread(mBTDev);
            mConnectThread.start();
            setState(STATE_CONNECTING, mBTDev.getName());
        }
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        LogUtil.d("connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        connectionSuccess(mBTDev.getName());
        //setState(STATE_CONNECTED, mBTDev.getName());
    }

    public synchronized void stop() {
        LogUtil.d("stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE, mBTDev.getName());
    }

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (getState() != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public void write(byte[] out, int pos, int len) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (getState() != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out, pos, len);
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
        }

        @Override
        public void run() {
            synchronized (mBoolConnecting) {
                LogUtil.i("BEGIN ConnectThread : %d", mBoolConnecting ? 1 : 0);
                setName("ConnectThread");

                LogUtil.e("1 !!!");
                BluetoothSocket tmp = null;

                // Get a BluetoothSocket for a connection with the
                // given BluetoothDevice
                try {
                    tmp = mmDevice.createRfcommSocketToServiceRecord(L2CAP_UUID);
                    LogUtil.e("2 !!!");
                } catch (IOException e) {
                    LogUtil.e("create() failed" + e.toString());
                }
                mmSocket = tmp;
                LogUtil.d("Socket : %s", mmSocket.toString());

                LogUtil.e("3 !!!");
                mAdapter = BluetoothAdapter.getDefaultAdapter();

                // Always cancel discovery because it will slow down a
                // connection
                mAdapter.cancelDiscovery();

                if (mmSocket == null)
                    LogUtil.e("socket is null !!!!");
                // Make a connection to the BluetoothSocket
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    LogUtil.e("4 !!!");
                    mBoolConnecting = true;
                    mmSocket.connect();
                } catch (IOException e) {
                    LogUtil.e("5 !!!" + e.toString());
                    mBoolConnecting = false;
                    connectionFailed(mBTDev.getName());
                    // Close the socket
                    try {
                        mmSocket.close();
                        mmSocket = null;
                    } catch (IOException e2) {
                        LogUtil.e("unable to close() socket during connection failure : "
                                + e2.toString());
                    }
                    // Start the service over to restart listening mode
                    // BTSerialPort.this.start();
                    return;
                }

                LogUtil.e("6 !!!");
                // Start the connected thread
                connected(mmSocket, mmDevice);
                mBoolConnecting = false;
            }
        }

        public void cancel() {

        }
    }

    private class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream     mmInStream;
        private OutputStream    mmOutStream;
        private boolean         forceStop;

        public ConnectedThread(BluetoothSocket socket) {
            LogUtil.d("create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                LogUtil.e("temp sockets not created : " + e.toString());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            forceStop = false;
        }

        @Override
        public void run() {
            LogUtil.i("BEGIN ConnectedThread " + BTSerialPort.this.getState());
            int bytes;

            // Keep listening to the InputStream while connected
            while (!forceStop && BTSerialPort.this.getState() != STATE_NONE)
                try {
                    // Read from the InputStream
                    int nRxSize = mmInStream.available();
                    if (nRxSize > mMinRxSize) {
                        ByteBuffer byteBuf = ByteBuffer.allocate(nRxSize);
                        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
                        bytes = mmInStream.read(byteBuf.array(), 0, nRxSize);
                        mHandler.obtainMessage(MESSAGE_READ, bytes, 0, byteBuf.array())
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    LogUtil.e("disconnected : " + e.toString());
                    connectionLost(mBTDev.getName());
                    break;
                }

            LogUtil.i("END ConnectedThread " + BTSerialPort.this.getState());

            try {
                mmInStream.close();
                mmOutStream.close();
                mmSocket.close();
                mmSocket = null;
            } catch (IOException e) {
                LogUtil.e("close() of connect socket failed : " + e.toString());
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

            } catch (IOException e) {
                LogUtil.e("Exception during write : %s" + e.toString());
                connectionLost(mBTDev.getName());
            }
        }

        public void write(byte[] buffer, int pos, int len) {
            try {
                mmOutStream.write(buffer, pos, len);

            } catch (IOException e) {
                LogUtil.e("Exception during write : " + e.toString());
            }
        }

        public void cancel() {
            forceStop = true;

            try {
                join(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
