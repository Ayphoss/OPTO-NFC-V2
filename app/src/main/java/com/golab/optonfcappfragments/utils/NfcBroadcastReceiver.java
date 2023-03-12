package com.golab.optonfcappfragments.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NfcBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context,"Broadcast Receiver Triggered",Toast.LENGTH_SHORT).show();
    }
}
