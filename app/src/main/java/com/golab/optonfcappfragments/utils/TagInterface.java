//package com.golab.optonfcappfragments.utils;
//
//import android.nfc.Tag;
//import android.nfc.tech.Ndef;
//
//import com.st.st25sdk.NFCTag;
//
//public class TagInterface {
//
//    public TagInterface(Tag tag) {
//        NFCTag.NfcTagTypes decodeTagType = decodeTagType(tag);
//        switch (AnonymousClass1.$SwitchMap$com$st$st25sdk$NFCTag$NfcTagTypes[decodeTagType.ordinal()]) {
//            case 1:
//                AndroidType5ReaderInterface androidType5ReaderInterface = new AndroidType5ReaderInterface(tag);
//                androidType5ReaderInterface.mTagType = decodeTagType;
//                androidType5ReaderInterface.mNdefInterface = AndroidNdefInterface.newInstance(androidType5ReaderInterface);
//                return androidType5ReaderInterface;
//            case 2:
//            case 3:
//                AndroidType4ReaderInterface androidType4ReaderInterface = new AndroidType4ReaderInterface(tag);
//                androidType4ReaderInterface.mTagType = decodeTagType;
//                androidType4ReaderInterface.mNdefInterface = AndroidNdefInterface.newInstance(androidType4ReaderInterface);
//                return androidType4ReaderInterface;
//            case 4:
//                AndroidType2ReaderInterface androidType2ReaderInterface = new AndroidType2ReaderInterface(tag);
//                androidType2ReaderInterface.mTagType = decodeTagType;
//                androidType2ReaderInterface.mNdefInterface = AndroidNdefInterface.newInstance(androidType2ReaderInterface);
//                return androidType2ReaderInterface;
//            case 5:
//                AndroidNFCAReaderInterface androidNFCAReaderInterface = new AndroidNFCAReaderInterface(tag);
//                androidNFCAReaderInterface.mTagType = decodeTagType;
//                androidNFCAReaderInterface.mNdefInterface = AndroidNdefInterface.newInstance(androidNFCAReaderInterface);
//                return androidNFCAReaderInterface;
//            case 6:
//                AndroidNFCBReaderInterface androidNFCBReaderInterface = new AndroidNFCBReaderInterface(tag);
//                androidNFCBReaderInterface.mTagType = decodeTagType;
//                androidNFCBReaderInterface.mNdefInterface = AndroidNdefInterface.newInstance(androidNFCBReaderInterface);
//                return androidNFCBReaderInterface;
//            default:
//                return null;
//        }
//    }
//
//    static /* synthetic */ class AnonymousClass1 {
//        static final /* synthetic */ int[] $SwitchMap$com$st$st25sdk$NFCTag$NfcTagTypes;
//
//        static {
//            int[] iArr = new int[NFCTag.NfcTagTypes.values().length];
//            $SwitchMap$com$st$st25sdk$NFCTag$NfcTagTypes = iArr;
//            try {
//                iArr[NFCTag.NfcTagTypes.NFC_TAG_TYPE_V.ordinal()] = 1;
//            } catch (NoSuchFieldError unused) {
//            }
//            try {
//                $SwitchMap$com$st$st25sdk$NFCTag$NfcTagTypes[NFCTag.NfcTagTypes.NFC_TAG_TYPE_4A.ordinal()] = 2;
//            } catch (NoSuchFieldError unused2) {
//            }
//            try {
//                $SwitchMap$com$st$st25sdk$NFCTag$NfcTagTypes[NFCTag.NfcTagTypes.NFC_TAG_TYPE_4B.ordinal()] = 3;
//            } catch (NoSuchFieldError unused3) {
//            }
//            try {
//                $SwitchMap$com$st$st25sdk$NFCTag$NfcTagTypes[NFCTag.NfcTagTypes.NFC_TAG_TYPE_2.ordinal()] = 4;
//            } catch (NoSuchFieldError unused4) {
//            }
//            try {
//                $SwitchMap$com$st$st25sdk$NFCTag$NfcTagTypes[NFCTag.NfcTagTypes.NFC_TAG_TYPE_A.ordinal()] = 5;
//            } catch (NoSuchFieldError unused5) {
//            }
//            try {
//                $SwitchMap$com$st$st25sdk$NFCTag$NfcTagTypes[NFCTag.NfcTagTypes.NFC_TAG_TYPE_B.ordinal()] = 6;
//            } catch (NoSuchFieldError unused6) {
//            }
//            try {
//                $SwitchMap$com$st$st25sdk$NFCTag$NfcTagTypes[NFCTag.NfcTagTypes.NFC_TAG_TYPE_F.ordinal()] = 7;
//            } catch (NoSuchFieldError unused7) {
//            }
//        }
//    }
//}
