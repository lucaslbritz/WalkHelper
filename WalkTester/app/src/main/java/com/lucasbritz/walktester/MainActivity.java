package com.lucasbritz.walktester;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;

public class MainActivity extends Activity implements SensorEventListener {

    private TextView tvAngle;
    private TextView tvMinAngle;
    private TextView tvMaxAngle;

    private SensorManager mSensorManager;

    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] mGravity;
    private float[] mGeomagnetic;

    private float azimut;
    private int rotation;

    private int minAngle = 359;
    private int maxAngle = 0;

    private boolean isRunningTest = false;

    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvAngle = (TextView) findViewById(R.id.tvAngle);
        tvMinAngle = (TextView) findViewById(R.id.tvMinAngle);
        tvMaxAngle = (TextView) findViewById(R.id.tvMaxAngle);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);

        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, accelerometer, 1000000, 1000000);
        mSensorManager.registerListener(this, magnetometer, 1000000, 1000000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(this, accelerometer);
        mSensorManager.unregisterListener(this, magnetometer);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == TYPE_ACCELEROMETER) {
            mGravity = event.values;
        }

        if (event.sensor.getType() == TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                azimut = orientation[0];
                rotation = (int) ((Math.toDegrees(azimut) + 360) % 360);

                tvAngle.setText(String.format("%dº", rotation));

                if (isRunningTest) {
                    testWalkAngle();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

    private void testWalkAngle() {
        minAngle = rotation < minAngle ? rotation : minAngle;
        maxAngle = rotation > maxAngle ? rotation : maxAngle;

        tvMinAngle.setText(String.format("%dº", minAngle));
        tvMaxAngle.setText(String.format("%dº", maxAngle));
    }

    public void startFinishTest(View view) {
        if (isRunningTest) {
            isRunningTest = false;
        } else {
            isRunningTest = true;
            minAngle = 359;
            maxAngle = 0;
            tvMinAngle.setText("");
            tvMaxAngle.setText("");
        }
    }
}
