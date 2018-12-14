package edu.wpi.adonay.cs4518_final;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener, OnInferenceCompleted, TextToSpeech.OnInitListener{

    private static final int N_SAMPLES = 200;
    private static List<Float> x;
    private static List<Float> y;
    private static List<Float> z;
    private TextView downstairsTextView;

    private TextView joggingTextView;
    private TextView sittingTextView;
    private TextView standingTextView;
    private TextView upstairsTextView;
    private TextView walkingTextView;

    private ToggleButton inferenceToggleButton;
    private ToggleButton sourceToggleButton;

    private TextView inferenceTaskCounterTextView;
    private TextView inferenceElapsedTimeTextView;
    private int inferenceCounter = 0;

    private boolean isInferenceOn = false;
    private boolean isRemote = false;

    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        x = new ArrayList<>( );
        y = new ArrayList<>();
        z = new ArrayList<>();

        downstairsTextView = findViewById(R.id.downstairs_prob);
        joggingTextView = findViewById(R.id.jogging_prob);
        sittingTextView = findViewById(R.id.sitting_prob);
        standingTextView = findViewById(R.id.standing_prob);
        upstairsTextView = findViewById(R.id.upstairs_prob);
        walkingTextView = findViewById(R.id.walking_prob);

        inferenceTaskCounterTextView = findViewById(R.id.inferenceTaskCounterTextView);
        inferenceElapsedTimeTextView = findViewById(R.id.inferenceElapsedTimeTextView);

        inferenceToggleButton = findViewById(R.id.inferenceToggle);
        sourceToggleButton = findViewById(R.id.inferenceSourceToggle);

        inferenceToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isInferenceOn = b;
            }
        });
        sourceToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isRemote = b;
            }
        });

        this.textToSpeech = new TextToSpeech(this, this);
    }

    protected void onPause() {
        getSensorManager().unregisterListener(this);
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(isInferenceOn){
            if (x.size() == N_SAMPLES && y.size() == N_SAMPLES && z.size() == N_SAMPLES) {
                inferenceCounter++;
                inferenceTaskCounterTextView.setText("Inference Task Count: " + inferenceCounter);
                activityPrediction();
            }
            x.add(event.values[0]);
            y.add(event.values[1]);
            z.add(event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void activityPrediction() {
        List<Float> data = new ArrayList<>();
        data.addAll(x);
        data.addAll(y);
        data.addAll(z);

        TextView[] probTextViewsArray = {downstairsTextView, joggingTextView, sittingTextView, standingTextView, upstairsTextView, walkingTextView};

        long startTime = SystemClock.uptimeMillis();
        RunInferenceAsync inferenceAsync = new RunInferenceAsync(this, probTextViewsArray, startTime, isRemote);
        inferenceAsync.execute(toFloatArray(data));

        x.clear();
        y.clear();
        z.clear();
    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    public void onInferenceCompleted(String activity, float probability, long elapsedTime) {
        inferenceCounter--;
        inferenceTaskCounterTextView.setText("Inference Task Count: " + inferenceCounter);
        inferenceElapsedTimeTextView.setText("Last Inference Elapsed Time: " + elapsedTime + "ms");
        textToSpeech.speak(activity, TextToSpeech.QUEUE_ADD, null, Integer.toString(new Random().nextInt()));
    }

    @Override
    public void onInit(int status) {
        if(status != TextToSpeech.ERROR) {
            textToSpeech.setLanguage(Locale.US);
        }
    }
}
