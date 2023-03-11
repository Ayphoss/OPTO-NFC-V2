package com.golab.optonfcappfragments.utils;

import android.nfc.NdefRecord;

import java.io.UnsupportedEncodingException;

public class ChipRecord implements ParsedNefRecord {

    private final String mEncoding;

    private final String mText;

    public ChipRecord(String encoding, String text) {
        // TODO: Implement check for OCF.
        if (encoding == null) {
            throw new IllegalArgumentException();
        } else {
            mEncoding = encoding;
        }

        if (text == null) {
            throw new IllegalArgumentException();
        } else {
            mText = text;
        }
    }

    @Override
    public String str() {
        return mText;
    }

    public String getText() {
        return mText;
    }

    public String getEncoding() {
        return mEncoding;
    }

    public static ChipRecord parse(NdefRecord record) {

        try {
            byte[] payload = record.getPayload();
            /*
             * payload[0] contains the "Status Byte Encodings" field, per the
             * NFC Forum "Text Record Type Definition" section 3.2.1.
             *
             * bit7 is the Text Encoding Field.
             *
             * if (Bit_7 == 0): The text is encoded in UTF-8 if (Bit_7 == 1):
             * The text is encoded in UTF16
             *
             * Bit_6 is reserved for future use and must be set to zero.
             *
             * Bits 5 to 0 are the length of the IANA language code.
             */
            String encoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
            int languageCodeLength =payload[0] & 0077;

            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            String text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, encoding);

            return new ChipRecord(languageCode, text);
        } catch (UnsupportedEncodingException e) {
            // Shouldn't happen unless a bad tag is scanned
            throw new IllegalArgumentException(e);
        }
    }

    public static boolean isOCF(NdefRecord record) {
        try {
            parse(record);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}