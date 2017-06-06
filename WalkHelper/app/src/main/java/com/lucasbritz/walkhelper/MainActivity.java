package com.lucasbritz.walkhelper;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;

    //----------------------------------------------------------------------------------------------
    // Direction

    private static final String RIGHT = "direita";
    private static final String LEFT = "esquerda";
    private static final String FRONT = "frente";

    private TextView tvLocation;
    private TextView tvRotation;

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

    private TextToSpeech textToSpeech;
    private String options;
    private ArrayList<String> breadcrumb;

    private boolean isFirstTime = true;
    private boolean toSpeech = true;

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
        breadcrumb = new ArrayList<>();

        // -----------------------------------------------------------------------------------------
        // Interaction

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

           speech(text);

            angleTo = 192;
//            defineDirectionTo();
        }
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
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build();
            filters = new ArrayList<ScanFilter>();

            scanLeDevice(true);
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
        // BLE

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }

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
    protected void onDestroy() {
        //------------------------------------------------------------------------------------------
        // BLE

        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;

        //------------------------------------------------------------------------------------------

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

        super.onActivityResult(requestCode, resultCode, data);
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

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            mLEScanner.startScan(filters, settings, mScanCallback);
        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };

    //----------------------------------------------------------------------------------------------

    private void speech(String text) {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.getDefault());
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });
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
            speech(textAngle);
            toSpeech = false;
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
