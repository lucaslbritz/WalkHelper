package com.lucasbritz.walkhelper;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;

public class MainActivity extends Activity implements SensorEventListener {

    BeaconRepository beaconRepository = new BeaconRepository();

    //----------------------------------------------------------------------------------------------
    // BLE

    private static final int PERMISSIONS_REQUEST_ALL_PERMISSIONS = 1;
    private static final String TAG = "MainActivity";
    private static final String BEACON_ADDRESS = "D3:8A:63:6D:FB:79";

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    private List<Beacon> beacons = beaconRepository.findAllBeacons();

    //----------------------------------------------------------------------------------------------
    // Direction

    private static final String RIGHT = "direita";
    private static final String LEFT = "esquerda";
    private static final String FRONT = "frente";

    private TextView tvLocation;
    private TextView tvRotation;
    private Button bTalk;

    private SensorManager mSensorManager;

    private float[] mGravity;
    private float[] mGeomagnetic;
    private float azimut;
    private int rotation;
    private int angleTo;

    private String textAngle;
    private String textDirection;

    private Sensor accelerometer;
    private Sensor magnetometer;

    private boolean start = false;
    private boolean runningRight = false;
    private boolean runningLeft = false;

    private long startTime;
    private long endTime;

    Vibrator vibrator;

    Handler handler;

    //----------------------------------------------------------------------------------------------
    // Interaction

    private static final int DESTINATION_OPTION = 1;
    private static final int LOCATION_OPTION = 2;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    private TextToSpeech textToSpeech;
    private String options;
    private String answer;

    private ArrayList<String> breadcrumb;
    private boolean isFirstTime = true;

    private boolean toSpeech = true;
    private boolean finishSpeech = false;

    private int speechOption = 0;

    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // -----------------------------------------------------------------------------------------
        // BLE

        if (needPermissions(this)) {
            requestPermissions();
        }

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();

            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();

