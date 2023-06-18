package com.example.myapplication;

import static java.lang.Integer.parseInt;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView voltage, current, power, power_factor, frequency, energy, alarm_bol, h_alarm;
    private CardView card_succes, card_error;
    private Resources res_collor;

    SharedPreferences preferences;

    ActivityResultLauncher<Intent> setting_activity_launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    //TextView textView = findViewById(R.id.textView);
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        link_to_device = preferences.getString("ipAdress", "http://192.168.31.77/");
                        request_fun();
                    }
                    //else {
//                        textView.setText("Ошибка доступа");
//                    }
                }
            });

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch on_off;

    String link_to_device;
    private boolean activity_request;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        voltage = findViewById(R.id.Data_voltage);
        current = findViewById(R.id.Data_current);
        power = findViewById(R.id.Data_power);
        power_factor = findViewById(R.id.Data_pow_fack);
        frequency = findViewById(R.id.Data_frequency);
        energy = findViewById(R.id.Data_energy);
        alarm_bol = findViewById(R.id.Data_alarm);
        h_alarm = findViewById(R.id.header_alarm);


        card_error = findViewById(R.id.card_con_error);
        card_succes = findViewById(R.id.card_con_succes);


        on_off = findViewById(R.id.switch_socet);

        res_collor = getResources();

        preferences = getSharedPreferences("systemData", MODE_PRIVATE);
        link_to_device = preferences.getString("ipAdress", "http://192.168.31.77/");

