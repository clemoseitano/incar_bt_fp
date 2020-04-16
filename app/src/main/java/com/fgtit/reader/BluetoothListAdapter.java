package com.fgtit.reader;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.Set;

public class BluetoothListAdapter extends BaseAdapter {
    Context context;
    LayoutInflater inflater;
    Object[] pairedDevices;
    BluetoothDevice selectedDevice;
    CheckBox lastCheck;

    public BluetoothListAdapter(Context context, Set<BluetoothDevice> pairedDevices){
        this.context = context;
        this.pairedDevices = pairedDevices.toArray();
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {
        return pairedDevices.length;
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Override
    public Object getItem(int position) {
        return pairedDevices[position];
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the item whose view
     *                    we want.
     * @param view The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible to convert
     *                    this view to display the correct data, this method can create a new view.
     *                    Heterogeneous lists can specify their number of view types, so that this View is
     *                    always of the right type (see {@link #getViewTypeCount()} and
     *                    {@link #getItemViewType(int)}).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        view = null;
        if(view == null){
            view = inflater.inflate(R.layout.device_list_item, null);
        }
        final BluetoothDevice bluetoothDevice = (BluetoothDevice) getItem(position);

        final CheckBox checkBox = view.findViewById(R.id.device_name);

        String deviceName = bluetoothDevice.getName();
        String address = bluetoothDevice.getAddress();

        if (deviceName != null && !deviceName.isEmpty()){
            checkBox.setText(deviceName);
        }else {
            checkBox.setText(address);
        }

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                update(checkBox);
                if (isChecked) {
                    selectedDevice = bluetoothDevice;
                }else {
                    selectedDevice = null;
                }
            }
        });

        return view;
    }

    public BluetoothDevice getSelectedBluetoothDevice(){
        return selectedDevice;
    }

    void update(CheckBox cb){
        if (lastCheck == null){
            lastCheck = cb;
            return;
        }

        lastCheck.setChecked(false);
        lastCheck = cb;
    }
}