        // -----------------------------------------------------------------------------------------
        // Direction

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);

        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        tvLocation = (TextView) findViewById(R.id.tvLocation);
        tvRotation = (TextView) findViewById(R.id.tvRotation);
        bTalk = (Button) findViewById(R.id.bTalk);
        breadcrumb = new ArrayList<>();

        // -----------------------------------------------------------------------------------------
        // Interaction

        // Test
        breadcrumb.add("01:01:01:01:01:01");
        breadcrumb.add("02:02:02:02:02:02");
        breadcrumb.add("03:03:03:03:03:03");
        breadcrumb.add("04:04:04:04:04:04");
        breadcrumb.add("05:05:05:05:05:05");
        breadcrumb.add("06:06:06:06:06:06");

        bTalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //------------------------------------------------------------------------------------------
        // BLE

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();

            scanLeDevice();
        }

        //------------------------------------------------------------------------------------------
        // Direction

        mSensorManager.registerListener(this, accelerometer, 500000, 500000);
        mSensorManager.registerListener(this, magnetometer, 500000, 500000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //------------------------------------------------------------------------------------------
        // Direction

        mSensorManager.unregisterListener(this, accelerometer);
        mSensorManager.unregisterListener(this, magnetometer);

        //------------------------------------------------------------------------------------------
        // Interaction

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //------------------------------------------------------------------------------------------
        // BLE

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }

        //------------------------------------------------------------------------------------------
        // Interaction

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    answer = result.get(0);
                }
                break;
            }
        }
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

    //----------------------------------------------------------------------------------------------
    // BLE

    private void scanLeDevice() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        }, SCAN_PERIOD);

        mLEScanner.startScan(filters, settings, mScanCallback);
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());

            String name = result.getDevice().getName();
            String address = result.getDevice().getAddress();
            int rssi = result.getRssi();

            discoveredBeacon(name, address, rssi);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }
    };

    private void discoveredBeacon(String name, String address, int rssi) {
        if (BEACON_ADDRESS.equalsIgnoreCase(address)
                && !breadcrumb.contains(address)) {
            // TODO
            Handler speechHandler = new Handler();
            Beacon beacon = beaconRepository.findBeaconByAddress(address);
            String text = "";

            if (beacon != null) {
                String addressBeaconFrom = breadcrumb.size() > 0
                        ? breadcrumb.get(breadcrumb.size() - 1)
                        : null;

                breadcrumb.add(beacon.getAddress());
                tvLocation.setText(beacon.getDescription());

                text = buildTextOptions(beacon, addressBeaconFrom);

                angleTo = 192;
//            defineDirectionTo();
            } else {
                text = "Você está em um local sem cobertura de beacons";
            }

            finishSpeech = false;
            speech(text);
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();

            final Runnable speechRunnable = new Runnable() {
                public void run() {
                    if (finishSpeech) {
                        finishSpeech = false;
                        promptSpeechInput();
                    }
                    speechHandler.postDelayed(this, 500);
                }
            };

            speechHandler.postDelayed(speechRunnable, 500);
        }
    }

    //----------------------------------------------------------------------------------------------

    private void speech(String text) {
        textToSpeech = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        finishSpeech = false;
                    }

                    @Override
                    public void onError(String utteranceId) {
                        finishSpeech = true;
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        finishSpeech = true;
                    }
                });

                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.getDefault());
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null,
                            TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                }
            }
        });
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Dispositivo móvel não suporta função de fala.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void defineDirectionTo(View view) {
        textDirection = "";
        start = true;

        calculateDirection();
    }

    private void calculateDirection() {
        final Runnable runnable = new Runnable() {
            public void run() {
                if ((LEFT).equals(textDirection)) {
                    textAngle = "esquerda";
                    toSpeech = true;
                    runningLeft = false;
                } else if ((RIGHT).equals(textDirection)) {
                    textAngle = "direita";
                    toSpeech = true;
                    runningRight = false;
                }
            }
        };

        int correctionAngle = rotation - angleTo;
        if (!isCorrectAngle() && ((correctionAngle < 0
                && correctionAngle >= -180) || correctionAngle > 180)) {

            // direita
            if (isFirstTime) {
                textAngle = "vire à direita até seu celular vibrar";
                isFirstTime = false;
                toSpeech = true;
            } else if (!(RIGHT).equals(textDirection)) {
                startTime = System.currentTimeMillis();
            } else if (startTime != 0) {
                endTime = System.currentTimeMillis();
                if (endTime - startTime >= 1000) {
                    textAngle = "direita";
                    toSpeech = true;
                    startTime = 0;
                    endTime = 0;
                    runningRight = false;
                    runningLeft = false;
                }
            }

            if ((RIGHT).equals(textDirection) && startTime == 0) {
                handler = new Handler();
                if (!runningRight) {
                    runningRight = true;
                    handler.postDelayed(runnable, 5000);
                }
            }

            textDirection = RIGHT;

        } else if (!isCorrectAngle() && ((correctionAngle > 0
                && correctionAngle <= 180) || correctionAngle < -180)) {

            // esquerda
            if (isFirstTime) {
                textAngle = "vire à esquerda até seu celular vibrar";
                isFirstTime = false;
                toSpeech = true;
            } else if (!(LEFT).equals(textDirection)) {
                startTime = System.currentTimeMillis();
            } else if (startTime != 0) {
                endTime = System.currentTimeMillis();
                if (endTime - startTime >= 1000) {
                    textAngle = "esquerda";
                    toSpeech = true;
                    startTime = 0;
                    endTime = 0;
                    runningRight = false;
                    runningLeft = false;
                }
            }

            if ((LEFT).equals(textDirection) && startTime == 0) {
                handler = new Handler();
                if (!runningLeft) {
                    runningLeft = true;
                    handler.postDelayed(runnable, 5000);
                }
            }

            textDirection = LEFT;

        } else {
            // frente
            if (isCorrectAngle() && !(FRONT).equals(textDirection) && startTime == 0) {
                vibrator.vibrate(500);
                textAngle = "em frente";
                isFirstTime = false;
                toSpeech = true;
                runningRight = false;
                runningLeft = false;
            }
            textDirection = FRONT;
        }

        if (toSpeech) {
            finishSpeech = false;
            speech(textAngle);
            Toast.makeText(this, textAngle, Toast.LENGTH_SHORT).show();
            toSpeech = false;
        }
    }

    private boolean isCorrectAngle() {
        int difAngle = Math.abs(angleTo - rotation);
        difAngle = difAngle > 300 ? 360 - difAngle : difAngle;

        return difAngle <= 5;
    }

    private String buildTextOptions(Beacon beacon, String addressBeaconFrom) {
        String options;
        Beacon beaconFrom = new Beacon();

        if (speechOption == 0) {
            options = "Você está em " + beacon.getDescription() + ". " +
                    "Deseja entrar com um destino ou apenas ser informado " +
                    "sobre sua localização ao longo do caminho?";
        } else {

            if (!addressBeaconFrom.isEmpty()) {
                beaconFrom = beaconRepository.findBeaconByAddress(addressBeaconFrom);

                beacon.getNeighborhood().remove(addressBeaconFrom);
            }
            int numNeighbors = beacon.getNeighborhood().size();

            if (numNeighbors > 1) {
                options = "Gostaria de ir para ";
                int angleFrom = addressBeaconFrom != null ? calculateAngle(beaconFrom, beacon) : 0;

                for (int i = 0; i < numNeighbors; i++) {
                    Beacon beaconTo = beaconRepository
                            .findBeaconByAddress(beacon.getNeighborhood().get(i));

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
                if (beacon.getNeighborhood().isEmpty() || addressBeaconFrom != null) {
                    options = "Não há suporte.";
                } else {
                    Beacon beaconTo = beaconRepository
                            .findBeaconByAddress(beacon.getNeighborhood().get(0));

                    double dist = calculateDistance(beacon, beaconTo);
                    String neighbor = beaconTo.getDescription();

                    options = neighbor; // + ", a " + dist + " metros.";
                }
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

    static public boolean needPermissions(Activity activity) {
        Log.d(TAG, "needPermissions: ");
        return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        Log.d(TAG, "requestPermissions: ");
        String[] permissions = new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        };
        requestPermissions(permissions, PERMISSIONS_REQUEST_ALL_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ALL_PERMISSIONS:
                boolean hasAllPermissions = true;
                for (int i = 0; i < grantResults.length; ++i) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        hasAllPermissions = false;
                        Log.e(TAG, "Unable to get permission " + permissions[i]);
                    }
                }
                if (hasAllPermissions) {
                    finish();
                } else {
                    Toast.makeText(this,
                            "Unable to get all required permissions", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                break;
            default:
                Log.e(TAG, "Unexpected request code");
        }
    }
}
