package com.golab.optonfcappfragments.ui.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.golab.optonfcappfragments.MainActivity;
import com.golab.optonfcappfragments.R;
import com.golab.optonfcappfragments.databinding.FragmentDashboardBinding;
import com.golab.optonfcappfragments.utils.NfcBroadcasts;

public class DashboardFragment extends Fragment {

    private static final int MIN_INTENSITY = 0;
    private static final int MAX_INTENSITY = 100;
    private static final int MAX_FREQUENCY = 254;
    private static final int MIN_FREQUENCY = 0;
    private static final int MAX_DUTY_CYCLE = 100;
    private static final int MIN_DUTY_CYCLE = 0;

    private FragmentDashboardBinding binding;

    private TextView mWriteText;
    private TextView mCountView;
    private MainActivity mMainActivity;

    private TextView mIntensity;
    private TextView mFrequency;
    private TextView mDutyCycle;

    private Button mWriteButton;

    private Button mPlusIntensity;
    private Button mMinusIntensity;
    private Button mPlusFrequency;
    private Button mMinusFrequency;
    private Button mPlusDuty;
    private Button mMinusDuty;

    private int mCounter;

    private CountDownTimer mCDTimer;

    private View mRoot;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);

        mRoot = binding.getRoot();

        this.mMainActivity = (MainActivity) getActivity();

        initButtons();
        initTextViews();

        IntentFilter filter = new IntentFilter(NfcBroadcasts.NFC_STOP_WRITE);

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (NfcBroadcasts.NFC_STOP_WRITE.equals(intent.getAction())) {
                    stopWrite();
                }
            }
        };

        requireActivity().registerReceiver(mReceiver, filter);

        mWriteButton = (Button) mRoot.findViewById(R.id.writeButton);

        // Button to initiate and count down write mode.
        mWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // The strength of the haptics on clock tick.
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                mCounter = 30;
                mMainActivity.setWriteMode(true);
                if (checkParameters()) {
                    mMainActivity.setParameters(mIntensity.getText().toString(), mFrequency.getText().toString(), mDutyCycle.getText().toString());
                    mCountView.setText(String.valueOf(mCounter));
                    mCountView.setVisibility(View.VISIBLE);
                    // Create 30 second timer to remain in write mode.
                    mCDTimer = new CountDownTimer(30000, 1000){
                        public void onTick(long millisUntilFinished){
                            mCountView.setText(String.valueOf(mCounter));
                            mCounter--;
                            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        }
                        public void onFinish(){
                            mMainActivity.setWriteMode(false);
                            mCountView.setVisibility(View.GONE);
                        }
                    }.start();
                }
            }
        });

        initMathButtons();

        return mRoot;
    }

    /**
     * Function to verify imputed parameters are withing acceptable values.
     *
     * @return boolean - True or false on weather the parameters are all valid.
     */
    private boolean checkParameters() {
        String toast;

        if (Integer.parseInt(mIntensity.getText().toString()) > MAX_INTENSITY) {
            toast = "Intensity exceeds " + MAX_INTENSITY;
        } else if (Integer.parseInt(mIntensity.getText().toString()) < MIN_INTENSITY){
            toast = "Intensity is less than " + MIN_INTENSITY;
        } else if (Integer.parseInt(mFrequency.getText().toString()) > MAX_FREQUENCY) {
            toast = "Frequency exceeds " + MAX_FREQUENCY;
        } else if (Integer.parseInt(mFrequency.getText().toString()) < MIN_FREQUENCY) {
            toast = "Frequency is less than " + MIN_FREQUENCY;
        } else if (Integer.parseInt(mDutyCycle.getText().toString()) > MAX_DUTY_CYCLE) {
            toast = "Duty Cycle exceeds " + MAX_DUTY_CYCLE;
        } else if (Integer.parseInt(mDutyCycle.getText().toString()) < MIN_DUTY_CYCLE) {
            toast = "Duty Cycle is less than " + MIN_DUTY_CYCLE;
        } else {
            return true;
        }

        mMainActivity.toastParamIssue(toast);
        return false;
    }

    /**
     * Function to initialize the buttons to increase or decrease parameter values.
     */
    private void initMathButtons() {
        mPlusIntensity = (Button) mRoot.findViewById(R.id.intensityPlus);

        mPlusIntensity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                if (!mIntensity.getText().toString().equals("")) {
                    if (Integer.parseInt(mIntensity.getText().toString()) < MAX_INTENSITY) {
                        mIntensity.setText(Integer.toString(Integer.parseInt(mIntensity.getText().toString()) + 1));
                    }
                } else {
                    mIntensity.setText("1");
                }
            }
        });

        mMinusIntensity = (Button) mRoot.findViewById(R.id.intensityMinus);

        mMinusIntensity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                if (!mIntensity.getText().toString().equals("")) {
                    if (Integer.parseInt(mIntensity.getText().toString()) > MIN_INTENSITY) {
                        mIntensity.setText(Integer.toString(Integer.parseInt(mIntensity.getText().toString()) - 1));
                    }
                } else {
                    mIntensity.setText("1");
                }
            }
        });

        mPlusFrequency = (Button) mRoot.findViewById(R.id.freqPlus);

        mPlusFrequency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                if (!mFrequency.getText().toString().equals("")) {
                    if (Integer.parseInt(mFrequency.getText().toString()) < MAX_FREQUENCY) {
                        mFrequency.setText(Integer.toString(Integer.parseInt(mFrequency.getText().toString()) + 1));
                    }
                } else {
                    mFrequency.setText("1");
                }
            }
        });

        mMinusFrequency = (Button) mRoot.findViewById(R.id.freqMinus);

        mMinusFrequency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                if (!mFrequency.getText().toString().equals("")) {
                    if (Integer.parseInt(mFrequency.getText().toString()) > MIN_FREQUENCY) {
                        mFrequency.setText(Integer.toString(Integer.parseInt(mFrequency.getText().toString()) - 1));
                    }
                } else {
                    mFrequency.setText("1");
                }
            }
        });

        mPlusDuty = (Button) mRoot.findViewById(R.id.dutyPlus);

        mPlusDuty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                if (!mDutyCycle.getText().toString().equals("")) {
                    if (Integer.parseInt(mDutyCycle.getText().toString()) < MAX_DUTY_CYCLE) {
                        mDutyCycle.setText(Integer.toString(Integer.parseInt(mDutyCycle.getText().toString()) + 1));
                    }
                } else {
                    mDutyCycle.setText("1");
                }
            }
        });

        mMinusDuty = (Button) mRoot.findViewById(R.id.dutyMinus);

        mMinusDuty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                if (!mDutyCycle.getText().toString().equals("")) {
                    if (Integer.parseInt(mDutyCycle.getText().toString()) > MIN_DUTY_CYCLE) {
                        mDutyCycle.setText(Integer.toString(Integer.parseInt(mDutyCycle.getText().toString()) - 1));
                    }
                } else {
                    mDutyCycle.setText("1");
                }
            }
        });
    }


    private void initTextViews() {
        mWriteText = (TextView) mRoot.findViewById(R.id.writeText);
        mCountView = (TextView) mRoot.findViewById(R.id.countView);

        mIntensity = (TextView) mRoot.findViewById(R.id.intensityNumberText);
        mFrequency = (TextView) mRoot.findViewById(R.id.frequencyNumberText);
        mDutyCycle = (TextView) mRoot.findViewById(R.id.dutyCycleNumberText);

        mCountView.setVisibility(View.GONE);
    }

    private void initButtons() {
        mWriteButton = (Button) mRoot.findViewById(R.id.writeButton);
    }

    /**
     * Function to take app out of write mode, cancels 30 second write mode timer.
     */
    public void stopWrite() {
        if (mCDTimer != null) {
            mCDTimer.cancel();
        }
        mMainActivity.setWriteMode(false);
        mCountView.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}