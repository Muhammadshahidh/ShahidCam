package com.shahid.cam.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.shahid.cam.Constants;
import com.shahid.cam.ui.HomeFragment;

public class TorchStateReceiver extends BroadcastReceiver {
    private HomeFragment homeFragment;

    public TorchStateReceiver(HomeFragment homeFragment) {
        this.homeFragment = homeFragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Constants.BROADCAST_ON_TORCH_STATE_CHANGED.equals(intent.getAction())) {
            boolean isTorchOn = intent.getBooleanExtra(Constants.INTENT_EXTRA_TORCH_STATE, false);
            
            // Update UI in HomeFragment
            if (homeFragment != null) {
                homeFragment.updateTorchUI(isTorchOn);
            }
        }
    }
}
