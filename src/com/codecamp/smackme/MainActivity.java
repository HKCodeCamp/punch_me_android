package com.codecamp.smackme;

import java.io.PrintWriter;
import java.net.Socket;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

    public class Punch implements Runnable {

        private String direction;
        private int force;

        public Punch(String direction, int force) {
            // store parameter for later user
            this.direction = direction;
            this.force = force;
        }

        public void run() {
            if (force > 2) {
                try {
                    Socket skt = new Socket("192.168.100.73", 9999);
                    PrintWriter out = new PrintWriter(skt.getOutputStream(),
                            true);
                    out.print("PUNCH" + direction + " " + force);
                    out.close();
                    skt.close();
                } catch (Exception e) {
                    Log.e("punch", "CANNOT CONNECT ", e);
                }
            }
        }
    }

    private float mLastX, mLastY, mLastZ;
    private boolean mInitialized;

    private SensorManager mSensorManager;

    private Sensor mAccelerometer;

    private final float NOISE = (float) 12.0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mInitialized = false;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // can be safely ignored for this demo
    }

    public void onSensorChanged(SensorEvent event) {
        TextView tvX = (TextView) findViewById(R.id.acc_x);
        TextView tvY = (TextView) findViewById(R.id.acc_y);
        TextView tvZ = (TextView) findViewById(R.id.acc_z);

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (!mInitialized) {
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            tvX.setText("0.0");
            tvY.setText("0.0");
            tvZ.setText("0.0");
            mInitialized = true;
        } else {

            float deltaX = mLastX - x;
            float deltaY = mLastY - y;
            float deltaZ = mLastZ - z;

            if (Math.abs(deltaX) < NOISE)
                deltaX = (float) 0.0;
            if (Math.abs(deltaY) < NOISE)
                deltaY = (float) 0.0;
            if (Math.abs(deltaZ) < NOISE)
                deltaZ = (float) 0.0;

            mLastX = x;
            mLastY = y;
            mLastZ = z;

            tvX.setText(Float.toString(deltaX));
            tvY.setText(Float.toString(deltaY));
            tvZ.setText(Float.toString(deltaZ));

            if ((deltaX + deltaY + deltaZ) != 0) {
                if (deltaY == 0 && deltaZ == 0) {
                    int force = (int) ((int) ((Math.abs(deltaX) - 12) / 18 * 10));
                    if (deltaX > 0) {
                        Log.d("PUNCH", "RIGHT " + force);
                        Runnable r = new Punch("RIGHT", force);
                        new Thread(r).start();
                    } else {
                        Log.d("PUNCH", "LEFT " + force);
                        Runnable r = new Punch("LEFT", force);
                        new Thread(r).start();
                    }
                }

                if (deltaX == 0 && deltaZ == 0) {
                    int force = (int) ((int) ((Math.abs(deltaY) - 12) / 18 * 10));
                    if (deltaY > 0) {
                        Log.d("PUNCH", "UP " + force);
                        Runnable r = new Punch("UP", force);
                        new Thread(r).start();
                    } else {
                        Log.d("PUNCH", "DOWN " + force);
                        Runnable r = new Punch("DOWN", force);
                        new Thread(r).start();
                    }

                }

            }

        }
    }

}
