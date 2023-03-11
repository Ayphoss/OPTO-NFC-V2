package com.golab.optonfcappfragments.ui.home;

import static com.golab.optonfcappfragments.utils.Utils.toHex;

import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.golab.optonfcappfragments.databinding.FragmentHomeBinding;
import com.golab.optonfcappfragments.utils.NdefMessageParser;
import com.golab.optonfcappfragments.utils.ParsedNefRecord;

import java.util.List;

public class HomeFragment extends Fragment {

    private Tag mTag;

    private TextView mText;

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mText = binding.textHome;
//        homeViewModel.getText().observe(getViewLifecycleOwner(), mText::setText);
        return root;
    }

    public void handleNfcTag(Intent intent) {

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

        mText.setText(sb.toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}