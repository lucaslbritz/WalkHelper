package com.lucasbritz.walkhelper;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Locale;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;

public class MainActivity extends Activity implements SensorEventListener {

    private static final String RIGHT = "direita";
    private static final String LEFT = "esquerda";
    private static final String FRONT = "frente";

    BeaconRepository beaconRepository = new BeaconRepository();

    private TextView tvLocation;
    private TextView tvRotation;
    private TextToSpeech textToSpeech;
    private TextToSpeech angleToSpeech;
    private String options;
    private ArrayList<String> breadcrumb;

    private SensorManager mSensorManager;

    private float[] mGravity;
    private float[] mGeomagnetic;
    private float azimut;
    private int rotation;
    private int angleTo;
    private int sumAngles;
    private int countAngles;

    private String textAngle;
    private String textDirection;

    private Sensor accelerometer;
    private Sensor magnetometer;

    private boolean isFirstTime = true;
    private boolean speech = true;
    private boolean start = false;

    private long startTime;
    private long endTime;

    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);

        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        tvLocation = (TextView) findViewById(R.id.tvLocation);
        tvRotation = (TextView) findViewById(R.id.tvRotation);
        breadcrumb = new ArrayList<>();

        // Test
        breadcrumb.add("1");
        breadcrumb.add("2");
        breadcrumb.add("3");
        breadcrumb.add("4");
        breadcrumb.add("5");
        breadcrumb.add("6");

        Beacon beacon = readBeacon(beaconRepository.createBeacon7());

        if (beacon != null) {
            String idBeaconFrom = breadcrumb.size() > 0 ? breadcrumb.get(breadcrumb.size() - 1) : null;
            breadcrumb.add(beacon.getId());
            tvLocation.setText(beacon.getDescription());

            options = buildTextOptions(beacon, idBeaconFrom);

            String text = "Você está em " + tvLocation.getText().toString()
                    + ". " + options;

            textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech.setLanguage(Locale.getDefault());
                        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
            });

            angleTo = 192;
