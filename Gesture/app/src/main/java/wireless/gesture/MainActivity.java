package wireless.gesture;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    // For output show acceleration of x,y,z axises.
    protected TextView xAcceleration;
    protected TextView yAcceleration;
    protected TextView zAcceleration;

    // For first version, I just use number as output, finally  we will use pictures and add actions
    // of those recognized gestures.
    protected TextView textResult;

    protected SensorManager sensorManager;
    protected Sensor accelerateSensor;
    protected DTWLib dtwLib;

    final int DIMENSION = DTWLib.DIMENSION;
    protected double gravity[] = new double[DIMENSION];
    protected double linear_acceleration[] = new double[DIMENSION];
    protected boolean recording = false;
    protected boolean detecting = false;
    Button recordButton;
    Button recognizeButton;
    Button endButton;

    final double alpha = 0.8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        dtwLib = new DTWLib();


        xAcceleration = (TextView) findViewById(R.id.xAcceleration);
        yAcceleration = (TextView) findViewById(R.id.yAcceleration);
        zAcceleration = (TextView) findViewById(R.id.zAcceleration);
        textResult = (TextView) findViewById(R.id.textResult);

        recordButton = (Button) findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                textResult.setText("Recording");
                if(!detecting && !recording) {
                    recording = true;
                    dtwLib.recordMode();
                    dtwLib.beginGesture();
                }
            }
        });

        recognizeButton = (Button) findViewById(R.id.recognizeButton);
        recognizeButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                if(!recording && !detecting) {
                    detecting = true;
                    textResult.setText("Detecting");
                    dtwLib.detectMode();
                    dtwLib.beginGesture();
                }
            }
        });

        endButton = (Button) findViewById(R.id.endButton);
        endButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                if(recording) {
                    recording = false;
                    dtwLib.endGesture();
                    textResult.setText("record over");
                }
                if(detecting) {
                    detecting = false;
                    int intResult = dtwLib.endGesture();
                    textResult.setText("result = " + Integer.toString(intResult));
                }
            }
        });

    }


    public void onSensorChanged(SensorEvent event) {
        /*
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];
        */
        if(recording || detecting) {
            linear_acceleration[0] = event.values[0] / DTWLib.EARTH_GRAVITY;
            linear_acceleration[1] = event.values[1] / DTWLib.EARTH_GRAVITY;
            linear_acceleration[2] = (event.values[2] - 1) / DTWLib.EARTH_GRAVITY;
            xAcceleration.setText("x = " + Double.toString(linear_acceleration[0]));
            yAcceleration.setText("y = " + Double.toString(linear_acceleration[1]));
            zAcceleration.setText("z = " + Double.toString(linear_acceleration[2]));
            dtwLib.addAccerelation(linear_acceleration);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
