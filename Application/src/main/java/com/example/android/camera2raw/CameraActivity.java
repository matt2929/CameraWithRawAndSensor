/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2raw;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import java.io.File;

import static com.example.android.camera2raw.SaveCSV.generateTimestamp;


/**
 * Activity displaying a fragment that implements RAW photo captures.
 */
public class CameraActivity extends Activity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private SensorManager senSensorManager;
    private Sensor senAccelerometer, senLightSensor,senGyroscope,senMagnetometer,senBarometer,senOrient;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z, rollingAverage = 1, temp, light;
    float accMag = 0;
    float accx = 0, accy = 0, accz = 0;
    float gyrox = 0, gyroy = 0, gyroz = 0;
    float magx = 0, magy = 0, magz = 0;
    float orix = 0, oriy = 0, oriz = 0;

    float barometer =0;
    String[] sensValues = {};
    String[] sensTitles = new String[]{"TimeStamp", "GravX", "GravY", "GravZ", "GravMag", "Rolling Average", "Light","magx" ,"magy","magz","gyrox","gyroy","gyroy","barometer","orix","oriy","oriz"};
    SaveCSV saveCSV;
    private static final int SHAKE_THRESHOLD = 600;
    private static final String TAG = "drive-quickstart";
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private boolean connected = false;
    Camera2RawFragment camera2RawFragment;
    private GoogleApiClient mGoogleApiClient;
    private Bitmap mBitmapToSave;
    public static String dateStampFile = generateTimestamp();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        File parentDirectory2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "LightData" + File.separator);
        if (!parentDirectory2.exists()) {
            parentDirectory2.mkdir();
        }

        File parentDirectory1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "LightData" + File.separator + dateStampFile + File.separator);
        if (!parentDirectory1.exists()) {
            parentDirectory1.mkdir();
        }
        saveCSV = new SaveCSV("CaptureVLCData", getApplicationContext(), parentDirectory1);
        saveCSV.setTitleBar(sensTitles);
        camera2RawFragment = Camera2RawFragment.newInstance().newInstance();
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, camera2RawFragment)
                    .commit();
        }
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        senLightSensor = senSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        senBarometer = senSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senMagnetometer = senSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        senOrient = senSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senBarometer,SensorManager.SENSOR_DELAY_NORMAL);
  //      senSensorManager.registerListener(this, senGyroscope,SensorManager.SENSOR_DELAY_NORMAL);
  //      senSensorManager.registerListener(this, senMagnetometer,SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senOrient,SensorManager.SENSOR_DELAY_NORMAL);


    }

    public void resetCSV() {
        dateStampFile = generateTimestamp();
        File parentDirectory2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "LightData" + File.separator);
        if (!parentDirectory2.exists()) {
            parentDirectory2.mkdir();
        }

        File parentDirectory1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "LightData" + File.separator + dateStampFile + File.separator);
        if (!parentDirectory1.exists()) {
            parentDirectory1.mkdir();
        }
        saveCSV = new SaveCSV("CaptureVLCData", getApplicationContext(), parentDirectory1);
        saveCSV.setTitleBar(sensTitles);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_GRAVITY) {
            accx = Math.abs(sensorEvent.values[0] - last_x);
            accy = Math.abs(sensorEvent.values[1] - last_y);
            accz = Math.abs(sensorEvent.values[2] - last_z);
            accMag = (float) Math.sqrt((Math.pow(accx, 2) + Math.pow(accy, 2) + Math.pow(accz, 2)));
            rollingAverage += accMag;
            rollingAverage = rollingAverage / 2;
            last_x = sensorEvent.values[0];
            last_y = sensorEvent.values[1];
            last_z = sensorEvent.values[2];
        }
        if (mySensor.getType() == Sensor.TYPE_LIGHT) {
            light = sensorEvent.values[0];
        }
        if(mySensor.getType() == Sensor.TYPE_PRESSURE){
            barometer = sensorEvent.values[0];
        }
        if(mySensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            magx = sensorEvent.values[0];
            magy = sensorEvent.values[1];
            magz = sensorEvent.values[2];

        }
        if(mySensor.getType() == Sensor.TYPE_ORIENTATION){
            orix = sensorEvent.values[0];
            oriy = sensorEvent.values[1];
            oriz = sensorEvent.values[2];

        }
        if(mySensor.getType() == Sensor.TYPE_GYROSCOPE){
            gyrox = sensorEvent.values[0];
            gyroy = sensorEvent.values[1];
            gyroz = sensorEvent.values[2];
        }
        sensValues = new String[]{"" + generateTimestamp(), "" + accx, "" + accy, "" + accz, "" + accMag, "" + rollingAverage, "" + light, "" +magx ,""+magy,""+magz,""+gyrox,""+gyroy,""+gyroy,""+barometer,""+orix,""+oriy,""+oriz};
        camera2RawFragment.setSensorText(sensTitles, sensValues);
        saveCSV.saveData(sensValues);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CAPTURE_IMAGE:
                // Called after a photo has been taken.
                if (resultCode == Activity.RESULT_OK) {
                    // Store the image data as a bitmap for writing later.
                    Bitmap bm = ((Bitmap) data.getExtras().get("data"));
                }
                break;
            case REQUEST_CODE_CREATOR:
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Image successfully saved.");
                    mBitmapToSave = null;
                    // Just start the camera again for another photo.
                    startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                            REQUEST_CODE_CAPTURE_IMAGE);
                }
                break;
        }
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }

    public String getDateStampFile() {
        return dateStampFile;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "API client connected.");
        connected = true;
        if (mBitmapToSave == null) {
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    REQUEST_CODE_CAPTURE_IMAGE);
            return;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    public String[] getSensorStrings() {
        return sensValues;
    }
}