//            defineDirectionTo();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, accelerometer, 500000, 500000);
        mSensorManager.registerListener(this, magnetometer, 500000, 500000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (angleToSpeech != null) {
            angleToSpeech.stop();
            angleToSpeech.shutdown();
        }

        mSensorManager.unregisterListener(this, accelerometer);
        mSensorManager.unregisterListener(this, magnetometer);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

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

                tvRotation.setText(String.format("%dº", rotation));

                if (start) {
                    calculateDirection();
                }
            }
        }
    }

    public void defineDirectionTo(View view) {
        textDirection = "";
        start = true;

        calculateDirection();
    }

    private void calculateDirection() {
//        if ((FRONT).equals(textDirection)) {
//            sumAngles += rotation;
//            countAngles++;
//            rotation = (int) sumAngles / countAngles;
//        }

        int correctionAngle = rotation - angleTo;
        if (!isCorrectAngle() && ((correctionAngle < 0
                && correctionAngle >= -180) || correctionAngle > 180)) {

            // direita
            if (isFirstTime) {
                textAngle = "vire à direita até seu celular vibrar";
                isFirstTime = false;
                speech = true;
            } else if (!(RIGHT).equals(textDirection)) {
                startTime = System.currentTimeMillis();
            } else if (startTime != 0) {
                endTime = System.currentTimeMillis();
                if (endTime - startTime >= 1000) {
                    textAngle = "direita";
                    speech = true;
                    startTime = 0;
                    endTime = 0;
                }
            }
            textDirection = RIGHT;

        } else if (!isCorrectAngle() && ((correctionAngle > 0
                && correctionAngle <= 180) || correctionAngle < -180)) {

            // esquerda
            if (isFirstTime) {
                textAngle = "vire à esquerda até seu celular vibrar";
                isFirstTime = false;
                speech = true;
            } else if (!(LEFT).equals(textDirection)) {
                startTime = System.currentTimeMillis();
            } else if (startTime != 0) {
                endTime = System.currentTimeMillis();
                if (endTime - startTime >= 1000) {
                    textAngle = "esquerda";
                    speech = true;
                    startTime = 0;
                    endTime = 0;
                }
            }
            textDirection = LEFT;

        } else {
            // frente
            if (isCorrectAngle() && !(FRONT).equals(textDirection) && startTime == 0) {
                vibrator.vibrate(500);
                textAngle = "em frente";
                isFirstTime = false;
                speech = true;
                sumAngles = rotation;
                countAngles = 1;
            }
            textDirection = FRONT;
        }

        if (speech) {
            speech = false;
            angleToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        angleToSpeech.setLanguage(Locale.getDefault());
                        angleToSpeech.speak(textAngle, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
            });
        }
    }

    private boolean isCorrectAngle() {
        int difAngle = Math.abs(angleTo - rotation);
        difAngle = difAngle > 300 ? 360 - difAngle : difAngle;

        return difAngle <= 5;
    }

    private String buildTextOptions(Beacon beacon, String idBeaconFrom) {
        String options;
        Beacon beaconFrom = new Beacon();

        if (!idBeaconFrom.isEmpty()) {
            beaconFrom = beacon.getNeighborhood().stream()
                    .filter(b -> b.getId().equals(idBeaconFrom)).findFirst().get();

            beacon.getNeighborhood().remove(beaconFrom);
        }
        int numNeighbors = beacon.getNeighborhood().size();

        if (numNeighbors > 1) {
            options = "Gostaria de ir para ";
            int angleFrom = idBeaconFrom != null ? calculateAngle(beaconFrom, beacon) : 0;

            for (int i = 0; i < numNeighbors; i++) {
                Beacon beaconTo = beacon.getNeighborhood().get(i);
                String neighbor = beaconTo.getDescription();
                int dist = calculateDistance(beacon, beaconTo);
                int angleTo = calculateAngle(beacon, beaconTo);

                String messageAngle = buildTextAngle(angleFrom, angleTo);

                if (i == 0) {
                    options += neighbor; // + ", a " + dist + " metros " + messageAngle;
                } else if (i == numNeighbors - 1) {
                    options += " ou " + neighbor; // + ", a " + dist + " metros " + messageAngle + "?";
                } else {
                    options += ", " + neighbor; // + ", a " + dist + " metros " + messageAngle;
                }
            }
        } else {
            if (beacon.getNeighborhood().isEmpty() || idBeaconFrom != null) {
                options = "Não há suporte.";
            } else {
                Beacon beaconTo = beacon.getNeighborhood().get(0);
                double dist = calculateDistance(beacon, beaconTo);
                String neighbor = beaconTo.getDescription();

                options = neighbor; // + ", a " + dist + " metros.";
            }
        }

        return options;
    }

    private String buildTextAngle(int angleFrom, int angleTo) {
        String message = "";
        int angle;
        if (angleTo > angleFrom) {
            angle = angleTo - angleFrom;
            message = " " + angle + "graus a esquerda";
        } else if (angleTo < angleFrom) {
            angle = angleFrom - angleTo;
            message = " " + angle + "graus a direita";
        } else {
            message = "em frente";
        }

        return message;
    }

    private Beacon readBeacon(JsonObject beaconData) {
        Gson gson = new Gson();
        Beacon beacon = null;
        if (beaconData.has("id") && beaconData.has("latitude")
                && beaconData.has("longitude") && beaconData.has("description")) {

            beacon = gson.fromJson(beaconData, Beacon.class);
        }
        return beacon;
    }

    private int calculateDistance(Beacon beaconFrom, Beacon beaconTo) {
        double latitudeFrom = beaconFrom.getLatitude();
        double longitudeFrom = beaconFrom.getLongitude();

        double latitudeTo = beaconTo.getLatitude();
        double longitudeTo = beaconTo.getLongitude();

        double earthRadius = 6371e3;

        double distLat = Math.toRadians(latitudeTo - latitudeFrom);
        double distLon = Math.toRadians(longitudeTo - longitudeFrom);

        latitudeFrom = Math.toRadians(latitudeFrom);
        latitudeTo = Math.toRadians(latitudeTo);

        double a = Math.sin(distLat/2) * Math.sin(distLat/2)
                + Math.cos(latitudeFrom) * Math.cos(latitudeTo)
                * Math.sin(distLon/2) * Math.sin(distLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        double distance = earthRadius * c;

        return (int) distance;
    }

    private int calculateAngle(Beacon beaconFrom, Beacon beaconTo) {
        double latitudeFrom = beaconFrom.getLatitude();
        double longitudeFrom = beaconFrom.getLongitude();

        double latitudeTo = beaconTo.getLatitude();
        double longitudeTo = beaconTo.getLongitude();

        double distLon = Math.toRadians(longitudeTo - longitudeFrom);

        latitudeFrom = Math.toRadians(latitudeFrom);
        latitudeTo = Math.toRadians(latitudeTo);

        double y = Math.sin(distLon) * Math.cos(latitudeTo);
        double x = Math.cos(latitudeFrom) * Math.sin(latitudeTo) - Math.sin(latitudeFrom)
                * Math.cos(latitudeTo) * Math.cos(distLon);

        double angle = Math.atan2(y, x);

        angle = Math.toDegrees(angle);
        angle = 360 - (angle + 360) % 360;

        return (int) angle;
    }
}
