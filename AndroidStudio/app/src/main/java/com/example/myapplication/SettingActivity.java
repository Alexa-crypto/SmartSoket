package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

public class SettingActivity  extends AppCompatActivity {
    String link_to_device;
    EditText set_ip_adress, set_alarmm;
    TextView show_time, alarm_value;
    Button but_set_alarm, but_update_time;
    Intent data = new Intent();
    private boolean activity_request = true;
    SharedPreferences preferences;

    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        init();
    }
    private void init(){
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        preferences = getSharedPreferences("systemData", MODE_PRIVATE);
        link_to_device = preferences.getString("ipAdress", "http://192.168.31.77/");
        set_alarmm = findViewById(R.id.ET_set_alarm);
        set_ip_adress = findViewById(R.id.ET_set_ip);
        set_ip_adress.setHint(link_to_device);
        but_set_alarm = findViewById(R.id.but_set_alarm);
        but_update_time = findViewById(R.id.but_update_time);
        show_time = findViewById(R.id.tv_time);
        alarm_value = findViewById(R.id.tv_alarm_value);
        request_fun();
        alarm_value.setText(MainActivity.get_alarm_value(link_to_device));
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {
        activity_request = false;
        data.putExtra("ip", link_to_device);
        setResult(RESULT_OK, data);
        finish();
    }
    public void click_set_alarm(View view) {
        but_set_alarm.setEnabled(false);
        POST_Reguest req = new POST_Reguest();
        req.set_Link(link_to_device + "set_alarm");
        try {
            req.set_request_body(set_alarmm.getText().toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        req.start();
        while (!req.get_end_of_request());

        if (req.get_request_success()) {
            alarm_value.setText(MainActivity.get_alarm_value(link_to_device));
            set_alarmm.setText("");
            Toast toast = Toast.makeText(getApplicationContext(),
                    "set alarm success!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "set alarm failed!", Toast.LENGTH_SHORT);
            toast.show();
        }
        but_set_alarm.setEnabled(true);
    }
    public void click_set_ip(View view) {
        activity_request = false;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("ipAdress", "http://" + set_ip_adress.getText().toString() + "/");
        editor.apply();

        link_to_device = preferences.getString("ipAdress", "http://192.168.31.77/");
        request_fun();
        set_ip_adress.setText("");
        set_ip_adress.setHint(link_to_device);
    }
    private void request_fun() {
        activity_request = true;
        Thread secThread;
        Runnable runaible = new Runnable() {
            String data;
            final String urlStr = link_to_device + "get_time";
            URLConnection urlConnection;
            URL url;
            InputStream isr;

            @Override
            public void run() {
                while (activity_request) {
                    try {
                        data = "";
                        url = new URL(urlStr);
                        urlConnection = url.openConnection();
                        urlConnection.setConnectTimeout(300);
                        urlConnection.setReadTimeout(300);

                        isr = urlConnection.getInputStream();
                        int i;
                        while ((i = isr.read()) != -1)
                            data += (char) i;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { show_time.setText(data); }
                        });
                        Log.d("MyLog", "Успіх -> " + data);
                    } catch (IOException ignored) {
                    } finally {
                        try {
                            if (isr != null) isr.close();
                        } catch (IOException ignored) {
                        }
                    }
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        };
        secThread = new Thread(runaible);
        secThread.start();
    }
    public void click_update_time(View view) {
        but_update_time.setEnabled(false);
        POST_Reguest req = new POST_Reguest();
        req.set_Link(link_to_device + "set_time");
        try {
            req.set_request_body(String.valueOf(System.currentTimeMillis() / 1000 + 10800));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        req.start();
        while (!req.get_end_of_request());

        if (req.get_request_success()) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "set data_time success!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "set data_time failed!", Toast.LENGTH_SHORT);
            toast.show();
        }
        but_update_time.setEnabled(true);
    }
}
