package com.elsistemas.bluetoothprinterlib.repository;

import android.bluetooth.BluetoothAdapter;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.elsistemas.bluetoothprinterlib.domain.IBluetoothPrinter;
import com.elsistemas.bluetoothprinterlib.utils.BluetoothDTO;
import com.elsistemas.bluetoothprinterlib.utils.PrinterCommands;
import com.elsistemas.bluetoothprinterlib.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kotlinx.coroutines.DisposableHandle;

public class BluetoothPrinter implements IBluetoothPrinter, DisposableHandle {
    private final Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private static ConnectedThread THREAD = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final Object initializationLock = new Object();

    public BluetoothPrinter(Context context) {
        this.context = context;

        init();
    }

    private void init(){
        synchronized (initializationLock){
            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    @Override
    public void dispose() {
        bluetoothAdapter = null;
        bluetoothManager = null;
    }

    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    //readSink.success(new String(buffer, 0, bytes));
                } catch (NullPointerException e) {
                    break;
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                outputStream.flush();
                outputStream.close();

                inputStream.close();

                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public String getPairedDevices() {
        List<Map<String, Object>> list = new ArrayList<>();

        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            Map<String, Object> ret = new HashMap<>();
            ret.put("address", device.getAddress());
            ret.put("name", device.getName());
            ret.put("type", device.getType());
            list.add(ret);
        }

        return BluetoothDTO.toJson(list);
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean connect(String address) throws Exception {
        try {
            if (THREAD != null) {
                return false;
            }

            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

            if (device == null) {
                return false;
            }

            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);

            if (socket == null) {
                return false;
            }

            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
                THREAD = new ConnectedThread(socket);
                THREAD.start();
                return true;
            } catch (Exception ex) {
                throw new Exception(ex.getMessage());
            }

        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    @Override
    public boolean disconnect() {
        try {
            if (THREAD == null) {
                throw new Exception("not connected");
            }

            AsyncTask.execute(() -> {
                THREAD.cancel();
                THREAD = null;
            });

            return true;
        } catch (Exception ex){
            return false;
        }
    }

    @Override
    public void printText(String text, int size, int align) throws Exception {
        byte[] cc = new byte[] { 0x1B, 0x21, 0x03 }; // 0- normal size text
        byte[] bb = new byte[] { 0x1B, 0x21, 0x08 }; // 1- only bold text
        byte[] bb2 = new byte[] { 0x1B, 0x21, 0x20 }; // 2- bold with medium text
        byte[] bb3 = new byte[] { 0x1B, 0x21, 0x10 }; // 3- bold with large text
        byte[] bb4 = new byte[] { 0x1B, 0x21, 0x30 }; // 4- strong text
        byte[] bb5 = new byte[] { 0x1B, 0x21, 0x50 }; // 5- extra strong text

        if (THREAD == null) {
            throw new Exception("not connected");
        }

        try {
            switch (size) {
                case 0:
                    THREAD.write(cc);
                    break;
                case 1:
                    THREAD.write(bb);
                    break;
                case 2:
                    THREAD.write(bb2);
                    break;
                case 3:
                    THREAD.write(bb3);
                    break;
                case 4:
                    THREAD.write(bb4);
                    break;
                case 5:
                    THREAD.write(bb5);
            }

            switch (align) {
                case 0:
                    // left align
                    THREAD.write(PrinterCommands.ESC_ALIGN_LEFT);
                    break;
                case 1:
                    // center align
                    THREAD.write(PrinterCommands.ESC_ALIGN_CENTER);
                    break;
                case 2:
                    // right align
                    THREAD.write(PrinterCommands.ESC_ALIGN_RIGHT);
                    break;
            }

            THREAD.write(text.getBytes());

            THREAD.write(PrinterCommands.FEED_LINE);
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    @Override
    public void printBlankLine(int quantity) throws Exception {
        if (THREAD == null) {
            throw new Exception("not connected");
        }

        try {
            if (quantity > 1){
                for (int i = 0; i < quantity; i++) {
                    THREAD.write(PrinterCommands.FEED_LINE);
                }
            } else {
                THREAD.write(PrinterCommands.FEED_LINE);
            }
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    @Override
    public void printImage(String pathImage) throws Exception {
        if (THREAD == null) {
            throw  new Exception("not connected");
        }

        try {
            Bitmap bmp = BitmapFactory.decodeFile(pathImage);
            if (bmp != null) {
                byte[] command = Utils.decodeBitmap(bmp);
                THREAD.write(PrinterCommands.ESC_ALIGN_CENTER);
                THREAD.write(command);
            }

        } catch (Exception ex) {
            throw  new Exception(ex.getMessage());
        }
    }

    @Override
    public void printImageBytes(byte[] bytes) throws Exception {
        if (THREAD == null) {
            throw  new Exception("not connected");
        }

        try {
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) {
                byte[] command = Utils.decodeBitmap(bmp);

                THREAD.write(PrinterCommands.ESC_ALIGN_CENTER);
                THREAD.write(command);
            } else {
                throw  new Exception("error to convert image");
            }
        } catch (Exception ex) {
            throw  new Exception("not connected");
        }
    }


}