//        chart_energy_month = findViewById(R.id.id_chart);
        request_fun();
        updateChart();
    }

    private void request_fun() {
        card_error.setVisibility(View.INVISIBLE);
        card_succes.setVisibility(View.VISIBLE);
        h_alarm.setText("Alarm active (" + get_alarm_value(link_to_device) + ")");
        activity_request = true;
        Thread secThread;
        Runnable runaible = new Runnable() {
            byte counter_error = 0;
            String data;
            String urlStr = link_to_device + "up_data";
            URLConnection urlConnection;
            URL url;
            InputStream isr;

            @Override
            public void run() {
                while (counter_error <= 5 && activity_request) {
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
                            public void run() {
                                String[] mas_data = data.split("/");
                                if (mas_data.length == 8) {
                                    voltage.setText(mas_data[0] + "V");
                                    current.setText(mas_data[1] + "A");
                                    power.setText(mas_data[2] + "W");
                                    power_factor.setText(mas_data[5] + "%");
                                    frequency.setText(mas_data[4] + "Hz");
                                    energy.setText(mas_data[3] + "W/h");
                                    on_off.setChecked(mas_data[6].equals("0") ? false : true);
                                    if (mas_data[7].equals("0")) {
                                        alarm_bol.setText("Not active");
                                        alarm_bol.setTextColor(res_collor.getColor(R.color.green, null));
                                    } else {
                                        alarm_bol.setText("Active");
                                        alarm_bol.setTextColor(res_collor.getColor(R.color.red, null));
                                    }
                                    //alarm_bol.setText(mas_data[7].equals("0") ? "Not active" : "Active");
                                }
                            }
                        });
                        if (counter_error > 0) counter_error = 0;
                        Log.d("MyLog", "Успіх -> " + data);
                    } catch (MalformedURLException e) {
                        counter_error++;
                    } catch (IOException e) {
                        counter_error++;
                    } finally {
                        try {
                            if (isr != null) isr.close();
                        } catch (IOException e) {
                        }
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity_request = false;
                        card_error.setVisibility(View.VISIBLE);
                        card_succes.setVisibility(View.INVISIBLE);
                    }
                });
            }
        };
        secThread = new Thread(runaible);
        secThread.start();
    }
    public void click_switch_socet(View view) {
        on_off.setEnabled(false);
        GET_Reguest req = new GET_Reguest();
        req.set_Link(link_to_device + "relay_switch");
        req.start();
        while (!req.get_end_of_request());
        if (req.get_request_success()) {
            on_off.setChecked(req.get_data().equals("1"));
            Log.d("MyLog", "Links -> get_request_success");
            Toast toast = Toast.makeText(getApplicationContext(),
                    "get_request_success!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            Log.d("MyLog", "Links -> get_request_failed");
            Toast toast = Toast.makeText(getApplicationContext(),
                    "get_request_failed!", Toast.LENGTH_SHORT);
            toast.show();
        }
        on_off.setEnabled(true);
    }
    public void click_conect_update(View view) {
        if (!activity_request)
            request_fun();
    }
    public void click_setting(View view) {
        activity_request = false;
        Intent intent = new Intent(this, SettingActivity.class);
        setting_activity_launcher.launch(intent);
    }
    public static String get_alarm_value(String link_to_device){
        //on_off.setEnabled(false);
        GET_Reguest req = new GET_Reguest();
        req.set_Link(link_to_device + "value_alarm");
        req.start();
        while (!req.get_end_of_request());
        if (req.get_request_success())
            return req.get_data() + " W";

        //on_off.setEnabled(true);
        return "error";
    }
    public void click_timer(View view) {
        AlertDialog.Builder timer_dialog = new AlertDialog.Builder(this);
        ConstraintLayout cl = (ConstraintLayout) getLayoutInflater().inflate(R.layout.dialog_timer, null);

        NumberPicker nb_on_h = cl.findViewById(R.id.nP_Hour_On);
        nb_on_h.setMinValue(0);
        nb_on_h.setMaxValue(23);
        NumberPicker nb_on_m = cl.findViewById(R.id.nP_Minut_On);
        nb_on_m.setMinValue(0);
        nb_on_m.setMaxValue(59);
        NumberPicker nb_off_h = cl.findViewById(R.id.nP_Hour_Off);
        nb_off_h.setMinValue(0);
        nb_off_h.setMaxValue(23);
        NumberPicker nb_off_m = cl.findViewById(R.id.nP_Minut_Off);
        nb_off_m.setMinValue(0);
        nb_off_m.setMaxValue(59);
        SwitchMaterial sw_timer = cl.findViewById(R.id.sw_Timer);

        GET_Reguest req = new GET_Reguest();
        req.set_Link(link_to_device + "value_timer");
        req.start();
        while (!req.get_end_of_request());
        if (req.get_request_success()) {
            String[] data_string = req.get_data().split("/");
            sw_timer.setChecked(data_string[0].equals("1"));
            nb_off_h.setValue(parseInt(data_string[1]));
            nb_off_m.setValue(parseInt(data_string[2]));
            nb_on_h.setValue(parseInt(data_string[3]));
            nb_on_m.setValue(parseInt(data_string[4]));
            Log.d("MyLog", "Links -> get_request_success");
            Toast toast = Toast.makeText(getApplicationContext(),
                    "get_request_success!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            Log.d("MyLog", "Links -> get_request_failed");
            Toast toast = Toast.makeText(getApplicationContext(),
                    "get_request_failed!", Toast.LENGTH_SHORT);
            toast.show();
        }

        sw_timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sw_timer.setEnabled(false);
                POST_Reguest req = new POST_Reguest();
                req.set_Link(link_to_device + "timer_set");
                try {
                    String str = (sw_timer.isChecked() ? "1/" : "0/") + String.valueOf(nb_off_h.getValue()) + "/" + String.valueOf(nb_off_m.getValue()) + "/"
                                +String.valueOf(nb_on_h.getValue()) + "/" + String.valueOf(nb_on_m.getValue());
                    req.set_request_body(str);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                req.start();
                while (!req.get_end_of_request()) ;

                if (req.get_request_success()) {
                    sw_timer.setChecked(req.get_data().equals("1"));
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "set alarm success!", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "set alarm failed!", Toast.LENGTH_SHORT);
                    toast.show();
                }
                sw_timer.setEnabled(true);
            }
        });

        timer_dialog.setView(cl);
        timer_dialog.show();


    }
    //доробити
    public void click_log(View view) {
        class LogElement {
            private String header_log;
            private String time;
            private int title;
            public LogElement(String header_log, String time, int title){
                this.header_log = header_log;
                this.time = time;
                this.title = title;
            }
            public String getHeader() {
                return this.header_log;
            }
            public void setHeader(String header_log) {
                this.header_log = header_log;
            }
            public String getTime() {
                return this.time;
            }
            public void setCTime(String time) {
                this.time = time;
            }
            public int getTitle() {
                return this.title;
            }
            public void setTitle(int title) {
                this.title = title;
            }
        }

        class LogAdapter  extends RecyclerView.Adapter<LogAdapter.ViewHolder>{

            private final LayoutInflater inflater;
            private final List<LogElement> logList;

            LogAdapter(Context context, List<LogElement> logList) {
                this.logList = logList;
                this.inflater = LayoutInflater.from(context);
            }
            @Override
            public LogAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = inflater.inflate(R.layout.log_item, parent, false);
                return new ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(LogAdapter.ViewHolder holder, int position) {
                LogElement element = logList.get(position);
                holder.title.setImageResource(element.getTitle());
                holder.header_log.setText(element.getHeader());
                holder.time.setText(element.getTime());
            }

            @Override
            public int getItemCount() {
                return logList.size();
            }

            class ViewHolder extends RecyclerView.ViewHolder {
                final ImageView title;
                final TextView header_log, time;
                ViewHolder(View view){
                    super(view);
                    title = view.findViewById(R.id.IV_title);
                    header_log = view.findViewById(R.id.TV_header);
                    time = view.findViewById(R.id.TV_time);
                }
            }
        }

        AlertDialog.Builder timer_dialog = new AlertDialog.Builder(this);
        LinearLayout cl = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_log, null);
        ArrayList<LogElement> statesf = new ArrayList<LogElement>();
        GET_Reguest req = new GET_Reguest();
        req.set_Link(link_to_device + "log_val");
        req.start();
        while (!req.get_end_of_request());
        if (req.get_request_success()) {
            req.data = req.data.replaceAll("\\s", "");
            String[] data_string = req.data.split("/");
            for (int i = 0; i < data_string.length; i++) {
                String value = "";
                String dataTime = "";

                int j = 0;
                while (data_string[i].charAt(j) != '&')
                    value += data_string[i].charAt(j++);
                j++;
                while (data_string[i].charAt(j) != '&')
                    dataTime += data_string[i].charAt(j++);
                j++;
                dataTime += " ___ ";
                while (data_string[i].length() != j)
                    dataTime += data_string[i].charAt(j++);
                statesf.add(new LogElement (value, dataTime + "___", value.equals("off") ? R.drawable.error : R.drawable.normal));
            }
        }

        RecyclerView recyclerView = cl.findViewById(R.id.RV_list);
        // создаем адаптер
        LogAdapter adapter = new LogAdapter(this, statesf);
        // устанавливаем для списка адаптер
        recyclerView.setAdapter(adapter);

        timer_dialog.setView(cl);
        timer_dialog.show();
    }
