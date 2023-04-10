package com.golab.optonfcappfragments.utils;

import android.nfc.NdefRecord;

import java.io.UnsupportedEncodingException;

public class ChipRecord implements ParsedNefRecord {

    private final byte[] mPayload;


    public ChipRecord(byte[] payload) {
        // TODO: Implement check for OCF.
        mPayload = payload;
    }

    @Override
    public byte[] payload() {
        return mPayload;
    }

    public static ChipRecord parse(NdefRecord record) {

        return new ChipRecord(record.getPayload());
    }

    public static boolean isIDF(NdefRecord record) {
        try {
            parse(record);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}