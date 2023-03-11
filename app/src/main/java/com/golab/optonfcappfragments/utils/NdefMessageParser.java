package com.golab.optonfcappfragments.utils;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import java.util.ArrayList;
import java.util.List;

public class NdefMessageParser {

    private NdefMessageParser() {}

    public static List<ParsedNefRecord> parse(NdefMessage message) {
        return getRecords(message.getRecords());

    }

    public static List<ParsedNefRecord> getRecords(NdefRecord[] records) {
        List<ParsedNefRecord> elements = new ArrayList<ParsedNefRecord>();

        // Here is where the payload is parsed and interpreted:
        for (final NdefRecord record : records) {
            if (ChipRecord.isOCF(record)) {
                elements.add(ChipRecord.parse(record));
            } else {
                elements.add(new ParsedNefRecord() {
                    public String str() {
                        return new String(record.getPayload());
                    }
                });
            }
        }

        return elements;
    }
}