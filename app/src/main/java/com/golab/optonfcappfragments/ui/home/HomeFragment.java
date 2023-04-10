package com.golab.optonfcappfragments.ui.home;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.golab.optonfcappfragments.MainActivity;
import com.golab.optonfcappfragments.R;
import com.golab.optonfcappfragments.databinding.FragmentHomeBinding;
import com.golab.optonfcappfragments.utils.NdefMessageParser;
import com.golab.optonfcappfragments.utils.NfcBroadcasts;
import com.golab.optonfcappfragments.utils.ParsedNefRecord;

import java.util.List;

public class HomeFragment extends Fragment {

    private TextView mText;
    private TextView mIntensity;
    private TextView mFrequency;
    private TextView mDutyCycle;

    private View mParameterLayout;

    private FragmentHomeBinding binding;


    private View mRoot;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        mRoot = binding.getRoot();

        initTextViews();

        initFragment();


        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (NfcBroadcasts.NFC_TAG_READ.equals(intent.getAction())) {
                    initFragment();
                }
            }
        };

        IntentFilter filter = new IntentFilter(NfcBroadcasts.NFC_TAG_READ);

        requireActivity().registerReceiver(mReceiver, filter);


//        homeViewModel.getText().observe(getViewLifecycleOwner(), mText::setText);
        return mRoot;
    }

    private void initFragment() {

        MainActivity mainActivity = (MainActivity) getActivity();

        String text = mainActivity.getReadString();
        String intensity = mainActivity.getIntensityString();
        String frequency = mainActivity.getFrequencyString();
        String dutyCycle = mainActivity.getDutyCycleString();


        if (text != null) {
            mText.setText(text);
            mFrequency.setText(frequency);
            mIntensity.setText(intensity);
            mDutyCycle.setText(dutyCycle);
            mParameterLayout.setVisibility(View.VISIBLE);
        } else {
            mText.setText("Tap NFC Tag");
        }

    }

    private void initTextViews() {
        mText = mRoot.findViewById(R.id.readText);
        mFrequency = mRoot.findViewById(R.id.frequencyText);
        mIntensity = mRoot.findViewById(R.id.intensityText);
        mDutyCycle = mRoot.findViewById(R.id.dutyCycleText);

        mParameterLayout = mRoot.findViewById(R.id.parametersConstraint);
        mParameterLayout.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}