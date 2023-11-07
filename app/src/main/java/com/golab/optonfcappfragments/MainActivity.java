package com.golab.optonfcappfragments;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.golab.optonfcappfragments.utils.NfcBroadcasts;
import com.golab.optonfcappfragments.utils.TagDiscovery;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.golab.optonfcappfragments.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.TagHelper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements TagDiscovery.onTagDiscoveryCompletedListener {

    public static final int CCFILE_SIZE = 8;
    private ActivityMainBinding binding;

    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private PendingIntent mPendingIntent;

    private NfcAdapter mNfcAdapter;
    private boolean mIsWriteMode;

    private Tag mTag;
    private NFCTag mNFCTag;

    private byte[] mPyld;
    private byte[] mUid;

    private String message;

    private final byte WRITE_BYTE = 0;

    private String mUidString = null;
    private String mIntensityString = null;
    private String mFrequencyString = null;
    private String mDutyCycleString = null;

    /* Intended to be parameters for read/write success/failure feedback, but effectively replaced
     * by STException.
    */
    enum Action {
        READ_ACTION
    };

    enum ActionStatus {
        UNKNOWN_FAILURE,
        READ_FAILED,
        READ_SUCCEEDED;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        mIsWriteMode = false;

        initNfcAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);

        enableForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();

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

            // Initialize the global variable with the read tag.
            mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (mTag != null) {
                new TagDiscovery(this).discoverTag(mTag);
            }
        }
    }

    /**
     * Function designed to overwrite the functionality of pressing the back button to exit
     * write mode.
     *
     */
    @Override
    public void onBackPressed() {
        if (mIsWriteMode) {
            sendBroadcast(new Intent(NfcBroadcasts.NFC_STOP_WRITE));
        }
    }

    private void initNfcAdapter() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Check if the NFC adapter is available
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
        }
    }

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


    public void toastParamIssue(String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
    }

    /* - - - - - - - READ - - - - - - */

    /**
     * Function to manage the discovery, reading, and displaying of a new NFC tag.
     *
     * IMPORTANT: This function will fail if called from the UI Thread.
     *
     * @param tag - A newly discovered NFC tag
     */
    public void handleReadIntent(NFCTag tag) {
        Callable<Void> read = (() -> {
            try {
                int adr = Integer.parseInt(((TextView) findViewById(R.id.readAddress)).getText().toString());
                // Bytes 0-7 Hold the CC file data
                mPyld = tag.readBytes(adr + CCFILE_SIZE, 4);
                mUid = tag.getUid();
//                    result = ActionStatus.READ_SUCCEEDED;
            } catch (STException e) {
                e.printStackTrace();
//                    result = ActionStatus.READ_FAILED;
            }
            return null;
        });

        ListenableFuture<Void> future = executor.submit(read);

        future.addListener(() -> {
            displayMsgs();
            sendBroadcast(new Intent(NfcBroadcasts.NFC_TAG_READ));
        }, getApplicationContext().getMainExecutor());

        try {
            future.get();
        } catch (InterruptedException | ExecutionException ex) {
            //pass
        }

    }

    /**
     * Functions to transfer the NFC parameters from the payload to the app for display.
     *
     * NOTE: If adding more parameters this is the function to modify.
     */
    private void displayMsgs() {
        if (mPyld != null) {
            if (mPyld[0] == WRITE_BYTE) {
                this.mIntensityString = "" + mPyld[0];

                this.mFrequencyString = "" + mPyld[2];

                this.mDutyCycleString = "" + mPyld[3];

                this.mUidString = "";

                // Uid of the NFC Chip
                for (byte b : mUid) {
                    this.mUidString += b;
                    this.mUidString += " ";
                }
            }
        } else {
            Toast.makeText(this, "Invalid IDF", Toast.LENGTH_LONG).show();
        }
    }

    public String getUidString() {
        return this.mUidString;
    }

    public String getIntensityString() { return this.mIntensityString; }
    public String getFrequencyString() { return this.mFrequencyString; }
    public String getDutyCycleString() { return this.mDutyCycleString; }

    /* - - - - - - - WRITE - - - - - - */

    /**
     * Function to manage the packaging and writing of app data to the NFC chip.
     *
     * @param tag - The ST NFCTag to be written to.
     */
    public void handleWriteIntent(NFCTag tag) {
        try {
            if (tag != null) {
                writeTag(tag, formatMessage());
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error in tag discovery!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Function to write toa ST NFCTag at the specified user address.
     *
     * IMPORTANT: This function will fail if called by the UI Thread.
     *
     * @param tag - The ST NFCTag to be written to.
     * @param pyld - The data to be written to the tag.
     */
    public void writeTag(NFCTag tag, byte[] pyld) {
        if (tag != null) {
            Callable<Void> write = (() -> {
                try {
                    if (tag.getCCWriteAccess() == 0) {
                        try {
                            // Blocks 0-7 are occupied by the CCFile.
                            tag.writeBytes((Integer.parseInt(((TextView) findViewById(R.id.writeAddress)).getText().toString()) + CCFILE_SIZE), pyld);
//                    result = ActionStatus.READ_SUCCEEDED;
                        } catch (STException e) {
                            e.printStackTrace();
//                    result = ActionStatus.READ_FAILED;
                        }
                    } else {
                        Toast.makeText(this, "Write mode not enabled!", Toast.LENGTH_LONG).show();
                    }
                } catch (STException e) {
                    e.printStackTrace();
                    if (e.equals(STException.STExceptionCode.TAG_NOT_IN_THE_FIELD)) {
                        Toast.makeText(this, "Tag not in range!", Toast.LENGTH_LONG).show();
                    }
                }
                return null;
            });

            ListenableFuture<Void> future = executor.submit(write);

            future.addListener(() -> {
                /* Sends intent broadcast to inform the DashboardFragment that the write is complete
                 * exit write mode.
                 */
                sendBroadcast(new Intent(NfcBroadcasts.NFC_STOP_WRITE));
            }, executor);

            try {
                future.get();
            } catch (InterruptedException | ExecutionException ex) {
                //pass
            }

        }

    }

    /**
     * Takes the data inputted on the DashboardFragment and packages it into a byte array to be
     * sent to an NFC tag.
     *
     * @return out - A byte[] containing the payload to be written to the NFC tag.
     * @throws UnsupportedEncodingException
     */
    private byte[] formatMessage() throws UnsupportedEncodingException {

        byte[] out = new byte[8];

        // byte[0] = 0 to ensure that the IF
        out[0] = WRITE_BYTE;
        out[1] = Byte.parseByte(this.mIntensityString);
        out[2] = Byte.parseByte(this.mFrequencyString);
        out[3] = Byte.parseByte(this.mDutyCycleString);

        return out;
    }

    /**
     * Overwritten function that executes onSuccess of TagDiscovery.discoverTag(). Chooses if the
     * tag should be written to or read from.
     *
     * @param nfcTag - The ST NFCTag to be written to.
     * @param productId - The ST tag type being written to.
     * @param error - Error thrown during execution or null.
     */
    @Override
    public void onTagDiscoveryCompleted(NFCTag nfcTag, TagHelper.ProductID productId, STException error) {

        if (error != null) {
            // Error with toasts since this function is not called from the UI Thread.
//            Toast.makeText(getApplication(), "Error while reading the tag: " + error.toString(), Toast.LENGTH_LONG).show();
            return;
        }

        if (nfcTag != null) {
            mNFCTag = nfcTag;

            if (mIsWriteMode) {
                handleWriteIntent(mNFCTag);
            } else {
                handleReadIntent(mNFCTag);
            }

        } else {
//            Toast.makeText(this, "Tag discovery failed!", Toast.LENGTH_LONG).show();
        }
    }

    public void setWriteMode(boolean b) {
        this.mIsWriteMode = b;
    }

    public void setMessage(String s) {
        this.message = s;
    }

    public void setParameters(String in, String fr, String dc) {
        this.mIntensityString = in;
        this.mFrequencyString = fr;
        this.mDutyCycleString = dc;
    }
}