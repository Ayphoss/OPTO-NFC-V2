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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private PendingIntent mPendingIntent;

    private NfcAdapter mNfcAdapter;
    private boolean mIsWriteMode;

    private NfcBroadcastReceiver mBroadcastReceiver;

    private Tag mTag;

    private Fragment mDashboardFragment;
    private Fragment mHomeFragment;

    // TODO: Replace with proper format
    private StringBuilder mReadString = null;

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

            // TODO: Handle for when the current fragment is in write mode but not yet writing
            if (mIsWriteMode) {
//                mDashboardFragment.handleNfcTag(intent);
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


        NdefMessage[] msgs;

        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];

            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
            }

        } else {

            byte[] empty = new byte[0];
            byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            byte[] payload = dumpTagData(tag).getBytes();
            // TODO: DO we really want type unknown here?
            NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);

            // TODO: find what the heck the curly brackets do here...
            NdefMessage msg = new NdefMessage(new NdefRecord[] {record});
            msgs = new NdefMessage[] {msg};
        }

        displayMsgs(msgs);
    }

    private String dumpTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        sb.append("ID (hex): \n");
        sb.append(toHex(id)).append('\n');

        String prefix = "android.nfc.tech.";
        sb.append("Technologies: ");
        for (String tech : tag.getTechList()) {
            sb.append(tech.substring(prefix.length()));
            sb.append(", ");
        }

        sb.delete(sb.length() - 2, sb.length());

        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareClassic.class)) {
                sb.append("\n");
                String type = "Unkown";

                try {
                    MifareClassic mifareTag = MifareClassic.get(tag);

                    switch(mifareTag.getType()) {
                        case MifareClassic.TYPE_CLASSIC:
                            type = "Classic";
                            break;
                        case MifareClassic.TYPE_PLUS:
                            type = "Plus";
                        case MifareClassic.TYPE_PRO:
                            type = "Pro";
                            break;
                    }

                    // TODO: Check if this is nessisary for the final version of the app.
                    sb.append("Mifare Classic Type: ");
                    sb.append(type);
                    sb.append("\n");

                    sb.append("Mifare size: ");
                    sb.append(mifareTag.getSize());
                    sb.append("\n");

                    sb.append("Mifare sectors: ");
                    sb.append(mifareTag.getSectorCount());
                    sb.append('\n');

                    sb.append("Mifare blocks: ");
                    sb.append(mifareTag.getBlockCount());
                } catch (Exception e) {
                    sb.append("Mifare classic error: " + e.getMessage());
                }
            }

            if (tech.equals(MifareUltralight.class.getName())) {
                sb.append('\n');
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                String type = "Unknown";
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "UltraLight C";
                        break;
                }

                sb.append("Mifare Ultralight Type: ");
                sb.append(type);
            }


            if (tech.equals(IsoDep.class.getName())) {
                IsoDep isoDepTag = IsoDep.get(tag);
            }

            if (tech.equals(Ndef.class.getName())) {
                Ndef.get(tag);
            }

            if (tech.equals(NdefFormatable.class.getName())) {
                NdefFormatable ndefFormatableTag = NdefFormatable.get(tag);
            }

        }

        return sb.toString();
    }


    private void displayMsgs(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) { return; }

        // Meant to build a string from the read NdefMessage
        StringBuilder sb = new StringBuilder();
        List<ParsedNefRecord> records = NdefMessageParser.parse(msgs[0]);
        final int size = records.size();

        for (int i = 0; i < size; i++) {
            ParsedNefRecord record = records.get(i);
            String str = record.str();
            sb.append(str).append("\n");
        }

        // TODO: Store tag data correctly
        mReadString = sb;
    }

    public StringBuilder getReadString() {
        return this.mReadString;
    }

    /* - - - - - - - WRITE - - - - - - */

    public void handleWriteIntent(Intent intent) {
        mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        try {
            writeTag(mTag, packageMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
    }

    private NdefMessage packageMessage() throws UnsupportedEncodingException {

        // TODO: Fix info pass for write
        // Casting to string since TextView.getText() returns CharSequence
        NdefRecord[] records = { createRecord((String) "STUB") };
        return new NdefMessage(records);
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String  lang        =   "en";
        byte[]  textBytes   =   text.getBytes();
        byte[]  langBytes   =   lang.getBytes();
        int     langLength  =   langBytes.length;
        int     textLength  =   textBytes.length;
        byte[]  payload     =   new byte[1 + langLength + textLength];

        payload[0] = (byte) langLength;

        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

        return record;
    }

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
}