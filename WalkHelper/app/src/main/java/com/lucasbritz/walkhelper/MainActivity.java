package com.lucasbritz.walkhelper;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Locale;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;

public class MainActivity extends Activity implements SensorEventListener {

    private TextView tvLocation;
    private TextView tvRotation;
    private TextToSpeech textToSpeech;
    private String options;
    private ArrayList<Long> breadcrumb;

    private SensorManager mSensorManager;

    private float[] mGravity;
    private float[] mGeomagnetic;
    private float azimut;
    private int rotation;

    private Sensor accelerometer;
    private Sensor magnetometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);

        tvLocation = (TextView) findViewById(R.id.tvLocation);
        tvRotation = (TextView) findViewById(R.id.tvRotation);
        breadcrumb = new ArrayList<>();

        // Test
        breadcrumb.add(1L);
        breadcrumb.add(2L);
        breadcrumb.add(3L);
        breadcrumb.add(4L);
        breadcrumb.add(5L);
        breadcrumb.add(6L);

        Beacon beacon = readBeacon(createBeacon7());

        if (beacon != null) {
            Long idBeaconFrom = breadcrumb.size() > 0 ? breadcrumb.get(breadcrumb.size() - 1) : null;
            breadcrumb.add(beacon.getId());
            tvLocation.setText(beacon.getLocation());

            options = buildTextOptions(beacon, idBeaconFrom);

            textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech.setLanguage(Locale.getDefault());
                        String toSpeak = "Você está em " + tvLocation.getText().toString() + ". ";
                        toSpeak += options;
                        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
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
                rotation = (int) (-azimut * 360 / (2 * 3.14159f));

                if (tvRotation.getText().toString() == "") {
                    tvRotation.setText(String.format("%dº", rotation));
                } else {
                    String aux = tvRotation.getText().toString().replace("º", "");
                    int aux2 = rotation - Integer.valueOf(aux);
                    if (aux2 > 4 || aux2 < -4) {
                        tvRotation.setText(String.format("%dº", rotation));
                    }
                }
            }
        }
    }

    private String buildTextOptions(Beacon beacon, Long idBeaconFrom) {
        String options;
        Beacon beaconFrom = new Beacon();

        if (idBeaconFrom != null) {
            beaconFrom = beacon.getNeighborhood().stream()
                    .filter(b -> b.getId() == idBeaconFrom).findFirst().get();

            beacon.getNeighborhood().removeIf (b -> b.getId() == idBeaconFrom);
        }
        int numNeighbors = beacon.getNeighborhood().size();

        if (numNeighbors > 1) {
            options = "Gostaria de ir para ";
            int angleFrom = idBeaconFrom != null ? calculateAngle(beaconFrom, beacon) : 0;

            for (int i = 0; i < numNeighbors; i++) {
                Beacon beaconTo = beacon.getNeighborhood().get(i);
                String neighbor = beaconTo.getLocation();
                int dist = calculateDistance(beacon, beaconTo);
                int angleTo = calculateAngle(beacon, beaconTo);

                String messageAngle = buildTextAngle(angleFrom, angleTo);

                if (i == 0) {
                    options += neighbor + ", a " + dist + " metros " + messageAngle;
                } else if (i == numNeighbors - 1) {
                    options += " ou " + neighbor + ", a " + dist + " metros " + messageAngle + "?";
                } else {
                    options += ", " + neighbor + ", a " + dist + " metros " + messageAngle;
                }
            }
        } else {
            if (beacon.getNeighborhood().isEmpty() || idBeaconFrom != null) {
                options = "Não há suporte.";
            } else {
                Beacon beaconTo = beacon.getNeighborhood().get(0);
                double dist = calculateDistance(beacon, beaconTo);
                String neighbor = beaconTo.getLocation();

                options = neighbor + ", a " + dist + " metros.";
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
                && beaconData.has("longitude") && beaconData.has("location")) {

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

    private JsonObject createBeacon1() {
        JsonObject beaconData = beacon1();

        JsonArray array = new JsonArray();
        array.add(beacon2());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject createBeacon2() {
        JsonObject beaconData = beacon2();

        JsonArray array = new JsonArray();
        array.add(beacon1());
        array.add(beacon3());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject createBeacon3() {
        JsonObject beaconData = beacon3();

        JsonArray array = new JsonArray();
        array.add(beacon2());
        array.add(beacon4());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject createBeacon4() {
        JsonObject beaconData = beacon4();

        JsonArray array = new JsonArray();
        array.add(beacon3());
        array.add(beacon5());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject createBeacon5() {
        JsonObject beaconData = beacon5();

        JsonArray array = new JsonArray();
        array.add(beacon4());
        array.add(beacon6());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject createBeacon6() {
        JsonObject beaconData = beacon6();

        JsonArray array = new JsonArray();
        array.add(beacon5());
        array.add(beacon7());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject createBeacon7() {
        JsonObject beaconData = beacon7();

        JsonArray array = new JsonArray();
        array.add(beacon6());
        array.add(beacon8());
        array.add(beacon9());
        array.add(beacon10());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject createBeacon8() {
        JsonObject beaconData = beacon8();

        JsonArray array = new JsonArray();
        array.add(beacon7());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject createBeacon9() {
        JsonObject beaconData = beacon9();

        JsonArray array = new JsonArray();
        array.add(beacon7());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject createBeacon10() {
        JsonObject beaconData = beacon3();

        JsonArray array = new JsonArray();
        array.add(beacon7());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject beacon1() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", 1);
        beaconData.addProperty("latitude", -29.792233);
        beaconData.addProperty("longitude", -51.154587);
        beaconData.addProperty("location", "Unisinos. Acesso principal");

        return beaconData;
    }

    private JsonObject beacon2() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", 2);
        beaconData.addProperty("latitude", -29.792576);
        beaconData.addProperty("longitude", -51.154477);
        beaconData.addProperty("location", "B02");

        return beaconData;
    }

    private JsonObject beacon3() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "3");
        beaconData.addProperty("latitude", "-29.792732");
        beaconData.addProperty("longitude", "-51.154429");
        beaconData.addProperty("location", "B03");

        return beaconData;
    }

    private JsonObject beacon4() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "4");
        beaconData.addProperty("latitude", "-29.792918");
        beaconData.addProperty("longitude", "-51.154365");
        beaconData.addProperty("location", "B04");

        return beaconData;
    }

    private JsonObject beacon5() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "5");
        beaconData.addProperty("latitude", "-29.793078");
        beaconData.addProperty("longitude", "-51.154331");
        beaconData.addProperty("location", "B05");

        return beaconData;
    }

    private JsonObject beacon6() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "6");
        beaconData.addProperty("latitude", "-29.793248");
        beaconData.addProperty("longitude", "-51.154283");
        beaconData.addProperty("location", "B06");

        return beaconData;
    }

    private JsonObject beacon7() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "7");
        beaconData.addProperty("latitude", "-29.793767");
        beaconData.addProperty("longitude", "-51.154152");
        beaconData.addProperty("location", "B07");

        return beaconData;
    }

    private JsonObject beacon8() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "8");
        beaconData.addProperty("latitude", "-29.793639");
        beaconData.addProperty("longitude", "-51.153535");
        beaconData.addProperty("location", "auditório central");

        return beaconData;
    }

    private JsonObject beacon9() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "9");
        beaconData.addProperty("latitude", "-29.794247");
        beaconData.addProperty("longitude", "-51.154771");
        beaconData.addProperty("location", "redondo");

        return beaconData;
    }

    private JsonObject beacon10() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "10");
        beaconData.addProperty("latitude", "-29.795341");
        beaconData.addProperty("longitude", "-51.153741");
        beaconData.addProperty("location", "corredor principal. Próximo ao Fratelo");

        return beaconData;
    }
}