//    доробити
    public void updateChart() {
//        BarChart chart_energy_month;
//        chart_energy_month = findViewById(R.id.BCh_chartEnergy);
        LineChart lineChart;
        lineChart = findViewById(R.id.lineChart);
        lineChart.setDrawGridBackground(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(false);
        lineChart.setExtraLeftOffset(20);
        lineChart.setExtraRightOffset(20);

//        YAxis yAxis = lineChart.getY();

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);

        YAxis leftYAxis = lineChart.getAxisLeft();
        leftYAxis.setGranularity(1f);
//        leftYAxis.setValueFormatter(new DefaultValueFormatter(0));

//        leftYAxis.setValueFormatter(new YAxisValueFormatter());


//        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);


        LocalDate currentDate = LocalDate.now();
        //on_off.setEnabled(false);
        POST_Reguest req = new POST_Reguest();
        req.set_Link(link_to_device + "energy_chart");
        try {
            req.set_request_body("/" + String.valueOf(currentDate.getMonthValue() - 1) + ".txt");
//            req.set_request_body("/0.txt");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        req.start();
        while (!req.get_end_of_request());
        Log.d("MyLog", "-> " + req.data);
        Toast toast = Toast.makeText(getApplicationContext(),
                "-> " + req.data, Toast.LENGTH_SHORT);
        toast.show();
        if (req.get_request_success() && !req.data.equals("Not File")) {
            req.data = req.data.replaceAll("\\s", "");
            String[] data_string = req.data.split("/");
//            ArrayList<BarEntry> chartData = new ArrayList<>();
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < data_string.length; i++) {
                String value_alarm = "";
                String valye_day = "";
                int j = 0;
                while (data_string[i].charAt(j) != '&')
                    value_alarm += data_string[i].charAt(j++);
                j++;
                while (data_string[i].charAt(j) != '.')
                    valye_day += data_string[i].charAt(j++);
                entries.add(new Entry(parseInt(valye_day), parseInt(value_alarm)));
//                chartData.add(new BarEntry(parseInt(valye_day), parseInt(value_alarm)));
            }

//            BarDataSet dataset = new BarDataSet(chartData, "# energy to day on moth");
//            BarData data = new BarData(dataset);
//            chart_energy_month.setData(data);
//            chart_energy_month.invalidate();
            LineDataSet dataSet = new LineDataSet(entries, "Values");
            dataSet.setColor(Color.BLUE);
            dataSet.setDrawValues(false);
            dataSet.setLineWidth(2f);
            dataSet.setDrawCircles(true);
            dataSet.setCircleRadius(5f);
            dataSet.setCircleColor(Color.BLUE);

            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);
            lineChart.invalidate();

            Log.d("MyLog", "Links -> get_request_success");
             toast = Toast.makeText(getApplicationContext(),
                    "get_request_success!", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            Log.d("MyLog", "Links -> get_request_failed");
             toast = Toast.makeText(getApplicationContext(),
                    "get_request_failed!", Toast.LENGTH_SHORT);
            toast.show();
        }
        //on_off.setEnabled(true);
    }

    public void click_UpChart(View view) {
        updateChart();
    }
}

