package com.golab.optonfcappfragments.ui.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.golab.optonfcappfragments.MainActivity;
import com.golab.optonfcappfragments.R;
import com.golab.optonfcappfragments.databinding.FragmentDashboardBinding;
import com.golab.optonfcappfragments.utils.NfcBroadcasts;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    private TextView mWriteText;
    private TextView mCountView;
    private Button mWriteButton;

    private int mCounter;

    private CountDownTimer mCDTimer;

    private View mRoot;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);

        mRoot = binding.getRoot();

        initButtons();
        initTextViews();

        mWriteButton = (Button) mRoot.findViewById(R.id.writeButton);

        mWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                mCounter = 30;
                ((MainActivity) getActivity()).setWriteMode(true);
                ((MainActivity) getActivity()).setMessage(mWriteText.getText().toString());
                mCountView.setText(String.valueOf(mCounter));
                mCountView.setVisibility(View.VISIBLE);
                mCDTimer = new CountDownTimer(30000, 1000){
                    public void onTick(long millisUntilFinished){
                        mCountView.setText(String.valueOf(mCounter));
                        mCounter--;
                        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    }
                    public void onFinish(){
                        ((MainActivity) getActivity()).setWriteMode(false);
                        mCountView.setVisibility(View.GONE);
                    }
                }.start();
            }
        });

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (NfcBroadcasts.NFC_STOP_WRITE.equals(intent.getAction())) {
                    stopWrite();
                }
            }
        };
        IntentFilter filter = new IntentFilter(NfcBroadcasts.NFC_STOP_WRITE);
        requireActivity().registerReceiver(mReceiver, filter);

        return mRoot;
    }


    private void initTextViews() {
        mWriteText = (TextView) mRoot.findViewById(R.id.writeText);
        mCountView = (TextView) mRoot.findViewById(R.id.countView);
        mCountView.setVisibility(View.GONE);
    }

    private void initButtons() {
        mWriteButton = (Button) mRoot.findViewById(R.id.writeButton);
    }

    public void stopWrite() {
        mCDTimer.cancel();
        ((MainActivity) getActivity()).setWriteMode(false);
        mCountView.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}