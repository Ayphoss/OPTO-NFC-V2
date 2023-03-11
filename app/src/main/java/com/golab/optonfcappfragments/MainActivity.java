package com.golab.optonfcappfragments;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.golab.optonfcappfragments.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.golab.optonfcappfragments.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private PendingIntent mPendingIntent;

    private NfcAdapter mNfcAdapter;
    private boolean mIsWriteMode;

    private HomeFragment mHomeFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHomeFragment = new HomeFragment();


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        initNfcAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO: Verify that this should be in onResume
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);

        enableForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // May be the wrong place if write stops working
        disableForegroundDispatch();
        if(mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            // TODO: Handle for when the current fragment is in write mode but not yet writing
            if (mIsWriteMode) {
                // TODO: Send info to WriteFragment
            } else {
                mHomeFragment.handleNfcTag(intent);
            }
        }
    }

    public void enableWriteMode() {
        this.mIsWriteMode = true;
    }

    public void disableWriteMode() {
        this.mIsWriteMode = false;
    }

    private void initNfcAdapter() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Check if the NFC adapter is available
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // TODO: Remove or replace for read and write intent
//    private void resolveIntent(Intent intent) {
//        String action = intent.getAction();
//
//        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
//                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
//                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
//
//            // Start the read activity to handle the rest
//            Intent readIntent = new Intent(MainActivity.this, ReadActivity.class);
//            readIntent.putExtra("nfcIntent", intent);
//            startActivity(readIntent);
//        }
//    }

    private void enableForegroundDispatch() {
        try {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        } catch (RuntimeException e) {
            Log.e("onResume Tag", "Error enabling NFC foreground dispatch", e);
        }
    }

    private void disableForegroundDispatch() {
        try {
            mNfcAdapter.disableForegroundDispatch(this);
        } catch (RuntimeException e) {
            Log.e("onPause Tag", "Error disabling NFC foreground dispatch", e);
        }
    }
}