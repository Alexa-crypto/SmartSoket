package com.example.myapplication;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

class POST_Reguest extends GET_Reguest {
    private String request_body = "";

    public void run() {
        super.end_of_request = false;
        super.request_success = false;
        URL url;
        HttpURLConnection urlConnection;
        InputStream isr = null;
        OutputStream os = null;
        try {
            url = new URL(link);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(300);
            urlConnection.setReadTimeout(300);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "text/plain");
            urlConnection.setRequestProperty("Content-Length", String.valueOf(request_body.length()));
            os = urlConnection.getOutputStream();
            os.write(request_body.getBytes());

            isr = urlConnection.getInputStream();
            int i;
            while ((i = isr.read()) != -1)
                data += (char) i;
            super.request_success = true;

        } catch (MalformedURLException e) {
            Log.d("MyLog", "Помилка_запиту 1");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("MyLog", "Помилка_запиту 2");
        } finally {
            try {
                if (isr != null) isr.close();
                if (os != null) os.close();
            } catch (IOException e) { }
        }
        super.end_of_request = true;
        Log.d("MyLog", "close_thread");
    }
    public void set_request_body(String data) throws UnsupportedEncodingException {
        request_body = data;
//        request_body = URLEncoder.encode(data, "UTF-8");
    }
}
