package com.example.myapplication;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

class GET_Reguest extends Thread {
    protected boolean end_of_request = false;
    protected boolean request_success = false;

    protected String link;
    protected String data = "";

    public void run() {
        request_success = false;
        end_of_request = false;
        data = "";
        URLConnection urlConnection;
        URL url;
        InputStream isr = null;
        try {
            url = new URL(link);
            urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(300);
            urlConnection.setReadTimeout(300);


            isr = urlConnection.getInputStream();


            int i;
            //Log.d("MyLog", urlConnection.getHeaderField("Content-Length"));
            //Log.d("MyLog", isr.available() + "");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while ((i = isr.read()) != -1)
                data += (char) i;
            if (urlConnection.getHeaderField("X-Android-Response-Source").equals("NETWORK 200")) {
                request_success = true;
                //Log.d("MyLog", urlConnection.getHeaderField("X-Android-Response-Source"));
            }
        } catch (MalformedURLException e) {
            Log.d("MyLog", "eror1");
            //e.printStackTrace();
        } catch (IOException e) {
            Log.d("MyLog", "eror2");
            //Log.d("MyLog", e.toString());
            //e.printStackTrace();
        } finally {
            try {
                if (isr != null) isr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Log.d("MyLog", "close_thread");
        end_of_request = true;
    }

    public String get_data() {
        return data;
    }
    public void set_Link(String link){
        this.link = link;
    }
    public boolean get_end_of_request(){
        return end_of_request;
    }
    public boolean get_request_success(){
        return request_success;
    }
    public void get_Headers(URLConnection urlConnection) {
        for (int i = 0; ; i++) {
            String headerName = urlConnection.getHeaderFieldKey(i);
            String headerValue = urlConnection.getHeaderField(i);
            if (headerName == null || headerValue == null)
                break;
            Log.d("MyLog", headerName + "===");
            Log.d("MyLog", headerValue);
        }
    }
}
