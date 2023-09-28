package com.golab.optonfcappfragments;

import static com.golab.optonfcappfragments.utils.Utils.toHex;

import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.golab.optonfcappfragments.utils.NdefMessageParser;
import com.golab.optonfcappfragments.utils.NfcBroadcastReceiver;
import com.golab.optonfcappfragments.utils.NfcBroadcasts;
import com.golab.optonfcappfragments.utils.ParsedNefRecord;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.golab.optonfcappfragments.databinding.ActivityMainBinding;
import com.st.st25android.AndroidReaderInterface;
import com.st.st25sdk.Helper;
import com.st.st25sdk.STException;
import com.st.st25sdk.ndef.MimeRecord;
import com.st.st25sdk.ndef.NDEFMsg;
import com.st.st25sdk.type5.Type5Tag;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private PendingIntent mPendingIntent;

    private NfcAdapter mNfcAdapter;
    private boolean mIsWriteMode;

    private NfcBroadcastReceiver mBroadcastReceiver;

    private Tag mTag;
    private ST25DVTag mST25DVTag;
    private Type5Tag mType5Tag;

    private byte[] mPyld;

    private Fragment mDashboardFragment;
    private Fragment mHomeFragment;

    private String message;

    private final byte WRITE_BYTE = 0;

    // TODO: Replace with proper format
    private String mReadString = null;
    private String mIntensityString = null;
    private String mFrequencyString = null;
    private String mDutyCycleString = null;

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

        mHomeFragment = getFragmentManager().findFragmentByTag("HomeFragment");
        mDashboardFragment = getFragmentManager().findFragmentByTag("DashboardFragment");


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

        mIsWriteMode = false;

        mBroadcastReceiver = new NfcBroadcastReceiver();

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

            // Get the tag
            mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (mIsWriteMode) {
                handleWriteIntent(intent);
            } else {
                handleReadIntent(intent);
                sendBroadcast(new Intent(NfcBroadcasts.NFC_TAG_READ));
//                sendBroadcast(new Intent().addCategory(Intent.CATEGORY_DEFAULT).setAction(NfcBroadcasts.NFC_TAG_READ));
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsWriteMode) {
            sendBroadcast(new Intent(NfcBroadcasts.NFC_STOP_WRITE));
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

    /* - - - - - - - READ - - - - - - */

    public void handleReadIntent(Intent intent) {

        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        // Set the tag we're currently interacting with
        mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        AndroidReaderInterface readerInterface = AndroidReaderInterface.newInstance(mTag);

        byte[] uid = Helper.reverseByteArray(mTag.getId());

        // Converts a Android tag to at ST25DVTag
        if (mTag != null) {
            mType5Tag = new Type5Tag(readerInterface, uid);
        } else {
            Toast.makeText(this, "Error Reading NFC Tag", Toast.LENGTH_LONG).show();
            return;
        }

        NdefMessage[] msgs;

        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];

            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
            }

        } else {


            new AsyncTaskHandler(Action.READ_ACTION, readerInterface, uid).execute();

        }

        displayMsgs();
    }


    private void displayMsgs() {

        // Meant to spin to wait for AsyncTask to finish
//        while (mPyld == null) { }

        if (mPyld != null) {
            if (mPyld[0] == 0) {
                String intensity = "" + mPyld[1];
                this.mIntensityString = intensity;

                String freq = "" + mPyld[2];
                this.mFrequencyString = freq;

                String duty = "" + mPyld[3];
                this.mDutyCycleString = duty;

                this.mReadString = intensity + freq + duty;
            }
        }
    }

    public String getReadString() {
        return this.mReadString;
    }

    public String getIntensityString() { return this.mIntensityString; }
    public String getFrequencyString() { return this.mFrequencyString; }
    public String getDutyCycleString() { return this.mDutyCycleString; }

    /* - - - - - - - WRITE - - - - - - */

    public void handleWriteIntent(Intent intent) {
        mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        AndroidReaderInterface readerInterface = AndroidReaderInterface.newInstance(mTag);
        byte[] uid = Helper.reverseByteArray(mTag.getId());


        try {
            if (mType5Tag != null) {
                writeTag(mType5Tag, packageSTMessage());
            }
            writeTag(mTag, packageMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
    }

    private NDEFMsg packageSTMessage() throws UnsupportedEncodingException {

        NDEFMsg ndefMsg = new NDEFMsg();

        byte[] pyld = formatMessage();
        ndefMsg.addRecord(createSTRecord(pyld));

        return ndefMsg;
    }

    private NdefMessage packageMessage() throws UnsupportedEncodingException {

        // TODO: Adjust for full payload package
        // Casting to string since TextView.getText() returns CharSequence

        // TODO: Adjust for each field
        byte[] pyld = formatMessage();

        NdefRecord[] records = { createRecord(pyld) };
        return new NdefMessage(records);
    }

    private byte[] formatMessage() throws UnsupportedEncodingException {

        byte[] out = new byte[4];

        out[0] = WRITE_BYTE;
        out[1] = Byte.parseByte(this.mIntensityString);
        out[2] = Byte.parseByte(this.mFrequencyString);
        out[3] = Byte.parseByte(this.mDutyCycleString);

        return out;
    }

    private NdefRecord createRecord(byte[] pyld) throws UnsupportedEncodingException {
//        String  lang        =   "en";
//        byte[]  pyldBytes   =   pyld;
//        byte[]  langBytes   =   lang.getBytes();
//        int     langLength  =   langBytes.length;
//        int     textLength  =   pyldBytes.length;
//        byte[]  payload     =   new byte[1 + langLength + textLength];
//
//        payload[0] = (byte) langLength;
//
//        System.arraycopy(langBytes, 0, payload, 1, langLength);
//        System.arraycopy(pyldBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord record = NdefRecord.createMime("text/plain", pyld);

        return record;
    }

    private MimeRecord createSTRecord(byte[] pyld) {

        MimeRecord record = new MimeRecord();
        record.setContent(pyld);

        return record;
    }

    // TODO: Update to format CC. Update to write ST25DV NDEF message.

    public void writeTag(Type5Tag tag, NDEFMsg message) {
        if (tag != null) {
            try {
                if (!tag.isCCFileValid()) {
                    tag.initEmptyCCFile();
                }
                tag.writeNdefMessage(message);
            } catch (STException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     *
     * Write to the NFC tag in proximity, format the tag if it is new.
     *
     * @param tag an NDEF compatible tag, NdefMessage - A NDEF formatted buffer
     *
     * @return null
     *
     */
    public void writeTag(Tag tag, NdefMessage message) throws FormatException {
        if (tag != null) {
            try {
                Ndef ndefTag = Ndef.get(tag);
                if (ndefTag == null) {
                    NdefFormatable nForm = NdefFormatable.get(tag);
                    if (nForm != null) {
                        nForm.connect();
                        nForm.format(message);
                        nForm.close();
                    }
                } else {
                    ndefTag.connect();
                    ndefTag.writeNdefMessage(message);
                    ndefTag.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    /* Async task is created to handle read/write requests to untangle them from the UI thread
     * to ensure that the read/write doesn't cause the UI to freeze
     */
    private class AsyncTaskHandler extends AsyncTask<Void, Void, ActionStatus> {
        Action mAction;
        byte[] mUid;
        AndroidReaderInterface mReaderInterface;


        public AsyncTaskHandler(Action action, AndroidReaderInterface readerInterface, byte[] uid) {
            mAction = action;
            mUid = uid;
            mReaderInterface = readerInterface;
        }

        @Override
        protected ActionStatus doInBackground(Void... voids) {
            ActionStatus result = ActionStatus.UNKNOWN_FAILURE;;

            switch (mAction) {
                case READ_ACTION:
                    try {
                        //Read 1 block (8 bytes) since it holds the whole data struct
                        mPyld = mType5Tag.readMultipleBlock(0, 1);
                        result = ActionStatus.READ_SUCCEEDED;
                    } catch (STException e) {
                        e.printStackTrace();
                        result = ActionStatus.READ_FAILED;
                    }
                    break;
            }


            return result;
        }

        protected void onPostExecute(Long result) {

        }


    }
}