package pw.kussa.balanbot.remotecontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private BluetoothController mBT;
    private SharedPreferences mSharedPref;

    private TextView mTextSetpoint;
    private TextView mTextMotorMultiplier;
    private TextView mTextMovementSpeed;
    private TextView mTextRotationSpeed;
    private TextView mTextKp;
    private TextView mTextKi;
    private TextView mTextKd;
    private TextView mTextStatus;

    private TextView mTextLeftMotorSpeed;
    private TextView mTextRightMotorSpeed;
    private TextView mRollValue;
    private TextView mYawValue;
    private TextView mRollSetpoint;
    private TextView mYawSetpoint;

    private SeekBar mSeekSetpoint;
    private SeekBar mSeekMotorMultiplier;
    private SeekBar mSeekMovementSpeed;
    private SeekBar mSeekRotationSpeed;
    private SeekBar mSeekKp;
    private SeekBar mSeekKi;
    private SeekBar mSeekKd;

    private Switch mSwitchBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get handles to Views

        mTextSetpoint = (TextView) findViewById(R.id.text_setpoint);
        mTextMotorMultiplier = (TextView) findViewById(R.id.text_motor_multiplier);
        mTextMovementSpeed = (TextView) findViewById(R.id.text_movement_speed);
        mTextRotationSpeed = (TextView) findViewById(R.id.text_rotation_speed);
        mTextKp = (TextView) findViewById(R.id.text_k_p);
        mTextKi = (TextView) findViewById(R.id.text_k_i);
        mTextKd = (TextView) findViewById(R.id.text_k_d);
        mTextStatus = (TextView) findViewById(R.id.text_status);

        mTextLeftMotorSpeed = (TextView) findViewById(R.id.text_left_motor_speed);
        mTextRightMotorSpeed = (TextView) findViewById(R.id.text_right_motor_speed);
        mRollValue = (TextView) findViewById(R.id.text_roll_value);
        mYawValue = (TextView) findViewById(R.id.text_yaw_value);
        mRollSetpoint = (TextView) findViewById(R.id.text_roll_setpoint);
        mYawSetpoint = (TextView) findViewById(R.id.text_yaw_setpoint);

        mSeekSetpoint = (SeekBar) findViewById(R.id.seek_setpoint);
        mSeekMotorMultiplier = (SeekBar) findViewById(R.id.seek_motor_multiplier);
        mSeekMovementSpeed = (SeekBar) findViewById(R.id.seek_movement_speed);
        mSeekRotationSpeed = (SeekBar) findViewById(R.id.seek_rotation_speed);
        mSeekKp = (SeekBar) findViewById(R.id.seek_k_p);
        mSeekKi = (SeekBar) findViewById(R.id.seek_k_i);
        mSeekKd = (SeekBar) findViewById(R.id.seek_k_d);

        mSwitchBalance = (Switch) findViewById(R.id.switch_balance);


        // Initialize values

        initValues();


        // Initialize Bluetooth

        if (!BluetoothController.enableBluetooth(this)) {
                Toast.makeText(MainActivity.this, "Error: This device does not have Bluetooth capability",
                        Toast.LENGTH_SHORT).show();
            return;
        }

        mBT = new BluetoothController("BALANBOT");


        // Create data receiver Thread

        final Handler handler = new Handler();

        Thread workerThread = new Thread(mBT.receiver(new BluetoothController.Receiver() {
            public void received(final byte[] data) {
                handler.post(new Runnable() {
                    public void run() {
//                        String str = new String(data);

                        // Status packet
                        if (data[0] == 's' && data.length >= 13) {
                            double yaw = ((double) ((int) data[1] << 8) + ((int) data[2] & 0xFF)) / 10.0 - 180.0;
                            double roll = ((double) ((int) data[3] << 8) + ((int) data[4] & 0xFF)) / 10.0 - 180.0;
                            int motor1Speed = ((int) data[5] << 8) + ((int) data[6] & 0xFF) - 255;
                            int motor2Speed = ((int) data[7] << 8) + ((int) data[8] & 0xFF) - 255;
                            double yawSetpoint = (((int) data[9] << 8) + ((int) data[10] & 0xFF)) / 10.0 - 180.0;
                            double rollSetpoint = (((int) data[11] << 8) + ((int) data[12] & 0xFF))  / 10.0 - 180.0;

                            mTextLeftMotorSpeed.setText(String.valueOf(motor1Speed));
                            mTextRightMotorSpeed.setText(String.valueOf(motor2Speed));
                            mRollValue.setText(String.format(Locale.US, "%.2f", roll));
                            mYawValue.setText(String.format(Locale.US, "%.2f", yaw));
                            mRollSetpoint.setText(String.format(Locale.US, "%.2f", rollSetpoint));
                            mYawSetpoint.setText(String.format(Locale.US, "%.2f", yawSetpoint));
                        }
                    }
                });
            }
        }));

        workerThread.start();

        // Disconnect handler

        mBT.setOnDisconnect(new Runnable() {
            public void run() {
                mTextStatus.setTextColor(0xFFFF0000);
                mTextStatus.setText(getString(R.string.status_disconnected));
                mTextLeftMotorSpeed.setText("0");
                mTextRightMotorSpeed.setText("0");
                mRollValue.setText("0");
                mYawValue.setText("0");
                mRollSetpoint.setText("0");
                mYawSetpoint.setText("0");
            }
        });


        // Initialize Buttons

        final Button button_connect = (Button) findViewById(R.id.button_connect);
        button_connect.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                try {
                    if (!mBT.connect()) {
                        Toast.makeText(MainActivity.this, "Error: Could not connect to Bluetooth device",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        mTextStatus.setTextColor(0xFF00FF00);
                        mTextStatus.setText(getString(R.string.status_connected));
                        sendValues();
                    }
                } catch (IOException ex) {
                    Toast.makeText(MainActivity.this, "Error: Could not connect to Bluetooth device",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });


        final Button button_forward = (Button) findViewById(R.id.button_forward);
        button_forward.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    mBT.write("F");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mBT.write("f");
                }
                return true;
            }
        });


        final Button button_backward = (Button) findViewById(R.id.button_backward);
        button_backward.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    mBT.write("B");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mBT.write("b");
                }
                return true;
            }
        });


        final Button button_left = (Button) findViewById(R.id.button_left);
        button_left.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    mBT.write("L");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mBT.write("l");
                }
                return true;
            }
        });


        final Button button_right = (Button) findViewById(R.id.button_right);
        button_right.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    mBT.write("R");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mBT.write("r");
                }
                return true;
            }
        });


        final Button button_send_values = (Button) findViewById(R.id.button_send_values);
        button_send_values.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendValues();
            }
        });


        mSwitchBalance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setBalance(b);
            }
        });

        // Initialize SeekBars

        mSeekSetpoint.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setSetpoint(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                mTextSetpoint.setText(String.format(Locale.US, getString(R.string.change_setpoint), setpointFromProgress(progress)));
            }
        });

        mSeekMotorMultiplier.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setMotorMultiplier(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                mTextMotorMultiplier.setText(String.format(Locale.US, getString(R.string.motor_multiplier), progress));
            }
        });

        mSeekMovementSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setMovementSpeed(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextMovementSpeed.setText(String.format(Locale.US, getString(R.string.movement_speed), progress));
            }
        });

        mSeekRotationSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setRotationSpeed(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextRotationSpeed.setText(String.format(Locale.US, getString(R.string.rotation_speed), progress));
            }
        });

        mSeekKp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setKp(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextKp.setText(String.format(Locale.US, getString(R.string.k_p), kpFromProgress(progress)));
            }
        });

        mSeekKi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setKi(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextKi.setText(String.format(Locale.US, getString(R.string.k_i), kiFromProgress(progress)));
            }
        });

        mSeekKd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setKd(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextKd.setText(String.format(Locale.US, getString(R.string.k_d), kdFromProgress(progress)));
            }
        });
    }

    private void initValues() {
        // Read values from SharedPref
        mSharedPref = this.getSharedPreferences(getString(R.string.preference_values), Context.MODE_PRIVATE);

        boolean balance = mSharedPref.getBoolean("balance", true);

        int setpoint = mSharedPref.getInt("setpoint", 50);
        int motorMultiplier = mSharedPref.getInt("motor_multiplier", 100);
        int movementSpeed = mSharedPref.getInt("movement_speed", 100);
        int rotationSpeed = mSharedPref.getInt("rotation_speed", 100);
        int kp = mSharedPref.getInt("k_p", 50);
        int ki = mSharedPref.getInt("k_i", 25);
        int kd = mSharedPref.getInt("k_d", 25);

        mSwitchBalance.setChecked(balance);

        mSeekSetpoint.setProgress(setpoint);
        mSeekMotorMultiplier.setProgress(motorMultiplier);
        mSeekMovementSpeed.setProgress(movementSpeed);
        mSeekRotationSpeed.setProgress(rotationSpeed);
        mSeekKp.setProgress(kp);
        mSeekKi.setProgress(ki);
        mSeekKd.setProgress(kd);

        mTextSetpoint.setText(String.format(Locale.US, getString(R.string.change_setpoint), setpointFromProgress(setpoint)));
        mTextMotorMultiplier.setText(String.format(Locale.US, getString(R.string.motor_multiplier), motorMultiplier));
        mTextMovementSpeed.setText(String.format(Locale.US, getString(R.string.movement_speed), movementSpeed));
        mTextRotationSpeed.setText(String.format(Locale.US, getString(R.string.rotation_speed), rotationSpeed));
        mTextKp.setText(String.format(Locale.US, getString(R.string.k_p), kpFromProgress(kp)));
        mTextKi.setText(String.format(Locale.US, getString(R.string.k_i), kiFromProgress(ki)));
        mTextKd.setText(String.format(Locale.US, getString(R.string.k_d), kdFromProgress(kd)));
    }

    private void sendValues() {
        mBT.writeParam('z', (byte) (mSwitchBalance.isChecked()? 1: 0));
        mBT.writeParam('x', (byte) mSeekSetpoint.getProgress());
        mBT.writeParam('m', (byte) mSeekMotorMultiplier.getProgress());
        mBT.writeParam('h', (byte) mSeekMovementSpeed.getProgress());
        mBT.writeParam('g', (byte) mSeekRotationSpeed.getProgress());
        mBT.writeParam('p', (byte) mSeekKp.getProgress());
        mBT.writeParam('i', (byte) mSeekKi.getProgress());
        mBT.writeParam('d', (byte) mSeekKd.getProgress());
    }

    private String setpointFromProgress(int progress) {
        return String.format(Locale.US, "%.2f", ((float)progress / 2.5) - 20.0);
    }

    private String kpFromProgress(int progress) {
        return String.format(Locale.US, "%.2f", (((float)progress) * 250.0f) / 255.0f);
    }

    private String kiFromProgress(int progress) {
        return String.format(Locale.US, "%.2f", (((float)progress) * 50.0f) / 255.0f);
    }

    private String kdFromProgress(int progress) {
        return String.format(Locale.US, "%.2f", (((float)progress) * 5.0f) / 255.0f);
    }

    private void setBalance(boolean balance) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean("balance", balance);
        editor.apply();

        mBT.writeParam('z', (byte) (balance? 1: 0));
    }

    private void setSetpoint(int progress) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt("setpoint", progress);
        editor.apply();

        mBT.writeParam('x', (byte) progress);
    }

    private void setMotorMultiplier(int progress) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt("motor_multiplier", progress);
        editor.apply();

        mBT.writeParam('m', (byte) progress);
    }

    private void setMovementSpeed(int progress) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt("movement_speed", progress);
        editor.apply();

        mBT.writeParam('h', (byte) progress);
    }

    private void setRotationSpeed(int progress) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt("rotation_speed", progress);
        editor.apply();

        mBT.writeParam('g', (byte) progress);
    }

    private void setKp(int progress) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt("k_p", progress);
        editor.apply();

        mBT.writeParam('p', (byte) progress);
    }

    private void setKi(int progress) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt("k_i", progress);
        editor.apply();

        mBT.writeParam('i', (byte) progress);
    }

    private void setKd(int progress) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt("k_d", progress);
        editor.apply();

        mBT.writeParam('d', (byte) progress);
    }
}
