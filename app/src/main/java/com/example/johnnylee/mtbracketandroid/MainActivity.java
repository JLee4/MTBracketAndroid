package com.example.johnnylee.mtbracketandroid;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.amazonaws.amplify.generated.graphql.ListRacersQuery;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    AWSAppSyncClient mAWSAppSyncClient;
    List<ListRacersQuery.Item> racers = null;
    List<ListRacersQuery.Item> categorizedRacers = null;
    ListRacersQuery.Item currentRacer = null;
    String[] categories = new String[] {
            "Men's A",
            "Men's B",
            "Men's C",
            "Women's A",
            "Women's B",
            "Women's C"
    };
    TextView timerTextView;
    long startTime = 0;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            long milliseconds = millis % 1000;

            timerTextView.setText(String.format(Locale.ENGLISH,"%s\nRacer Number: %s\n%02d:%02d.%d", currentRacer.name(), currentRacer.racerNumber(), minutes, seconds, milliseconds));

            timerHandler.postDelayed(this, 1);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();
        final DatabaseAPI databaseAPI = new DatabaseAPI(mAWSAppSyncClient);

        setContentView(R.layout.activity_main);
        Button startStop = (Button) findViewById(R.id.start_stop);
        Button reset = (Button) findViewById(R.id.reset);
        Button send =( Button) findViewById(R.id.send);
        startStop.setEnabled(false);
        reset.setEnabled(false);
        send.setEnabled(false);
        Spinner s = (Spinner) findViewById(R.id.category_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                racers = databaseAPI.getRacers();
                categorizedRacers = new ArrayList<>();
                if (racers != null) {
                    for (ListRacersQuery.Item racer : racers) {
                        if (categories[position].equals(racer.category())) {
                            categorizedRacers.add(racer);
                        }
                    }
                    Collections.sort(categorizedRacers, new Comparator<ListRacersQuery.Item>() {
                        @Override
                        public int compare(ListRacersQuery.Item o1, ListRacersQuery.Item o2) {
                            return Integer.parseInt(o1.racerNumber()) - Integer.parseInt(o2.racerNumber());
                        }
                    });
                    ArrayList<String> categorizedRacerNumbers = new ArrayList<>();
                    categorizedRacerNumbers.add("");
                    for (ListRacersQuery.Item racer : categorizedRacers) {
                        categorizedRacerNumbers.add(racer.racerNumber());
                    }
                    Spinner racerSpinner = (Spinner) findViewById(R.id.racer_spinner);
                    ArrayAdapter<String> racerAdapter = new ArrayAdapter<>(getApplicationContext(),
                            android.R.layout.simple_spinner_item, categorizedRacerNumbers);
                    racerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    racerSpinner.setAdapter(racerAdapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        timerTextView = (TextView) findViewById(R.id.timerTextView);
        timerTextView.setTextColor(Color.BLACK);
        timerTextView.setText(String.format(Locale.ENGLISH, "00:00.000"));
        Spinner racerSpinner = (Spinner) findViewById(R.id.racer_spinner);
        racerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    currentRacer = categorizedRacers.get(position - 1);
                    timerTextView.setText(String.format(Locale.ENGLISH,"%s\nRacer Number: %s\n00:00.000", currentRacer.name(), currentRacer.racerNumber()));
                    Button startStop = (Button) findViewById(R.id.start_stop);
                    Button reset = (Button) findViewById(R.id.reset);
                    Button send = (Button) findViewById(R.id.send);
                    startStop.setEnabled(true);
                    reset.setEnabled(true);
                    send.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        startStop.setText("Start");
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                Button reset = (Button) findViewById(R.id.reset);
                if (b.getText().equals("Stop")) {
                    timerHandler.removeCallbacks(timerRunnable);
                    reset.setEnabled(true);
                    b.setText("Start");
                } else {
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    reset.setEnabled(false);
                    b.setText("Stop");
                }
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerTextView.setText(String.format(Locale.ENGLISH,"%s\nRacer Number: %s\n00:00.000", currentRacer.name(), currentRacer.racerNumber()));
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkAvailable()) {
                    String time = timerTextView.getText().toString();
                    time = time.substring(time.length() - 9, time.length());
                    DatabaseAPI.runMutation(currentRacer.id(), time);
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            // Network is present and connected
            isAvailable = true;
        }
        return isAvailable;
    }
}

