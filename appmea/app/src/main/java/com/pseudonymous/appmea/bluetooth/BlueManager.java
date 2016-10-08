package com.pseudonymous.appmea.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;

import com.pseudonymous.appmea.MainActivity;

/**
 * Created by David Smerkous on 9/30/16.
 *
 */

public class BlueManager {
    public static boolean has_bluetooth = false;
    public static BluetoothAdapter b_adapter = null;
    private final static int REQ_BT = 1;


    public void initAdapter() {
        b_adapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean init() {
        initAdapter();
        if(!hasBluetooth()) return false;

        if (!b_adapter.isEnabled()) {
            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, REQ_BT);
        }
        return true;
    }

    public static boolean hasBluetooth() {
        has_bluetooth = (b_adapter != null);
        return has_bluetooth;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQ_BT){
            Log.d("BLUETOOTH_MODULE", "GOT BLUETOOTH " + resultCode);
        }
    }
}
