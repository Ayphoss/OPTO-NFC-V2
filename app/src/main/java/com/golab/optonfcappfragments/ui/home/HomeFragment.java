package com.golab.optonfcappfragments.ui.home;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.golab.optonfcappfragments.MainActivity;
import com.golab.optonfcappfragments.R;
import com.golab.optonfcappfragments.databinding.FragmentHomeBinding;
import com.golab.optonfcappfragments.utils.NfcBroadcasts;

public class HomeFragment extends Fragment {

    private MainActivity mMainActivity;
    private TextView mUid;
    private TextView mUidHeader;
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

        mMainActivity = (MainActivity) getActivity();

        initFragment();


        IntentFilter filter = new IntentFilter(NfcBroadcasts.NFC_TAG_READ);

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (NfcBroadcasts.NFC_TAG_READ.equals(intent.getAction())) {
                    initFragment();
                }
            }
        };

        requireActivity().registerReceiver(mReceiver, filter);


//        homeViewModel.getText().observe(getViewLifecycleOwner(), mText::setText);

        return mRoot;
    }

    private void initFragment() {

        String text = mMainActivity.getUidString();
        String intensity = mMainActivity.getIntensityString();
        String frequency = mMainActivity.getFrequencyString();
        String dutyCycle = mMainActivity.getDutyCycleString();


        if (text != null) {
            mText.setVisibility(View.GONE);
            mUid.setText(text);
            mUid.setVisibility(View.VISIBLE);
            mUidHeader.setVisibility(View.VISIBLE);
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
        mUid = mRoot.findViewById(R.id.uidValue);
        mUidHeader = mRoot.findViewById(R.id.uid);
        mFrequency = mRoot.findViewById(R.id.frequencyText);
        mIntensity = mRoot.findViewById(R.id.intensityText);
        mDutyCycle = mRoot.findViewById(R.id.dutyCycleText);

        mParameterLayout = mRoot.findViewById(R.id.parametersConstraint);

        mUid.setVisibility(View.GONE);
        mUidHeader.setVisibility(View.GONE);
        mParameterLayout.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}