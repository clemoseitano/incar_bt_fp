/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fgtit.reader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This Activity appears as a dialog. It lists any paired devices and devices
 * detected in the area after discovery. When a device is chosen by the user,
 * the MAC address of the device is sent back to the parent Activity in the
 * result Intent.
 */
public class DeviceListActivity extends AppCompatActivity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private BluetoothListAdapter mNewDevicesArrayAdapter;
    private BluetoothListAdapter mPairedDevicesArrayAdapter;

    Set<BluetoothDevice> pairedDevices;
    Set<BluetoothDevice> newDevices;
    ListView newDevicesListView;
    ListView pairedListView;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    Button okButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.device_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
//        Button scanButton = (Button) findViewById(R.id.button_scan);
//        scanButton.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                doDiscovery();
//                v.setVisibility(View.GONE);
//            }
//        });

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = preferences.edit();

        // Get a set of currently paired devices
        pairedDevices = mBtAdapter.getBondedDevices();
        newDevices = new HashSet<>();

        // Find and set up the ListView for paired devices
        pairedListView = findViewById(R.id.paired_devices);
        newDevicesListView =  findViewById(R.id.new_devices);
        okButton = findViewById(R.id.ok);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // If there are paired devices, add each one to the ArrayAdapter
        pairedDevices = getRelevantDevices(pairedDevices);
        if (pairedDevices.size() > 0) {
            mPairedDevicesArrayAdapter = new BluetoothListAdapter(this, pairedDevices);
            pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        } else {
            Toast.makeText(this, R.string.no_bt_devices_found, Toast.LENGTH_LONG).show();
        }
        doDiscovery();

        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClicked();
            }
        });
    }

    Set<BluetoothDevice> getRelevantDevices(Set<BluetoothDevice> pairedDvs){
        Set<BluetoothDevice> devices = new HashSet<>();
        for (BluetoothDevice dev : pairedDvs) {
            String nm = dev.getName();
            if (nm.startsWith("HF") || nm.startsWith("hf") || nm.startsWith("Hf") || nm.startsWith("hF")){
                devices.add(dev);
            }
        }

        return devices;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    public void onItemClicked() {
        // Cancel discovery because it's costly and we're about to connect
        mBtAdapter.cancelDiscovery();
        BluetoothListAdapter listAdapter = (BluetoothListAdapter)newDevicesListView.getAdapter();
        BluetoothDevice bluetoothDevice = null;
        if (listAdapter != null){
            bluetoothDevice = listAdapter.getSelectedBluetoothDevice();
        }

        if (bluetoothDevice == null){
            bluetoothDevice = ((BluetoothListAdapter)pairedListView.getAdapter()).getSelectedBluetoothDevice();
        }

        Intent intent = new Intent();
        if (bluetoothDevice != null) {
            editor.putString("selected_bt_device", bluetoothDevice.getAddress());
            editor.apply();
            // Create the result Intent and include the MAC address
            intent.putExtra(EXTRA_DEVICE_ADDRESS, bluetoothDevice.getAddress());
            setResult(Activity.RESULT_OK, intent);
        }else {
            Toast.makeText(this, R.string.unable_to_connect, Toast.LENGTH_LONG).show();
            setResult(Activity.RESULT_CANCELED, intent);
        }
        finish();
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    newDevices.add(device);
                    mNewDevicesArrayAdapter = new BluetoothListAdapter(DeviceListActivity.this, newDevices);
                    // Find and set up the ListView for newly discovered devices
                    newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter == null || mNewDevicesArrayAdapter.getCount() == 0) {
                    Toast.makeText(context, R.string.no_bt_devices_found, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

}
