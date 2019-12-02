package com.fgtit;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.fgtit.reader.Constants;
import com.fgtit.reader.FPReaderActivity;

/**
 * Copyright (c) 2019 Farmerline LTD. All rights reserved.
 * Created by Clement Osei Tano K on 02/12/2019.
 */
public class FingerprintReader {
    private AppCompatActivity context;
    private int requestCode;
    private int verificationCount = 1;
    private boolean captureImages = false;
    private String fingers;

    public FingerprintReader(AppCompatActivity context) {
        this.context = context;
    }

    /**
     * A method to start enrolling fingerprints
     *
     * @throws Exception an exception with a message of what went wrong
     */
    public void enroll() throws Exception {
        if (context == null) {
            throw new NullPointerException("Context cannot be null");
        } else if (requestCode <= 0) {
            throw new IllegalArgumentException("Request code should be a positive number less than five digits long");
        } else {
            Intent intent = new Intent(context, FPReaderActivity.class);
            intent.putExtra(Constants.VERIFICATION_COUNT_KEY, verificationCount);
            intent.putExtra(Constants.CAPTURE_IMAGES_KEY, captureImages);
            intent.putExtra(Constants.ENROL_FINGER_KEY, true);
            intent.putExtra(Constants.FINGERS_KEY, fingers);
            intent.putExtra("is_external", true);

            context.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * A method to start enrolling fingerprints
     *
     * @throws Exception an exception with a message of what went wrong
     */
    public void verify() throws Exception {
        if (context == null) {
            throw new NullPointerException("Context cannot be null");
        } else if (requestCode <= 0) {
            throw new IllegalArgumentException("Request code should be a positive number less than five digits long");
        } else {
            Intent intent = new Intent(context, FPReaderActivity.class);
            intent.putExtra(Constants.VERIFICATION_COUNT_KEY, verificationCount);
            intent.putExtra(Constants.CAPTURE_IMAGES_KEY, captureImages);
            intent.putExtra(Constants.ENROL_FINGER_KEY, false);
            intent.putExtra(Constants.FINGERS_KEY, fingers);
            intent.putExtra("is_external", true);

            context.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * A method to set the request code for the results of the activity
     *
     * @param rc the request code
     * @return the current instance of this class
     */
    public FingerprintReader setRequestCode(int rc) {
        this.requestCode = rc;
        return this;
    }

    /**
     * A method to set the number of times a single finger is to be scanned
     *
     * @param rc the number of times to verify
     * @return the current instance of this class
     */
    public FingerprintReader setVerificationCount(int rc) {
        this.verificationCount = rc;
        return this;
    }

    /**
     * A method to set the flag to allow images to be saved when enrolling fingerprints
     *
     * @param rc the flag state
     * @return the current instance of this class
     */
    public FingerprintReader recordImages(boolean rc) {
        this.captureImages = rc;
        return this;
    }

    /**
     * A method to set the fingers to be scanned
     *
     * @param fingersList the fingers to be scanned
     * @return the current instance of this class
     */
    public FingerprintReader setFingers(String... fingersList) {
        for (String finger : fingersList) {
            fingers = TextUtils.isEmpty(fingers) ? (fingers + finger) : (fingers + "-" + finger);
        }
        return this;
    }
}
