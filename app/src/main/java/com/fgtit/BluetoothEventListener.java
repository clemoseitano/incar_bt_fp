package com.fgtit;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

/**
 * Copyright (c) 2019 Farmerline LTD. All rights reserved.
 * Created by Clement Osei Tano K on 12/12/2019.
 */
public interface BluetoothEventListener {
    /**
     * A method to publish the name of the device when it is connected.
     *
     * @param newDeviceName the name of the device
     */
    public void onDeviceNameChanged(String newDeviceName);

    /**
     * A method to publish a message when interacting with the device, it could be status messages
     * or other messages.
     *
     * @param message the message being published from the device
     */
    public void onPublishMessage(String message);

    /**
     * A method to publish data received from the device as a byte array
     *
     * @param data the data being published from the device
     * @param size the size of the array
     */
    public void onPublishHexData(byte[] data, int size);

    /**
     * A method to publish a bitmap image when it is ready for display
     *
     * @param bmp the image
     */
    public void onBitmapReady(Bitmap bmp);

    /**
     * A method to publish data when a request is completed.
     *
     * @param respondentId the id of the respondent whose fingerprint data was collected or matched
     */
    public void onRequestCompleted(@Nullable String respondentId);
}
