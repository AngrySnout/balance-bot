package pw.kussa.balanbot.remotecontrol;

import android.app.Activity;
import android.bluetooth.*;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothController {
    private String mDeviceName;
    BluetoothDevice mmDevice;
    BluetoothSocket mmSocket;
    private OutputStream btOutStream;
    private InputStream btInputStream;
    private static BluetoothAdapter mBluetoothAdapter;
    private Runnable mOnDisconnect;
    private Handler mHandler;
    private long mLastReplyMillis;

    public void setOnDisconnect(Runnable target) {
        mOnDisconnect = target;
    }

    public BluetoothController(String deviceName) {
        mDeviceName = deviceName;
        mHandler = new Handler();
    }

    public InputStream getInputStream() {
        return btInputStream;
    }

    public boolean connect() throws IOException {
        if (mBluetoothAdapter == null) return false;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device: pairedDevices) {
                if(device.getName().equals(mDeviceName)) {
                    mmDevice = device;
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("0001101-0000-1000-8000-00805F9B34FB"));
                    mmSocket.connect();
                    btOutStream = mmSocket.getOutputStream();
                    btInputStream = mmSocket.getInputStream();
                    mLastReplyMillis = System.currentTimeMillis();
                    return true;
                }
            }
        }
        return false;
    }

    public void disconnect() {
        try {
            if (mmSocket != null) mmSocket.close();
        } catch (IOException ex) {}

        mmSocket = null;
        mmDevice = null;
        btInputStream = null;
        btOutStream = null;

        mHandler.post(mOnDisconnect);
    }

    public static boolean enableBluetooth(Activity activity) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBluetooth, 0);
            }
        } else {
            return false;
        }
        return true;
    }

    public boolean write(byte[] bytes) {
        if (btOutStream != null) {
            try {
                btOutStream.write(bytes);
            } catch(IOException ex) {
                disconnect();
                return false;
            }
        } else {
            disconnect();
            return false;
        }
        return true;
    }

    public boolean write(String text) {
        return write(text.getBytes());
    }

    public boolean writeParam(char message, byte param) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) message;
        bytes[1] = param;
        return write(bytes);
    }

    public interface Receiver {
        void received(byte[] data);
    }

    public Runnable receiver(final Receiver target) {
        return new Runnable() {
            final byte delimiter = 10;
            boolean stopWorker = false;
            int readBufferPosition = 0;
            byte[] readBuffer = new byte[1024];
            byte[] packetBytes = new byte[1024];

            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    InputStream inputStream = getInputStream();
                    if (inputStream == null) {
                        SystemClock.sleep(100);
                        continue;
                    }

                    if (System.currentTimeMillis() - mLastReplyMillis > 1000) {
                        disconnect();
                    }

                    try {
                        int bytesAvailable = inputStream.read(packetBytes);

                        if (bytesAvailable > 0) {
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];

                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    readBufferPosition = 0;

                                    target.received(encodedBytes);
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }

                            mLastReplyMillis = System.currentTimeMillis();
                        }
                    } catch (IOException ex) {
                        System.out.println("Error: IOException caught");
                        disconnect();
                    }
                }
            }
        };
    }
}
