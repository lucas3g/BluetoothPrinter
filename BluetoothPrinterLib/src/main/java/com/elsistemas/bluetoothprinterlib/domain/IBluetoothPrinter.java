package com.elsistemas.bluetoothprinterlib.domain;

public interface IBluetoothPrinter {
    String getPairedDevices();

    boolean connect(String address) throws Exception;

    boolean disconnect();

    void printText(String text, int size, int align) throws Exception;

    void printBlankLine(int quantity) throws Exception;

    void printImage(String pathImage) throws Exception;

    void printImageBytes(byte [] bytes) throws Exception;
}
