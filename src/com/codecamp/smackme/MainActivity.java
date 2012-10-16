package com.codecamp.smackme;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

    public class Picture implements Runnable {
        private byte[] data;

        public Picture(byte[] data) {
            this.data = data;
        }

        public void run() {
            try {
                Socket skt = new Socket("192.168.5.107", 9999);
                String message = "IMAGE\n";
                // PrintWriter out = new PrintWriter(skt.getOutputStream(),
                // true);
                // out.print(message);
                // out.print(data);
                // out.close();
                OutputStream outputStream = skt.getOutputStream();
                outputStream.write(message.getBytes());
                outputStream.write(data);
                outputStream.flush();
                outputStream.close();

                skt.close();
            } catch (Exception e) {
                Log.e("punch", "CANNOT CONNECT ", e);
            }
        }

    }

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
                    Socket skt = new Socket("192.168.5.107", 9999);
                    PrintWriter out = new PrintWriter(skt.getOutputStream(),
                            true);
                    out.print("PUNCH " + direction + " " + force);
                    out.close();
                    skt.close();
                } catch (Exception e) {
                    Log.e("punch", "CANNOT CONNECT ", e);
                }
            }
        }
    }

    protected static final int REQ_TAKE_PICTURE = 0;

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

        final Button button = (Button) findViewById(R.id.camera);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Intent takeIntent = new Intent(Intent.ACTION_GET_CONTENT);
                takeIntent.setType("image/*");
                startActivityForResult(
                        Intent.createChooser(takeIntent, "Select Picture"),
                        REQ_TAKE_PICTURE);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case REQ_TAKE_PICTURE:
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                cursor.close();
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                Log.d("Image", "taken");
                Runnable r = new Picture(stream.toByteArray());
                new Thread(r).start();

            }
        }
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
